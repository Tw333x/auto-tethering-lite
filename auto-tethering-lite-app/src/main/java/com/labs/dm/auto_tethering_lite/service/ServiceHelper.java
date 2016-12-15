package com.labs.dm.auto_tethering_lite.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper class responsible for communication with WIFI and mobile services
 * <p/>
 * Created by Daniel Mroczka on 2015-10-26.
 */
public class ServiceHelper {

    private final Context context;
    private final WifiManager wifiManager;
    private final String TAG = "ServiceHelper";

    public ServiceHelper(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Returns true if currently Wi-Fi tethering is enabled.
     *
     * @return
     */
    public boolean isTetheringWiFi() {
        try {
            final Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (IllegalAccessException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (InvocationTargetException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, ex.getMessage());
        }

        return false;
    }

    /**
     * Returns true if internet connection provided by mobile is currently active.
     *
     * @return
     */
    public boolean isConnectedToInternetThroughMobile() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
    }

    public boolean isConnectedToInternetThroughWiFi() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    }

    /**
     * Changing Wifi Tethering state
     *
     * @param enable
     */
    public void setWifiTethering(boolean enable) {
        wifiManager.setWifiEnabled(false);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    Log.i(TAG, "setWifiTethering to " + enable);
                    method.invoke(wifiManager, null, enable);
                } catch (Exception ex) {
                    Log.e(TAG, "Switch on tethering", ex);
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    /**
     * Connecting to internet through mobile phone
     * Works only for Android < 5.0
     *
     * @param enabled
     */
    public void setMobileDataEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "Unimplemented setMobileDataEnabled on Android 5.0!");
            return;
        }
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e) {
            Log.e(TAG, "Changing mobile connection state", e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void enableWifi() {
        wifiManager.setWifiEnabled(true);
    }

}
