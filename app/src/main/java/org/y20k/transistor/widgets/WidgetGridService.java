package org.y20k.transistor.widgets;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class WidgetGridService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.i("TWGS", "init views factory");
        return new WidgetGridViewFactory(this.getApplicationContext(), intent);
    }
}
