package com.labs.dm.auto_tethering_lite.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    public TetheringService() {
        super("TetheringService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ServiceHelper helper = new ServiceHelper(getApplicationContext());

        if (!helper.isTetheringWiFi()) {
            helper.setWifiTethering(true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && !helper.isConnectedToInternetThroughMobile()) {
                helper.setMobileDataEnabled(true);
            }
        } else {
            helper.setWifiTethering(false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && helper.isConnectedToInternetThroughMobile()) {
                helper.setMobileDataEnabled(false);
            }
        }
    }
}
