package com.paidaki.xposed.xpogofinder;

import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import static com.paidaki.xposed.xpogofinder.Util.*;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private CheckBox cbEnabled;
    private EditText etServer;
    private Button btnSave;
    private EnabledListener enabledListener;
    private BroadcastReceiver mMessageReceiver;

    private class ServiceListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean done = intent.getBooleanExtra(SERVICE_MESSAGE_ID, false);

            if (done) {
                cbEnabled.setOnCheckedChangeListener(null);
                cbEnabled.setChecked(false);
                cbEnabled.setOnCheckedChangeListener(enabledListener);
            }
        }
    }

    private class ServerCheckJob extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String url = urls[0];
            String response = null;

            try {
                response = sendGetRequest(url, CONN_TIMEOUT);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            handleServerResponse(response);
        }
    }

    private class EnabledListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked) {
                cbEnabled.setChecked(false);
                cbEnabled.setEnabled(false);
                new ServerCheckJob().execute(preferences.getString(SERVER_KEY, ""));
                return;
            }
            stopUpdateService();
        }
    }

    private void stopUpdateService() {
        SharedPreferences.Editor prefEditor = preferences.edit();

        prefEditor.putBoolean(ENABLED_KEY, false);
        prefEditor.apply();
        Util.clearNotification(MainActivity.this, NOTIFICATION_ID);
        Toast.makeText(getApplicationContext(), "Update service stopped", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(MainActivity.this, DataUpdateService.class);
        stopService(intent);
    }

    private void handleServerResponse(String response) {
        boolean success = false;

        if (response != null) {
            try {
                HashMap<String, String> gpsData = parseJSON(new JSONObject(response));

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

                    prefEditor.putBoolean(ENABLED_KEY, true);
                    prefEditor.apply();

                    success = true;
                    Intent intent = new Intent(this, DataUpdateService.class);
                    startService(intent);
                    makeNotification(this, NOTIFICATION_ID);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
        String toastText = success ? "Update service started" : "Server error occurred";
        Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG).show();

        cbEnabled.setOnCheckedChangeListener(null);
        cbEnabled.setChecked(success);
        cbEnabled.setOnCheckedChangeListener(enabledListener);
        cbEnabled.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(SERVICE_INTENT));
        reloadSettings();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onStop();
    }

    private void initialize() {
        preferences = getSharedPreferences(COMMON_PREF, Context.MODE_WORLD_READABLE);
        enabledListener = new EnabledListener();
        cbEnabled = (CheckBox) findViewById(R.id.cbEnabled);
        etServer = (EditText) findViewById(R.id.etServer);
        btnSave = (Button) findViewById(R.id.btnSave);
        mMessageReceiver = new ServiceListener();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        addListeners();
    }

    private void reloadSettings() {
        boolean serverIsSet = preferences.contains(SERVER_KEY);

        if (!serverIsSet) {
            SharedPreferences.Editor prefEditor = preferences.edit();
            prefEditor.putString(SERVER_KEY, DEFAULT_SERVER);
            prefEditor.apply();
        }
        cbEnabled.setOnCheckedChangeListener(null);
        cbEnabled.setChecked(preferences.getBoolean(ENABLED_KEY, false));
        cbEnabled.setOnCheckedChangeListener(enabledListener);
        etServer.setHint(preferences.getString(SERVER_KEY, DEFAULT_SERVER));
    }

    private void addListeners() {
        cbEnabled.setOnCheckedChangeListener(enabledListener);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = String.valueOf(etServer.getText()).trim();
                etServer.setText("");

                if (!text.isEmpty()) {
                    SharedPreferences.Editor prefEditor = preferences.edit();

                    prefEditor.putString(SERVER_KEY, text);
                    prefEditor.apply();
                    reloadSettings();
                }
            }
        });
    }
}
