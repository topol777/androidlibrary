package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.topol777.androidlibrary.R;

public class PinchView extends FrameLayout implements GestureDetector.OnGestureListener {
    public float start; // starting sloop
    public float end;
    float current;
    float centerx = 0.5f;
    float centery = 0.5f;
    Rect page; // page rect, area on screen where to draw bm
    Rect box; // box is smaller then 'page rect', cut by layout bounds, scrolling offscreen can increase scroll distance
    float sx;
    float sy;
    public Bitmap bm;
    Rect src;
    GestureDetectorCompat gestures;
    int rotation = 0;

    public View toolbar;
    public ImageView image;
    public MarginLayoutParams lp;

    public static Rect getImageBounds(ImageView imageView) {
        RectF bounds = new RectF();
        Drawable drawable = imageView.getDrawable();
        if (drawable != null)
            imageView.getImageMatrix().mapRect(bounds, new RectF(drawable.getBounds()));
        Rect r = new Rect();
        bounds.round(r);
        return r;
    }

    public static void rotateRect(Rect rect, int degrees, int px, int py) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, px, py);
        RectF rectF = new RectF(rect);
        matrix.mapRect(rectF);
        rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }

    public static void rotateRect(RectF rect, float degrees) {
        rotateRect(rect, degrees, rect.centerX(), rect.centerY());
    }

    public static void rotateRect(RectF rect, float degrees, float px, float py) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, px, py);
        matrix.mapRect(rect);
    }

    public PinchView(Context context, Rect v, Bitmap bm) {
        super(context);
        this.page = v;
        this.box = new Rect(v);
        this.bm = bm;

        LayoutInflater inflater = LayoutInflater.from(context);

        lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.NO_GRAVITY); // API9 requires gravity to be set
        image = new AppCompatImageView(context);
        image.setImageBitmap(bm);
        addView(image, lp);
        toolbar = inflater.inflate(R.layout.pinch_toolbar, this, false);
        addView(toolbar);

        View left = toolbar.findViewById(R.id.pinch_left);
        left.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addRotate(-90);
            }
        });
        View right = toolbar.findViewById(R.id.pinch_right);
        right.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addRotate(90);
            }
        });
        View close = toolbar.findViewById(R.id.pinch_close);
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pinchClose();
            }
        });

        src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
        gestures = new GestureDetectorCompat(context, this);
        gestures.setIsLongpressEnabled(false);
    }

    void addRotate(int a) {
        rotation += a;
        rotation %= 360;
        PopupWindowCompat.setRotationCompat(image, rotation);
        limitsOff();
        calc();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestures.onTouchEvent(event))
            return true;
        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        box.set(page);
        box.intersect(left, top, right, bottom);
    }

    public void scale(final float r) {
        final int w = getWidth();
        final int h = getHeight();
        onScale(new ScaleGestureDetector(getContext(), null) {
            @Override
            public float getCurrentSpan() {
                return w * (r - 1f);
            }

            @Override
            public float getFocusX() {
                return w / 2;
            }

            @Override
            public float getFocusY() {
                return h / 2;
            }
        });
        onScaleEnd();
    }

    public void onScale(ScaleGestureDetector detector) {
        float k = lp.width / (float) page.width();

        current = (detector.getCurrentSpan() - start) * k;

        centerx = (detector.getFocusX() - lp.leftMargin) / lp.width;
        centery = (detector.getFocusY() - lp.topMargin) / lp.height;

        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        float w = page.width() + end + current;
        float h = w * ratio;
        float l = page.left + sx - currentx;
        float t = page.top + sy - currenty * ratio;
        float r = l + w;
        float b = t + h;

        RectF p = new RectF(l, t, r, b);
        rotateRect(p, rotation);

        if (p.width() > box.width()) {
            if (p.left > box.left)
                centerx = 0;
            if (p.right < box.right)
                centerx = 1;
        }

        if (p.height() > box.height()) {
            if (p.top > box.top)
                centery = 0;
            if (p.bottom < box.bottom)
                centery = 1;
        }

        limitsOff();
        calc();
    }

    public void onScaleEnd() {
        float ratio = page.height() / (float) page.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        sx -= currentx;
        sy -= currenty * ratio;

        end += current;
        current = 0;

        calc();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        sx -= distanceX;
        sy -= distanceY;

        limitsOff();
        calc();

        return true;
    }

    void limitsOff() {
        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        float w = page.width() + end + current;
        float h = w * ratio;
        float l = page.left + sx - currentx;
        float t = page.top + sy - currenty * ratio;
        float r = l + w;
        float b = t + h;

        RectF p = new RectF(l, t, r, b);
        rotateRect(p, rotation);

        if (p.width() < box.width()) {
            end = end - (p.width() - box.width());
            sx = sx - (p.left - box.left);
            limitsOff();
            return;
        }
        if (p.height() < box.height()) {
            end = end - (p.height() - box.height());
            sy = sy - (p.top - box.top);
            limitsOff();
            return;
        }

        if (p.left > box.left)
            sx = sx - (p.left - box.left);
        if (p.top > box.top)
            sy = sy - (p.top - box.top);
        if (p.right < box.right)
            sx = sx - (p.right - box.right);
        if (p.bottom < box.bottom)
            sy = sy - (p.bottom - box.bottom);
    }

    void calc() {
        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        float w = page.width() + end + current;
        float h = w * ratio;
        float l = page.left + sx - currentx;
        float t = page.top + sy - currenty * ratio;

        lp = (MarginLayoutParams) image.getLayoutParams();
        lp.leftMargin = (int) l;
        lp.topMargin = (int) t;
        lp.width = (int) w;
        lp.height = (int) h;
        image.setLayoutParams(lp);
    }

    public void close() {
        if (bm != null) {
            bm.recycle();
            bm = null;
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Rect r = new Rect();
        image.getHitRect(r);
        if (!r.contains((int) e.getX(), (int) e.getY())) {
            pinchClose();
            return true;
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void pinchClose() {
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }
}
