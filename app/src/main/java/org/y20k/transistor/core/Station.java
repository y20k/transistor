/**
 * Station.java
 * Implements the Station class
 * A Station handles station-related data, e.g. the name and the streaming URL
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.core;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;

import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;


/**
 * Station class
 */
public final class Station implements TransistorKeys, Cloneable, Comparable<Station>, Parcelable {

    /* Define log tag */
    private static final String LOG_TAG = Station.class.getSimpleName();

    /* Regular expression to extract content-type and charset from header string - supported content types are in TransistorKeys*/
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("([^;]*)(; ?charset=([^;]+))?");


    /* Main class variables */
    private File mStationImageFile;
    private long mStationImageSize;
    private String mStationName;
    private File mStationPlaylistFile;
    private Uri mStreamUri;
    private String mPlaylistFileContent;
    private int mPlayback;
    private boolean mSelected;
    private String mMetadata;
    private String mMimeType;
    private int mChannelCount;
    private int mSampleRate;
    private int mBitrate;
    private Bundle mStationFetchResults;


    /* Generic Constructor */
    public Station(File stationImageFile, long stationImageSize, String stationName, File stationPlaylistFile, Uri streamUri, String playlistFileContent, int playback, boolean selected, String metadata, String mimeType, int channelCount, int sampleRate, int bitrate, Bundle stationFetchResults) {
        mStationImageFile = stationImageFile;
        mStationImageSize = stationImageSize;
        mStationName = stationName;
        mStationPlaylistFile = stationPlaylistFile;
        mStreamUri = streamUri;
        mPlaylistFileContent = playlistFileContent;
        mPlayback = playback;
        mSelected = selected;
        mMetadata = metadata;
        mMimeType = mimeType;
        mChannelCount = channelCount;
        mSampleRate = sampleRate;
        mBitrate = bitrate;
        mStationFetchResults = stationFetchResults;
    }


    /* Constructor when given file from the Collection folder */
    public Station(File file) {
        // create results bundle
        mStationFetchResults = new Bundle();

        // store file
        mStationPlaylistFile = file;

        // read local playlist file from  Collection folder
        mPlaylistFileContent = readPlaylist(getInputStream(file));

        // parse station data
        parse(mPlaylistFileContent);

        // set image file object
        File folder = mStationPlaylistFile.getParentFile();
        if (folder != null) {
            setStationImageFile(folder);
        }

        // initialize variables that are set during playback to default values
        initializePlaybackMetadata();
    }


    /* Constructor when given folder and remote location (-> the internet) */
    public Station(File folder, URL fileLocation) {
        // create results bundle
        mStationFetchResults = new Bundle();

        // determine content type of remote file
        ContentType contentType = getContentType(fileLocation);

        // content type is raw audio file
        if (isAudioFile(contentType)) {
            // use raw audio file for station data
            mStreamUri = Uri.parse(fileLocation.toString().trim());
            mStationName = getStationName(fileLocation);
            // save results
            mStationFetchResults.putParcelable(RESULT_STREAM_TYPE, contentType);
            mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_ONE_STREAM);
        }

