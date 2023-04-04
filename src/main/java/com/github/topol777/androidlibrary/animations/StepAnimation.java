package com.github.topol777.androidlibrary.animations;

import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class StepAnimation extends Animation {
    public View view;
    public boolean expand;

    public interface LateCreator {
        StepAnimation create();
    }

    public static Animation apply(LateCreator c, View v, boolean expand, boolean animate) {
        Animation old = v.getAnimation();
        if (Build.VERSION.SDK_INT < 11)
            animate = false;
        if (old instanceof StepAnimation) {
            StepAnimation m = (StepAnimation) old;

            long cur = AnimationUtils.currentAnimationTimeMillis();
            long past = cur - m.getStartTime() - m.getStartOffset();
            long left = m.getDuration() - past;
            long offset = cur - m.getStartTime() - left;

            if (animate) {
                if (m.hasStarted() && !m.hasEnded()) {
                    if (m.expand != expand) {
                        m.expand = expand;
                        m.setStartOffset(offset);
                    } // else keep rolling. do nothing
                    return m;
                } else {
                    if (!m.hasStarted())
                        m.restore();
                    StepAnimation mm = c.create();
                    if (mm.animationReady()) {
                        mm.startAnimation(v);
                        return mm;
                    } // else do nothing, animation already applied
                }
            } else {
                if (m.hasStarted() && !m.hasEnded()) {
                    v.clearAnimation();
                    m.restore();
                }
                if (!m.hasStarted())
                    m.restore();
                StepAnimation mm = c.create();
                mm.restore();
                mm.end();
            }
            return null;
        } else {
            StepAnimation mm = c.create();
            if (animate) {
                if (mm.animationReady()) {
                    mm.startAnimation(v);
                    return mm;
                } // else do nothing. animation already applied
            } else {
                mm.restore();
                mm.end();
            }
            return null;
        }
    }

    public StepAnimation(View view, boolean expand) {
        this.view = view;
        this.expand = expand;
    }

    public boolean animationReady() { // start animation only if view in proper visible state. gone for expanding, and visible for collapsing.
        return (expand && view.getVisibility() == View.GONE) || (!expand && view.getVisibility() == View.VISIBLE);
    }

    public void startAnimation(View v) {
        init(); // here view maybe showen for first time to start animation.
        float s = 0; // do first step. to hide view (if animation slide out view) on first calc(), then slide it out.
        if (Build.VERSION.SDK_INT < 23) // some old androids API does not start animation on 0dp views (19 api does not, 20-22 not tested).
            s = 0.001f;
        calc(s, new Transformation());
        v.startAnimation(this);
    }

    public void init() {
        view.setVisibility(View.VISIBLE); // animation does not start on older API if inital state of view is hidden. show view here.
    }

    public void calc(float i, Transformation t) {
    }

    public void restore() {
    }

    public void end() {
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        calc(interpolatedTime, t);

        if (interpolatedTime >= 1) {
            restore();
            end();
        }
    }
}