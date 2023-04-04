package com.github.topol777.androidlibrary.widgets;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerViewHeader extends RecyclerView.ItemDecoration {
    public View header;
    public RecyclerView r;
    public RecyclerView.OnItemTouchListener touch = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView r, MotionEvent e) {
            RecyclerView.ViewHolder h = r.findViewHolderForAdapterPosition(0);
            if (h != null && header.getVisibility() == View.VISIBLE) {
                if (e.getY() < h.itemView.getTop()) {
                    header.dispatchTouchEvent(e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView r, MotionEvent e) {
            if (header.getVisibility() == View.VISIBLE) {
                header.onTouchEvent(e);
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    };

    public RecyclerViewHeader(RecyclerView r, View h) {
        header = h;
        this.r = r;
        r.addOnItemTouchListener(touch);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (header.getVisibility() != View.VISIBLE)
            return;
        header.layout(parent.getLeft(), 0, parent.getRight(), header.getMeasuredHeight());
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (parent.getChildAdapterPosition(view) == 0) {
                c.save();
                c.clipRect(parent.getLeft(), parent.getTop(), parent.getRight(), view.getTop());
                final int height = header.getMeasuredHeight();
                final float top = view.getTop() - height;
                c.translate(0, top);
                header.draw(c);
                c.restore();
                ViewCompat.postInvalidateOnAnimation(parent); // TODO detect header has animation elements (like progres bar)
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos == 0) {
            if (header.getMeasuredWidth() <= 0)
                header.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.AT_MOST));
            outRect.set(0, header.getMeasuredHeight(), 0, 0);
        } else {
            outRect.setEmpty();
        }
    }

}
