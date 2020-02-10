package org.y20k.transistor.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WidgetGridViewFactory implements RemoteViewsService.RemoteViewsFactory {
    private List<Station> m_stations = new ArrayList<>();
    private Context m_context;
    private int m_appWidgetId;
    private int m_itemSize;
    private Map<String,Long> m_stationsIds = new HashMap<>();

    private long idProvider = System.currentTimeMillis();

    WidgetGridViewFactory(Context context, Intent intent) {
        m_context = context;
        m_appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        m_stations = StationListHelper.loadStationListFromStorage(this.m_context);
        LogHelper.d("TWV", "Stations count : " + m_stations.size());
        m_itemSize = 192; // todo
    }

    @Override
    public void onDataSetChanged() {
        LogHelper.d("TWV", "updating widget content");
        m_stations = StationListHelper.loadStationListFromStorage(this.m_context);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return m_stations.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        Station station = this.m_stations.get(i);
        LogHelper.d("TWV",
                    "Create view for station : [" + i + "]" + station.getStationName() + "(" + station.getStationId() + ")");
        boolean playing = station.getPlaybackState() != TransistorKeys.PLAYBACK_STATE_STOPPED;
        RemoteViews rv = new RemoteViews(this.m_context.getPackageName(), R.layout.widget_item);
        ImageHelper imageHelper = new ImageHelper(station, this.m_context);
        rv.setImageViewBitmap(R.id.widgetItemStationIcon, imageHelper.createSquareImage(this.m_itemSize, false));
        rv.setViewVisibility(R.id.widgetItemPlaying, playing ? View.VISIBLE : View.GONE);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, station.getStationId());
        rv.setOnClickFillInIntent(R.id.widgetItemStationIcon, fillInIntent);
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int i) {
        Station station = this.m_stations.get(i);
        Long id = this.m_stationsIds.get(station.getStationId());
        if(id == null) {
            id = idProvider++;
            this.m_stationsIds.put(station.getStationId(), id);
        }
        return id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
