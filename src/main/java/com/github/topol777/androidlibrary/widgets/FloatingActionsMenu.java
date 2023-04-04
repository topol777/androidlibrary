package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

public class FloatingActionsMenu extends net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu {
    protected Handler handler = new Handler();
    protected OnFloatingActionsMenuUpdateListener listener = new OnFloatingActionsMenuUpdateListener() {
        @Override
        public void onMenuExpanded() {
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                if (isEnclosing(v.getClass(), net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu.class))
                    continue;
                if (v instanceof FloatingActionButton)
                    v.setClickable(true);
            }
        }

        @Override
        public void onMenuCollapsed() {
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                if (isEnclosing(v.getClass(), net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu.class))
                    continue;
                if (v instanceof FloatingActionButton)
                    v.setClickable(false);
            }
        }
    };

    public static boolean isEnclosing(Class c, Class e) {
        return c.isAnonymousClass() && c.getEnclosingClass() == e;
    }

    public FloatingActionsMenu(Context context) {
        super(context);
        create();
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        create();
    }

    public void create() {
        if (Build.VERSION.SDK_INT < 11) { // API10 bug
            setOnFloatingActionsMenuUpdateListener(listener);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    expand();
                    collapseImmediately();
                }
            });
        }
    }
}
