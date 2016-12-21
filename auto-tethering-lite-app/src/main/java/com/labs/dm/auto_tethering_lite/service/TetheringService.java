package com.labs.dm.auto_tethering_lite.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering_lite.R;
import com.labs.dm.auto_tethering_lite.activity.ConfigurationActivity;

import static com.labs.dm.auto_tethering_lite.AppProperties.EDIT_MODE;
import static com.labs.dm.auto_tethering_lite.AppProperties.INTERNET_ON;
import static com.labs.dm.auto_tethering_lite.AppProperties.NOTIFICATION_ID;
import static com.labs.dm.auto_tethering_lite.AppProperties.WIFI_ON;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private static final String TAG = "TetheringService";
    private SharedPreferences prefs;
    private ServiceHelper helper;

    public TetheringService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        helper = new ServiceHelper(getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        new TetheringAsyncTask().doInBackground();
        if (intent.getBooleanExtra("boot", false)) {
            push();
        }
    }

    private class TetheringAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            ServiceHelper helper = new ServiceHelper(getApplicationContext());
            if (!helper.isTetheringWiFi()) {
                onConnect();
                helper.setWifiTethering(true);
                if (!helper.isConnectedToInternetThroughMobile()) {
                    helper.setMobileDataEnabled(true);
                }
            } else {
                helper.setWifiTethering(false);
                boolean shouldDisconnectInternet = !prefs.getBoolean(INTERNET_ON, false);
                if (shouldDisconnectInternet && helper.isConnectedToInternetThroughMobile()) {
                    helper.setMobileDataEnabled(false);
                }
                onDisconnect();
            }
            return null;
        }
    }

    private void onConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putBoolean(INTERNET_ON, helper.isConnectedToInternetThroughMobile()).apply();
            prefs.edit().putBoolean(WIFI_ON, helper.isConnectedToInternetThroughWiFi()).apply();
        } else {
            prefs.edit().putBoolean(INTERNET_ON, helper.isConnectedToInternetThroughMobile()).commit();
            prefs.edit().putBoolean(WIFI_ON, helper.isConnectedToInternetThroughWiFi()).commit();
        }
    }

    private void onDisconnect() {
        if (prefs.getBoolean(WIFI_ON, false)) {
            helper.enableWifi();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().remove(INTERNET_ON).apply();
            prefs.edit().remove(WIFI_ON).apply();
        } else {
            prefs.edit().remove(INTERNET_ON).commit();
            prefs.edit().remove(WIFI_ON).commit();
        }
    }

    private void push() {
        final int notificationId = 1234;
        String caption = getString(R.string.push_msg);
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
        intent.putExtra(EDIT_MODE, true);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setTicker(caption)
                    .setContentText(caption)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.app)
                    .setStyle(new Notification.BigTextStyle().bigText(caption).setBigContentTitle(getText(R.string.app_name)));

            notify = builder.build();
        } else {
            notify = new Notification(R.drawable.app, caption, System.currentTimeMillis());
            notify.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), caption, pendingIntent);
        }
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        notificationManager.notify(notificationId, notify);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notificationManager.cancel(notificationId);
            }
        }, 60000);
    }

}
