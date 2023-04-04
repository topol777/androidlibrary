package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;

public class ErrorDialog extends AlertDialog.Builder {
    public static final String TAG = ErrorDialog.class.getSimpleName();

    public static String ERROR = "Error"; // title

    public static Throwable getCause(Throwable e) { // get to the bottom
        Throwable c = null;
        while (e != null) {
            c = e;
            e = e.getCause();
        }
        return c;
    }

    public static String toMessage(Throwable e) { // eat RuntimeException's
        Throwable p = e;
        while (e instanceof RuntimeException) {
            e = e.getCause();
            if (e != null)
                p = e;
        }
        String msg = p.getMessage();
        if (msg == null || msg.isEmpty())
            msg = p.getClass().getCanonicalName();
        return msg;
    }

    public ErrorDialog(@NonNull Context context, Throwable e) {
        this(context, toMessage(e));
    }

    public ErrorDialog(@NonNull Context context, String msg) {
        super(context);
        setTitle(ERROR);
        setMessage(msg);
        setIcon(android.R.drawable.ic_dialog_alert);
        setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    public static void Post(final Context context, final Throwable e) {
        Log.e(TAG, "Error", e);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Error(context, toMessage(e));
            }
        });
    }

    public static void Post(final Context context, final String e) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Error(context, e);
            }
        });
    }

    public static AlertDialog Error(Context context, Throwable e) {
        Log.e(TAG, "Error", e);
        return Error(context, ErrorDialog.toMessage(e));
    }

    public static AlertDialog Error(Context context, String msg) {
        ErrorDialog builder = new ErrorDialog(context, msg);
        return builder.show();
    }
}
