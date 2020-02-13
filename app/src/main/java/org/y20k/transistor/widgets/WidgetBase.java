package org.y20k.transistor.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public abstract class WidgetBase extends AppWidgetProvider {

    protected abstract int getColumns();

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.i("TWB", "updateAppWidget : " + appWidgetId);

        // Construct the RemoteViews object
        RemoteViews rv = new RemoteViews(context.getPackageName(), appWidgetManager.getAppWidgetInfo(appWidgetId).initialLayout);

        Intent remoteAdapterIntent = new Intent(context, WidgetGridService.class);
        remoteAdapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        remoteAdapterIntent.putExtra(WidgetGridViewFactory.COLUMNS, getColumns());
        remoteAdapterIntent.setData(Uri.parse(remoteAdapterIntent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.widgetGrid, remoteAdapterIntent);

        rv.setEmptyView(R.id.widgetGrid, R.id.empty_view);

        Intent playIntent = new Intent(context, WidgetBase.class);
        playIntent.setAction(TransistorKeys.ACTION_WIDGET);
        playIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        playIntent.setData(Uri.parse(playIntent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setPendingIntentTemplate(R.id.widgetGrid, PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Intent launchTransistorIntent = new Intent(context, MainActivity.class);
        launchTransistorIntent.setAction(Intent.ACTION_MAIN);
        rv.setOnClickPendingIntent(R.id.widgetLaunchAppButton, PendingIntent.getActivity(context, 0, launchTransistorIntent,
                                                                                         PendingIntent.FLAG_UPDATE_CURRENT));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i("TWB", "onUpdate : " + Arrays.toString(appWidgetIds));

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.i("TWB", "onAppWidgetOptionsChanged");
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

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent.getAction().equals(TransistorKeys.ACTION_WIDGET)) {
            String id = intent.getStringExtra(TransistorKeys.EXTRA_STATION_ID);
            if(id == null) {
                Log.i("TWB", "no station ID");
                return;
            }
            List<Station> stationsList = StationListHelper.loadStationListFromStorage(context);
            Station station = null;
            for(Station s : stationsList) {
                if(s.getStationId().equals(id)) {
                    station = s;
                    break;
                }
            }
            Intent playPauseIntent = new Intent(context, PlayerService.class);
            if(station.getPlaybackState() == TransistorKeys.PLAYBACK_STATE_STOPPED)
                playPauseIntent.setAction(TransistorKeys.ACTION_PLAY);
            else
                playPauseIntent.setAction(TransistorKeys.ACTION_STOP);
            playPauseIntent.putExtra(TransistorKeys.EXTRA_STATION, station);

            try {
                PendingIntent.getService(context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT).send();
                //context.startService(playPauseIntent);
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}

