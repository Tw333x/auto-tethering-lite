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

    public TetheringService() {
        super("TetheringService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        new TetheringAsyncTask().doInBackground();
    }

    private class TetheringAsyncTask extends AsyncTask<Boolean, Void, Void> {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        @Override
        protected Void doInBackground(Boolean... params) {
            ServiceHelper helper = new ServiceHelper(getApplicationContext());
            if (!helper.isTetheringWiFi()) {
                prefs.edit().putBoolean(INTERNET_ON, helper.isConnectedToInternetThroughMobile()).apply();
                prefs.edit().putBoolean(WIFI_ON, helper.isConnectedToInternetThroughWiFi()).apply();
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
                if (prefs.getBoolean(WIFI_ON, false)) {
                    helper.enableWifi();
                }

                prefs.edit().remove(INTERNET_ON).apply();
                prefs.edit().remove(WIFI_ON).apply();
            }
            return null;
        }
    }
}