        // content type is playlist
        else if (isPlaylist(contentType)) {
            // read remote playlist file and put result into mPlaylistFileContent
            mPlaylistFileContent = readPlaylist(getInputStream(fileLocation));

            // parse station data
            int parseResult = parse(mPlaylistFileContent);

            // put results in bundle (for StationFetcher)
            if (parseResult == CONTAINS_ONE_STREAM && mStreamUri != null) {
                mStationName = getStationName(fileLocation);
                // save results
                mStationFetchResults.putParcelable(RESULT_PLAYLIST_TYPE, contentType);
                mStationFetchResults.putParcelable(RESULT_STREAM_TYPE, getContentType(mStreamUri));
                mStationFetchResults.putString(RESULT_FILE_CONTENT, mPlaylistFileContent);
                mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_ONE_STREAM);

            } else if (parseResult == CONTAINS_MULTIPLE_STREAMS)  {
                // save result - let StationFetcher handle that
                mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_MULTIPLE_STREAMS);

            } else {
                // save error flag and file content in results
                mStationFetchResults.putParcelable(RESULT_PLAYLIST_TYPE, contentType);
                mStationFetchResults.putString(RESULT_FILE_CONTENT, "\n[File probably does not contain a valid streaming URL.]");
                mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_NO_STREAM);
            }
        // content type is none of the above
        } else if (contentType != null && contentType.type != null) {
            // save results and return
            mStationFetchResults.putParcelable(RESULT_STREAM_TYPE, contentType);
            mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_NO_STREAM);
            return;

        // no content type
        } else {
            // save error flag in results and return
            mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_NO_STREAM);
            return;
        }

        // set Transistor's playlist file object
        setStationPlaylistFile(folder);

        // set Transistor's image file object
        setStationImageFile(folder);

        // initialize variables that are set during playback to default values
        initializePlaybackMetadata();
    }


    /* Constructor when given folder and file on sd card */
    public Station(File folder, ContentResolver contentResolver, Uri uri) {
        // create results bundle
        mStationFetchResults = new Bundle();

        // get file from Uri
        mStationPlaylistFile = new File(uri.getPath());

        // read local file and put result into mPlaylistFileContent
        mPlaylistFileContent = readPlaylist(getInputStream(contentResolver, uri));

        // parse station data
        int parseResult = parse(mPlaylistFileContent);

        // put results in bundle (for StationFetcher)
        if (parseResult == CONTAINS_ONE_STREAM  &&  mStreamUri != null) {
            // save results
            mStationFetchResults.putParcelable(RESULT_STREAM_TYPE, getContentType(mStreamUri));
            mStationFetchResults.putString(RESULT_FILE_CONTENT, mPlaylistFileContent);
            mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_ONE_STREAM);

        } else if (parseResult == CONTAINS_MULTIPLE_STREAMS) {
            // save result - let StationFetcher handle that
            mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_MULTIPLE_STREAMS);

        } else {
            // save error flag and file content in results
            mStationFetchResults.putString(RESULT_FILE_CONTENT, "\n[File probably does not contain a valid streaming URL.]");
            mStationFetchResults.putInt(RESULT_FETCH_STATUS, CONTAINS_NO_STREAM);
        }

        // set Transistor's playlist file object
        setStationPlaylistFile(folder);

        // set Transistor's image file object
        setStationImageFile(folder);

        // initialize variables that are set during playback to default values
        initializePlaybackMetadata();
    }


    /* Constructor when given MediaMetadata (e.g. from Android Auto)  */
    @SuppressLint("WrongConstant")
    public Station (MediaMetadataCompat stationMediaMetadata) {
        mStationName = stationMediaMetadata.getString(METADATA_KEY_TITLE);
        mStreamUri = Uri.parse(stationMediaMetadata.getString(METADATA_KEY_MEDIA_URI));
        mPlayback = PLAYBACK_STATE_STOPPED;
        if (stationMediaMetadata.getString(METADATA_CUSTOM_KEY_IMAGE_FILE) != null) {
            mStationImageFile = new File(stationMediaMetadata.getString(METADATA_CUSTOM_KEY_IMAGE_FILE));
            mStationImageSize = mStationImageFile.length();
        }
        if (stationMediaMetadata.getString(METADATA_CUSTOM_KEY_PLAYLIST_FILE) != null) {
            mStationPlaylistFile = new File(stationMediaMetadata.getString(METADATA_CUSTOM_KEY_PLAYLIST_FILE));
        }
//        mPlaylistFileContent = "";
//        mMetadata = "";
//        mMimeType = "";
//        mChannelCount = 0;
//        mSampleRate = 0;
//        mBitrate = 0;
//        mStationFetchResults = null;
    }


    /* Copy Constructor */
    public Station(Station station) {
        this(station.getStationImageFile(), station.getStationImageSize(), station.getStationName(), station.getStationPlaylistFile(), station.getStreamUri(), station.getPlaylistFileContent(), station.getPlaybackState(), station.getSelectionState(), station.getMetadata(), station.getMimeType(), station.getChannelCount(), station.getSampleRate(), station.getBitrate(), station.getStationFetchResults());
    }


    /* Constructor used by CREATOR */
    protected Station(Parcel in) {
        mStationImageFile = new File (in.readString());
        mStationImageSize = in.readLong();
        mStationName = in.readString();
        mStationPlaylistFile = new File (in.readString());
        mStreamUri = in.readParcelable(Uri.class.getClassLoader());
        mPlaylistFileContent = in.readString();
        mStationFetchResults = in.readBundle(Bundle.class.getClassLoader());
        mPlayback = in.readInt();
        mSelected = in.readByte() != 0;
        mMetadata = in.readString();
        mMimeType = in.readString();
        mChannelCount = in.readInt();
        mSampleRate = in.readInt();
        mBitrate = in.readInt();
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


    /* Resets state of station */
    public void resetState() {
        initializePlaybackMetadata();
    }


    /* Initializes variables that are set during playback */
    private void initializePlaybackMetadata() {
        mPlayback = PLAYBACK_STATE_STOPPED;
        mMetadata = "";
        mMimeType = "";
        mChannelCount = -1;
        mSampleRate = -1;
        mBitrate = -1;
    }


    /* Construct string representation of m3u mStationPlaylistFile */
    private String createM3u() {

        // construct m3u String
        StringBuilder sb = new StringBuilder("");
        sb.append("#EXTM3U\n\n");
        sb.append("#EXTINF:-1,");
        sb.append(mStationName);
        sb.append("\n");
        sb.append(mStreamUri.toString());
        sb.append("\n");

        return sb.toString();
    }


//    /* Downloads remote playlist file */
//    private String downloadPlaylistFile(URL fileLocation) {
//
//        LogHelper.v(LOG_TAG, "Downloading... " + fileLocation.toString());
//
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(
//                fileLocation.openStream()))) {
//
//            String line;
//            int counter = 0;
//            StringBuilder sb = new StringBuilder("");
//
//            // read until last last reached or until sanity limit of 32 lines
//            while ((line = br.readLine()) != null && counter < 32) {
//                sb.append(line);
//                sb.append("\n");
//                counter++;
//            }
//
//            if (sb.length() == 0) {
//                LogHelper.e(LOG_TAG, "Input stream was empty: " + fileLocation.toString());
//            }
//
//            // set mPlaylistFileContent and return String
//            mPlaylistFileContent = sb.toString();
//            return sb.toString();
//
//        } catch (IOException e) {
//            LogHelper.e(LOG_TAG, "Unable to get playlist file from server: " + fileLocation.toString());
//            // set mPlaylistFileContent and return null
//            mPlaylistFileContent = "[HTTP error. Unable to get playlist file from server: " + fileLocation.toString() + "]";
//            return null;
//        }
//    }


    /* Reads InputStream from a playlist file or location and return it as String*/
    private String readPlaylist(InputStream inputStream) {

        LogHelper.v(LOG_TAG, "Reading input stream... ");
        try {
            // write input stream line by line to string
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            int counter = 0;

            // read until last last reached or until sanity limit of 32 lines
            while ((line = br.readLine()) != null && counter < 32) {
                sb.append(line);
                sb.append("\n");
                counter++;
            }
            inputStream.close();

            if (sb.length() == 0) {
                LogHelper.e(LOG_TAG, "Input stream was empty.");
            }

            // return content as String
            return sb.toString();

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to read playlist file.");
            // set mPlaylistFileContent and return null
            mPlaylistFileContent = "[IO error. Unable to read playlist file.]";
            return null;
        }

    }


    /* Get InputStream from remote location (URL) */
    private InputStream getInputStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable open remote location: " + e.toString());
            return null;
        }
    }


    /* Get InputStream from File */
    private InputStream getInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            LogHelper.e(LOG_TAG, "Unable open file: " + e.toString());
            return null;
        }
    }


    /* Get InputStream from Uri using ContentResolver */
    private InputStream getInputStream(ContentResolver contentResolver, Uri uri) {
        try {
            return contentResolver.openInputStream(uri);
        } catch (FileNotFoundException e) {
            LogHelper.e(LOG_TAG, "Unable open file: " + e.toString());
            return null;
        }
    }



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
        ContentType contentType = null;
        try {
            HttpURLConnection connection = createConnection(fileLocation, 0);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            String contentTypeHeader = connection.getContentType();

            if (contentTypeHeader != null) {
                LogHelper.i(LOG_TAG, "Determining content type. Result: " + contentTypeHeader);
                Matcher matcher = CONTENT_TYPE_PATTERN.matcher(contentTypeHeader.trim().toLowerCase(Locale.ENGLISH));
                if (matcher.find()) {
                    contentType = new ContentType();
                    String contentTypeString = matcher.group(1);
                    String charsetString = matcher.group(3);
                    if (contentTypeString != null) {
                        contentType.type = contentTypeString.trim();
                    }
                    if (charsetString != null) {
                        contentType.charset = charsetString.trim();
                    }
                    if (contentType.type.contains("application/octet-stream")) {
                        LogHelper.w(LOG_TAG, "Special case \"application/octet-stream\": use file name to set correct content type.");
                        String headerFieldContentDisposition = connection.getHeaderField("Content-Disposition");
                        if (headerFieldContentDisposition != null) {
                            String fileName = headerFieldContentDisposition.split("=")[1].replace("\"",""); //getting value after '=' & stripping any "s
                            if (fileName.endsWith(FILE_EXTENSION_PLS)) {
                                contentType.type = CONTENT_TYPES_PLS[0];
                                LogHelper.i(LOG_TAG, "Found .pls playlist file: " +  fileName);
                            } else if (fileName.endsWith(FILE_EXTENSION_M3U)) {
                                contentType.type = CONTENT_TYPES_M3U[0];
                                LogHelper.i(LOG_TAG, "Found .m3u playlist file: " +  fileName);
                            } else {
                                LogHelper.i(LOG_TAG, "File name does not seem to be a playlist: " +  fileName);
                            }
                        } else {
                            LogHelper.i(LOG_TAG, "Unable to get file name from \"Content-Disposition\" header field.");
                        }
                    }
                }
            } else {
                LogHelper.w(LOG_TAG, "Unable to determine content type. Type is null.");
            }
            connection.disconnect();
        } catch (Exception e) {
            LogHelper.e(LOG_TAG, "Unable to determine content type. HTTP connection failed");
            e.printStackTrace();
        }
        return contentType;
    }


    /* Determines if given content type is a playlist */
    private boolean isPlaylist(ContentType contentType) {
        if (contentType != null && contentType.type != null) {
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
        if (contentType != null && contentType.type != null) {
            for (String[] array : new String[][]{CONTENT_TYPES_MPEG, CONTENT_TYPES_OGG, CONTENT_TYPES_AAC, CONTENT_TYPES_HLS}) {
                if (Arrays.asList(array).contains(contentType.type)) {
                    return true;
                }
            }
        }
        return false;
    }



    /* Determines name of station based on the location URL */
    private String getStationName(URL fileLocationUrl) {

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



    /* Parses string representation of mStationPlaylistFile */
    private int parse(String fileContent) {

        mPlaylistFileContent = fileContent;

        // check for null
        if (fileContent == null) {
            return CONTAINS_NO_STREAM;
        }

        // prepare scanner
        Scanner in = new Scanner(fileContent);
        String line;
        ArrayList<String> uris = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();

        while (in.hasNextLine()) {
            // get a line from file content
            line = in.nextLine();

            // M3U: found station name
            if (line.contains("#EXTINF:-1,")) {
                names.add(line.substring(11).trim());
                // M3U: found stream URL
            } else if (line.startsWith("http")) {
                uris.add(line.trim());
            }

            // PLS: found station name
            else if (line.matches("^Title[0-9]+=.*")) {
                names.add(line.substring(line.indexOf("=") + 1).trim());
                // PLS: found stream URL
            } else if (line.matches("^File[0-9]+=http.*")) {
                uris.add(line.substring(line.indexOf("=") + 1).trim());
            }

        }

        in.close();

        // CASE 1: playlist was empty - let StationFetcher handle that
        if (uris.size() == 0) {
            LogHelper.e(LOG_TAG, "Unable to parse: " + fileContent + uris.toString());
            return CONTAINS_NO_STREAM;
        }

        // CASE 1: playlist contains multiple streams
        else if (uris.size() > 1) {
            LogHelper.v(LOG_TAG, "Playlist contains multiple stations: " + fileContent);
            mStationFetchResults.putStringArrayList(RESULT_LIST_OF_URIS, uris);
            mStationFetchResults.putStringArrayList(RESULT_LIST_OF_NAMES, names);
            return CONTAINS_MULTIPLE_STREAMS;
        }

        // CASE 3: playlist has one stream
        else {
            // get Uri and Name
            mStreamUri = Uri.parse(uris.get(0));

            // get station name
            if (names.size() >= 1) {
                // use name extracted from playlist
                mStationName = names.get(0);
            } else if (mStationPlaylistFile != null && mStationName == null) {
                // try to construct name of station from remote mStationPlaylistFile name
                mStationName = mStationPlaylistFile.getName().substring(0, mStationPlaylistFile.getName().lastIndexOf("."));
            } else if (mStationPlaylistFile == null && mStationName == null) {
                // use default name
                mStationName = "New Station";
            }
            // file content string parsed successfully
            return CONTAINS_ONE_STREAM;
        }
    }


    /* Writes station as m3u to storage */
    public void writePlaylistFile(File folder) {

        setStationPlaylistFile(folder);

        if (mStationPlaylistFile.exists()) {
            LogHelper.w(LOG_TAG, "File exists. Overwriting " + mStationPlaylistFile.getName() + " " + mStationName + " " + mStreamUri);
        }

        LogHelper.v(LOG_TAG, "Saving... " + mStationPlaylistFile.toString());

        String m3uString = createM3u();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mStationPlaylistFile))) {
            bw.write(m3uString);
        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to write PlaylistFile " + mStationPlaylistFile.toString());
        }

    }


    /* Fetches station image from the internet */
    public Bitmap fetchImageFile(URL stationURL) {

        // check image file and url for null
        if (mStationImageFile == null || stationURL == null) {
            LogHelper.e(LOG_TAG, "Unable to fetch favicon.");
            return null;
        }

        // initialize stationImage to NULL
        Bitmap stationImage = null;

        // Get favicon address
        String host = stationURL.getHost();
        if (!host.startsWith("www")) {
            int index = host.indexOf(".");
            host = "www" + host.substring(index);
        }
        String faviconUrlString = "http://" + host + "/favicon.ico";

        try {
            // try to get image from favicon location
            LogHelper.v(LOG_TAG, "fetching favicon " + mStationImageFile.toString() + "from " + faviconUrlString);
            HttpURLConnection connection = createConnection(new URL(faviconUrlString), 0);

            // get image data and decode stream
            InputStream inputStream = connection.getInputStream();
            stationImage = BitmapFactory.decodeStream(inputStream);

            // close stream and disconnect connection
            inputStream.close();
            connection.disconnect();

            return stationImage;

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to load favicon from URL: " + faviconUrlString);
            e.printStackTrace();
            return stationImage;
        }
//        try {
//            // open connection
//            HttpURLConnection connection = (HttpURLConnection)(new URL(faviconUrlString).openConnection());
//
//            // handle redirects
//            boolean redirect = false;
//            int status = connection.getResponseCode();
//            if (status != HttpURLConnection.HTTP_OK) {
//                if (status == HttpURLConnection.HTTP_MOVED_TEMP
//                        || status == HttpURLConnection.HTTP_MOVED_PERM
//                        || status == HttpURLConnection.HTTP_SEE_OTHER)
//                    redirect = true;
//            }
//            if (redirect) {
//                // get redirect url from "location" header field
//                String newUrl = connection.getHeaderField("Location");
//                connection = (HttpURLConnection) new URL(newUrl).openConnection();
//            }
//
//            // get image data and decode stream
//            InputStream inputStream = connection.getInputStream();
//            stationImage = BitmapFactory.decodeStream(inputStream);
//
//            // close stream and disconnect connection
//            inputStream.close();
//            connection.disconnect();
//
//            return stationImage;
//
//        } catch (IOException e) {
//            LogHelper.e(LOG_TAG, "Unable to load favicon from URL: " + faviconUrlString);
//            e.printStackTrace();
//            return stationImage;
//        }


    }


    /* Creates a http connection from given url */
    private HttpURLConnection createConnection(URL fileLocation, int redirectCount) {
        HttpURLConnection connection = null;

        try {
            // try to open connection
            LogHelper.i(LOG_TAG, "Opening http connection.");
            connection = (HttpURLConnection)fileLocation.openConnection();

            // check for redirects
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {

                    // handle redirects
                    LogHelper.i(LOG_TAG, "Following a redirect.");
                    // get redirect url from "location" header field
                    String redirectUrl = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (redirectCount < 5) {
                        // create new connection with redirect url
                        connection = createConnection(new URL(redirectUrl), redirectCount + 1);
                    } else {
                        connection = null;
                        LogHelper.e(LOG_TAG, "Too many redirects.");
                    }

                }
            }

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to open http connection.");
            e.printStackTrace();
        }

        return connection;
    }


    /* Writes given station image as png to storage */
    public void writeImageFile(Bitmap stationImage) {

        if (stationImage != null) {
            // write image to storage
            try (FileOutputStream out = new FileOutputStream(mStationImageFile)) {
                stationImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                LogHelper.v(LOG_TAG, "Writing favicon to storage.");
            } catch (IOException e) {
                e.printStackTrace();
                LogHelper.e(LOG_TAG, "Unable to write favicon to storage.");
            }
        } else {
            LogHelper.e(LOG_TAG, "Favicon not available (null).");
        }

    }


    /* Checks if two stations have the same stream Uri */
    public boolean streamEquals(Station station) {
        return mStreamUri.equals(station.getStreamUri());
    }


    /* Getter for download error flag */
    public Bundle getStationFetchResults() {
        return mStationFetchResults;
    }


    /* Getter for playlist file object representing station */
    public File getStationPlaylistFile() {
        return mStationPlaylistFile;
    }


    /* Getter for file object representing station image */
    public File getStationImageFile() {
        return mStationImageFile;
    }


    /* Getter for size of station image */
    public long getStationImageSize() {
        return mStationImageSize;
    }


    /* Getter for name of station */
    public String getStationName() {
        return mStationName;
    }


    /* Getter for playback state */
    public int getPlaybackState() {
        return mPlayback;
    }


    /* Getter for selection state (if station is selected in UI) */
    public boolean getSelectionState() {
        return mSelected;
    }


    /* Getter for station id */
    public String getStationId() {
        // since we don't have a unique ID, we fake one using the hashcode of the stream address and append it to the station name
        return mStationName.toLowerCase() + String.valueOf(getStreamUri().hashCode());
    }

    /* Getter for URL of stream */
    public Uri getStreamUri() {
        return mStreamUri;
    }


    /* Getter for Content of playlist file */
    public String getPlaylistFileContent() {
        return mPlaylistFileContent;
    }


    /* Getter for metadata of currently playing song */
    public String getMetadata() {
        return mMetadata;
    }


    /* Getter for MIME type of station during playback */
    public String getMimeType() {
        return mMimeType;
    }


    /* Getter for channel count ((mono / stereo) of station during playback */
    public int getChannelCount() {
        return mChannelCount;
    }


    /* Getter for sample rate of station during playback */
    public int getSampleRate() {
        return mSampleRate;
    }


    /* Getter for bitrate of station during playback */
    public int getBitrate() {
        return mBitrate;
    }


    /* Setter for playlist file object of station */
    public void setStationPlaylistFile(File folder) {
        if (mStationName != null) {
            // strip out problematic characters
            String stationNameCleaned = mStationName.replaceAll("[:/]", "_");
            // construct location of m3u playlist file from station name and folder
            String fileLocation = folder.toString() + "/" + stationNameCleaned + ".m3u";
            mStationPlaylistFile = new File(fileLocation);
        } else {
            mStationPlaylistFile = null;
        }
    }


    /* Setter for image file object of station */
    public void setStationImageFile(File folder) {
        if (mStationName != null) {
            // strip out problematic characters
            String stationNameCleaned = mStationName.replaceAll("[:/]", "_");
            // construct location of png image file from station name and folder
            String fileLocation = folder.toString() + "/" + stationNameCleaned + ".png";
            mStationImageFile = new File(fileLocation);
            mStationImageSize = mStationImageFile.length();
        } else {
            mStationImageFile = null;
            mStationImageSize = 0;
        }
    }


    /* Setter for name of station */
    public void setStationName(String stationName) {
        mStationName = stationName;
    }


    /* Setter for playback state */
    public void setPlaybackState(int playback) {
        mPlayback = playback;
    }


    /* Getter for selection state (if station is selected in UI) */
    public void setSelectionState(boolean selected) {
        mSelected = selected;
    }


    /* Setter for URL of station */
    public void setStreamUri(Uri streamUri) {
        mStreamUri = streamUri;
    }


    /* Setter for Metadata of currently playing media */
    public void setMetadata(String metadata) {
        mMetadata = metadata;
    }


    /* Setter for MIME type of station during playback */
    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }


    /* Setter for channel count (mono / stereo) of station during playback */
    public void setChannelCount(int channelCount) {
        mChannelCount = channelCount;
    }


    /* Setter for sample rate of station during playback */
    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }


    /* Setter for bitrate of station during playback */
    public void setBitrate(int bitrate) {
        mBitrate = bitrate;
    }


    @Override
    public int compareTo(@NonNull Station otherStation) {
        // Compares two stations: returns "1" if name if this station is greater than name of given station
        return mStationName.compareToIgnoreCase(otherStation.mStationName);
    }


    @Override
    public String toString() {
        return "Station [Name=" + mStationName + ", Playlist = " + mStationPlaylistFile + ", Uri=" + mStreamUri + ", Playback" + mPlayback + "]";
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mStationImageFile.toString());
        dest.writeLong(mStationImageSize);
        dest.writeString(mStationName);
        dest.writeString(mStationPlaylistFile.toString());
        dest.writeParcelable(mStreamUri, flags);
        dest.writeString(mPlaylistFileContent);
        dest.writeBundle(mStationFetchResults);
        dest.writeInt(mPlayback);
        dest.writeByte((byte) (mSelected ? 1 : 0));     //if mSelected == true, byte == 1
        dest.writeString(mMetadata);
        dest.writeString(mMimeType);
        dest.writeInt(mChannelCount);
        dest.writeInt(mSampleRate);
        dest.writeInt(mBitrate);
    }


    /**
     * Container class representing the content-type and charset string
     * received from the response header of an HTTP server.
     */
    public class ContentType implements Parcelable {
        String type;
        String charset;

        /* Constructor */
        public ContentType() {
            type = null;
            charset = null;
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
            String typeString;
            String charsetString;
            if (type == null) {
                typeString = "NULL";
            } else {
                typeString = type;
            }
            if (charset == null) {
                charsetString = "NULL";
            } else {
                charsetString = charset;
            }
            return "ContentType{type='" + typeString + "'" + ", charset='" + charsetString + "'}";
        }

    }

}
