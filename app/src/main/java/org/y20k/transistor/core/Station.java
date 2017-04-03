/**
 * Station.java
 * Implements the Station class
 * A Station handles station-related data, e.g. the name and the streaming URL
 * <p>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p>
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.core;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;
import org.y20k.transistor.sqlcore.StationsDbContract;
import org.y20k.transistor.sqlcore.StationsDbHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Station class
 */
public final class Station implements Comparable<Station>, Parcelable {

    /* Define log tag */
    private static final String LOG_TAG = Station.class.getSimpleName();

    /*fields */
    public int _ID;
    public String UNIQUE_ID;
    public String TITLE;
    public String SUBTITLE;
    public String IMAGE_PATH;
    //local image name
    public String IMAGE_FILE_NAME;
    public String URI;
    public String CONTENT_TYPE;
    public String DESCRIPTION;
    public String RATING;
    public String CATEGORY;
    public String IS_FAVOURITE;
    public String THUMP_UP_STATUS;
    private File mStationImageFile;
    private Bitmap mStationImage;
    private ArrayList<Station> mInsertedStations = new ArrayList<Station>();

    /* Supported xml import file content types */
    private static final String[] CONTENT_TYPES_IMPORT_XML = {"application/xml"};


    /* Supported audio file content types */
    private static final String[] CONTENT_TYPES_MPEG = {"audio/mpeg"};
    private static final String[] CONTENT_TYPES_OGG = {"audio/ogg", "application/ogg"};
    private static final String[] CONTENT_TYPES_AAC = {"audio/aac", "audio/aacp"};

    /* Supported playlist content types */
    private static final String[] CONTENT_TYPES_PLS = {"audio/x-scpls"};
    private static final String[] CONTENT_TYPES_M3U = {"audio/x-mpegurl", "application/vnd.apple.mpegurl", "audio/mpegurl"};

    /* Regular expression to extract content-type and charset from header string */
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("([^;]*)(; ?charset=([^;]+))?");


    /* Main class variables */
    private String mPlaylistFileContent;
    private boolean mPlayback;
    private Bundle mStationFetchResults;


//    /* Constructor when given file from the Collection folder */
//    public Station(File file) {
//        // read and parse playlist file
//        mStationPlaylistFile = file;
//        if (mStationPlaylistFile.exists()) {
//            parse(readPlaylistFile(mStationPlaylistFile));
//        }
//
//        // set image file object
//        File folder = mStationPlaylistFile.getParentFile();
//        if (folder != null) {
//            setStationImageFile(folder);
//        }
//
//        // set playback state
//        mPlayback = false;
//    }


    /* Constructor when given folder and remote location */
    public Station(File folder, URL fileLocation, Activity mActivity) throws IOException, XmlPullParserException {
        MainConstructor(folder, fileLocation, mActivity);
    }

    private void MainConstructor(File folder, URL fileLocation, Activity mActivity) throws XmlPullParserException, IOException {
        // create results bundle
        mStationFetchResults = new Bundle();

        // determine content type of remote file
        ContentType contentType = getContentType(fileLocation);
        ContentType resultContentType = contentType;

        LogHelper.v(LOG_TAG, "Content type of given file is " + contentType);

        //is XML import file
        if (isXMLFile(contentType)) {
            //READ THE XML FILE ITEMS
            readXmlEntries(fileLocation, mActivity);
            // set playback state
            mPlayback = false;
            return;
        }
        // content type is raw audio file
        else if (isAudioFile(contentType)) {
            // use raw audio file for station data
            URI = fileLocation.toString().trim();
            TITLE = detactStationName(fileLocation);
            // save results
            mStationFetchResults.putParcelable(TransistorKeys.RESULT_STREAM_TYPE, contentType);
            mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, false);
        }

