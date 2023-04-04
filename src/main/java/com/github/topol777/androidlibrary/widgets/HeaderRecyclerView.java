package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class HeaderRecyclerView extends RecyclerView {
    public HeaderRecyclerView(Context context) {
        super(context);
    }

    public HeaderRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HeaderRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        Adapter a = getAdapter();
        while (a instanceof WrapperRecyclerAdapter) {
            if (a instanceof HeaderRecyclerAdapter)
                ((HeaderRecyclerAdapter) a).updateGridHeaderFooter(layout);
            a = ((WrapperRecyclerAdapter) a).getWrappedAdapter();
        }
    }
}
