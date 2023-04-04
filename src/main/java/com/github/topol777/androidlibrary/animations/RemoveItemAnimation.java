package com.github.topol777.androidlibrary.animations;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class RemoveItemAnimation extends StepAnimation {
    ViewGroup list;
    View convertView;
    Runnable run;
    int h;
    int w;
    ViewGroup.LayoutParams lp;
    ViewGroup.LayoutParams lpOrig;
    float mPivotX, mPivotY;

    int duration = 500;

    int alphaOffset = 0;
    int alphaDuration = (int) (duration * 0.6);

    int scaleOffset = alphaDuration;
    int scaleDuration = (int) (duration * 0.4);

    Handler handler;

    public static void apply(final ViewGroup list, final View v, final Runnable run) {
        apply(new LateCreator() {
            @Override
            public StepAnimation create() {
                return new RemoveItemAnimation(list, v, run);
            }
        }, v, false, true);
    }

    public static void restore(final View v) {
        Animation a = v.getAnimation();
        if (a instanceof RemoveItemAnimation) {
            v.clearAnimation();
            RemoveItemAnimation m = (RemoveItemAnimation) a;
            m.restore();
        }
        v.setVisibility(View.VISIBLE);
    }

    public RemoveItemAnimation(ViewGroup list, View v, Runnable run) {
        super(v, false);
        this.convertView = v;
        this.list = list;
        this.run = run;

        this.handler = new Handler();

        this.h = v.getHeight();
        this.w = v.getWidth();
        this.lp = v.getLayoutParams();
        this.lpOrig = new ViewGroup.LayoutParams(lp);

        setInterpolator(new AccelerateInterpolator());
        setDuration(duration);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);

        convertView.setVisibility(View.VISIBLE);

        mPivotX = resolveSize(RELATIVE_TO_SELF, 0.5f, width, parentWidth);
        mPivotY = resolveSize(RELATIVE_TO_SELF, 0.5f, height, parentHeight);
    }

    float intTime(int off, int dur) {
        long cur = AnimationUtils.currentAnimationTimeMillis();
        long start = getStartTime();

        if (start == -1)
            start = cur;

        long past = cur - start;
        long wait = off - past;
        long left = dur + wait;

        if (wait > 0)
            return -1;

        if (left < 0)
            return 1f;
        else
            return getInterpolator().getInterpolation((dur - left) / (float) dur);
    }

    @Override
    public void calc(float i, Transformation t) {
        float ia = intTime(alphaOffset, alphaDuration);
        if (ia >= 0) {
            t.setAlpha(1 - ia);
            t.getMatrix().setTranslate(w * ia, 0);
        }
        float is = intTime(scaleOffset, scaleDuration);
        if (is >= 0) {
            is = 1 - is;
            t.getMatrix().setScale(is, is, mPivotX, 0);
            lp.height = (int) (h * is);
            convertView.requestLayout();
        }
    }

    public void restore() {
        lp.height = lpOrig.height;
        convertView.requestLayout();
    }

    public void end() {
        convertView.setVisibility(View.GONE);
        if (run != null) {
            handler.post(run);
            run = null;
        }
    }
}
