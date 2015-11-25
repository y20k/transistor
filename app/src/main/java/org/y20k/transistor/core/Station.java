/**
 * Station.java
 * Implements the Station class
 * A Station handles station-related data, e.g. the name and the streaming URL
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;


/**
 * Station class
 */
public class Station implements Comparable<Station> {

    /* Define log tag */
    private static final String LOG_TAG = Station.class.getSimpleName();


    /* Main class variables */
    private Bitmap mStationImage;
    private File mStationImageFile;
    private String mStationName;
    private File mStationPlaylistFile;
    private URL mStreamURL;
    private String mRemoteFileContent;
    private boolean mDownloadError;


    /* Constructor when given mStationPlaylistFile */
    public Station(File file) {
        // set mStationPlaylistFile and filename
        mStationPlaylistFile = file;

        // read mStationPlaylistFile
        if (mStationPlaylistFile.exists()) {
            read();
        }

        // set image file object
        File folder = mStationPlaylistFile.getParentFile();
        if (folder != null) {
            setStationImageFile(folder);
        }
    }


    /* Constructor when given name and stream url */
    public Station(File folder, String stationName, URL streamURL) {
        // set name and url object for station
        mStationName = stationName;
        mStreamURL = streamURL;

        // set playlist file object - name of station required
        setStationPlaylistFile(folder);

        // set image file object
        setStationImageFile(folder);
    }


    /* Constructor when given folder and remote location for playlist file */
    public Station(File folder, URL fileLocation) {
        // download and parse station data from remote playlist file
        if (downloadPlaylistFile(fileLocation)) {
            setDownloadError(false);
        } else {
            // set error flag
            setDownloadError(true);
        }

        // set playlist file object - name of station required
        setStationPlaylistFile(folder);

        // download favicon and store bitmap object
        downloadImageFile(fileLocation);

        // set image file object
        setStationImageFile(folder);

    }


    /* Compares two stations */
    @Override
    public int compareTo(Station otherStation) {
        // return "1" if name if this station is greater than name of given station
        return mStationName.compareToIgnoreCase(otherStation.mStationName);
    }


    /* Construct string representation of m3u mStationPlaylistFile */
    private String createM3u() {
        String m3uString;

        // construct m3uString
        StringBuilder sb = new StringBuilder("");
        sb.append("#EXTM3U\n\n");
        sb.append("#EXTINF:-1,");
        sb.append(mStationName);
        sb.append("\n");
        sb.append(mStreamURL.toString());
        sb.append("\n");
        m3uString = sb.toString();

        return m3uString;
    }


    /* Downloads remote playlist file and parses station */
    private boolean downloadPlaylistFile(URL fileLocation) {

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
            // get fileContent from StringBuilder result
            mRemoteFileContent = sb.toString();

            if (sb.length() == 0) {
                Log.e(LOG_TAG, "Input stream was empty: " + fileLocation.toString());
            }

            // set name of station
            mStationName = fileLocation.toString().substring(
                    fileLocation.toString().lastIndexOf('/') + 1,
                    fileLocation.toString().lastIndexOf('.'));

            // parse result of downloadPlaylistFile
            if (parse(mRemoteFileContent)) {
                return true;
            } else {
                mRemoteFileContent = "File does not contain valid streaming URL:\n" + mRemoteFileContent;
                return false;
            }

        } catch (IOException e) {
            mRemoteFileContent = "HTTP error. Unable to get playlist file from server " + fileLocation.toString();
            Log.e(LOG_TAG, mRemoteFileContent);
            return false;
        }
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

        // prepare scanner
        Scanner in = new Scanner(fileContent);
        String line;

        while (in.hasNextLine()) {

            // get a line from file content
            line = in.nextLine();

            // M3U: found station name
            if (line.contains("#EXTINF:-1,")) {
                mStationName = line.substring(11).trim();
            // M3U: found stream URL
            } else if (line.startsWith("http") &&
                    !line.contains("wmv") &&
                    !line.contains("ogg") &&
                    !line.contains("m3u")) {
                try {
                    mStreamURL = new URL(line.trim());
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, line.trim() + "is not a valid URL");
                }
            }

