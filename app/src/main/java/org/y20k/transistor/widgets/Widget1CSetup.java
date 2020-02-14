package org.y20k.transistor.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class Widget1CSetup extends AppCompatActivity {

    private GridView m_gridView;
    private FloatingActionButton m_saveBtn;

    private List<Station> m_stationsList;
    private Map<String, Long> m_stationsIds = new HashMap<>();
    private long m_idGenerator = System.currentTimeMillis();

    private String m_selection = null;

    private int m_itemSize;

    private int m_appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget1csetup);
        m_gridView = findViewById(R.id.widget1CSetupGrid);
        m_saveBtn = findViewById(R.id.widget1CSetupSave);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            m_appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        m_stationsList = StationListHelper.loadStationListFromStorage(this);
        m_itemSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 15, getResources().getDisplayMetrics());
        int nbCols = getResources().getDisplayMetrics().widthPixels / (m_itemSize + 16);
        m_gridView.setNumColumns(nbCols);
        m_itemSize = getResources().getDisplayMetrics().widthPixels / nbCols - 16;

        m_gridView.setAdapter(new StationsAdapter());

        m_saveBtn.setEnabled(false);
        m_saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TransistorKeys.PREF_WIDGET_STATION_ID + "_" + m_appWidgetId, m_selection);
        editor.apply();

        Widget1C.updateAppWidget(this, AppWidgetManager.getInstance(this), m_appWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, m_appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }


    private class StationsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return m_stationsList.size();
        }

        @Override
        public Object getItem(int i) {
            return m_stationsList.get(i);
        }

        @Override
        public long getItemId(int i) {
            Station station = m_stationsList.get(i);

            Long id = m_stationsIds.get(station.getStationId());
            if(id == null) {
                id = m_idGenerator++;
                m_stationsIds.put(station.getStationId(), id);
            }

            return id;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view == null)
                view = LayoutInflater.from(Widget1CSetup.this).inflate(R.layout.widget_item, null);

            final Station station = m_stationsList.get(i);

            ImageHelper imageHelper = new ImageHelper(station, Widget1CSetup.this);
            boolean selected = m_selection != null && m_selection.equals(station.getStationId());
            ((ImageView)view.findViewById(R.id.widgetItemStationIcon)).setImageBitmap(imageHelper.createSquareImage(m_itemSize, false));
            view.findViewById(R.id.widgetItemPlaying).setVisibility(selected ? View.VISIBLE : View.GONE);
            ((TextView)view.findViewById(R.id.widgetItemTitle)).setText(station.getStationName());
            view.setPadding(8, 8, 8, 8);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    m_selection = station.getStationId();
                    m_saveBtn.setEnabled(true);
                    notifyDataSetInvalidated();
                }
            });

            return view;
        }
    }
}
