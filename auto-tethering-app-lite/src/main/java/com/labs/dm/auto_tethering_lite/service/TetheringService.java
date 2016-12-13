package com.labs.dm.auto_tethering_lite.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.labs.dm.auto_tethering_lite.AppProperties;
import com.labs.dm.auto_tethering_lite.R;
import com.labs.dm.auto_tethering_lite.TetherIntents;
import com.labs.dm.auto_tethering_lite.Utils;
import com.labs.dm.auto_tethering_lite.activity.MainActivity;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;
import static com.labs.dm.auto_tethering_lite.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering_lite.AppProperties.ACTIVATE_KEEP_SERVICE;
import static com.labs.dm.auto_tethering_lite.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering_lite.AppProperties.RETURN_TO_PREV_STATE;
import static com.labs.dm.auto_tethering_lite.TetherIntents.CHANGE_NETWORK_STATE;
import static com.labs.dm.auto_tethering_lite.TetherIntents.EXIT;
import static com.labs.dm.auto_tethering_lite.TetherIntents.RESUME;
import static com.labs.dm.auto_tethering_lite.TetherIntents.TETHERING;
import static com.labs.dm.auto_tethering_lite.TetherIntents.WIDGET;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private static final String TAG = "TetheringService";
    private static final int CHECK_DELAY = 5;
    private static final int NOTIFICATION_ID = 1234;

    private boolean forceOff = false, forceOn = false;
    private boolean changeMobileState;
    private boolean initial3GStatus, initialTetheredStatus, initialWifiStatus;
    private boolean blockForceInternet;
    private boolean runFromActivity;
    private boolean flag = true;
    private boolean internetOn;
    private long lastAccess = getTime().getTimeInMillis();
    private BroadcastReceiver receiver;
    private String lastNotificationTickerText;
    private SharedPreferences prefs;
    private ServiceHelper serviceHelper;
    private Notification notification;

    private enum Status {
        DEFAULT
    }

    private Status status = Status.DEFAULT;

    private final String[] invents = {TETHERING, WIDGET, RESUME, EXIT,
            CHANGE_NETWORK_STATE, TetherIntents.TETHER_ON, TetherIntents.TETHER_OFF, TetherIntents.INTERNET_ON, TetherIntents.INTERNET_OFF};

    public TetheringService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceHelper = new ServiceHelper(getApplicationContext());
        init();
        registerReceivers();
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        for (String invent : invents) {
            filter.addAction(invent);
        }
        receiver = new MyBroadcastReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        runAsForeground();
    }

    private void init() {
        initial3GStatus = serviceHelper.isConnectedToInternetThroughMobile();
        initialTetheredStatus = serviceHelper.isTetheringWiFi();
        initialWifiStatus = serviceHelper.isConnectedToInternetThroughWiFi();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        runFromActivity = intent.getBooleanExtra("runFromActivity", false);
        int state = intent.getIntExtra("state", -1);
        if (state == 1) {
            execute(ServiceAction.TETHER_OFF);
        } else if (state == 0) {
            execute(ServiceAction.TETHER_ON);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (isServiceActivated()) {
            showNotification(getString(R.string.service_started), getNotificationIcon());

            onService();
        }

        while (flag) {
            try {
                boolean connected3G = serviceHelper.isConnectedToInternetThroughMobile();
                boolean tethered = serviceHelper.isTetheringWiFi();

                if (!(forceOff || forceOn) && (isServiceActivated() || keepService())) {
                    if (enabled()) {

                    } else {

                        if (tethered || connected3G) {
                        }
                    }
                } else if (forceOn) {
                    if (!blockForceInternet && !serviceHelper.isConnectedToInternetThroughMobile()) {
                        execute(ServiceAction.INTERNET_ON);
                    }
                    if (!serviceHelper.isTetheringWiFi()) {
                        execute(ServiceAction.TETHER_ON);
                    }
                }
                if (!keepService()) {
                    flag = false;
                }

                TimeUnit.SECONDS.sleep(CHECK_DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


    private boolean enabled() {
        return true;
    }

    private boolean keepService() {
        return prefs.getBoolean(AppProperties.ACTIVATE_KEEP_SERVICE, true);
    }

    /**
     * Turns tethering in separate thread.
     *
     * @param state
     * @return true if changed the state or false if not
     */
    private boolean tetheringAsyncTask(boolean state) {
        if (serviceHelper.isTetheringWiFi() == state) {
            return false;
        }

        if (Utils.isAirplaneModeOn(getApplicationContext())) {
            showNotification("Tethering blocked due to activated Airplane Mode", getNotificationIcon());
            return false;
        }

        if (state && prefs.getBoolean("wifi.connected.block.tethering", false) && serviceHelper.isConnectedToInternetThroughWiFi()) {
            showNotification("Tethering blocked due to active connection to WiFi Network", getNotificationIcon());
            return false;
        }

        new TurnOnTetheringAsyncTask().doInBackground(state);
        return true;
    }

    private boolean isServiceActivated() {
        return runFromActivity || prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false);
    }


    private Calendar getTime() {
        return Calendar.getInstance();
    }

    private void updateLastAccess() {
        lastAccess = getTime().getTimeInMillis();
    }

    /**
     * Turns mobile data in separate thread.
     *
     * @param state
     * @return true if changed the state or false if not
     */
    private boolean internetAsyncTask(boolean state) {
        if (serviceHelper.isConnectedToInternetThroughMobile() == state) {
            return false;
        }

        if (state && Utils.isAirplaneModeOn(getApplicationContext())) {
            return false;
        }

        new TurnOn3GAsyncTask().doInBackground(state);
        return true;
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(ACTIVATE_TETHERING, false);
    }

    private boolean isActivated3G() {
        return prefs.getBoolean(ACTIVATE_3G, false);
    }

    private class TurnOn3GAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            internetOn = params[0];
            serviceHelper.setMobileDataEnabled(params[0]);
            return null;
        }
    }

    private class TurnOnTetheringAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            updateLastAccess();
            serviceHelper.setWifiTethering(params[0]);
            return null;
        }
    }

    private void runAsForeground() {
        if (notification == null) {
            this.notification = buildNotification(getString(R.string.service_started));
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String caption) {
        return buildNotification(caption, getNotificationIcon());
    }

    private int getNotificationIcon() {
        boolean tethering = serviceHelper.isTetheringWiFi();
        boolean internet = serviceHelper.isConnectedToInternetThroughMobile();

        if (tethering && internet) {
            return R.drawable.app_on;
        } else if (tethering || internet) {
            return R.drawable.app_yellow;
        } else {
            return R.drawable.app_off;
        }
    }

    private Notification buildNotification(String caption, int icon) {
        lastNotificationTickerText = caption;
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exitIntent = new Intent(EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setTicker(caption)
                    .setContentText(caption)
                    .setContentTitle(getText(R.string.app_name))
                    .setOngoing(true)
                    .setSmallIcon(icon)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setStyle(new Notification.BigTextStyle().bigText(caption).setBigContentTitle(getText(R.string.app_name)));

            Intent onIntent = new Intent(TETHERING);
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(this, 0, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            int drawable = R.drawable.ic_service24;
            String ticker = "Service ON";

            if (forceOff && !forceOn) {
                drawable = R.drawable.ic_wifi_off;
                ticker = "Tethering OFF";
            } else if (forceOn && !forceOff) {
                drawable = R.drawable.ic_wifi_on;
                ticker = "Tethering ON";
            }

            builder.addAction(drawable, ticker, onPendingIntent);

            builder.addAction(R.drawable.ic_exit24, "Exit", exitPendingIntent);
            notify = builder.build();
        } else {
            notify = new Notification(icon, caption, System.currentTimeMillis());
            notify.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), caption, pendingIntent);
        }
        return notify;
    }

    private void updateNotification() {
        showNotification(lastNotificationTickerText, getNotificationIcon());
    }

    private void showNotification(String body, int icon) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = buildNotification(body, icon);
        notificationManager.cancelAll();
        notificationManager.notify(NOTIFICATION_ID, notification);
        Log.i(TAG, "Notification: " + body);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        flag = false;
        revertToInitialState();
        stopForeground(true);
        stopSelf();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void revertToInitialState() {
        if (prefs.getBoolean(RETURN_TO_PREV_STATE, false) && prefs.getBoolean(ACTIVATE_KEEP_SERVICE, true)) {
            new TurnOn3GAsyncTask().doInBackground(initial3GStatus);
            new TurnOnTetheringAsyncTask().doInBackground(initialTetheredStatus);
        }
        if (initialWifiStatus) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    serviceHelper.enableWifi();
                }
            }, 1000);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, intent.getAction());
            switch (intent.getAction()) {
                case TETHERING:
                    // Correct order of execution: Turn OFF -> Turn ON -> Service ON -> ...
                    if (forceOn && !forceOff) {
                        // Turn OFF
                        forceOff = true;
                        forceOn = false;
                        blockForceInternet = false;
                        execute(ServiceAction.TETHER_OFF);
                        execute(ServiceAction.INTERNET_OFF);
                    } else if (!forceOff && !forceOn) {
                        // Turn ON
                        forceOn = true;
                        blockForceInternet = false;
                        execute(ServiceAction.INTERNET_ON);
                        execute(ServiceAction.TETHER_ON);
                    } else {
                        // Service ON
                        forceOff = false;
                        forceOn = false;
                        onService();
                        updateNotification();
                    }
                    break;

                case WIDGET:
                    changeMobileState = intent.getExtras().getBoolean("changeMobileState", false);
                    blockForceInternet = true;

                    if (serviceHelper.isTetheringWiFi()) {
                        forceOff = true;
                        forceOn = false;
                        execute(ServiceAction.TETHER_OFF);
                    } else {
                        forceOn = true;
                        forceOff = false;
                        status = Status.DEFAULT;
                        execute(ServiceAction.TETHER_ON);
                    }

                    if (changeMobileState) {
                        forceInternetConnect();
                    }
                    break;


                case CHANGE_NETWORK_STATE:
                    updateNotification();
                    break;

                case TetherIntents.TETHER_ON:
                    execute(ServiceAction.TETHER_ON);
                    break;

                case TetherIntents.TETHER_OFF:
                    execute(ServiceAction.TETHER_OFF);
                    break;

                case TetherIntents.INTERNET_ON:
                    execute(ServiceAction.INTERNET_ON);
                    break;

                case TetherIntents.INTERNET_OFF:
                    execute(ServiceAction.INTERNET_OFF);
                    break;

                case EXIT:
                    stopSelf();
                    break;
            }
        }

        private void forceInternetConnect() {
            if (forceOff) {
                execute(ServiceAction.INTERNET_OFF);
            } else if (forceOn) {

            }
        }
    }

    private void onService() {
        boolean tethering = serviceHelper.isTetheringWiFi();
        boolean mobileOn = serviceHelper.isConnectedToInternetThroughMobile();

        if (isActivated3G() && !mobileOn) {
            execute(ServiceAction.INTERNET_ON);
        } else if (internetOn && !isActivated3G() && mobileOn) {
            execute(ServiceAction.INTERNET_OFF);
        }

        if (isActivatedTethering() && !tethering) {
            execute(ServiceAction.TETHER_ON);
        } else if (tethering && !isActivatedTethering()) {
            execute(ServiceAction.INTERNET_OFF);
        }
    }

    private void execute(ServiceAction... serviceAction) {
        for (ServiceAction action : serviceAction) {
            execute(action, 0);
        }
    }

    private void execute(ServiceAction serviceAction, int msg) {
        boolean action = serviceAction.isOn();
        boolean showNotify = false;
        if (serviceAction.isInternet() && serviceHelper.isConnectedOrConnectingToInternet() != action) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "Current Android OS doesn't support turn mobile data!");
            } else if (!internetAsyncTask(action)) {
                return;
            }
            showNotify = true;
        }
        if (serviceAction.isTethering() && serviceHelper.isTetheringWiFi() != action) {
            if (!tetheringAsyncTask(action)) {
                return;
            }
            showNotify = true;
        }

        Log.i(TAG, "Execute action: " + serviceAction.toString());
        notify(serviceAction, msg, showNotify);
    }

    private void notify(ServiceAction serviceAction, int msg, boolean showNotify) {
        Status oldStatus = status;
        int id = R.string.service_started;
        int icon = getIcon(serviceAction);
        switch (serviceAction) {
            case TETHER_ON:
                updateLastAccess();
                id = R.string.notification_tethering_restored;
                status = Status.DEFAULT;
                break;
            case TETHER_OFF:
                id = R.string.notification_tethering_off;
                break;
            case INTERNET_ON:
                if (!Utils.isAirplaneModeOn(getApplicationContext())) {
                    updateLastAccess();
                    status = Status.DEFAULT;
                    id = R.string.notification_internet_restored;
                }
                break;
            case INTERNET_OFF:
                id = R.string.notification_internet_off;
                break;
            default:
                Log.e(TAG, "Missing default notification!");
        }
        if (msg != 0) {
            id = msg;
        }

        if (showNotify || !status.equals(oldStatus)) {
            showNotification(getString(id), icon);
        }
    }

    //TODO replace with getNotificationIcon()
    private int getIcon(ServiceAction serviceAction) {
        int icon = R.drawable.app_off;
        if (serviceAction.name().contains("IDLE")) {
            icon = R.drawable.app_off;
        } else if (serviceHelper.isConnectedToInternetThroughMobile() && serviceHelper.isTetheringWiFi()) {
            icon = R.drawable.app_on;
        } else if (serviceHelper.isConnectedOrConnectingToInternet() || serviceHelper.isTetheringWiFi()) {
            icon = R.drawable.app_yellow;
        }
        return icon;
    }
}
