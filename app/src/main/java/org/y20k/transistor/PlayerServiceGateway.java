package org.y20k.transistor;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.List;

public class PlayerServiceGateway extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(TransistorKeys.ACTION_WIDGET)) {
            String id = intent.getStringExtra(TransistorKeys.EXTRA_STATION_ID);
            if(id == null) {
                Log.i("TWB", "no station ID");
                return;
            }
            else
                Log.i("TWB", "touched station " + id);
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
            playPauseIntent.setData(Uri.parse(playPauseIntent.toUri(Intent.URI_INTENT_SCHEME)));

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(playPauseIntent);
                else
                    context.startService(playPauseIntent);
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }
}
