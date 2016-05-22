/**
 * Station.java
 * Implements the Station class
 * A Station handles station-related data, e.g. the name and the streaming URL
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
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

    /* Supported audio file content types */
    private static final String[] CONTENT_TYPES_MPEG = {"audio/mpeg"};
    private static final String[] CONTENT_TYPES_OGG = {"audio/ogg", "application/ogg"};

    /* Supported playlist content types */
    private static final String[] CONTENT_TYPES_PLS = {"audio/x-scpls"};
    private static final String[] CONTENT_TYPES_M3U = { // See https://en.wikipedia.org/wiki/M3U for the list of MIME types
            "audio/mpegurl",
            "audio/x-mpegurl",
            "application/mpegurl",
            "application/x-mpegurl",
            "application/vnd.apple.mpegurl",
    };

    private static final String[] CONTENT_TYPES_UNKNOWN = {"application/octet-stream"};
    private static final String[] FILE_EXTENSIONS_M3U = {".m3u", ".m3u8", ".M3U", ".M3U8"};
    private static final String[] FILE_EXTENSIONS_PLS = {".pls", ".PLS"};

    /* Regular expression to extract content-type and charset from header string */
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("([^;]*)(; ?charset=([^;]+))?");


    /* Main class variables */
    private Bitmap mStationImage;
    private File mStationImageFile;
    private String mStationName;
    private File mStationPlaylistFile;
    private Uri mStreamUri;
    private String mPlaylistFileContent;
    private boolean mStationFetchError;


    /* Constructor when given file from the Collection folder */
    public Station(File file) {
        // set mStationPlaylistFile and filename
        mStationPlaylistFile = file;

        // read and parse mStationPlaylistFile
        if (mStationPlaylistFile.exists()) {
            parse(readPlaylistFile(mStationPlaylistFile));
        }

        // set image file object
        File folder = mStationPlaylistFile.getParentFile();
        if (folder != null) {
            setStationImageFile(folder);
        }

    }


    /* Constructor when given folder and remote location */
    public Station(File folder, URL fileLocation) {

        // determine content type of remote file
        ContentType contentType = getContentType(fileLocation);
        Log.v(LOG_TAG, "Content type of given file is " + contentType);

        // content type is raw audio file
        if (isAudioFile(contentType)) {
            // use raw audio file for station data
            mStreamUri = Uri.parse(fileLocation.toString().trim());
            mStationName = getStationName(fileLocation);
            setStationFetchError(false);
        }

        // content type is playlist
        else if (isPlaylist(contentType)) {
            // download and parse station data from playlist file
            mPlaylistFileContent = downloadPlaylistFile(fileLocation);

            // parse result of downloadPlaylistFile
            if (parse(mPlaylistFileContent) && streamUriIsAudioFile()) {
                mStationName = getStationName(fileLocation);
                setStationFetchError(false);
            } else {
                mPlaylistFileContent = mPlaylistFileContent + "\n[File probably does not contain a valid streaming URL.]";
                setStationFetchError(true);
            }

        // content type is none of the above
        } else {
            // set error flag and return
            setStationFetchError(true);
            return;
        }

        // set Transistor's playlist file object
        setStationPlaylistFile(folder);

        // download favicon and store bitmap object
        downloadImageFile(fileLocation);

        // set Transistor's image file object
        setStationImageFile(folder);
    }


    /* Constructor when given folder and file on sd card */
    public Station(File folder, Uri fileLocation) {

        File localFile = new File(fileLocation.getPath());

        // read local file and put result into mPlaylistFileContent
        if (localFile.exists()) {
            mPlaylistFileContent = readPlaylistFile(localFile);
        } else {
            Log.v(LOG_TAG, "File does not exist " + localFile);
        }

        // parse the raw content of playlist file (mPlaylistFileContent)
        if (parse(mPlaylistFileContent) && streamUriIsAudioFile()) {
            mStationFetchError = false;
        } else {
            mPlaylistFileContent = mPlaylistFileContent + "\n[File probably does not contain a valid streaming URL.]";
            mStationFetchError = true;
        }

        // set Transistor's playlist file object
        setStationPlaylistFile(folder);

        // set Transistor's image file object
        setStationImageFile(folder);

    }


    /* Constructor used by CREATOR */
    protected Station(Parcel in) {
        mStationImage = in.readParcelable(Bitmap.class.getClassLoader());
        mStationImageFile = new File (in.readString());
        mStationName = in.readString();
        mStationPlaylistFile = new File (in.readString());
        mStreamUri = in.readParcelable(Uri.class.getClassLoader());
        mPlaylistFileContent = in.readString();
        mStationFetchError = in.readByte() != 0;
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


    /* Downloads remote playlist file */
    private String downloadPlaylistFile(URL fileLocation) {

        Log.v(LOG_TAG, "Downloading... " + fileLocation.toString());

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
                Log.e(LOG_TAG, "Input stream was empty: " + fileLocation.toString());
            }

            // set mPlaylistFileContent and return String
            mPlaylistFileContent = sb.toString();
            return sb.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to get playlist file from server: " + fileLocation.toString());
            // set mPlaylistFileContent and return null
            mPlaylistFileContent = "[HTTP error. Unable to get playlist file from server: " + fileLocation.toString() + "]";
            return null;
        }
    }


    /* Reads local playlist file */
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
            Log.e(LOG_TAG, "Unable to read playlist file: " + playlistFile.toString());
            // set mPlaylistFileContent and return null
            mPlaylistFileContent = "[IO error. Unable to read playlist file: " + playlistFile.toString()  + "]";
            return null;
        }

    }


    /* Returns content type for given URL */
    private ContentType getContentType(URL fileLocation) {
        try {
            HttpURLConnection connection = (HttpURLConnection)fileLocation.openConnection();
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
                    // Generic octet-stream MIME type, check if we can guess file type from extension
                    // This hack is needed only for playlists, most stations set audio stream type correctly
                    if (Arrays.asList(CONTENT_TYPES_UNKNOWN).contains(contentType.type)) {
                        for (String ext: FILE_EXTENSIONS_M3U) {
                            if (fileLocation.getFile() != null && fileLocation.getFile().endsWith(ext) ||
                                    fileLocation.getQuery() != null && fileLocation.getQuery().endsWith(ext)) {
                                contentType.type = CONTENT_TYPES_M3U[0];
                            }
                        }
                        for (String ext: FILE_EXTENSIONS_PLS) {
                            if (fileLocation.getFile() != null && fileLocation.getFile().endsWith(ext) ||
                                    fileLocation.getQuery() != null && fileLocation.getQuery().endsWith(ext)) {
                                contentType.type = CONTENT_TYPES_PLS[0];
                            }
                        }
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
            for (String[] array : new String[][]{CONTENT_TYPES_MPEG, CONTENT_TYPES_OGG}) {
                if (Arrays.asList(array).contains(contentType.type)) {
                    return true;
                }
            }
        }
        return false;
    }


    /* Checks if stream URI of station is valid audio file */
    private boolean streamUriIsAudioFile () {

        if (mStreamUri == null) {
            return false;
        }

        try {
            // determine content type of remote file
            URL streamURL = new URL(mStreamUri.toString());
            ContentType contentType = getContentType(streamURL);
            Log.v(LOG_TAG, "Content type of URL within playlist: " + contentType);

            return isAudioFile(contentType);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

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


    /* Downloads remote favicon file */
    private void downloadImageFile(URL fileLocation) {

        // get domain
        String host = fileLocation.getHost();

        // strip subdomain and add www if necessary
        if (!host.startsWith("www")) {
            int index = host.indexOf(".");
            host = "www" + host.substring(index);
        }

        // get favicon location
        String faviconLocation = "http://" + host + "/favicon.ico";

        // download favicon
        Log.v(LOG_TAG, "Downloading favicon: " + faviconLocation);
        try (InputStream in = new URL(faviconLocation).openStream()) {
            mStationImage = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error downloading: " + faviconLocation);
        }

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
        mStationName = null;
        mStreamUri = null;

        while (in.hasNextLine()) {

            // get a line from file content
            line = in.nextLine();

            // M3U: found station name
            if (line.contains("#EXTINF:-1,")) {
                mStationName = line.substring(11).trim();
            // M3U: found stream URL
            } else if (line.startsWith("http")) {
                mStreamUri = Uri.parse(line.trim());
            }

            // PLS: found station name
            else if (line.startsWith("Title1=")) {
                mStationName = line.substring(7).trim();
            // PLS: found stream URL
            } else if (line.startsWith("File1=http")) {
                mStreamUri = Uri.parse(line.substring(6).trim());
            }

            // Use the first entry, some stations shuffle playlist to put less used mirror to the top of the list
            if (mStreamUri != null && mStationName != null) {
                break;
            }
        }

        in.close();

        if (mStreamUri == null) {
            Log.e(LOG_TAG, "Unable to parse: " + fileContent);
            return false;
        }

        // try to construct name of station from remote mStationPlaylistFile name
        if (mStationPlaylistFile != null && mStationName == null) {
            mStationName = mStationPlaylistFile.getName().substring(0, mStationPlaylistFile.getName().lastIndexOf("."));
        } else if (mStationPlaylistFile == null && mStationName == null) {
            mStationName = "New Station";
        }

        // file content string parsed successfully
        return true;

    }


    /* Writes station as m3u to storage */
    public void writePlaylistFile(File folder) {

        setStationPlaylistFile(folder);

        if (mStationPlaylistFile.exists()) {
            Log.w(LOG_TAG, "File exists. Overwriting " + mStationPlaylistFile.getName() + " " + mStationName + " " + mStreamUri);
        }

        Log.v(LOG_TAG, "Saving... " + mStationPlaylistFile.toString());

        String m3uString = createM3u();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mStationPlaylistFile))) {
            bw.write(m3uString);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to write PlaylistFile " + mStationPlaylistFile.toString());
        }

    }


    /* Writes station image as png to storage */
    public void writeImageFile() {

        Log.v(LOG_TAG, "Saving favicon: " + mStationImageFile.toString());

        // write image to storage
        try (FileOutputStream out = new FileOutputStream(mStationImageFile)) {
            mStationImage.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to save favicon: " + mStationImage.toString());
        }

    }


    /* Getter for download error flag */
    public boolean getStationFetchError() {
        return mStationFetchError;
    }


    /* Getter for playlist file object representing station */
    public File getStationPlaylistFile() {
        return mStationPlaylistFile;
    }


    /* Getter for file object representing station image */
    public File getStationImageFile() {
        return mStationImageFile;
    }


    /* Getter for raw content of a playlist file */
    public String getPlaylistFileContent() {
        return mPlaylistFileContent;
    }


    /* Getter for station image */
    public Bitmap getStationImage() {
        return mStationImage;
    }


    /* Getter for name of station */
    public String getStationName() {
        return mStationName;
    }


    /* Setter for playlist file object of station */
    public void setStationPlaylistFile(File folder) {
        if (mStationName != null) {
            // strip out problematic characters
            String stationNameCleaned = mStationName.replaceAll("[:/]", "_");
            // construct location of m3u playlist file from station name and folder
            String fileLocation = folder.toString() + "/" + stationNameCleaned + ".m3u";
            mStationPlaylistFile = new File(fileLocation);
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
        }
    }


    /* Setter for download error flag */
    private void setStationFetchError(boolean stationFetchError) {
        mStationFetchError = stationFetchError;
    }


    /* Setter for name of station */
    public void setStationName(String newStationName) {
        mStationName = newStationName;
    }


    /* Getter for URL of stream */
    public Uri getStreamUri() {
        return mStreamUri;
    }


    /* Setter for URL of station */
    public void setStreamUri(Uri newStreamUri) {
        mStreamUri = newStreamUri;
    }


    @Override
    public int compareTo(@NonNull Station otherStation) {
        // Compares two stations: returns "1" if name if this station is greater than name of given station
        return mStationName.compareToIgnoreCase(otherStation.mStationName);
    }


    @Override
    public String toString() {
        return "Station [mStationName=" + mStationName + ", mStationPlaylistFile=\" + mStationPlaylistFile + \", mStreamUri=" + mStreamUri + "]";
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mStationImage, flags);
        dest.writeString(mStationImageFile.toString());
        dest.writeString(mStationName);
        dest.writeString(mStationPlaylistFile.toString());
        dest.writeParcelable(mStreamUri, flags);
        dest.writeString(mPlaylistFileContent);
        dest.writeByte((byte) (mStationFetchError ? 1 : 0));
    }


    /**
     * Container class representing the content-type and charset string
     * received from the response header of an HTTP server.
     */
    private class ContentType {
        String type;
        String charset;

        @Override
        public String toString() {
            return "ContentType{type='" + type + "'" + ", charset='" + charset + "'}";
        }
    }

}
