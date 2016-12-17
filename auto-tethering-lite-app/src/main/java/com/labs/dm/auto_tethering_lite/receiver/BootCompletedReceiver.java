package com.labs.dm.auto_tethering_lite.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering_lite.service.TetheringService;

import static com.labs.dm.auto_tethering_lite.AppProperties.AUTO_START;

/**
 * Main responsibility of this receiver is to start TetheringService instance just after boot has been completed
 * <p>
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(AUTO_START, false)) {
            Intent serviceIntent = new Intent(context, TetheringService.class);
            serviceIntent.putExtra("boot", true);
            context.startService(serviceIntent);
        }
    }
}
