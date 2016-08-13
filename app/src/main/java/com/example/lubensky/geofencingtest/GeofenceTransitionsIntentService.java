package com.example.lubensky.geofencingtest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by lubensky on 10.08.16.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";

    private LocalBroadcastManager broadcaster;

    public GeofenceTransitionsIntentService() {
        super( TAG);
    }

    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance( this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent( intent);

        if( geofencingEvent.hasError()) {
            Log.e( TAG, "Geofencing error");

            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Intent geofencingIntent = new Intent( MainActivity.GEOFENCE_TRANSITION_ACTION);

        geofencingIntent.putExtra( MainActivity.GEOFENCE_TRANSITION, geofenceTransition);
        broadcaster.sendBroadcast( geofencingIntent);

        Vibrator v = ( Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        switch( geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                v.vibrate( MainActivity.VIBRATE_DURATION_ENTER);
                Log.i( TAG, "ENTER");
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Log.i( TAG, "DWELL");
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.i( TAG, "EXIT");
                v.vibrate( MainActivity.VIBRATE_DURATION_EXIT);
                break;
            default:
                Log.e( TAG, "unknown geofence transition");
        }
    }
}
