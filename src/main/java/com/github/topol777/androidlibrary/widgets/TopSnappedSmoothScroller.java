package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.support.v7.widget.LinearSmoothScroller;

public class TopSnappedSmoothScroller extends LinearSmoothScroller {
    public TopSnappedSmoothScroller(Context context) {
        super(context);
    }

    @Override
    protected int getVerticalSnapPreference() {
        return SNAP_TO_ANY;
    }

    @Override
    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int
            snapPreference) {
        switch (snapPreference) {
            case SNAP_TO_START:
                return boxStart - viewStart;
            case SNAP_TO_END:
                return boxEnd - viewEnd;
            case SNAP_TO_ANY:
                int dtBox = boxEnd - boxStart;
                int dtView = viewEnd - viewStart;
                if (dtBox < dtView) {
                    return -viewStart;
                }
                final int dtStart = boxStart - viewStart;
                if (dtStart > 0) {
                    return dtStart;
                }
                final int dtEnd = boxEnd - viewEnd;
                if (dtEnd < 0) {
                    return dtEnd;
                }
                break;
            default:
                throw new IllegalArgumentException("snap preference should be one of the"
                        + " constants defined in SmoothScroller, starting with SNAP_");
        }
        return 0;
    }
}
