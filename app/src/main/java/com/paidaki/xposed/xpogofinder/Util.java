package com.paidaki.xposed.xpogofinder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

class Util {

    static final String LATITUDE_KEY = "latitude";
    static final String LONGITUDE_KEY = "longitude";
    static final String ALTITUDE_KEY = "altitude";
    static final String ACCURACY_KEY = "accuracy";
    static final String SPEED_KEY = "speed";
    static final String BEARING_KEY = "bearing";
    static final String LOG_TAG = "XPoGoFinder";
    static final String COMMON_PREF = "xpogofinder_settings";
    static final String ENABLED_KEY = "enabled";
    static final String SERVER_KEY = "server";
    static final String SERVICE_MESSAGE_ID = "serviceCompleted";
    static final String SERVICE_INTENT = "serviceIntent";
    static final String DEFAULT_SERVER = "http://192.168.1.66:8066";
    static final int NOTIFICATION_ID = 9384892;
    static int CONN_TIMEOUT = 2000;     //2 Seconds

    private static final String GET_METHOD = "GET";
    private static final String USER_AGENT_PROPERTY = "User-Agent";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int STATUS_OK = 200;

    private Util() {
    }

    static String sendGetRequest(String url, int timeout) throws IOException {
        String response = null;
        URL server = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) server.openConnection();

        conn.setRequestMethod(GET_METHOD);
        conn.setRequestProperty(USER_AGENT_PROPERTY, USER_AGENT);
        if (timeout > 0) conn.setConnectTimeout(timeout);

        int responseCode = conn.getResponseCode();

        if (responseCode == STATUS_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder buffer = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                buffer.append(inputLine);
            }
            in.close();
            response = String.valueOf(buffer);
        }
        return response;
    }

    static HashMap<String, String> parseJSON(JSONObject json) throws JSONException {
        HashMap<String, String> data = new HashMap<>();

        JSONArray results = json.getJSONArray("results");
        JSONObject firstResult = results.getJSONObject(0);
        JSONObject location = firstResult.getJSONObject("location");

        data.put(LATITUDE_KEY, location.getString(LATITUDE_KEY));
        data.put(LONGITUDE_KEY, location.getString(LONGITUDE_KEY));
        data.put(ALTITUDE_KEY, firstResult.getString(ALTITUDE_KEY));
        data.put(SPEED_KEY, firstResult.getString(SPEED_KEY));
        data.put(BEARING_KEY, firstResult.getString(BEARING_KEY));
        data.put(ACCURACY_KEY, firstResult.getString(ACCURACY_KEY));

        return data;
    }

    static void makeNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle("XPoGoFinder")
                .setContentText("XPoGoFinder is running...")
                .setSmallIcon(getNotificationIcon())
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent);
        Notification notification;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(notificationId, notification);
    }

    static void clearNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    private static int getNotificationIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.mipmap.notification_icon : R.mipmap.ic_launcher;
    }
}
