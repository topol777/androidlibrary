package com.github.topol777.androidlibrary.widgets;

import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

public class HeaderRecyclerAdapter extends RecyclerView.Adapter implements WrapperRecyclerAdapter {
    public static final int TYPE_HEADER = -1;
    public static final int TYPE_FOOTER = -2;

    protected RecyclerView.LayoutManager layoutManager;
    protected final RecyclerView.Adapter wrapped;
    protected View headerView, footerView;
    protected View empty;

    public HeaderRecyclerAdapter(@NonNull RecyclerView.Adapter wrapped) {
        this.wrapped = wrapped;
        this.wrapped.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                notifyDataSetChanged();
                updateEmpty();
            }

            public void onItemRangeChanged(int positionStart, int itemCount) {
                int start = hasHeader() ? 1 : 0;
                notifyItemRangeChanged(positionStart + start, itemCount);
            }

            public void onItemRangeInserted(int positionStart, int itemCount) {
                int start = hasHeader() ? 1 : 0;
                notifyItemRangeInserted(positionStart + start, itemCount);
                updateEmpty();
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                int start = hasHeader() ? 1 : 0;
                notifyItemRangeRemoved(positionStart + start, itemCount);
                updateEmpty();
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                int start = hasHeader() ? 1 : 0;
                notifyItemMoved(fromPosition + start, toPosition + start);
            }
        });
    }

    public void setHeaderView(View view) {
        headerView = view;
        notifyDataSetChanged();
    }

    public void setFooterView(View view) {
        footerView = view;
        notifyDataSetChanged();
    }

    public void setEmptyView(View v) {
        empty = v;
        updateEmpty();
    }

    public void updateEmpty() {
        if (empty == null)
            return;
        if (wrapped.getItemCount() == 0) {
            if (empty.getVisibility() == View.GONE)
                updateEmpty(true);
        } else {
            if (empty.getVisibility() == View.VISIBLE)
                updateEmpty(false);
        }
    }

    public void updateEmpty(boolean b) {
        empty.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    void updateGridHeaderFooter(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    boolean isShowHeader = (position == 0 && hasHeader());
                    boolean isShowFooter = (position == getItemCount() - 1 && hasFooter());
                    if (isShowFooter || isShowHeader) {
                        return gridLayoutManager.getSpanCount();
                    }
                    return 1;
                }
            });
        }
    }

    public boolean hasHeader() {
        return headerView != null && headerView.getVisibility() != View.GONE;
    }

    public boolean hasFooter() {
        return footerView != null && footerView.getVisibility() != View.GONE;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        layoutManager = recyclerView.getLayoutManager();
        updateGridHeaderFooter(layoutManager);
    }

    @Override
    public int getItemCount() {
        return wrapped.getItemCount() + (hasHeader() ? 1 : 0) + (hasFooter() ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0)
            return TYPE_HEADER;
        if (hasFooter() && position == getItemCount() - 1)
            return TYPE_FOOTER;
        return wrapped.getItemViewType(getWrappedPosition(position));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        if (viewType == TYPE_HEADER)
            itemView = headerView;
        else if (viewType == TYPE_FOOTER)
            itemView = footerView;
        if (itemView != null) {
            if (layoutManager instanceof StaggeredGridLayoutManager) {
                ViewGroup.LayoutParams targetParams = itemView.getLayoutParams();
                StaggeredGridLayoutManager.LayoutParams StaggerLayoutParams;
                if (targetParams != null) {
                    StaggerLayoutParams = new StaggeredGridLayoutManager.LayoutParams(targetParams.width, targetParams.height);
                } else {
                    StaggerLayoutParams = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                StaggerLayoutParams.setFullSpan(true);
                itemView.setLayoutParams(StaggerLayoutParams);
            }
            return new RecyclerView.ViewHolder(itemView) {
            };
        }
        return wrapped.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER || getItemViewType(position) == TYPE_FOOTER)
            return;
        if (holder instanceof WrapperRecyclerAdapter.ViewHolder)
            ((WrapperRecyclerAdapter.ViewHolder) holder).adapter = this;
        wrapped.onBindViewHolder(holder, getWrappedPosition(position));
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        wrapped.setHasStableIds(hasStableIds);
    }

    @Override
    public long getItemId(int position) {
        return wrapped.getItemId(position);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        wrapped.onViewRecycled(holder);
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        return wrapped.onFailedToRecycleView(holder);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        wrapped.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        wrapped.onViewDetachedFromWindow(holder);
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        wrapped.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public RecyclerView.Adapter getWrappedAdapter() {
        return wrapped;
    }

    @Override
    public int getWrappedPosition(int pos) {
        int start = hasHeader() ? 1 : 0;
        int end = getItemCount() - (hasFooter() ? 1 : 0);
        if (pos < start)
            return -1;
        if (pos >= end)
            return -1;
        pos -= start;
        return pos;
    }
}
