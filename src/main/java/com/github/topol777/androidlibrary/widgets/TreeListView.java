package com.github.topol777.androidlibrary.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class TreeListView extends ListView {

    public OnToggleListener toggleListener;
    public GestureDetectorCompat gestures;

    public static class TreeNode {
        public TreeNode parent;
        public boolean selected = false;
        public boolean expanded = false;
        public Object tag;
        public int level;
        public ArrayList<TreeNode> nodes = new ArrayList<>();

        public TreeNode() {
            level = -1;
        }

        public TreeNode(TreeNode p) {
            parent = p;
            level = p.level + 1;
        }

        public TreeNode(Object tag) {
            this.tag = tag;
        }

        public TreeNode(TreeNode p, Object tag) {
            this(p);
            this.tag = tag;
        }
    }

    public static class TreeHolder {
        public View itemView;

        public TreeHolder(View itemView) {
            this.itemView = itemView;
        }
    }

    public static class TreeAdapter extends BaseAdapter {
        public TreeNode root = new TreeNode();
        public ArrayList<TreeNode> items = new ArrayList<>();

        public TreeAdapter() {
        }

        public void load() {
            items.clear();
            load(root);
            notifyDataSetChanged();
        }

        public void load(TreeNode tt) {
            for (TreeNode t : tt.nodes) {
                items.add(t);
                if (t.expanded)
                    load(t);
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public TreeNode getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }

    public interface OnToggleListener {
        void onItemToggled(View view, int position, long id);
    }

    public TreeListView(Context context) {
        super(context);
        gestures = new GestureDetectorCompat(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                int first = getFirstVisiblePosition();
                int motionPosition = pointToPosition((int) e.getX(), (int) e.getY());
                View child = getChildAt(motionPosition - first);
                if (child != null && child.hasFocusable()) {
                    if (performItemClick(child, motionPosition, getAdapter().getItemId(motionPosition)))
                        return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }

    public TreeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TreeListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public TreeListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        super.setOnItemClickListener(listener);
    }

    public void setOnToggleListener(OnToggleListener l) {
        toggleListener = l;
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        Adapter a = getAdapter();
        if (a instanceof HeaderViewListAdapter) {
            int start = ((HeaderViewListAdapter) a).getHeadersCount();
            int end = a.getCount() - ((HeaderViewListAdapter) a).getFootersCount();
            if (position < start)
                return false;
            if (position >= end)
                return false;
            position -= start;
            a = ((HeaderViewListAdapter) a).getWrappedAdapter();
        }
        TreeAdapter t = (TreeAdapter) a;
        TreeNode n = t.getItem(position);
        if (!n.nodes.isEmpty()) {
            n.expanded = !n.expanded;
            t.load();
            if (toggleListener != null)
                toggleListener.onItemToggled(view, position, id);
        }
        return super.performItemClick(view, position, id);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (gestures.onTouchEvent(ev))
            return true;
        return super.onTouchEvent(ev);
    }
}
