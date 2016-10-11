package com.paidaki.xposed.xpogofinder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import static com.paidaki.xposed.xpogofinder.Util.*;

public class DataUpdateService extends Service {

    private static int MAX_FAILED_TRIES = 10;
    private static int INTERVAL = 1000;     // 1 Second
    private static int WARNING_TIMEOUT = 10000;     //10 Seconds

    private SharedPreferences preferences;
    private long lastUpdate;
    private String server;
    private int fails;
    private boolean isRunning;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        preferences = getSharedPreferences(COMMON_PREF, Context.MODE_WORLD_READABLE);
        server = preferences.getString(SERVER_KEY, "");
        isRunning = true;
        fails = 0;
        lastUpdate = 0;

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (isRunning) {
                    if (System.currentTimeMillis() - lastUpdate <= INTERVAL) {
                        continue;
                    }
                    try {
                        String response = Util.sendGetRequest(server, CONN_TIMEOUT);
                        HashMap<String, String> gpsData = Util.parseJSON(new JSONObject(response));

                        if (gpsData.get(LATITUDE_KEY) != null &&
                                gpsData.get(LONGITUDE_KEY) != null &&
                                gpsData.get(ALTITUDE_KEY) != null &&
                                gpsData.get(ACCURACY_KEY) != null &&
                                gpsData.get(SPEED_KEY) != null &&
                                gpsData.get(BEARING_KEY) != null) {

                            SharedPreferences.Editor prefEditor = preferences.edit();

                            prefEditor.putString(LATITUDE_KEY, gpsData.get(LATITUDE_KEY));
                            prefEditor.putString(LONGITUDE_KEY, gpsData.get(LONGITUDE_KEY));
                            prefEditor.putString(ALTITUDE_KEY, gpsData.get(ALTITUDE_KEY));
                            prefEditor.putString(ACCURACY_KEY, gpsData.get(ACCURACY_KEY));
                            prefEditor.putString(SPEED_KEY, gpsData.get(SPEED_KEY));
                            prefEditor.putString(BEARING_KEY, gpsData.get(BEARING_KEY));

                            prefEditor.apply();
                            lastUpdate = System.currentTimeMillis();
                            fails = 0;
                        }
                    } catch (IOException | JSONException e) {
                        fails++;
                        Log.e(LOG_TAG, this.getClass().getSimpleName() + " - Exception: " + e.getMessage());
                    }

                    if (fails >= MAX_FAILED_TRIES || !isRunning) {
                        isRunning = false;

                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Update service stops in " + WARNING_TIMEOUT / 1000 + " seconds", Toast.LENGTH_LONG).show();
                            }
                        });
                        try {
                            Thread.sleep(WARNING_TIMEOUT);
                        } catch (InterruptedException e) {
                            Log.e(LOG_TAG, this.getClass().getSimpleName() + " - InterruptedException: " + e.getMessage());
                        }
                        SharedPreferences.Editor prefEditor = preferences.edit();

                        prefEditor.putBoolean(ENABLED_KEY, false);
                        prefEditor.apply();

                        Intent intent = new Intent(SERVICE_INTENT);
                        intent.putExtra(SERVICE_MESSAGE_ID, true);
                        LocalBroadcastManager.getInstance(DataUpdateService.this).sendBroadcast(intent);

                        Util.clearNotification(DataUpdateService.this, NOTIFICATION_ID);
                    }
                }
                stopSelf();
            }
        }).start();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
    }
}
