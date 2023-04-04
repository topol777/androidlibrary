package com.github.topol777.androidlibrary.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.view.CollapsibleActionView;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.github.topol777.androidlibrary.R;

import java.util.ArrayList;
import java.util.TreeSet;

@Keep
public class ToolbarActionView extends LinearLayoutCompat implements CollapsibleActionView {
    public MenuBuilder menu;
    public CollapsibleActionView listener;
    public Menu appbar; // dropdown menu (three dots)
    ArrayList<MenuItem> items = new ArrayList<>(); // appbar added items
    int old;

    public static Activity from(Context context) {
        if (context instanceof Activity)
            return (Activity) context;
        if (context instanceof ContextWrapper)
            return from(((ContextWrapper) context).getBaseContext());
        throw new RuntimeException("unknown context");
    }

    public static void hideMenu(Menu m, int id) {
        MenuItem item = m.findItem(id);
        item.setVisible(false);
    }

    public static int getFirst(Menu m) {
        TreeSet<Integer> ii = new TreeSet<>();
        for (int i = m.size() - 1; i >= 0; i--) {
            MenuItem item = m.getItem(i);
            int o = item.getOrder();
            if (o != 0)
                ii.add(o);
        }
        return ii.first();
    }

    @SuppressLint("RestrictedApi")
    public static int getShowAsActionFlag(MenuItem item) {
        MenuItemImpl itemImpl = ((MenuItemImpl) item);
        if (itemImpl.requiresActionButton()) return MenuItemImpl.SHOW_AS_ACTION_ALWAYS;
        else if (itemImpl.requestsActionButton()) return MenuItemImpl.SHOW_AS_ACTION_IF_ROOM;
        else if (itemImpl.showsTextAsAction()) return MenuItemImpl.SHOW_AS_ACTION_WITH_TEXT;
        else return MenuItemImpl.SHOW_AS_ACTION_NEVER;
    }

    public static void setTint(MenuItem item, int color) {
        Drawable d = item.getIcon();
        d = DrawableCompat.wrap(d);
        d.mutate();
        d.setColorFilter(color, PorterDuff.Mode.SRC_ATOP); // DrawableCompat.setTint(d, color);
        item.setIcon(d);
    }

    public static void setEnable(MenuItem item, boolean b) {
        if (b) {
            item.setEnabled(true);
            setTint(item, Color.WHITE);
        } else {
            item.setEnabled(false);
            setTint(item, Color.GRAY);
        }
    }

    public ToolbarActionView(Context context) {
        super(context);
    }

    public ToolbarActionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToolbarActionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("RestrictedApi")
    public void create(Menu appbar, int id) {
        this.appbar = appbar;
        menu = new MenuBuilder(getContext());

        final Activity a = from(getContext());
        a.getMenuInflater().inflate(id, menu);

        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            AppCompatImageButton image = new AppCompatImageButton(getContext(), null, R.attr.toolbarNavigationButtonStyle);
            image.setId(item.getItemId());
            image.setImageDrawable(item.getIcon());
            image.setColorFilter(Color.WHITE);
            LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START;
            image.setLayoutParams(lp);
            image.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    a.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
                }
            });
            image.setVisibility(item.isVisible() ? VISIBLE : GONE);
            addView(image);
        }
    }

    @SuppressLint("RestrictedApi")
    void hideLast() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View c = getChildAt(i);
            if (c.getVisibility() != GONE) {
                pack(c.getId());
                return;
            }
        }
    }

    @SuppressLint("RestrictedApi")
    void pack(int id) {
        MenuItem m = menu.findItem(id);
        if (items.contains(m))
            return;
        View c = findViewById(id);
        c.setVisibility(GONE);
        appbar.add(Menu.NONE, m.getItemId(), getFirst(appbar) - 1, m.getTitle()); // do we need better order then FIRST?
        items.add(m);
        if (!m.isEnabled()) {
            MenuItem a = appbar.findItem(m.getItemId());
            a.setVisible(false);
        }
    }

    @SuppressLint("RestrictedApi")
    void pop(int id) {
        View c = findViewById(id);
        c.setVisibility(VISIBLE);
        MenuItem m = menu.findItem(id);
        appbar.removeItem(id);
        items.remove(m);
    }

    @Override
    public void onActionViewExpanded() {
        if (listener != null)
            listener.onActionViewExpanded();
    }

    @Override
    public void onActionViewCollapsed() {
        if (listener != null)
            listener.onActionViewCollapsed();
        clear();
    }

    public void clear() {
        for (MenuItem m : items) {
            appbar.removeItem(m.getItemId());
            View v = findViewById(m.getItemId());
            v.setVisibility(VISIBLE);
        }
        items.clear();
        old = 0;
    }

    @SuppressLint("RestrictedApi")
    public void hide(int id) {
        View v = findViewById(id);
        v.setVisibility(GONE);
        MenuItem a = appbar.findItem(id);
        if (a != null)
            a.setVisible(false);
        MenuItem m = menu.findItem(id);
        m.setVisible(false);
    }

    @SuppressLint("RestrictedApi")
    public void show(int id) {
        MenuItem a = appbar.findItem(id);
        if (a != null) {
            a.setVisible(true);
            return;
        }
        MenuItem m = menu.findItem(id);
        m.setVisible(true);
        View v = findViewById(id);
        v.setVisibility(VISIBLE);
    }

    @SuppressLint("RestrictedApi")
    public void setEnable(int id, boolean b) {
        MenuItem a = appbar.findItem(id);
        if (a != null) {
            a.setVisible(b);
            return;
        }
        MenuItem m = menu.findItem(id);
        setEnable(m, b);
        AppCompatImageButton v = (AppCompatImageButton) findViewById(id);
        v.setEnabled(b);
        v.setColorFilter(b ? Color.WHITE : Color.GRAY);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = MeasureSpec.getSize(widthMeasureSpec);
        if (old != size) {
            old = size;
            for (int i = 0; i < menu.size(); i++) {
                final MenuItem item = menu.getItem(i);
                if (getShowAsActionFlag(item) == MenuItemImpl.SHOW_AS_ACTION_NEVER)
                    pack(item.getItemId());
            }
            super.onMeasure(0, 0);
            while (getMeasuredWidth() > size) {
                hideLast();
                super.onMeasure(0, 0);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
