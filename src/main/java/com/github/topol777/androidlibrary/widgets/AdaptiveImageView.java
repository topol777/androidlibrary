package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class AdaptiveImageView extends AppCompatImageView {

    public static Rect getAdaptivePaddings(View view) {
        int nw = 0;
        int nh = 0;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            nw = lp.width;
            nh = lp.height;
        }
        if (nw <= 0)
            nw = view.getWidth();
        if (nh <= 0)
            nh = view.getHeight();
        if (nw <= 0)
            nw = view.getMeasuredWidth();
        if (nh <= 0)
            nh = view.getMeasuredHeight();
        if (nw <= 0 || nh <= 0)
            throw new RuntimeException("Adaptive Icon view must be fixed");
        return RemoteNotificationCompat.getAdaptivePaddings(view.getContext(), nw, nh);
    }

    public static void setAdaptiveIcon(ImageView view, int id) {
        Rect r = getAdaptivePaddings(view);
        view.setPadding(r.left, r.top, r.right, r.bottom);
        view.setImageResource(id);
    }

    public AdaptiveImageView(Context context) {
        super(context);
    }

    public AdaptiveImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AdaptiveImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        Rect r = getAdaptivePaddings(this);
        setPadding(r.left, r.top, r.right, r.bottom);
    }
}