        // content type is playlist
        else if (isPlaylist(contentType)) {
            // download and parse station data from playlist file
            mPlaylistFileContent = downloadPlaylistFile(fileLocation);

            // parse result of downloadPlaylistFile
            if (parse(mPlaylistFileContent) && URI != null) {
                TITLE = detactStationName(fileLocation);
                // save results
                mStationFetchResults.putParcelable(TransistorKeys.RESULT_PLAYLIST_TYPE, contentType);
                resultContentType = getContentType(Uri.parse(URI));
                mStationFetchResults.putParcelable(TransistorKeys.RESULT_STREAM_TYPE, resultContentType);
                mStationFetchResults.putString(TransistorKeys.RESULT_FILE_CONTENT, mPlaylistFileContent);
                mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, false);

            } else {
                // save error flag and file content in results
                mStationFetchResults.putParcelable(TransistorKeys.RESULT_PLAYLIST_TYPE, contentType);
                mStationFetchResults.putString(TransistorKeys.RESULT_FILE_CONTENT, "\n[File probably does not contain a valid streaming URL.]");
                mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, true);
            }

            // content type is none of the above
        } else if (contentType != null) {
            // save results and return
            mStationFetchResults.putParcelable(TransistorKeys.RESULT_STREAM_TYPE, contentType);
            mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, true);
            return;

            // no content type
        } else {
            // save error flag in results and return
            mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, true);
            return;
        }

        // set Transistor's playlist file object
        //setStationPlaylistFile(folder);


        // set playback state
        mPlayback = false;

        // set Transistor's image file object
        final String uNIQUE_ID = fileLocation.toString().replaceAll("[:/]", "_");
        IMAGE_FILE_NAME = uNIQUE_ID + ".png";

        //v2
        //add station to DB
        // strip out problematic characters
        UNIQUE_ID = uNIQUE_ID;
        CONTENT_TYPE = resultContentType.getTypeString();
        IMAGE_PATH = getFavIconUrlString(URI); //default to fav icon
        AddStationItemToDb(this, mActivity); //save to DB
    }

    /* Constructor when given folder and file on sd card */
    public Station(File folder, Uri fileLocation, Activity mActivity) {

        // create results bundle
        mStationFetchResults = new Bundle();

        // read local file and put result into mPlaylistFileContent
        File localFile = new File(fileLocation.getPath());
        if (localFile.exists()) {
            mPlaylistFileContent = readPlaylistFile(localFile);
        } else {
            LogHelper.v(LOG_TAG, "File does not exist " + localFile);
        }

        // parse the raw content of playlist file (mPlaylistFileContent)
        if (parse(mPlaylistFileContent) && URI != null) {
            URL streamURL = null;
            try {
                MainConstructor(folder, streamURL, mActivity);
                streamURL = new URL(URI);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private String readPlaylistFile(File playlistFile) {

        try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
            String line;
            int counter = 0;
            StringBuilder sb = new StringBuilder("");

            // read until last line reached or until line five
            while ((line = br.readLine()) != null && counter < 5) {
                sb.append(line);
                sb.append("\n");
                counter++;
            }

            // set mPlaylistFileContent and return String
            mPlaylistFileContent = sb.toString();
            return sb.toString();

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to read playlist file: " + playlistFile.toString());
            // set mPlaylistFileContent and return null
            mPlaylistFileContent = "[IO error. Unable to read playlist file: " + playlistFile.toString() + "]";
            return null;
        }

    }

    public Station() {
        //nothing for now
    }

//    public void resetStationImage(Activity mActivity) {
//        mStationImage = null;
//        getStationImage(mActivity);
//    }

//    /* Getter for station image */
//    public Bitmap getAndCacheStationImage(Context mActivityOrContext) {
//        //if already there return it
//
//        if (mStationImage != null) { //memory cache
//            return mStationImage;
//        }
//        //try to find on desk by channel UNIQUE_ID
//        // get collection folder
//        Bitmap channelImage = null;
//        try {
//            StorageHelper storageHelper = new StorageHelper(mActivityOrContext);
//            final File mFolder = storageHelper.getCollectionDirectory();
//            final String fileImageName = IMAGE_FILE_NAME;
//            //try find the file on desk
//            File imageActualFile = new File(mFolder.getPath() + "//" + fileImageName);
//            if (!imageActualFile.exists()) {
//                //file not found
//                //try download the file
//                channelImage = downloadImageFile(IMAGE_PATH);
//                //Save Image to desk
//                if (channelImage != null) {
//                    writeImageFile(mFolder, channelImage);
//                }
//            } else {
//                //file already exists, then get it from file system
//                channelImage = BitmapFactory.decodeStream(new FileInputStream(imageActualFile));
//            }
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        mStationImage = channelImage;
//        return channelImage;
//    }

    //v2
    private void readXmlEntries(URL fileLocation, Activity mActivity) throws XmlPullParserException, IOException {
        InputStream stream = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) fileLocation.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            stream = conn.getInputStream();

            //parse
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            List entries = new ArrayList();

            parser.require(XmlPullParser.START_TAG, null, "channels");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                // Starts by looking for the entry tag
                if (name.equals("entry")) {
                    parser.require(XmlPullParser.START_TAG, null, "entry");

                    Station stationItem = new Station();
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        String tagName = parser.getName();
                        if (tagName.equals("unique_id")) {
                            parser.require(XmlPullParser.START_TAG, null, "unique_id");
                            stationItem.UNIQUE_ID = readXmlElementText(parser);
                            stationItem.IMAGE_FILE_NAME = stationItem.UNIQUE_ID + ".png";
                        } else if (tagName.equals("title")) {
                            parser.require(XmlPullParser.START_TAG, null, "title");
                            stationItem.TITLE = readXmlElementText(parser);
                        } else if (tagName.equals("subtitle")) {
                            parser.require(XmlPullParser.START_TAG, null, "subtitle");
                            stationItem.SUBTITLE = readXmlElementText(parser);
                        } else if (tagName.equals("image")) {
                            parser.require(XmlPullParser.START_TAG, null, "image");
                            stationItem.IMAGE_PATH = readXmlElementText(parser);
                        } else if (tagName.equals("uri")) {
                            parser.require(XmlPullParser.START_TAG, null, "uri");
                            stationItem.URI = readXmlElementText(parser);
                        } else if (tagName.equals("content_type")) {
                            parser.require(XmlPullParser.START_TAG, null, "content_type");
                            stationItem.CONTENT_TYPE = readXmlElementText(parser);
                        } else if (tagName.equals("rating")) {
                            parser.require(XmlPullParser.START_TAG, null, "rating");
                            stationItem.RATING = readXmlElementText(parser);
                        } else if (tagName.equals("category")) {
                            parser.require(XmlPullParser.START_TAG, null, "category");
                            stationItem.CATEGORY = readXmlElementText(parser);
                        } else if (tagName.equals("description")) {
                            parser.require(XmlPullParser.START_TAG, null, "description");
                            stationItem.DESCRIPTION = readXmlElementText(parser);
                        } else {
                            skipXmlTagParse(parser);
                        }
                    }

                    if (stationItem.UNIQUE_ID != null && !stationItem.UNIQUE_ID.isEmpty()
                            && stationItem.URI != null && !stationItem.URI.isEmpty()) {
                        //add default Image URL
                        if (stationItem.IMAGE_PATH == null || stationItem.IMAGE_PATH == "") {
                            IMAGE_PATH = getFavIconUrlString(stationItem.URI); //default to fav icon
                        }
                        //add station to db
                        AddStationItemToDb(stationItem, mActivity);

                        //for reference and to inform the adaptor we need the list of mInsertedStations
                        mInsertedStations.add(stationItem);
                    }
                } else {
                    skipXmlTagParse(parser);
                }
            }

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    //v2
    private void skipXmlTagParse(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    //v2
    private String readXmlElementText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    //v2
    private boolean isXMLFile(ContentType contentType) {
        if (contentType != null) {
            for (String[] array : new String[][]{CONTENT_TYPES_IMPORT_XML}) {
                if (Arrays.asList(array).contains(contentType.type)) {
                    return true;
                }
            }
        }
        return false;
    }


//    /* Constructor when given folder and file on sd card */
//    public Station(File folder, Uri fileLocation) {
//
//        // create results bundle
//        mStationFetchResults = new Bundle();
//
//        // read local file and put result into mPlaylistFileContent
//        File localFile = new File(fileLocation.getPath());
//        if (localFile.exists()) {
//            mPlaylistFileContent = readPlaylistFile(localFile);
//        } else {
//            LogHelper.v(LOG_TAG, "File does not exist " + localFile);
//        }
//
//        // parse the raw content of playlist file (mPlaylistFileContent)
//        if (parse(mPlaylistFileContent) && mStreamUri != null) {
//            // save results
//            mStationFetchResults.putParcelable(TransistorKeys.RESULT_STREAM_TYPE, getContentType(mStreamUri));
//            mStationFetchResults.putString(TransistorKeys.RESULT_FILE_CONTENT, mPlaylistFileContent);
//            mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, false);
//
//        } else {
//            // save error flag and file content in results
//            mStationFetchResults.putString(TransistorKeys.RESULT_FILE_CONTENT, "\n[File probably does not contain a valid streaming URL.]");
//            mStationFetchResults.putBoolean(TransistorKeys.RESULT_FETCH_ERROR, true);
//        }
//
//        // set Transistor's playlist file object
//        setStationPlaylistFile(folder);
//
//        // set Transistor's image file object
//        setStationImageFile(folder);
//
//        // set playback state
//        mPlayback = false;
//
//    }


    /* Constructor used by CREATOR */
    protected Station(Parcel in) {
        TITLE = in.readString();
        URI = in.readString();
        mStationFetchResults = in.readBundle(Bundle.class.getClassLoader());
        mPlayback = in.readByte() != 0; // true if byte != 0

        _ID = in.readInt();
        UNIQUE_ID = in.readString();
        SUBTITLE = in.readString();
        IMAGE_PATH = in.readString();
        IMAGE_FILE_NAME = in.readString();
        CONTENT_TYPE = in.readString();
        DESCRIPTION = in.readString();
        RATING = in.readString();
        CATEGORY = in.readString();
        IS_FAVOURITE = in.readString();
        THUMP_UP_STATUS = in.readString();

        LogHelper.v(LOG_TAG, "Station re-created from parcel. State of playback is: " + mPlayback);
    }


    /* CREATOR for Collection object used to do parcel related operations */
    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };


