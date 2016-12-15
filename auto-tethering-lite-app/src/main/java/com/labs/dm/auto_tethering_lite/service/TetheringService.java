package com.labs.dm.auto_tethering_lite.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import static com.labs.dm.auto_tethering_lite.AppProperties.INTERNET_ON;
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
        prefs.edit().putBoolean(INTERNET_ON, helper.isConnectedToInternetThroughMobile()).apply();
        prefs.edit().putBoolean(WIFI_ON, helper.isConnectedToInternetThroughWiFi()).apply();
    }

    private void onDisconnect() {
        if (prefs.getBoolean(WIFI_ON, false)) {
            helper.enableWifi();
        }

        prefs.edit().remove(INTERNET_ON).apply();
        prefs.edit().remove(WIFI_ON).apply();

    }
}
