package com.github.topol777.androidlibrary.animations;

import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.animation.Animation;

import java.util.ArrayList;

public class ExpandItemAnimator extends DefaultItemAnimator {
    public static String TAG = ExpandItemAnimator.class.getSimpleName();

    public ArrayList<RecyclerView.ViewHolder> pending = new ArrayList<>();
    public ArrayList<RecyclerView.ViewHolder> animations = new ArrayList<>();
    public ArrayList<RecyclerView.ViewHolder> binding = new ArrayList<>();
    public RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            ExpandItemAnimator.this.onScrollStateChanged(newState);
        }
    };

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        pending.add(viewHolder);
        return true;
    }

    @Override
    public boolean animateChange(@NonNull final RecyclerView.ViewHolder oldHolder, @NonNull final RecyclerView.ViewHolder newHolder, @NonNull RecyclerView.ItemAnimator.ItemHolderInfo preInfo, @NonNull RecyclerView.ItemAnimator.ItemHolderInfo postInfo) {
        while (pending.remove(newHolder))
            ;
        if(binding.contains(newHolder)) {
            dispatchAnimationFinished(newHolder); // reduce mIsRecyclableCount
            return false;
        }
        Animation a = apply(newHolder, true);
        if (a == null) {
            dispatchAnimationFinished(newHolder); // reduce mIsRecyclableCount
            return false;
        }
        animations.add(newHolder);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                dispatchChangeStarting(newHolder, false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                while (animations.remove(newHolder))
                    dispatchChangeFinished(newHolder, false); // reduce mIsRecyclableCount
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        super.endAnimation(item);
        while (animations.remove(item)) {
            apply(item, false); // reset animation
            dispatchChangeFinished(item, false);
        }
        while (pending.remove(item))
            ;
        while (binding.remove(item)) {
            apply(item, false); // reset animation
        }
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() || !animations.isEmpty() || !binding.isEmpty();
    }

    @Override
    public void endAnimations() {
        super.endAnimations();
        animations.clear();
        pending.clear();
        binding.clear();
    }

    public void onBindViewHolder(final RecyclerView.ViewHolder h, int position) {
        if (animations.contains(h))
            return;
        if (pending.contains(h) || binding.contains(h)) { // API19< we need to start animation within onBindViewHolder()
            Animation a = apply(h, true);
            if (a == null)
                return;
            binding.add(h);
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    while (binding.remove(h))
                        ;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            return;
        }
        apply(h, false);
    }

    public void onScrollStateChanged(int state) {
        for (RecyclerView.ViewHolder h : animations) {
            Animation a = h.itemView.getAnimation();
            if (a instanceof ExpandAnimation)
                ((ExpandAnimation) a).adjust = false;
        }
    }

    public Animation apply(RecyclerView.ViewHolder h, boolean animate) {
        return null;
    }
}
