package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.github.topol777.androidlibrary.R;

public class DotsTabView extends AppCompatImageView {
    public static final String DOTS = "⋮";

    Drawable d;

    public static void update(TabLayout tab, int index) {
        DotsTabView v = new DotsTabView(tab.getContext(), tab.getTabTextColors());
        TabLayout.Tab t = tab.getTabAt(index);
        v.updateLayout(t);
    }

    public DotsTabView(Context context, ColorStateList colors) {
        super(context);
        d = ContextCompat.getDrawable(context, R.drawable.ic_more_vert_24dp);
        setImageDrawable(d);
        setColorFilter(colors.getDefaultColor());
    }

    public void updateLayout(TabLayout.Tab tab) {
        tab.setCustomView(this);
        ViewParent p = getParent();
        if (p instanceof LinearLayout) { // TabView extends LinearLayout
            LinearLayout l = (LinearLayout) p;
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) l.getLayoutParams();
            if (lp != null) {
                lp.weight = 0;
                lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int left = l.getMeasuredHeight() / 2 - d.getIntrinsicWidth() / 2;
                int right = left;
                left -= l.getPaddingLeft();
                right -= l.getPaddingRight();
                if (left < 0)
                    left = 0;
                if (right < 0)
                    right = 0;
                setPadding(left, 0, right, 0);
            }
        }
    }
}
