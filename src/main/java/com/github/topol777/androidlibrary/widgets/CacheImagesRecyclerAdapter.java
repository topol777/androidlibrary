package com.github.topol777.androidlibrary.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;

public abstract class CacheImagesRecyclerAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
    public static final String TAG = CacheImagesRecyclerAdapter.class.getSimpleName();

    public Adapter adapter;

    public class Adapter extends CacheImagesAdapter {
        public Adapter(Context context) {
            super(context);
        }

        @Override
        public Bitmap downloadImage(Uri cover, File f) throws IOException {
            return CacheImagesRecyclerAdapter.this.downloadImage(cover, f);
        }

        @Override
        public Bitmap downloadImageTask(DownloadImageTask task) {
            return CacheImagesRecyclerAdapter.this.downloadImageTask(task);
        }

        @Override
        public void downloadTaskUpdate(DownloadImageTask task, Object item, Object view) {
            CacheImagesRecyclerAdapter.this.downloadTaskUpdate(task, item, view);
        }

        @Override
        public void downloadTaskDone(DownloadImageTask task) {
            CacheImagesRecyclerAdapter.this.downloadTaskDone(task);
        }

        public void downloadTaskDoneSuper(DownloadImageTask task) {
            super.downloadTaskDone(task);
        }
    }

    public CacheImagesRecyclerAdapter(Context context) {
        adapter = new Adapter(context);
    }

    public Context getContext() {
        return adapter.context;
    }

    public void clearTasks() {
        adapter.clearTasks();
    }

    public void downloadTaskClean(Object view) {
        adapter.downloadTaskClean(view);
    }

    public void downloadTask(Object item, Object view) {
        adapter.downloadTask(item, view);
    }

    public Bitmap downloadImage(Uri cover, File f) throws IOException {
        return adapter.downloadImage(cover, f);
    }

    public Bitmap downloadImage(Uri cover) {
        return adapter.downloadImage(cover);
    }

    public Bitmap downloadImageTask(CacheImagesAdapter.DownloadImageTask task) {
        return null;
    }

    public void downloadTaskDone(CacheImagesAdapter.DownloadImageTask task) {
        adapter.downloadTaskDoneSuper(task);
    }

    public void downloadTaskUpdate(CacheImagesAdapter.DownloadImageTask task, Object i, Object o) {
    }

    public void updateView(CacheImagesAdapter.DownloadImageTask task, ImageView image, ProgressBar progress) {
        adapter.updateView(task, image, progress);
    }

}
