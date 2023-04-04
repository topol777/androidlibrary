package com.github.topol777.androidlibrary.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;

import com.github.topol777.androidlibrary.app.NotificationManagerCompat;

// Add users permission to app manifest:
//
// <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
//
public class SilencePreferenceCompat extends SwitchPreferenceCompat {

    boolean resume = false;

    @TargetApi(23)
    public static boolean isNotificationPolicyAccessGranted(Context context) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        return nm.isNotificationPolicyAccessGranted();
    }

    @TargetApi(23)
    public static boolean isNotificationPolicyAccessGranted(Context context, Intent intent) {
        if (!OptimizationPreferenceCompat.isCallable(context, intent))
            return true;
        return isNotificationPolicyAccessGranted(context);
    }

    @TargetApi(23)
    public static Intent accessIntent() {
        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @TargetApi(21)
    public SilencePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        create();
    }

    @TargetApi(21)
    public SilencePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    public SilencePreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public SilencePreferenceCompat(Context context) {
        super(context);
        create();
    }

    public void create() {
        onResume();
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean b = (boolean) newValue;
            if (b) {
                Intent intent = accessIntent();
                if (!isNotificationPolicyAccessGranted(getContext(), intent)) {
                    getContext().startActivity(intent);
                    resume = true;
                    return false;
                }
            }
        }
        return super.callChangeListener(newValue);
    }

    public void onResume() {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = accessIntent();
            if (!isNotificationPolicyAccessGranted(getContext(), intent)) {
                setChecked(false);
            } else {
                if (resume) {
                    setChecked(true);
                    resume = false;
                }
            }
        }
    }
}