//    /* Construct string representation of m3u mStationPlaylistFile */
//    private String createM3u() {
//
//        // construct m3u String
//        StringBuilder sb = new StringBuilder("");
//        sb.append("#EXTM3U\n\n");
//        sb.append("#EXTINF:-1,");
//        sb.append(mStationName);
//        sb.append("\n");
//        sb.append(mStreamUri.toString());
//        sb.append("\n");
//
//        return sb.toString();
//    }


    /* Downloads remote playlist file */
    private String downloadPlaylistFile(URL fileLocation) {

        LogHelper.v(LOG_TAG, "Downloading... " + fileLocation.toString());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                fileLocation.openStream()))) {

            String line;
            int counter = 0;
            StringBuilder sb = new StringBuilder("");

            // read until last last reached or until line five
            while ((line = br.readLine()) != null && counter < 5) {
                sb.append(line);
                sb.append("\n");
                counter++;
            }

            if (sb.length() == 0) {
                LogHelper.e(LOG_TAG, "Input stream was empty: " + fileLocation.toString());
            }

            // set mPlaylistFileContent and return String
            mPlaylistFileContent = sb.toString();
            return sb.toString();

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to get playlist file from server: " + fileLocation.toString());
            // set mPlaylistFileContent and return null
            mPlaylistFileContent = "[HTTP error. Unable to get playlist file from server: " + fileLocation.toString() + "]";
            return null;
        }
    }


