package com.github.topol777.androidlibrary.widgets;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerViewFooter extends RecyclerView.ItemDecoration {
    public View footer;
    public RecyclerView r;
    public RecyclerView.OnItemTouchListener touch = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView r, MotionEvent e) {
            int last = r.getAdapter().getItemCount() - 1;
            RecyclerView.ViewHolder h = r.findViewHolderForAdapterPosition(last);
            if (h != null && footer.getVisibility() == View.VISIBLE) {
                int end = h.itemView.getBottom() + footer.getMeasuredHeight();
                if (e.getY() >= h.itemView.getBottom() && e.getY() < end) {
                    e = MotionEvent.obtain(e);
                    e.offsetLocation(0, -h.itemView.getBottom());
                    footer.dispatchTouchEvent(e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView r, MotionEvent e) {
            if (footer.getVisibility() == View.VISIBLE) {
                int last = r.getAdapter().getItemCount() - 1;
                RecyclerView.ViewHolder h = r.findViewHolderForAdapterPosition(last);
                e = MotionEvent.obtain(e);
                e.offsetLocation(0, -h.itemView.getBottom());
                footer.onTouchEvent(e);
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    };

    public RecyclerViewFooter(final RecyclerView r, View h) {
        this.footer = h;
        this.r = r;
        r.addOnItemTouchListener(touch);
    }

    public void close() {
        this.r.removeOnItemTouchListener(touch);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (footer.getVisibility() != View.VISIBLE)
            return;
        int last = parent.getAdapter().getItemCount() - 1;
        footer.layout(parent.getLeft(), 0, parent.getRight(), footer.getMeasuredHeight());
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (parent.getChildAdapterPosition(view) == last) {
                c.save();
                c.clipRect(parent.getLeft(), view.getBottom(), parent.getRight(), parent.getBottom());
                final float top = view.getBottom();
                c.translate(0, top);
                footer.draw(c);
                c.restore();
                ViewCompat.postInvalidateOnAnimation(parent); // TODO detect footer has animation elements (like progres bar)
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int last = parent.getAdapter().getItemCount() - 1;
        int pos = parent.getChildAdapterPosition(view);
        if (pos == last && footer.getVisibility() != View.GONE) {
            if (footer.getMeasuredWidth() <= 0)
                footer.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.AT_MOST));
            outRect.set(0, 0, 0, footer.getMeasuredHeight());
        } else {
            outRect.setEmpty();
        }
    }

}
