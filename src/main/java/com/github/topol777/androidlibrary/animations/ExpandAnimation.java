package com.github.topol777.androidlibrary.animations;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.github.topol777.androidlibrary.widgets.PopupWindowCompat;

public class ExpandAnimation extends MarginAnimation {
    // if we have only two concurrent animations in one app. only one 'expand' should have control of showChild function.
    public static ExpandAnimation lastExpander;

    public RecyclerView list;
    public View convertView; // item view
    public View expandView; // rotating arrow view
    public boolean partial;
    public boolean adjust = true; // adjust list position

    public static Animation apply(final RecyclerView list, final View itemView, final View toolbarView, final View expandView, final boolean expand, boolean animate) {
        return apply(new LateCreator() {
            @Override
            public MarginAnimation create() {
                return new ExpandAnimation(list, itemView, toolbarView, expandView, expand);
            }
        }, toolbarView, expand, animate);
    }

    public ExpandAnimation(RecyclerView list, View itemView, View toolbarView, View expandView, boolean expand) {
        super(toolbarView, expand);
        this.convertView = itemView;
        this.list = list;
        this.expandView = expandView;
        if (expand)
            lastExpander = this;
        if (lastExpander != null && lastExpander.hasEnded())
            lastExpander = null;
    }

    @Override
    public void init() {
        super.init();
        final int paddedTop = list.getPaddingTop();
        final int paddedBottom = list.getHeight() - list.getPaddingTop() - list.getPaddingBottom();
        partial = convertView.getTop() < paddedTop;
        partial |= convertView.getBottom() > paddedBottom;
    }

    public void expandRotate(float e) {
        PopupWindowCompat.setRotationCompat(expandView, e);
    }

    @Override
    public void calc(final float i, Transformation t) {
        super.calc(i, t);

        float e = expand ? -(1 - i) : (1 - i);
        expandRotate(180 * e);

        if (Build.VERSION.SDK_INT >= 19) {
            if (!expand && lastExpander != null) { // collapse and double animation (collapse&&expand)
                // do not adjustChild
            } else {
                // ViewGroup will crash on null pointer without this post pone.
                // seems like some views are removed by RecyclingView when they
                // gone off screen.
                list.post(new Runnable() {
                    @Override
                    public void run() {
                        adjustChild(i);
                    }
                });
            }
        }
    }

    @TargetApi(19)
    public void adjustChild(float i) {
        if (!adjust)
            return;

        final int paddedTop = list.getPaddingTop();
        final int paddedBottom = list.getHeight() - list.getPaddingTop() - list.getPaddingBottom();

        if (convertView.getTop() < paddedTop) {
            int off = convertView.getTop() - paddedTop;
            if (partial)
                off = (int) (off * i);
            list.scrollBy(0, off);
        } else if (convertView.getBottom() > paddedBottom) {
            int off = convertView.getBottom() - paddedBottom;
            if (partial)
                off = (int) (off * i);
            list.scrollBy(0, off);
        }
    }

    @Override
    public void restore() {
        super.restore();
        expandRotate(0);
    }

    @Override
    public void end() {
        super.end();
    }
}
