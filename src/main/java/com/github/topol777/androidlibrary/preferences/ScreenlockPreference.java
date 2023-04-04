package com.github.topol777.androidlibrary.preferences;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;

import com.github.topol777.androidlibrary.activities.AppCompatThemeActivity;

public class ScreenlockPreference extends ListPreference {
    public static Handler handler = new Handler(Looper.getMainLooper());

    public static void showLocked(Window window, String key) {
        Context context = window.getContext();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int sec = Integer.parseInt(shared.getString(key, "0"));
        switch (sec) {
            case -1: // no need to keep on
                break;
            case 0: // no need for delay handler call
                keepScreenOn(true, window, key);
                break;
        }
    }

    public static void keepScreenOn(boolean enable, Window w, String key) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(w.getContext());
        int sec = Integer.parseInt(shared.getString(key, "0"));
        if (sec < 0)
            enable = false;
        if (enable)
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void onResume(Activity a, String key) {
        onUserInteraction(a, key);
        keepScreenOn(true, a.getWindow(), key);
    }

    // call it for Activity.onUserInteraction
    public static void onUserInteraction(final Activity activity, final String key) {
        final Context context = activity;
        final Window window = activity.getWindow();

        onUserInteractionRemove();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int sec = Integer.parseInt(shared.getString(key, "0"));
        if (sec <= 0)
            return;

        long next = sec * 1000;
        Runnable inactivity = new Runnable() {
            @Override
            public void run() {
                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (myKM.inKeyguardRestrictedInputMode()) {
                    AppCompatThemeActivity.moveTaskToBack(activity);
                } else {
                    keepScreenOn(false, window, key);
                }
            }
        };
        handler.postDelayed(inactivity, next);
    }

    public static void onUserInteractionRemove() {
        handler.removeCallbacksAndMessages(null);
    }

    public static void onPause(Activity a, String key) {
        onUserInteractionRemove();
    }

    public ScreenlockPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        create();
    }

    public ScreenlockPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    public ScreenlockPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public ScreenlockPreference(Context context) {
        super(context);
        create();
    }

    void create() {
    }
}
