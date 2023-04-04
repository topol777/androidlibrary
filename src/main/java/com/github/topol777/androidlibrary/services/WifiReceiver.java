package com.github.topol777.androidlibrary.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * &lt;uses-permission android:name="android.permission.ACCESS_WIFI_STATE" /&gt;
 * &lt;uses-permission android:name="android.permission.CHANGE_WIFI_STATE" /&gt;
 * &lt;uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /&gt;
 */
public abstract class WifiReceiver extends BroadcastReceiver {

    public static String TAG = WifiReceiver.class.getSimpleName();

    public Context context;
    public IntentFilter filter = new IntentFilter();

    public static boolean isConnectedWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = cm.getActiveNetworkInfo();
        if (n != null) { // connected to the internet
            switch (n.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_ETHERNET:
                    return true;
            }
        }
        return false;
    }

    public WifiReceiver(Context context) {
        this.context = context;
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    public void create() {
        context.registerReceiver(this, filter);
        if (getWifi()) {
            if (isConnectedWifi(context))
                resume();
            else
                pause();
        } else {
            resume();
        }
    }

    public void close() {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, intent.toString() + " " + action);
        if (action == null)
            return;
        boolean wifi = getWifi();
        if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            Log.d(TAG, state.toString() + " " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
            if (wifi) { // suplicant only correspond to 'wifi only'
                if (isConnectedWifi(context)) { // maybe 'state' have incorrect state. check system service additionaly.
                    resume();
                    return;
                }
                pause();
                return;
            }
        }
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo n = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            Log.d(TAG, n.toString());
            if (n.isConnected()) {
                if (wifi) { // wifi only?
                    switch (n.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                        case ConnectivityManager.TYPE_ETHERNET:
                            resume();
                            return;
                    }
                } else { // resume for any connection type
                    resume();
                    return;
                }
            }
            // if not state.isConnected() maybe it is not correct, check service information
            if (wifi) {
                if (isConnectedWifi(context)) {
                    resume();
                    return;
                }
            } else {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) { // connected to the internet
                    resume();
                    return;
                }
            }
            pause();
        }
    }

    public abstract void resume();

    public abstract void pause();

    public abstract boolean getWifi();
}
