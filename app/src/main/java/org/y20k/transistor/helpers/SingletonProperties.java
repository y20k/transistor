package org.y20k.transistor.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Singleton Class holds application session variables (properties)
 */
public class SingletonProperties {
    private static SingletonProperties mInstance = null;

    public PlaybackStatus CurrentSelectedStation_Playback_Status = PlaybackStatus.STOPPED;
    public String CurrentStation_ID = null;
    public long CurrentSelectedStation_ID = -1;

    private static Context mContext;

    protected SingletonProperties() {
    }

    /**
     *This function supposed to be called only from application level onCreate, one time call per application open
     */
    public static void setContextToApplicationContext(Context context) {
        if (null == mInstance) {
            mContext = context.getApplicationContext();
            mInstance = new SingletonProperties();
        }
    }

    /**
     *This function return instance of the SingletonProperties application level object
     */
    public static synchronized SingletonProperties getInstance() {
        if (null == mInstance) {
            mInstance = new SingletonProperties();
        }
        return mInstance;
    }

    public long getLastRunningStation_ID() {
        //get from application setting/ preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        return settings.getLong(TransistorKeys.PREF_STATION_ID_LAST, -1);
    }

    /**
     * return true if status is loading or playing
     * @return true if status is loading or playing
     */
    public boolean getIsPlayback() {
        return (CurrentSelectedStation_Playback_Status==PlaybackStatus.LOADING || CurrentSelectedStation_Playback_Status==PlaybackStatus.PLAYING);
    }
    public void setLastRunningStation_ID(long lastRunningStation_ID) {
        //add to application setting/ preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(TransistorKeys.PREF_STATION_ID_LAST, lastRunningStation_ID);
        editor.apply();
    }
}