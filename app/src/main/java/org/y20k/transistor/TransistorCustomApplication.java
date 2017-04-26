package org.y20k.transistor;

import android.app.Application;
import android.content.res.Configuration;

import com.facebook.drawee.backends.pipeline.Fresco;

import org.y20k.transistor.helpers.SingletonProperties;

/**
 * Created by Tarek on 2017-03-30.
 */

public class TransistorCustomApplication extends Application {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        // Required initialization logic here!
        Fresco.initialize(this);

        //initialize SingletonProperties
        SingletonProperties.setContextToApplicationContext(getApplicationContext());
    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