            // PLS: found station name
            else if (line.startsWith("Title1=") &&
                    !line.contains("wmv") &&
                    !line.contains("ogg") &&
                    !line.contains("m3u")) {
                mStationName = line.substring(7).trim();
            // PLS: found stream URL
            } else if (line.startsWith("File1=http")) {
                try {
                    mStreamURL = new URL(line.substring(6).trim());
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, line.substring(6).trim() + "is not a valid URL");
                }
            }
        }

        in.close();

        // try to construct name of station from remote mStationPlaylistFile name
        if (mStationPlaylistFile != null && mStationName == null) {
            mStationName = mStationPlaylistFile.getName().substring(0, mStationPlaylistFile.getName().lastIndexOf("."));
        } else if (mStationPlaylistFile == null && mStationName == null) {
            mStationName = "New Station";
        }

        if (mStreamURL != null) {
            // log station name and URL
            Log.v(LOG_TAG, "Name: " + mStationName);
            Log.v(LOG_TAG, "URL: " + mStreamURL.toString());
            return true;
        } else {
            // log error
            Log.e(LOG_TAG, "Unable to parse: " + fileContent);
            return false;
        }

    }


    /* Reads local mStationPlaylistFile and parses station */
    private void read() {

        String fileContent;

        try (BufferedReader br = new BufferedReader(new FileReader(mStationPlaylistFile))) {
            String line;
            int counter = 0;
            StringBuilder sb = new StringBuilder("");

            // read until last last reached or until line five
            while ((line = br.readLine()) != null && counter < 5) {
                sb.append(line);
                sb.append("\n");
                counter++;
            }
            // point fileContent to StringBuilder result
            fileContent = sb.toString();

            // parse result of read operation and create new station
            parse(fileContent);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to read mStationPlaylistFile " + mStationPlaylistFile.toString());
        }

    }


    /* Custom toString method */
    @Override
    public String toString() {
        return "Station [mStationName=" + mStationName + ", mStationPlaylistFile=\" + mStationPlaylistFile + \", mStreamURL=" + mStreamURL + "]";
    }


    /* Writes station as m3u to storage */
    public void writePlaylistFile(File folder) {

        setStationPlaylistFile(folder);

        if (mStationPlaylistFile.exists()) {
            Log.w(LOG_TAG, "File exists. Overwriting " + mStationPlaylistFile.getName() + " " + mStationName + " " + mStreamURL);
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
    public boolean getDownloadError() {
        return mDownloadError;
    }


    /* Getter for playlist file object representing station */
    public File getStationPlaylistFile() {
        return mStationPlaylistFile;
    }


    /* Getter for file object representing station image */
    public File getStationImageFile() {
        return mStationImageFile;
    }


    /* Getter for content of remote file image */
    public String getRemoteFileContent() {
        return mRemoteFileContent;
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
        // construct location of m3u playlist file from station name and folder
        String fileLocation = folder.toString() + "/" + mStationName + ".m3u";
        mStationPlaylistFile = new File(fileLocation);
    }


    /* Setter for image file object of station */
    public void setStationImageFile(File folder) {
        // construct location of png image file from station name and folder
        String fileLocation = folder.toString() + "/" + mStationName + ".png";
        mStationImageFile = new File(fileLocation);
    }


    /* Setter for download error flag */
    public void setDownloadError(boolean downloadError) {
        mDownloadError = downloadError;
    }


    /* Setter for name of station */
    public void setStationName(String newStationName) {
        mStationName = newStationName;
    }


    /* Getter for URL of stream */
    public URL getStreamURL() {
        return mStreamURL;
    }


    /* Setter for URL of station */
    public void setStreamURL(URL newStreamURL) {
        mStreamURL = newStreamURL;
    }

}
