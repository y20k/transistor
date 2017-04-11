package org.y20k.transistor.helpers;

/**
 * Created by Tarek on 2017-04-05.
 */

public class SingletonProperties {
    private static SingletonProperties mInstance= null;

    public PlaybackStatus CurrentSelectedStation_Playback_Status = PlaybackStatus.STOPPED;
    public String CurrentStation_ID = null;
    public String CurrentSelectedStation_ID = null;
    public String LastRunningStation_ID = null;

    protected SingletonProperties(){}

    public static synchronized SingletonProperties getInstance(){
        if(null == mInstance){
            mInstance = new SingletonProperties();
        }
        return mInstance;
    }

}