//    /* Reads local playlist file */
//    private String readPlaylistFile(File playlistFile) {
//        try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
//            String line;
//            int counter = 0;
//            StringBuilder sb = new StringBuilder("");
//
//            // read until last line reached or until line five
//            while ((line = br.readLine()) != null && counter < 5) {
//                sb.append(line);
//                sb.append("\n");
//                counter++;
//            }
//
//            // set mPlaylistFileContent and return String
//            mPlaylistFileContent = sb.toString();
//            return sb.toString();
//
//        } catch (IOException e) {
//            LogHelper.e(LOG_TAG, "Unable to read playlist file: " + playlistFile.toString());
//            // set mPlaylistFileContent and return null
//            mPlaylistFileContent = "[IO error. Unable to read playlist file: " + playlistFile.toString() + "]";
//            return null;
//        }
//
//    }


    /* Returns content type for given Uri */
    private ContentType getContentType(Uri streamUri) {
        if (streamUri == null) {
            return null;
        }
        try {
            // determine content type of remote file
            URL streamURL = new URL(streamUri.toString());
            return getContentType(streamURL);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }


    /* Returns content type for given URL */
    private ContentType getContentType(URL fileLocation) {
        try {
            HttpURLConnection connection = (HttpURLConnection) fileLocation.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            String contentTypeHeader = connection.getContentType();

            if (contentTypeHeader != null) {
                Matcher matcher = CONTENT_TYPE_PATTERN.matcher(contentTypeHeader.trim().toLowerCase(Locale.ENGLISH));
                if (matcher.matches()) {
                    ContentType contentType = new ContentType();
                    String contentTypeString = matcher.group(1);
                    String charsetString = matcher.group(3);
                    if (contentTypeString != null) {
                        contentType.type = contentTypeString.trim();
                    }
                    if (charsetString != null) {
                        contentType.charset = charsetString.trim();
                    }
                    return contentType;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /* Determines if given content type is a playlist */
    private boolean isPlaylist(ContentType contentType) {
        if (contentType != null) {
            for (String[] array : new String[][]{CONTENT_TYPES_PLS, CONTENT_TYPES_M3U}) {
                if (Arrays.asList(array).contains(contentType.type)) {
                    return true;
                }
            }
        }
        return false;
    }


    /* Determines if given content type is an audio file */
    private boolean isAudioFile(ContentType contentType) {
        if (contentType != null) {
            for (String[] array : new String[][]{CONTENT_TYPES_MPEG, CONTENT_TYPES_OGG, CONTENT_TYPES_AAC}) {
                if (Arrays.asList(array).contains(contentType.type)) {
                    return true;
                }
            }
        }
        return false;
    }


    /* Determines name of station based on the location URL */
    private String detactStationName(URL fileLocationUrl) {

        String stationName;
        String fileLocation = fileLocationUrl.toString();
        int stationNameStart = fileLocation.lastIndexOf('/') + 1;
        int stationNameEnd = fileLocation.lastIndexOf('.');

        // try to cut out station name from given URL
        if (stationNameStart < stationNameEnd) {
            stationName = fileLocation.substring(stationNameStart, stationNameEnd);
        } else {
            stationName = fileLocation;
        }

        return stationName;
    }


    //    /* download Image File from url */
    private Bitmap downloadImageFile(String theImageExternalUrl) throws MalformedURLException {
        //check if there is image URL availabe in object
        URL fileLocation;
        if (theImageExternalUrl != null && !theImageExternalUrl.isEmpty()) {
            fileLocation = new URL(theImageExternalUrl);
            // download favicon
            LogHelper.v(LOG_TAG, "Downloading channelimage: " + fileLocation.toString());
            try (InputStream in = fileLocation.openStream()) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Error downloading: " + fileLocation.toString());
            }
        } else {
            //useless , but leave it for compatibility
            String faviconLocation = getFavIconUrlString(URI);

            // download favicon
            LogHelper.v(LOG_TAG, "Downloading favicon: " + faviconLocation);
            try (InputStream in = new URL(faviconLocation).openStream()) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Error downloading: " + faviconLocation);
            }
        }
        return null;
    }

    @NonNull
    private String getFavIconUrlString(String inpotUrl) throws MalformedURLException {
        URL fileLocation;//Image path not found
        //Then try get image from fav icon of the host site
        fileLocation = new URL(inpotUrl);
        String host = fileLocation.getHost();

        // strip subdomain and add www if necessary
        if (!host.startsWith("www")) {
            int index = host.indexOf(".");
            host = "www" + host.substring(index);
        }

        // get favicon location
        return "http://" + host + "/favicon.ico";
    }


    /* Parses string representation of mStationPlaylistFile */
    private boolean parse(String fileContent) {

        mPlaylistFileContent = fileContent;

        // check for null
        if (fileContent == null) {
            return false;
        }

        // prepare scanner
        Scanner in = new Scanner(fileContent);
        String line;

        while (in.hasNextLine()) {

            // get a line from file content
            line = in.nextLine();

            // M3U: found station name
            if (line.contains("#EXTINF:-1,")) {
                TITLE = line.substring(11).trim();
                // M3U: found stream URL
            } else if (line.startsWith("http")) {
                URI = line.trim();
            }

            // PLS: found station name
            else if (line.startsWith("Title1=")) {
                TITLE = line.substring(7).trim();
                // PLS: found stream URL
            } else if (line.startsWith("File1=http")) {
                URI = line.substring(6).trim();
            }

        }

        in.close();

        if (URI == null || URI == "") {
            LogHelper.e(LOG_TAG, "Unable to parse: " + fileContent);
            return false;
        }

        // try to construct name of station from remote mStationPlaylistFile name
        if ((TITLE == null || TITLE == "") && URI != null && URI != "") {
            try {
                TITLE = detactStationName(new URL(URI));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                LogHelper.e(LOG_TAG, "Unable to parse: " + fileContent);
                return false;
            }
        } else {
            TITLE = "New Station";
        }

        // file content string parsed successfully
        return true;

    }


//    /* Writes station as m3u to storage */
//    public void writePlaylistFile(File folder) {
//
//        setStationPlaylistFile(folder);
//
//        if (mStationPlaylistFile.exists()) {
//            LogHelper.w(LOG_TAG, "File exists. Overwriting " + mStationPlaylistFile.getName() + " " + TITLE + " " + mStreamUri);
//        }
//
//        LogHelper.v(LOG_TAG, "Saving... " + mStationPlaylistFile.toString());
//
//        String m3uString = createM3u();
//
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mStationPlaylistFile))) {
//            bw.write(m3uString);
//        } catch (IOException e) {
//            LogHelper.e(LOG_TAG, "Unable to write PlaylistFile " + mStationPlaylistFile.toString());
//        }
//
//    }


//    /* Writes station image as png to storage */
//    public void writeImageFile() {
//
//        LogHelper.v(LOG_TAG, "Saving favicon: " + mStationImageFile.toString());
//
//        // write image to storage
//        try (FileOutputStream out = new FileOutputStream(mStationImageFile)) {
//            mStationImage.compress(Bitmap.CompressFormat.PNG, 100, out);
//        } catch (IOException e) {
//            LogHelper.e(LOG_TAG, "Unable to save favicon: " + mStationImage.toString());
//        }
//
//    }

    /* Writes station image as png to storage */
    public void writeImageFile(File folder, Bitmap downloadedImage) {
        String fileLocation = folder.toString() + "/" + IMAGE_FILE_NAME;
        LogHelper.v(LOG_TAG, "Saving channel image : " + fileLocation.toString());
        File mStationImageFile = new File(fileLocation);
        // write image to storage
        try (FileOutputStream out = new FileOutputStream(mStationImageFile)) {
            downloadedImage.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to save channel image: " + downloadedImage.toString());
        }
    }


    /* Getter for download error flag */
    public Bundle getStationFetchResults() {
        return mStationFetchResults;
    }


//    /* Getter for file object representing station image */
//    public File getStationImageFile() {
//        return mStationImageFile;
//    }


    /* Getter for playback state */
    public boolean getPlaybackState() {
        return mPlayback;
    }


    /* Getter for URL of stream */
    public Uri getStreamUri() {
        return Uri.parse(URI);
    }


    /* Setter for image file object of station */
    public File getStationImageFileReference(File folder) {
        if (IMAGE_FILE_NAME != null && IMAGE_FILE_NAME != "") {
            // construct location of png image file from station name and folder
            String fileLocation = folder.toString() + "/" + IMAGE_FILE_NAME;
            mStationImageFile = new File(fileLocation);
        } else {
            // construct location of png image file from station name and folder
            String fileLocation = folder.toString() + "/" + UNIQUE_ID + ".png";
            mStationImageFile = new File(fileLocation);
        }
        return mStationImageFile;
    }

    /* return cached image File  */
    public File getStationImage(final Context cntxt) {
        //try get the image from file cache
        Bitmap channelImage  =null;
        StorageHelper storageHelper = new StorageHelper(cntxt);
        final File folder = storageHelper.getCollectionDirectory();
        if (IMAGE_FILE_NAME != null && IMAGE_FILE_NAME != "") {
            // construct location of png image file from station name and folder
            String fileLocation = folder.toString() + "/" + IMAGE_FILE_NAME;
            mStationImageFile = new File(fileLocation);
        } else {
            // construct location of png image file from station name and folder
            String fileLocation = folder.toString() + "/" + UNIQUE_ID + ".png";
            mStationImageFile = new File(fileLocation);
        }
        if (mStationImageFile.exists()) {
            //file already exists, then get it from file system
//            try {
//                channelImage = BitmapFactory.decodeStream(new FileInputStream(mStationImageFile));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
            return  mStationImageFile;
        } else {
            //return null
            //and download it in background
            //check stations images ready on desk cache
            Thread prepareThread = new Thread() {
                @Override
                public void run() {
                    boolean downloadDoneSuccessfully = false;
                    try{
                        //load all stations and ensure images are cached
                        final StationsDbHelper mDbHelper = new StationsDbHelper(cntxt);
                        //try download the file
                        Bitmap downloadedImage = downloadImageFile(IMAGE_PATH);
                        //Save Image to desk
                        if (downloadedImage != null) {
                            writeImageFile(folder, downloadedImage);
                            downloadDoneSuccessfully=true;
                        }
                    }  catch (MalformedURLException e) {
                        e.printStackTrace();
                    }finally {
                        // send local broadcast
                        if(downloadDoneSuccessfully) {
                            Intent i = new Intent();
                            i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
                            i.putExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, TransistorKeys.STATION_CHANGED_IMAGE);
                            i.putExtra(TransistorKeys.EXTRA_STATION, Station.this);
                            i.putExtra(TransistorKeys.EXTRA_STATION_ID, _ID);
                            LocalBroadcastManager.getInstance(cntxt.getApplicationContext()).sendBroadcast(i);
                        }
                    }
                }
                @Override
                public State getState() {
                    return super.getState();
                }
            };
            prepareThread.start();
        }

        return null;
    }


    /* Setter for playback state */
    public void setPlaybackState(boolean playback) {
        mPlayback = playback;
    }


    @Override
    public int compareTo(@NonNull Station otherStation) {
        // Compares two stations: returns "1" if name if this station is greater than name of given station
        return TITLE.compareToIgnoreCase(otherStation.TITLE);
    }


    @Override
    public String toString() {
        return "Station [Name=" + TITLE + ", Uri=" + URI + "]";
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(TITLE);
        dest.writeString(URI);
        dest.writeBundle(mStationFetchResults);
        dest.writeByte((byte) (mPlayback ? 1 : 0));  // if mPlayback == true, byte == 1
        dest.writeInt(_ID);
        dest.writeString(UNIQUE_ID);
        dest.writeString(SUBTITLE);
        dest.writeString(IMAGE_PATH);
        dest.writeString(IMAGE_FILE_NAME);
        dest.writeString(CONTENT_TYPE);
        dest.writeString(DESCRIPTION);
        dest.writeString(RATING);
        dest.writeString(CATEGORY);
        dest.writeString(IS_FAVOURITE);
        dest.writeString(THUMP_UP_STATUS);
    }


    /**
     * Container class representing the content-type and charset string
     * received from the response header of an HTTP server.
     */
    public class ContentType implements Parcelable {
        String type;
        String charset;


        /* Constructor (default) */
        public ContentType() {
        }

        /* Constructor used by CREATOR */
        protected ContentType(Parcel in) {
            type = in.readString();
            charset = in.readString();
        }

        /* CREATOR for ContentType object used to do parcel related operations */
        public final Creator<ContentType> CREATOR = new Creator<ContentType>() {
            @Override
            public ContentType createFromParcel(Parcel in) {
                return new ContentType(in);
            }

            @Override
            public ContentType[] newArray(int size) {
                return new ContentType[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(type);
            dest.writeString(charset);
        }

        @Override
        public String toString() {
            return "ContentType{type='" + type + "'" + ", charset='" + charset + "'}";
        }

        public String getTypeString() {
            return type;
        }
    }


    /* add station data to DB SQLite */
    public static void AddStationItemToDb(Station stationItem, Activity mActivity) {
        //db test
        StationsDbHelper mDbHelper = new StationsDbHelper(mActivity);
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Filter results WHERE "title" = 'My Title'
        String selection = StationsDbContract.StationEntry.COLUMN_UNIQUE_ID + " = ?";
        String[] selectionArgs = {stationItem.UNIQUE_ID};

        String[] projection = {
                StationsDbContract.StationEntry._ID,
                StationsDbContract.StationEntry.COLUMN_UNIQUE_ID
        };
        String sortOrder =
                StationsDbContract.StationEntry.COLUMN_UNIQUE_ID + " DESC";
        Cursor cursor = db.query(
                StationsDbContract.StationEntry.TABLE_NAME, // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder
        );
        if (cursor.getCount() == 0) {
            //record not found
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(StationsDbContract.StationEntry.COLUMN_NAME_TITLE, stationItem.TITLE);
            values.put(StationsDbContract.StationEntry.COLUMN_UNIQUE_ID, stationItem.UNIQUE_ID);
            values.put(StationsDbContract.StationEntry.COLUMN_NAME_SUBTITLE, stationItem.SUBTITLE);
            values.put(StationsDbContract.StationEntry.COLUMN_DESCRIPTION, stationItem.DESCRIPTION);
            values.put(StationsDbContract.StationEntry.COLUMN_IMAGE_PATH, stationItem.IMAGE_PATH);
            values.put(StationsDbContract.StationEntry.COLUMN_IMAGE_FILE_NAME, stationItem.IMAGE_FILE_NAME);
            values.put(StationsDbContract.StationEntry.COLUMN_URI, stationItem.URI);
            values.put(StationsDbContract.StationEntry.COLUMN_CONTENT_TYPE, stationItem.CONTENT_TYPE);
            //values.put(StationsDbContract.StationEntry.COLUMN_RATING, stationItem.RATING);
            values.put(StationsDbContract.StationEntry.COLUMN_CATEGORY, stationItem.CATEGORY);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(StationsDbContract.StationEntry.TABLE_NAME, null, values);
        }
        db.close();
    }

    public ArrayList<Station> getInsertedStations() {
        return mInsertedStations;
    }
}
