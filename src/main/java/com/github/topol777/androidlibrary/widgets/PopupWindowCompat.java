package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.github.topol777.androidlibrary.R;

public class PopupWindowCompat {

    public static void setRotationCompat(View view, float rotation) {
        if (Build.VERSION.SDK_INT >= 11) {
            ViewCompat.setRotation(view, rotation); // missing api 10 support
        } else {
            RotateAnimation animation = new RotateAnimation(rotation, rotation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            animation.setDuration(0);
            animation.setFillAfter(true);
            view.startAnimation(animation);
        }
    }

    public static Rect getOnScreenRect(View v) {
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return new Rect(loc[0], loc[1], loc[0] + v.getWidth(), loc[1] + v.getHeight());
    }

    public static float getDimension(Context context, int id) {
        TypedValue tv = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(id, tv, true);
        if (found) {
            switch (tv.type) {
                case TypedValue.TYPE_DIMENSION:
                    return context.getResources().getDimension(tv.resourceId);
                default:
                    return context.getResources().getDimension(id);
            }
        } else {
            throw new RuntimeException("not found");
        }
    }

    public static void setTintCompat(Drawable d, int color) {
        d = DrawableCompat.wrap(d);
        if (Build.VERSION.SDK_INT >= 11)
            DrawableCompat.setTint(d.mutate(), color); // missing api 10 support, even it has BaseDrawableImpl
        else
            d.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public static void showAsDropDown(PopupWindow p, View anchor, int gravity) {
        Context context = anchor.getContext();
        View v = p.getContentView();
        Resources r = context.getResources();
        float f = getDimension(context, R.attr.dialogPreferredPadding);
        DisplayMetrics dm = r.getDisplayMetrics();
        int w = (int) (dm.widthPixels - f * 2);
        int h = (int) (dm.heightPixels - f * 2);
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST));
        h = v.getMeasuredHeight();
        p.setHeight(h);
        p.setWidth(w);
        int x = (dm.widthPixels - w) / 2, y = 0;
        Rect rect = getOnScreenRect(anchor);
        switch (gravity) {
            case Gravity.TOP: // popup at top of anchor
                if (rect.top - h < 0)
                    y = rect.bottom;
                else
                    y = rect.top - h;
                break;
            case Gravity.BOTTOM: // popup at bottom of anchor
                if (rect.bottom + h > dm.heightPixels)
                    y = rect.top - h;
                else
                    y = rect.bottom;
                break;
        }
        p.setFocusable(true);
        p.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    public static void showAsTooltip(PopupWindow p, View anchor, int gravity) {
        showAsTooltip(p, anchor, gravity, -1);
    }

    public static void showAsTooltip(PopupWindow p, View anchor, int gravity, int maxwidth) {
        showAsTooltip(p, anchor, gravity, ThemeUtils.getThemeColor(anchor.getContext(), R.attr.colorButtonNormal), maxwidth);
    }

    public static void showAsTooltip(final PopupWindow p, View anchor, int gravity, int background, int maxwidth) {
        Context context = anchor.getContext();

        final View v = p.getContentView();
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null)
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        LinearLayout tooltip = new LinearLayout(context);
        tooltip.setLayoutParams(new ViewGroup.LayoutParams(lp.width, lp.height));
        tooltip.setOrientation(LinearLayout.VERTICAL);

        AppCompatImageView up = new AppCompatImageView(context);
        up.setImageResource(R.drawable.popup_triangle);
        setRotationCompat(up, 180);
        tooltip.addView(up, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final FrameLayout content = new FrameLayout(context);
        content.setBackgroundResource(R.drawable.popup_round);
        LinearLayout.LayoutParams clp;
        if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT)
            clp = new LinearLayout.LayoutParams(lp.width, 0, 1);
        else
            clp = new LinearLayout.LayoutParams(lp.width, lp.height);
        tooltip.addView(content, clp);

        AppCompatImageView down = new AppCompatImageView(context);
        down.setImageResource(R.drawable.popup_triangle);
        tooltip.addView(down, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewParent parent = v.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(v);
        }

        content.addView(v);
        p.setContentView(tooltip);

        switch (gravity) { // hide arrows, to calcualte correct height
            case Gravity.TOP: // popup at top of anchor
                up.setVisibility(View.GONE);
                down.setVisibility(View.VISIBLE);
                break;
            case Gravity.BOTTOM: // popup at bottom of anchor
                up.setVisibility(View.VISIBLE);
                down.setVisibility(View.GONE);
                break;
        }

        Resources r = context.getResources();
        float f = getDimension(context, R.attr.dialogPreferredPadding);
        DisplayMetrics dm = r.getDisplayMetrics();
        int w = (int) (dm.widthPixels - f * 2);
        int h = (int) (dm.heightPixels - f * 2);
        int mw = View.MeasureSpec.AT_MOST;
        if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT)
            mw = View.MeasureSpec.EXACTLY;
        int mh = View.MeasureSpec.AT_MOST;
        if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT)
            mh = View.MeasureSpec.EXACTLY;
        if (maxwidth > 0)
            w = Math.min(maxwidth, w);
        tooltip.measure(View.MeasureSpec.makeMeasureSpec(w, mw), View.MeasureSpec.makeMeasureSpec(h, mh));
        h = tooltip.getMeasuredHeight();
        w = tooltip.getMeasuredWidth();
        p.setHeight(h);
        p.setWidth(w);
        Rect rect = getOnScreenRect(anchor);
        int x = rect.centerX() - w / 2;
        if (x < f)
            x = (int) f;
        int xr = (int) (dm.widthPixels - f - w);
        if (x > xr)
            x = xr;
        int y = 0;
        switch (gravity) {
            case Gravity.TOP: // popup at top of anchor
                if (rect.top - h < 0) {
                    y = rect.bottom;
                    gravity = Gravity.BOTTOM;
                } else {
                    y = rect.top - h;
                }
                break;
            case Gravity.BOTTOM: // popup at bottom of anchor
                if (rect.bottom + h > dm.heightPixels) {
                    y = rect.top - h;
                    gravity = Gravity.TOP;
                } else {
                    y = rect.bottom;
                }
                break;
        }

        ImageView arrow = null;
        switch (gravity) { // if tooltip window were repositioned, update arrows again
            case Gravity.TOP: // popup at top of anchor
                up.setVisibility(View.GONE);
                down.setVisibility(View.VISIBLE);
                arrow = down;
                break;
            case Gravity.BOTTOM: // popup at bottom of anchor
                up.setVisibility(View.VISIBLE);
                down.setVisibility(View.GONE);
                arrow = up;
                break;
        }

        Drawable triangle = arrow.getDrawable();
        setTintCompat(triangle, background);
        Drawable round = content.getBackground();
        setTintCompat(round, background);

        int l = rect.centerX() - x - arrow.getMeasuredWidth() / 2;
        int ll = ThemeUtils.dp2px(context, 10); // round background range left
        if (l < ll)
            l = ll;
        int lr = w - ThemeUtils.dp2px(context, 10) - arrow.getMeasuredWidth(); // round background range right
        if (l > lr)
            l = lr;
        ((LinearLayout.LayoutParams) arrow.getLayoutParams()).leftMargin = l;

        p.setFocusable(true);
        p.setOutsideTouchable(true);
        p.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        p.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
        p.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                content.removeAllViews();
            }
        });
    }

}
