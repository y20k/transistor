package org.y20k.transistor.sqlcore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.util.SortedList;

import org.y20k.transistor.core.Station;

import java.util.ArrayList;

import static org.y20k.transistor.sqlcore.StationsDbContract.StationEntry.TABLE_NAME;

/**
 * Created by Tarek on 2017-03-11.
 */

public class StationsDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "StationsDb.db";


    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    StationsDbContract.StationEntry._ID + " INTEGER PRIMARY KEY," +
                    StationsDbContract.StationEntry.COLUMN_UNIQUE_ID + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_NAME_TITLE + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_CATEGORY + " TEXT," +

                    StationsDbContract.StationEntry.COLUMN_IMAGE_PATH + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_IMAGE_FILE_NAME + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_URI + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_CONTENT_TYPE + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_DESCRIPTION + " TEXT," +
                    StationsDbContract.StationEntry.COLUMN_RATING + " INTEGER," +
                    StationsDbContract.StationEntry.COLUMN_IS_FAVOURITE + " INTEGER," +
                    StationsDbContract.StationEntry.COLUMN_THUMP_UP_STATUS + " INTEGER," +

                    StationsDbContract.StationEntry.COLUMN_NAME_SUBTITLE + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public StationsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    //delete station from DB
    public int DeleteStation(int station_ID) {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        //update db
        String strFilter = StationsDbContract.StationEntry._ID + " = " + String.valueOf(station_ID);
        return db.delete(TABLE_NAME, strFilter, null);
    }


    //delete station from DB
    public int UpdateImagePath(int station_ID, String imagePath) {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        //update db
        String strFilter = StationsDbContract.StationEntry._ID + " = " + String.valueOf(station_ID);
        ContentValues cnt = new ContentValues();
        cnt.put(StationsDbContract.StationEntry.COLUMN_IMAGE_PATH, imagePath);
        return db.update(TABLE_NAME, cnt, strFilter, null);
    }


    //rename station from DB
    public int RenameStation(int station_ID, String newStationName) {
        SQLiteDatabase db = this.getWritableDatabase();
        //update db
        ContentValues newValues = new ContentValues();
        newValues.put(StationsDbContract.StationEntry.COLUMN_NAME_TITLE, newStationName);
        String strFilter = StationsDbContract.StationEntry._ID + " = " + String.valueOf(station_ID);
        return db.update(TABLE_NAME, newValues, strFilter, null);
    }

    //delete station from DB
    public int ChangeRatingOfStation(int station_ID, int newRating) {
        SQLiteDatabase db = this.getWritableDatabase();
        //update db
        ContentValues newValues = new ContentValues();
        newValues.put(StationsDbContract.StationEntry.COLUMN_RATING, newRating);
        String strFilter = StationsDbContract.StationEntry._ID + " = " + String.valueOf(station_ID);
        return db.update(TABLE_NAME, newValues, strFilter, null);
    }

    //custom method
    public void FillListOfAllStations(SortedList<Station> mStationListTemp) {
        ArrayList<Station> mStationListArr = new ArrayList<>();
        FillListOfAllStationsBase(mStationListArr);
        for (int i = 0; i < mStationListArr.size(); i++) {
            mStationListTemp.add(mStationListArr.get(i));
        }
    }
    public void FillListOfAllStations(ArrayList<Station> mStationListTemp) {
        FillListOfAllStationsBase(mStationListTemp);
    }
    private void FillListOfAllStationsBase(ArrayList<Station> mStationListTemp) {
        Cursor cursor;
        //get stations from DB
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Filter results WHERE "title" = 'My Title'
        String selection = StationsDbContract.StationEntry.COLUMN_URI + " IS NOT NULL AND "
                + StationsDbContract.StationEntry.COLUMN_URI + "  != \"\"";

        String[] projection = {
                StationsDbContract.StationEntry._ID,
                StationsDbContract.StationEntry.COLUMN_UNIQUE_ID,
                StationsDbContract.StationEntry.COLUMN_NAME_TITLE,
                StationsDbContract.StationEntry.COLUMN_NAME_SUBTITLE,
                StationsDbContract.StationEntry.COLUMN_IMAGE_PATH,
                StationsDbContract.StationEntry.COLUMN_IMAGE_FILE_NAME,
                StationsDbContract.StationEntry.COLUMN_URI,
                StationsDbContract.StationEntry.COLUMN_CONTENT_TYPE,
                StationsDbContract.StationEntry.COLUMN_DESCRIPTION,
                StationsDbContract.StationEntry.COLUMN_RATING,
                StationsDbContract.StationEntry.COLUMN_CATEGORY,
                StationsDbContract.StationEntry.COLUMN_IS_FAVOURITE,
                StationsDbContract.StationEntry.COLUMN_THUMP_UP_STATUS
        };
        String sortOrder =
                StationsDbContract.StationEntry.COLUMN_UNIQUE_ID + " DESC";
        cursor = db.query(
                TABLE_NAME, // The table to query
                projection,                                 // The columns to return
                selection,                                  // The columns for the WHERE clause
                null,                                       // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                sortOrder
        );


        try {
            while (cursor.moveToNext()) {
                Station station = new Station();
                station._ID = cursor.getInt(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry._ID));
                station.UNIQUE_ID = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_UNIQUE_ID));
                station.TITLE = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_NAME_TITLE));
                station.SUBTITLE = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_NAME_SUBTITLE));
                station.IMAGE_PATH = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_IMAGE_PATH));
                station.IMAGE_FILE_NAME = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_IMAGE_FILE_NAME));
                station.URI = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_URI));
                station.CONTENT_TYPE = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_CONTENT_TYPE));
                station.DESCRIPTION = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_DESCRIPTION));
                station.RATING = cursor.getInt(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_RATING));
                station.CATEGORY = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_CATEGORY));
                station.IS_FAVOURITE = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_IS_FAVOURITE));
                station.THUMP_UP_STATUS = cursor.getString(
                        cursor.getColumnIndexOrThrow(StationsDbContract.StationEntry.COLUMN_THUMP_UP_STATUS));

                mStationListTemp.add(station);
            }
        } finally {
            cursor.close();
        }
    }

    //get stations count
    public int GetStationsCount() {
        String countQuery = "SELECT  * FROM " + StationsDbContract.StationEntry.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }

}
