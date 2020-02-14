package org.y20k.transistor.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerServiceGateway;
import org.y20k.transistor.R;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.Arrays;

/**
 * Implementation of App Widget functionality.
 */
public abstract class WidgetBase extends AppWidgetProvider {

    protected abstract int getColumns();

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        LogHelper.d("TWB", "updateAppWidget : " + appWidgetId);

        // Construct the RemoteViews object
        RemoteViews rv = new RemoteViews(context.getPackageName(), appWidgetManager.getAppWidgetInfo(appWidgetId).initialLayout);

        Intent remoteAdapterIntent = new Intent(context, WidgetGridService.class);
        remoteAdapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        remoteAdapterIntent.putExtra(WidgetGridViewFactory.COLUMNS, getColumns());
        remoteAdapterIntent.setData(Uri.parse(remoteAdapterIntent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.widgetGrid, remoteAdapterIntent);

        rv.setEmptyView(R.id.widgetGrid, R.id.empty_view);

        Intent playIntent = new Intent(context, PlayerServiceGateway.class);
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

