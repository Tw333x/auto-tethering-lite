package com.labs.dm.auto_tethering_lite.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering_lite.activity.ConfigurationActivity;
import com.labs.dm.auto_tethering_lite.service.ServiceHelper;
import com.labs.dm.auto_tethering_lite.service.TetheringService;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.content.Context.MODE_PRIVATE;
import static com.labs.dm.auto_tethering_lite.AppProperties.EDIT_MODE;
import static com.labs.dm.auto_tethering_lite.Utils.getWidgetId;

/**
 * Created by Daniel Mroczka on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    private static final int DOUBLE_CLICK_DELAY = 800;
    private static final String TAG = "WidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        ServiceHelper helper = new ServiceHelper(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isTetheringWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);
            Intent intent = new Intent(context, getClass());
            intent.setAction("widget.click");
            intent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        context.getSharedPreferences("widget", 0).edit().putInt("clicks", 0).apply();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals("widget.click")) {
            int clickCount = context.getSharedPreferences("widget", MODE_PRIVATE).getInt("clicks", 0);
            context.getSharedPreferences("widget", MODE_PRIVATE).edit().putInt("clicks", ++clickCount).apply();

            final Handler handler = new Handler() {
                public void handleMessage(Message msg) {
                    int clickCount = context.getSharedPreferences("widget", MODE_PRIVATE).getInt("clicks", 0);
                    Log.i(TAG, "ClickCount: " + clickCount);

                    if (clickCount > 1) {
                        Intent i = new Intent(context, ConfigurationActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra(EXTRA_APPWIDGET_ID, getWidgetId(intent));
                        i.putExtra(EDIT_MODE, true);
                        context.startActivity(i);
                    } else {
                        Intent serviceIntent = new Intent(context, TetheringService.class);
                        context.startService(serviceIntent);
                    }

                    context.getSharedPreferences("widget", MODE_PRIVATE).edit().putInt("clicks", 0).apply();
                }
            };

            if (clickCount == 1) new Thread() {
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            wait(DOUBLE_CLICK_DELAY);
                        }
                        handler.sendEmptyMessage(0);
                    } catch (InterruptedException ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }.start();
        }
        super.onReceive(context, intent);
    }
}