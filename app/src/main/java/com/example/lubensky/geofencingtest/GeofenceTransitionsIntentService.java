package com.example.lubensky.geofencingtest;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by lubensky on 10.08.16.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";

    public GeofenceTransitionsIntentService() {
        super( TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
