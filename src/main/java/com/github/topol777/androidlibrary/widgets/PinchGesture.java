package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class PinchGesture implements ScaleGestureDetector.OnScaleGestureListener {
    public ScaleGestureDetector scale;
    public boolean scaleTouch = false;
    public PinchView pinch;
    public Context context;

    public PinchGesture(Context context) {
        this.context = context;
        scale = new ScaleGestureDetector(context, this);
        ScaleGestureDetectorCompat.setQuickScaleEnabled(scale, false);
    }

    public boolean isScaleTouch(MotionEvent e) {
        if (pinch != null)
            return true;
        if (e.getPointerCount() >= 2)
            return true;
        return false;
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (isScaleTouch(e)) {
            if (scaleTouch && (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_DOWN) && e.getPointerCount() == 1)
                scaleTouch = false;
            if (e.getPointerCount() == 2)
                scaleTouch = true;
            scale.onTouchEvent(e);
            if (pinch != null) {
                if (!scaleTouch)
                    pinch.onTouchEvent(e);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        scaleTouch = true;
        if (pinch == null)
            return false;
        pinch.onScale(detector);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        scaleTouch = true;
        if (pinch == null) {
            float x = detector.getFocusX();
            float y = detector.getFocusY();
            onScaleBegin(x, y);
        }
        pinch.start = detector.getCurrentSpan();
        return true;
    }

    public void onScaleBegin(float x, float y) {
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        scaleTouch = true;
        if (isPinch()) { // double end?
            pinch.onScaleEnd();
            if (pinch.end < 0)
                pinchClose();
        }
    }

    public boolean isPinch() {
        return pinch != null;
    }

    public void pinchOpen(Rect v, Bitmap bm) {
        pinch = new PinchView(context, v, bm) {
            @Override
            public void pinchClose() {
                PinchGesture.this.pinchClose();
            }
        };
    }

    public void pinchClose() {
        if (pinch != null) {
            pinch.close();
            pinch = null;
        }
    }
}
