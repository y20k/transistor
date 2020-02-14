package org.y20k.transistor.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import org.y20k.transistor.PlayerServiceGateway;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of App Widget functionality.
 */
public class Widget1C extends AppWidgetProvider {
    private static final Map<String, Bitmap> stationsBitmaps = Collections.synchronizedMap(new HashMap<>());

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        LogHelper.d("TWB", "updateAppWidget : " + appWidgetId);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String id = settings.getString(TransistorKeys.PREF_WIDGET_STATION_ID + "_" + appWidgetId, null);

        Station station = null;
        if(id != null)
        {
            List<Station> stationsList = StationListHelper.loadStationListFromStorage(context);
            for (Station s : stationsList) {
                if(s.getStationId().equals(id)) {
                    station = s;
                    break;
                }
            }
        }

        // Construct the RemoteViews object
        RemoteViews rv = new RemoteViews(context.getPackageName(), appWidgetManager.getAppWidgetInfo(appWidgetId).initialLayout);
        if(station != null) {
            boolean playing = station.getPlaybackState() != TransistorKeys.PLAYBACK_STATE_STOPPED;

            Bitmap bitmap = stationsBitmaps.get(id);
            if(bitmap == null) {
                ImageHelper imageHelper = new ImageHelper(station, context);
                bitmap = imageHelper.createSquareImage(512, false);
                stationsBitmaps.put(id, bitmap);
            }

            rv.setImageViewBitmap(R.id.widgetItemStationIcon, bitmap);
            rv.setViewVisibility(R.id.widgetItemPlaying, playing ? View.VISIBLE : View.GONE);
            rv.setTextViewText(R.id.widgetItemTitle, station.getStationName());
        }

        Intent playIntent = new Intent(context, PlayerServiceGateway.class);
        playIntent.setAction(TransistorKeys.ACTION_WIDGET);
        playIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        playIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, id);
        playIntent.setData(Uri.parse(playIntent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setOnClickPendingIntent(R.id.widgetItemStationIcon, PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        rv.setOnClickPendingIntent(R.id.widgetItemTitle, PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        rv.setOnClickPendingIntent(R.id.widgetItemPlaying, PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        LogHelper.d("TWB", "onUpdate : " + Arrays.toString(appWidgetIds));

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        LogHelper.d("TWB", "onAppWidgetOptionsChanged");
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetGrid);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}

