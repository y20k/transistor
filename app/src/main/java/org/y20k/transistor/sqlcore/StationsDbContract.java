package org.y20k.transistor.sqlcore;

import android.provider.BaseColumns;

/**
 * Created by Tarek on 2017-03-11.
 */

public final class StationsDbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private StationsDbContract() {}

    /* Inner class that defines the table contents */
    public static class StationEntry implements BaseColumns {
        public static final String TABLE_NAME = "stations";

        public static final String COLUMN_UNIQUE_ID = "unique_id";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_SUBTITLE = "subtitle";
        public static final String COLUMN_IMAGE_PATH = "image";
        public static final String COLUMN_IMAGE_FILE_NAME = "image_file_name";
        public static final String COLUMN_SMALL_IMAGE_FILE_NAME = "small_image_file_name";
        public static final String COLUMN_URI = "uri";
        public static final String COLUMN_CONTENT_TYPE = "content_type";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_RATING = "rating";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_HTML_DESCRIPTION = "html_description";
        public static final String COLUMN_SMALL_IMAGE_URL = "small_image_URL";
        public static final String COLUMN_IS_FAVOURITE = "is_favourite";
        public static final String COLUMN_THUMP_UP_STATUS = "thump_up_status";
    }
}
