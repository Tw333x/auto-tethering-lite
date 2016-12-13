package com.labs.dm.auto_tethering_lite.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.labs.dm.auto_tethering_lite.service.TetheringService;

/**
 * Main responsibility of this receiver is to start TetheringService instance just after boot has been completed
 * <p>
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, TetheringService.class);
        context.startService(serviceIntent);
    }
}