package com.github.topol777.androidlibrary.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.github.topol777.androidlibrary.R;
import com.github.topol777.androidlibrary.app.Storage;
import com.github.topol777.androidlibrary.crypto.MD5;
import com.github.topol777.androidlibrary.sound.MediaPlayerCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheImagesAdapter {
    public static final String TAG = CacheImagesAdapter.class.getSimpleName();

    public static int COVER_SIZE = 128;

    public static int CACHE_MB = 30;
    public static int CACHE_DAYS = 30;
    public static String CACHE_NAME = "cacheadapter_";

    public static String[] IMAGES = new String[]{"webp", "png", "jpg", "jpeg", "gif", "bmp"};

    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    public static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    public static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    public static final int KEEP_ALIVE_SECONDS = 30;

    public Context context;
    public Map<Object, DownloadImageTask> downloadViews = new HashMap<>();
    public Map<Object, DownloadImageTask> downloadItems = new HashMap<>();
    public Map<DownloadImageTask, Runnable> tasks = new ConcurrentHashMap<>();
    public Map<Runnable, DownloadImageTask> runs = new ConcurrentHashMap<>();
    protected DownloadImageTask current;

    public UriImagesExecutor executor = new UriImagesExecutor();

    public static Rect getImageSize(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        if (options.outWidth == -1 || options.outHeight == -1)
            return null;
        return new Rect(0, 0, options.outWidth, options.outHeight);
    }

    public static Bitmap createScaled(InputStream is) { // make image equals max or less
        return createScaled(is, COVER_SIZE);
    }

    public static Bitmap createScaled(InputStream is, int max) { // make image equals max or less
        DoubleReadInputStream dris = new DoubleReadInputStream(is);
        Rect size = getImageSize(dris);
        if (size == null)
            return null;
        dris.reload();
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        if (size.width() < size.height())
            bitmapOptions.inSampleSize = (int) Math.ceil(size.width() / (double) max);
        else
            bitmapOptions.inSampleSize = (int) Math.ceil(size.height() / (double) max);
        return BitmapFactory.decodeStream(dris, null, bitmapOptions);
    }

    public static Bitmap createScaled(Bitmap bm) {
        return createScaled(bm, CacheImagesAdapter.COVER_SIZE);
    }

    public static Bitmap createScaled(Bitmap bm, int max) { // scaled by min
        float ratio;
        if (bm.getWidth() < bm.getHeight())
            ratio = max / (float) bm.getWidth();
        else
            ratio = max / (float) bm.getHeight();
        int w = (int) (bm.getWidth() * ratio);
        int h = (int) (bm.getHeight() * ratio);
        Bitmap sbm = Bitmap.createScaledBitmap(bm, w, h, true);
        if (sbm != bm)
            bm.recycle();
        bm = sbm;
        return bm;
    }

    public static Bitmap createThumbnail(InputStream is) {
        Bitmap bm = createScaled(is, COVER_SIZE);
        if (bm == null)
            return null;
        return createThumbnail(bm);
    }

    public static Bitmap createThumbnail(Bitmap bm) { // scale by min width and cut rest
        int size;
        if (bm.getWidth() < bm.getHeight())
            size = bm.getWidth();
        else
            size = bm.getHeight();
        int l = (bm.getWidth() - size) / 2;
        int t = (bm.getHeight() - size) / 2;
        Bitmap sbm = Bitmap.createBitmap(COVER_SIZE, COVER_SIZE, Bitmap.Config.RGB_565);
        Rect src = new Rect(l, t, l + size, t + size);
        Rect dst = new Rect(0, 0, sbm.getWidth(), sbm.getHeight());
        Canvas canvas = new Canvas(sbm);
        canvas.drawBitmap(bm, src, dst, null);
        bm.recycle();
        return sbm;
    }

    @TargetApi(10)
    public static Bitmap createVideoThumbnail(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor pfd = MediaPlayerCompat.getFD(context, uri);
        FileDescriptor fd = pfd.getFileDescriptor();
        return createVideoThumbnail(fd);
    }

    @TargetApi(10)
    public static Bitmap createVideoThumbnail(FileDescriptor fd) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever(); // API10
        try {
            retriever.setDataSource(fd);
            Bitmap bm = retriever.getFrameAtTime(-1);
            if (bm == null)
                return null;
            return CacheImagesAdapter.createThumbnail(bm);
        } catch (Exception ignore) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    @TargetApi(23)
    public static Bitmap createVideoThumbnail(MediaDataSource source) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(source);
            Bitmap bm = retriever.getFrameAtTime(-1);
            if (bm == null)
                return null;
            return CacheImagesAdapter.createThumbnail(bm);
        } catch (Exception ignore) {
            Log.d(TAG, "read error", ignore);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
                Log.d(TAG, "release error", ignore);
            }
        }
        return null;
    }


    public static boolean isAudio(String name) {
        String mime = Storage.getTypeByName(name);
        return mime.startsWith("audio/");
    }

    @TargetApi(10)
    public static Bitmap createAudioThumbnail(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor pfd = MediaPlayerCompat.getFD(context, uri);
        FileDescriptor fd = pfd.getFileDescriptor();
        return createAudioThumbnail(fd);
    }

    @TargetApi(10)
    public static Bitmap createAudioThumbnail(FileDescriptor fd) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever(); // API10
        try {
            retriever.setDataSource(fd);
            byte[] buf = retriever.getEmbeddedPicture();
            if (buf == null)
                return null;
            Bitmap bm = BitmapFactory.decodeByteArray(buf, 0, buf.length);
            if (bm == null)
                return null;
            return CacheImagesAdapter.createThumbnail(bm);
        } catch (Exception ignore) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    @TargetApi(23)
    public static Bitmap createAudioThumbnail(MediaDataSource source) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever(); // API10
        try {
            retriever.setDataSource(source);
            byte[] buf = retriever.getEmbeddedPicture();
            Bitmap bm = BitmapFactory.decodeByteArray(buf, 0, buf.length);
            if (bm == null)
                return null;
            return CacheImagesAdapter.createThumbnail(bm);
        } catch (Exception ignore) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static boolean isImage(String name) { // supported images by BitmapFactory
        name = name.toLowerCase();
        for (String s : IMAGES) {
            if (name.endsWith("." + s))
                return true;
        }
        return false;
    }

    public static boolean isVideo(String name) {
        String mime = Storage.getTypeByName(name);
        return mime.startsWith("video/");
    }

    public static File getCache(Context context) {
        File cache = context.getExternalCacheDir();
        if (cache == null || !Storage.canWrite(cache))
            cache = context.getCacheDir();
        return cache;
    }

    public static File cacheUri(Context context, Uri u) {
        File cache = getCache(context);
        return new File(cache, CACHE_NAME + MD5.digest(u.toString()));
    }

    synchronized public static void cacheClear(Context context) {
        File cache = getCache(context);
        File[] ff = cache.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(CACHE_NAME);
            }
        });
        if (ff == null)
            return;
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -CACHE_DAYS);
        long total = 0;
        for (File f : ff) {
            long last = f.lastModified();
            if (last < c.getTimeInMillis())
                f.delete();
            else
                total += f.length();
        }
        Arrays.sort(ff, new SortDate());
        for (int i = 0; i < ff.length && total > CACHE_MB * 1024 * 1024; i++) {
            File f = ff[i];
            long size = f.length();
            f.delete();
            total -= size;
        }
    }

    synchronized public static void cachePurge(Context context) {
        File cache = getCache(context);
        File[] ff = cache.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(CACHE_NAME);
            }
        });
        if (ff == null)
            return;
        for (File f : ff)
            f.delete();
    }

    public static void cacheTouch(File cache) {
        Storage.touch(cache);
    }

    public static class SortDate implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return Long.valueOf(o1.lastModified()).compareTo(o2.lastModified());
        }
    }

    public static class ReadByteArrayOutputStream extends ByteArrayOutputStream {
        public int read(int pos) {
            if (pos >= count)
                return -1;
            return buf[pos];
        }

        public int read(int pos, byte[] b, int off, int len) {
            if (pos >= count)
                return 0;
            len = Math.min(len, count - pos);
            System.arraycopy(buf, pos, b, off, len);
            return len;
        }

        public int getSize() {
            return count;
        }

        public void ensureCapacity(int minCapacity) { // ByteArrayOutputStream.ensureCapacity()
            if (minCapacity - buf.length > 0)
                grow(minCapacity);
        }

        public void grow(int minCapacity) { // ByteArrayOutputStream.grow()
            int oldCapacity = buf.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity < 0) {
                if (minCapacity < 0) // overflow
                    throw new OutOfMemoryError();
                newCapacity = Integer.MAX_VALUE;
            }
            buf = Arrays.copyOf(buf, newCapacity);
        }

        public int write(InputStream is, int len) throws IOException {
            ensureCapacity(count + len);
            int total = 0;
            int l;
            while (len > 0 && (l = is.read(buf, count, len)) > 0) {
                count += l;
                total += l;
                len -= l;
            }
            return total;
        }
    }

    public static class DoubleReadInputStream extends SeekInputStream { // double read inputstream. only effective if first read was partial and small (few bytes)
        public boolean reset = false; // clear cache after second read

        public DoubleReadInputStream(InputStream is) {
            super(is);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (cache != null && reset && pos >= cache.getSize())
                cache = null;
            return b;
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            int l = super.read(b, off, len);
            if (cache != null && reset && pos >= cache.getSize())
                cache = null;
            return l;
        }

        public void reload() { // reset position to zero, free buffer after second read
            seek(0);
            reset = true;
        }
    }

    public static class SeekInputStream extends InputStream { // seek read inputstream. only effective if read's are small and at the begnning of file (few bytes)
        public InputStream is;
        public long count; // inputstream position
        public ReadByteArrayOutputStream cache = new ReadByteArrayOutputStream();
        public int pos; // current reading position

        public SeekInputStream(InputStream is) {
            this.is = is;
            this.count = 0;
        }

        public void fill(long pos, int size) throws IOException {
            int ll = (int) (pos + size - cache.getSize());
            if (ll > 0)
                count += cache.write(is, ll);
        }

        @Override
        public int read() throws IOException {
            if (cache == null) {
                int b = is.read();
                pos++;
                count++;
                return b;
            } else {
                fill(pos, 1);
                int b = cache.read(pos);
                pos++;
                return b;
            }
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            if (cache == null) {
                int l = is.read(b, off, len);
                pos += l;
                count += l;
                return l;
            } else {
                fill(pos, len);
                int l = cache.read(pos, b, off, len);
                pos += l;
                return l;
            }
        }

        public void seek(long pos) {
            this.pos = (int) pos;
        }
    }

    public class UriImagesExecutor extends ThreadPoolExecutor {
        public final BlockingQueue<Runnable> queue;
        public final ThreadFactory factory;

        public UriImagesExecutor(BlockingQueue<Runnable> q, ThreadFactory f) {
            super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, q, f);
            this.queue = q;
            this.factory = f;
            allowCoreThreadTimeOut(true);
        }

        public UriImagesExecutor() {
            this(new LinkedBlockingQueue<Runnable>(128), new ThreadFactory() {
                AtomicInteger mCount = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "CacheImagesAdapter #" + mCount.getAndIncrement());
                }
            });
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            DownloadImageTask task = runs.get(r);
            if (task != null) {
                synchronized (task.lock) {
                    task.start = true;
                }
            }
        }

        @Override
        public void execute(Runnable command) {
            tasks.put(current, command);
            runs.put(command, current);
            super.execute(command);
            current = null;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            DownloadImageTask task = runs.remove(r);
            if (task != null)
                tasks.remove(task);
        }

        public void execute(DownloadImageTask task) {
            current = task;
            if (Build.VERSION.SDK_INT < 11)
                task.execute();
            else
                task.executeOnExecutor(this);
        }

        public void cancel(DownloadImageTask task) {
            task.cancel(true);
            Runnable r = tasks.remove(task);
            if (r != null) {
                queue.remove(r);
                runs.remove(r);
            }
        }
    }

    public static class DownloadImageTask extends AsyncTask<Object, Void, Bitmap> {
        public final Object lock = new Object();
        public Bitmap bm;
        public Object item;
        public HashSet<Object> views = new HashSet<>(); // one task can set multiple ImageView's, except reused ones;
        public boolean start; // start download thread
        public boolean done; // done downloading (may be failed)
        CacheImagesAdapter a;

        public DownloadImageTask(CacheImagesAdapter a, Object item, Object v) {
            this.a = a;
            this.item = item;
            views.add(v);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Bitmap doInBackground(Object... urls) {
            return a.downloadImageTask(this);
        }

        protected void onPostExecute(Bitmap result) {
            if (isCancelled())
                return;
            done = true;
            if (result != null)
                bm = result;
            a.downloadTaskDone(this);
        }
    }

    public CacheImagesAdapter(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void refresh() {
    }

    public void downloadTaskClean(Object view) {
        DownloadImageTask task = downloadViews.get(view);
        if (task != null) { // reuse image
            task.views.remove(view);
            downloadViews.remove(view);
            synchronized (task.lock) {
                if (!task.start && task.views.size() == 0 && !task.done) {
                    executor.cancel(task);
                    downloadItems.remove(task.item);
                }
            }
        }
    }

    public void downloadTask(Object item, Object view) {
        downloadTaskClean(view);
        DownloadImageTask task = downloadItems.get(item);
        if (task != null) { // add new ImageView to populate on finish
            if (task.done) {
                downloadTaskUpdate(task, item, view);
                return;
            }
            task.views.add(view);
            downloadViews.put(view, task);
        }
        if (task == null) {
            task = new DownloadImageTask(this, item, view);
            downloadViews.put(view, task);
            downloadItems.put(item, task);
            executor.execute(task);
        }
        downloadTaskUpdate(task, item, view);
    }

    public void downloadTaskDone(DownloadImageTask task) {
        for (Object o : task.views)
            downloadTaskUpdate(task, task.item, o);
    }

    public void downloadTaskUpdate(DownloadImageTask task, Object item, Object view) {
    }

    public Bitmap downloadImageTask(DownloadImageTask task) {
        return null;
    }

    public void clearTasks() {
        for (Object item : downloadViews.keySet()) {
            DownloadImageTask t = downloadViews.get(item);
            executor.cancel(t);
        }
        downloadViews.clear();

        for (Object item : downloadItems.keySet()) {
            DownloadImageTask t = downloadItems.get(item);
            executor.cancel(t);
        }
        downloadItems.clear();

        tasks.clear();
        runs.clear();
    }

    public Bitmap downloadImage(Uri cover, File f) throws IOException {
        InputStream is = new URL(cover.toString()).openStream();
        Bitmap bm = createThumbnail(is);
        is.close();
        FileOutputStream os = new FileOutputStream(f);
        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
        return bm;
    }

    public Bitmap downloadImage(Uri cover) {
        try {
            cacheClear(context);
            String s = cover.getScheme();
            if (s.startsWith(WebViewCustom.SCHEME_HTTP)) {
                File f = cacheUri(context, cover);
                if (f.length() > 0) {
                    try {
                        return BitmapFactory.decodeStream(new FileInputStream(f));
                    } catch (Exception e) {
                        Log.d(TAG, "unable to read cache", e);
                        f.delete();
                    }
                }
                return downloadImage(cover, f);
            } else {
                return BitmapFactory.decodeFile(cover.getPath());
            }
        } catch (Exception e) {
            Log.e(TAG, "broken download", e);
        }
        return null;
    }

    public void updateView(DownloadImageTask task, ImageView image, ProgressBar progress) {
        if (task != null && task.bm != null)
            image.setImageBitmap(task.bm);
        else
            image.setImageResource(R.drawable.ic_image_black_24dp);
        progress.setVisibility((task == null || task.done) ? View.GONE : View.VISIBLE);
    }
}
