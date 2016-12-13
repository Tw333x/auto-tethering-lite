package com.labs.dm.auto_tethering_lite;

import android.content.Intent;
import android.os.Bundle;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

/**
 * Created by Daniel Mroczka
 */
public class Utils {

    public static int getWidgetId(Intent intent) {
        Bundle extras = intent.getExtras();
        int appWidgetId = INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
        }
        return appWidgetId;
    }
}
