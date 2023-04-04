package com.github.topol777.androidlibrary.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.view.WindowCallbackWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;

public abstract class AppCompatFullscreenThemeActivity extends AppCompatThemeActivity {
    public static int UI_ANIMATION_DELAY = 300;
    public static int AUTOHIDE_DELAY = 1500;

    public static int HIDE_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE;

    public static int SHOW_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

    public static void setDecorFitsSystemWindows(Window w, boolean b) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                w.getClass().getMethod("setDecorFitsSystemWindows", boolean.class).invoke(w, b);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void hideStatusBars(Window w) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Class WindowInsetsController = Class.forName("android.view.WindowInsetsController");
                final Object insetsController = Window.class.getMethod("getInsetsController").invoke(w);
                if (insetsController != null) {
                    Class Type = Class.forName("android.view.WindowInsets$Type");
                    WindowInsetsController.getMethod("hide", int.class).invoke(insetsController, Type.getMethod("statusBars").invoke(null));
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void showStatusBars(Window w) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Class WindowInsetsController = Class.forName("android.view.WindowInsetsController");
                final Object insetsController = Window.class.getMethod("getInsetsController").invoke(w);
                if (insetsController != null) {
                    Class Type = Class.forName("android.view.WindowInsets$Type");
                    WindowInsetsController.getMethod("show", int.class).invoke(insetsController, Type.getMethod("statusBars").invoke(null));
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setFitsSystemWindows(Activity a, boolean b) { // set fitsSystemWindows on root view
        View v = a.findViewById(android.R.id.content);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = g.getChildAt(i);
                setFitsSystemWindows(c, b);
            }
        }
    }

    public void setFitsSystemWindows(View v, boolean b) {
        if (Build.VERSION.SDK_INT >= 14)
            v.setFitsSystemWindows(b);
    }

    public final Handler handler = new Handler();
    public Window w;
    public View decorView;
    public boolean fullscreen;
    public Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            hideSystemUI();
        }
    };
    public Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.show();
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        w = getWindow();
        w.setCallback(new WindowCallbackWrapper(w.getCallback()) {
            @SuppressLint("RestrictedApi")
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                super.onWindowFocusChanged(hasFocus);
                if (hasFocus)
                    setFullscreen(fullscreen);
            }
        });
        decorView = w.getDecorView();
        if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 30) {
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    AppCompatFullscreenThemeActivity.this.onSystemUiVisibilityChange(visibility);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(mShowPart2Runnable);
        handler.removeCallbacks(mHidePart2Runnable);
    }

    public void toggle() {
        setFullscreen(!fullscreen);
    }

    public void onSystemUiVisibilityChange(int visibility) {
        if (Build.VERSION.SDK_INT >= 11) {
            if (fullscreen) {
                handler.removeCallbacks(mHidePart2Runnable);
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) // visible
                    handler.postDelayed(mHidePart2Runnable, AUTOHIDE_DELAY);
            }
        }
    }

    public void setFullscreen(boolean b) {
        if (fullscreen == b) {
            if (b) // fix bug when system UI reappear after screen went off
                hideSystemUI();
            return;
        }
        fullscreen = b;
        if (b) {
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.hide();

            handler.removeCallbacks(mShowPart2Runnable);
            handler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
        } else {
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            showSystemUI();
            handler.removeCallbacks(mHidePart2Runnable);
            handler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        }
    }

    public void hideSystemUI() {
        setDecorFitsSystemWindows(w, false);
        hideStatusBars(w);
        if (Build.VERSION.SDK_INT >= 11)
            decorView.setSystemUiVisibility(HIDE_FLAGS);
    }

    public void showSystemUI() {
        setDecorFitsSystemWindows(w, true);
        showStatusBars(w);
        if (Build.VERSION.SDK_INT >= 11)
            decorView.setSystemUiVisibility(SHOW_FLAGS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreen(fullscreen); // refresh
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
