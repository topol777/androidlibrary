package com.github.topol777.androidlibrary.sound;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.topol777.androidlibrary.app.Storage;
import com.github.topol777.androidlibrary.preferences.AboutPreferenceCompat;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// To load different exoplayer versions '2.7.3' API 14+ or 'r2.5.4' API9+
// dynamically loading requires reflection to load clasees.
public class MediaPlayerCompat {
    public static String TAG = MediaPlayerCompat.class.getSimpleName();

    public static final int MEDIA_ERROR_SYSTEM = -2147483648;

    public static ClassLoader classLoader = MediaPlayerCompat.class.getClassLoader();

    public Listener listener;

    public static Class forName(String className) throws ClassNotFoundException {
        return Class.forName(className, true, classLoader);
    }

    public static String toStringError(int what, int extra) {
        String str = "";
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                str += "MEDIA_ERROR_UNKNOWN";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                str += "MEDIA_ERROR_SERVER_DIED";
                break;
            default:
                str += what;
                break;
        }
        str += " ";
        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:
                str += "MEDIA_ERROR_IO";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                str += "MEDIA_ERROR_MALFORMED";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                str += "MEDIA_ERROR_UNSUPPORTED";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                str += "MEDIA_ERROR_TIMED_OUT";
                break;
            case MEDIA_ERROR_SYSTEM:
                str += "MEDIA_ERROR_SYSTEM";
                break;
            default:
                str += extra;
                break;
        }
        return str;
    }

    public static ParcelFileDescriptor getFD(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor pfd;
        String s = uri.getScheme();
        if (s.equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver resolver = context.getContentResolver();
            if (uri.getAuthority().startsWith(Storage.SAF))
                pfd = resolver.openFileDescriptor(uri, "r"); // SAF always real file
            else
                pfd = resolver.openFileDescriptor(uri, "rw"); // 'rw' - always real file, no pipe allowed
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            pfd = ParcelFileDescriptor.open(Storage.getFile(uri), ParcelFileDescriptor.MODE_READ_ONLY);
        } else {
            throw new Storage.UnknownUri();
        }
        return pfd;
    }

    public static InputStream openInputStream(Context context, Uri uri) {
        try {
            String s = uri.getScheme();
            if (s.equals(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver resolver = context.getContentResolver();
                return resolver.openInputStream(uri);
            } else if (s.equals(ContentResolver.SCHEME_FILE)) {
                return new FileInputStream(Storage.getFile(uri));
            } else {
                throw new Storage.UnknownUri();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setDataSource(Context context, MediaPlayer player, Uri uri) throws IOException {
        ParcelFileDescriptor pfd = getFD(context, uri);
        FileDescriptor fd = pfd.getFileDescriptor();
        player.setDataSource(fd);
        pfd.close();
    }

    @TargetApi(10)
    public static void setDataSource(Context context, MediaMetadataRetriever m, Uri uri) throws IOException {
        ParcelFileDescriptor pfd = getFD(context, uri);
        FileDescriptor fd = pfd.getFileDescriptor();
        m.setDataSource(fd);
        pfd.close();
    }

    public static MediaPlayerCompat create(final Context context, final Uri uri) {
        try {
            final Wrapper w = new Wrapper(createExoPlayer25(context, uri));
            w.player.addListener(new Listener() {
                @Override
                public void onReady() {
                    if (w.listener != null)
                        w.listener.onReady();
                }

                @Override
                public void onEnd() {
                    w.setPlayWhenReady(false); // stop autoplay
                    if (w.listener != null)
                        w.listener.onEnd();
                }

                @Override
                public void onError(Exception e) {
                    if (e instanceof UnrecognizedInputFormatException) {
                        boolean b = w.player.getPlayWhenReady();
                        w.player.release();
                        w.player = MediaPlayerCompat.createMediaPlayer(context, uri);
                        if (w.player != null) {
                            w.player.addListener(new Listener() {
                                @Override
                                public void onReady() {
                                    if (w.listener != null)
                                        w.listener.onReady();
                                }

                                @Override
                                public void onEnd() {
                                    if (w.listener != null)
                                        w.listener.onEnd();
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (w.listener != null)
                                        w.listener.onError(e);
                                }
                            });
                            w.player.prepare();
                            w.player.setPlayWhenReady(b);
                            return;
                        }
                    }
                    if (w.listener != null)
                        w.listener.onError(e);
                }
            });
            return w;
        } catch (RuntimeException e) {
            Log.e(TAG, "exo create failed", e);
            return createMediaPlayer(context, uri);
        }
    }

    public static MediaPlayerCompat createMediaPlayer(final Context context, final Uri uri) {
        final android.media.MediaPlayer mp = createMediaPlayer(context, uri, null);
        if (mp == null)
            return null;
        return new MediaPlayerCompat() {
            android.media.MediaPlayer player = mp;
            boolean prepared = false;
            boolean preparedPlay = false;

            {
                setListeners();
            }

            public void setListeners() {
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        prepared = true;
                        if (preparedPlay)
                            player.start();
                        if (listener != null)
                            listener.onReady();
                    }
                });
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (listener != null)
                            listener.onEnd();
                    }
                });
                player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        if (listener != null)
                            listener.onError(new Exception(toStringError(what, extra)));
                        return true;
                    }
                });
            }

            @Override
            public void prepare() {
                player.prepareAsync();
            }

            @Override
            public long getCurrentPosition() {
                if (!prepared)
                    return -1;
                return player.getCurrentPosition();
            }

            @Override
            public void seekTo(long pos) {
                player.seekTo((int) pos);
            }

            @Override
            public void release() {
                player.release();
            }

            @Override
            public boolean getPlayWhenReady() {
                if (!prepared)
                    return preparedPlay;
                return player.isPlaying();
            }

            @Override
            public void setPlayWhenReady(boolean b) {
                if (!prepared) {
                    preparedPlay = b;
                    return;
                }
                if (b) {
                    if (!player.isPlaying())
                        player.start();
                } else {
                    if (player.isPlaying())
                        player.pause();
                }
            }

            @Override
            public long getDuration() {
                if (!prepared)
                    return 0;
                return player.getDuration();
            }

            @Override
            public Bitmap getArtwork() {
                if (Build.VERSION.SDK_INT >= 10) {
                    try {
                        MediaMetadataRetriever m = new MediaMetadataRetriever();
                        setDataSource(context, m, uri);
                        byte[] art = m.getEmbeddedPicture();
                        if (art == null)
                            return null;
                        return BitmapFactory.decodeByteArray(art, 0, art.length);
                    } catch (IOException e) {
                        Log.e(TAG, "artwork", e);
                        return null;
                    }
                } else {
                    return super.getArtwork();
                }
            }

            @TargetApi(21)
            @Override
            public void setAudioAttributes(AudioAttributes aa) { // ExoPlayer can call setAudioAttributes while playing, MediaPlayer can't
                int pos = player.getCurrentPosition();
                boolean b = player.isPlaying();
                player.release();
                player = createMediaPlayer(context, uri, aa);
                setListeners();
                player.seekTo(pos);
                if (b)
                    player.start();
            }

            @Override
            public void setAudioStreamType(int streamType) {
                int pos = player.getCurrentPosition();
                boolean b = player.isPlaying();
                player.release();
                player = createMediaPlayer(context, uri, streamType);
                setListeners();
                player.seekTo(pos);
                if (b)
                    player.start();
            }

            @Override
            public View createView() {
                return new MovieView(context, uri);
            }
        };
    }

    public static MediaPlayer createMediaPlayer(final Context context, final Uri uri, AudioAttributes aa) { // MediaPlayer.create(context, u) failed with ':' in uri
        final MediaPlayer mp = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21) {
            final AudioAttributes a = aa != null ? aa : new AudioAttributes.Builder().build();
            mp.setAudioAttributes(a);
        }
        try {
            setDataSource(context, mp, uri);
            return mp;
        } catch (IOException e) {
            Log.d(TAG, "unable to create MediaPlayer", e);
            return null;
        }
    }

    public static MediaPlayer createMediaPlayer(final Context context, final Uri uri, int streamType) {
        final MediaPlayer mp = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21)
            mp.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(streamType).build());
        else
            mp.setAudioStreamType(streamType);
        try {
            setDataSource(context, mp, uri);
            return mp;
        } catch (IOException e) {
            Log.d(TAG, "unable to create MediaPlayer", e);
            return null;
        }
    }

    public static Object createExoTrackSelector() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Class BandwidthMeter = forName("com.google.android.exoplayer2.upstream.BandwidthMeter");
        Class DefaultBandwidthMeter = forName("com.google.android.exoplayer2.upstream.DefaultBandwidthMeter");
        Object defaultBandwidthMeter = DefaultBandwidthMeter.newInstance();
        Object videoTrackSelectionFactory = forName("com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection$Factory").getConstructor(BandwidthMeter).newInstance(defaultBandwidthMeter);
        Class TrackSelection$Factory = forName("com.google.android.exoplayer2.trackselection.TrackSelection$Factory");
        Class DefaultTrackSelector = forName("com.google.android.exoplayer2.trackselection.DefaultTrackSelector");
        final Object trackSelector = DefaultTrackSelector.getConstructor(TrackSelection$Factory).newInstance(videoTrackSelectionFactory);
        return trackSelector;
    }

    @SuppressWarnings("unchecked")
    public static Object createExoPlayer(Context context) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Class TrackSelector = forName("com.google.android.exoplayer2.trackselection.TrackSelector");
        Object trackSelector = createExoTrackSelector();
        return forName("com.google.android.exoplayer2.ExoPlayerFactory").getMethod("newSimpleInstance", Context.class, TrackSelector).invoke(null, context, trackSelector);
    }

    @SuppressWarnings("unchecked")
    public static MediaPlayerCompat createExoPlayer(final Context context, final Object player, final Object source) {
        try {
            final Class Player = forName("com.google.android.exoplayer2.Player");
            final Class ExoPlayer = forName("com.google.android.exoplayer2.ExoPlayer");
            final Class SimpleExoPlayer = forName("com.google.android.exoplayer2.SimpleExoPlayer");
            Class EventListener = forName("com.google.android.exoplayer2.Player$EventListener");
            final int STATE_BUFFERING = ExoPlayer.getField("STATE_BUFFERING").getInt(null);
            final int STATE_IDLE = ExoPlayer.getField("STATE_IDLE").getInt(null);
            final int STATE_READY = ExoPlayer.getField("STATE_READY").getInt(null);
            final int STATE_ENDED = ExoPlayer.getField("STATE_ENDED").getInt(null);
            final Class ExoPlaybackException = forName("com.google.android.exoplayer2.ExoPlaybackException");
            final Class UnrecognizedInputFormatException = forName("com.google.android.exoplayer2.source.UnrecognizedInputFormatException");
            final Class ParserException = forName("com.google.android.exoplayer2.ParserException");
            final Method getCurrentPosition = Player.getDeclaredMethod("getCurrentPosition");
            final Method seekTo = Player.getDeclaredMethod("seekTo", long.class);
            final Method release = Player.getDeclaredMethod("release");
            final Method getPlayWhenReady = Player.getDeclaredMethod("getPlayWhenReady");
            final Method setPlayWhenReady = Player.getDeclaredMethod("setPlayWhenReady", boolean.class);
            final Method getDuration = Player.getDeclaredMethod("getDuration");
            final Method getCurrentTrackSelections = Player.getDeclaredMethod("getCurrentTrackSelections");
            final Class AudioAttributes = forName("com.google.android.exoplayer2.audio.AudioAttributes");
            final Method setAudioAttributes = SimpleExoPlayer.getDeclaredMethod("setAudioAttributes", AudioAttributes);
            final Method setAudioStreamType = SimpleExoPlayer.getDeclaredMethod("setAudioStreamType", int.class);
            Class TrackSelectionArray = forName("com.google.android.exoplayer2.trackselection.TrackSelectionArray");
            final Field TrackSelectionsLength = TrackSelectionArray.getField("length");
            final Method TrackSelectionsGet = TrackSelectionArray.getDeclaredMethod("get", int.class);
            Class TrackSelection = forName("com.google.android.exoplayer2.trackselection.TrackSelection");
            final Method TrackSelectionLength = TrackSelection.getDeclaredMethod("length");
            final Method TrackSelectionGetFormat = TrackSelection.getDeclaredMethod("getFormat", int.class);
            Class Metadata = forName("com.google.android.exoplayer2.metadata.Metadata");
            final Method MetadataLength = Metadata.getDeclaredMethod("length");
            final Method MetadataGet = Metadata.getDeclaredMethod("get", int.class);
            Class Format = forName("com.google.android.exoplayer2.Format");
            final Field FormatMetadata = Format.getField("metadata");
            final Class ApicFrame = forName("com.google.android.exoplayer2.metadata.id3.ApicFrame");
            final Field pictureData = ApicFrame.getField("pictureData");
            final Class Util = forName("com.google.android.exoplayer2.util.Util");
            final Method getAudioUsageForStreamType = Util.getMethod("getAudioUsageForStreamType", int.class);
            final Method getAudioContentTypeForStreamType = Util.getMethod("getAudioContentTypeForStreamType", int.class);
            Class C = forName("com.google.android.exoplayer2.C");
            final long TIME_UNSET = C.getField("TIME_UNSET").getLong(null);
            final Class PlayerView = forName("com.google.android.exoplayer2.ui.PlayerView");
            final Class AudioAttributes$Builder = forName("com.google.android.exoplayer2.audio.AudioAttributes$Builder");
            final Class MediaSource = forName("com.google.android.exoplayer2.source.MediaSource");
            final MediaPlayerCompat mp = new MediaPlayerCompat() {
                @Override
                public void prepare() {
                    try {
                        ExoPlayer.getDeclaredMethod("prepare", MediaSource).invoke(player, source);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public long getCurrentPosition() {
                    try {
                        return (long) getCurrentPosition.invoke(player);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void seekTo(long pos) {
                    try {
                        seekTo.invoke(player, pos);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void release() {
                    try {
                        release.invoke(player);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public boolean getPlayWhenReady() {
                    try {
                        return (boolean) getPlayWhenReady.invoke(player);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void setPlayWhenReady(boolean b) {
                    try {
                        setPlayWhenReady.invoke(player, b);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public long getDuration() {
                    try {
                        long d = (long) getDuration.invoke(player);
                        if (d == TIME_UNSET)
                            return 0;
                        return d;
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Bitmap getArtwork() {
                    try {
                        Object tt = getCurrentTrackSelections.invoke(player);
                        if (tt == null)
                            return null;
                        for (int i = 0; i < TrackSelectionsLength.getInt(tt); i++) {
                            Object selection = TrackSelectionsGet.invoke(tt, i);
                            if (selection != null) {
                                for (int j = 0; j < (int) TrackSelectionLength.invoke(selection); j++) {
                                    Object metadata = FormatMetadata.get(TrackSelectionGetFormat.invoke(selection, j));
                                    if (metadata != null) {
                                        for (int k = 0; k < (int) MetadataLength.invoke(metadata); k++) {
                                            Object metadataEntry = MetadataGet.invoke(metadata, k);
                                            if (ApicFrame.isInstance(metadataEntry)) {
                                                byte[] data = (byte[]) pictureData.get(metadataEntry);
                                                return BitmapFactory.decodeByteArray(data, 0, data.length);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @TargetApi(21)
                @Override
                public void setAudioAttributes(AudioAttributes audioAttributes) {
                    try {
                        Object b = AudioAttributes$Builder.newInstance();
                        AudioAttributes$Builder.getDeclaredMethod("setUsage", int.class).invoke(b, audioAttributes.getUsage());
                        AudioAttributes$Builder.getDeclaredMethod("setContentType", int.class).invoke(b, audioAttributes.getContentType());
                        Object aa = AudioAttributes$Builder.getDeclaredMethod("build").invoke(b);
                        setAudioAttributes.invoke(player, aa);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void setAudioStreamType(int streamType) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        try {
                            int usage = (int) getAudioUsageForStreamType.invoke(null, streamType);
                            int contentType = (int) getAudioContentTypeForStreamType.invoke(null, streamType);
                            Object b = AudioAttributes$Builder.newInstance();
                            AudioAttributes$Builder.getDeclaredMethod("setUsage", int.class).invoke(b, usage);
                            AudioAttributes$Builder.getDeclaredMethod("setContentType", int.class).invoke(b, contentType);
                            Object aa = AudioAttributes$Builder.getDeclaredMethod("build").invoke(b);
                            setAudioAttributes.invoke(player, aa);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            setAudioStreamType.invoke(player, streamType); // to support ExoMediaPlayer API21<
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                @Override
                public View createView() {
                    try {
                        View v = (View) PlayerView.getConstructor(Context.class).newInstance(context);
                        PlayerView.getDeclaredMethod("setPlayer", Player).invoke(v, player);
                        PlayerView.getDeclaredMethod("setUseController", boolean.class).invoke(v, false);
                        return v;
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            InvocationHandler e = new InvocationHandler() {
                void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    if (playbackState == STATE_READY) {
                        if (mp.listener != null)
                            mp.listener.onReady();
                    }
                    if (playbackState == STATE_ENDED) {
                        if (mp.listener != null)
                            mp.listener.onEnd();
                    }
                }

                void onPlayerError(Exception e) {
                    e = (Exception) e.getCause();
                    if (UnrecognizedInputFormatException.isInstance(e) || ParserException.isInstance(e))
                        e = new UnrecognizedInputFormatException(e);
                    if (mp.listener != null)
                        mp.listener.onError(e);
                }

                void onTimelineChanged(Object timeline, @Nullable Object manifest, int reason) {
                    if (mp.listener instanceof ExoListener)
                        ((ExoListener) mp.listener).onTimelineChanged();
                }

                void onTracksChanged(Object trackGroups, Object trackSelections) {
                    if (mp.listener instanceof ExoListener)
                        ((ExoListener) mp.listener).onTracksChanged();
                }

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    switch (method.getName()) {
                        case "onPlayerStateChanged":
                            onPlayerStateChanged((boolean) args[0], (int) args[1]);
                            break;
                        case "onPlayerError":
                            onPlayerError((Exception) args[0]);
                            break;
                        case "onTimelineChanged":
                            onTimelineChanged(args[0], args[1], (int) args[2]);
                            break;
                        case "onTracksChanged":
                            onTracksChanged(args[0], args[1]);
                            break;
                    }
                    return null;
                }
            };
            Player.getDeclaredMethod("addListener", EventListener).invoke(player, Proxy.newProxyInstance(EventListener.getClassLoader(), new Class[]{EventListener}, e));
            return mp;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object createExoSource(Context context, Uri uri) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class Util = forName("com.google.android.exoplayer2.util.Util");
        Object dataSourceFactory = forName("com.google.android.exoplayer2.upstream.DefaultDataSourceFactory").getConstructor(Context.class, String.class).newInstance(context, Util.getMethod("getUserAgent", Context.class, String.class).invoke(null, context, AboutPreferenceCompat.getApplicationName(context)));
        Class DataSource$Factory = forName("com.google.android.exoplayer2.upstream.DataSource$Factory");
        Class ExtractorMediaSource = forName("com.google.android.exoplayer2.source.ExtractorMediaSource");
        Class DefaultExtractorsFactory = forName("com.google.android.exoplayer2.extractor.DefaultExtractorsFactory");
        Class EventListener = forName("com.google.android.exoplayer2.source.ExtractorMediaSource$EventListener");
        Class ExtractorsFactory = forName("com.google.android.exoplayer2.extractor.ExtractorsFactory");
        return ExtractorMediaSource.getConstructor(Uri.class, DataSource$Factory, ExtractorsFactory, Handler.class, EventListener).newInstance(uri, dataSourceFactory, DefaultExtractorsFactory.newInstance(), null, null);
    }

    public static MediaPlayerCompat createExoPlayer25(Context context, Uri uri) { // 2.7 compatible
        try {
            Object player = createExoPlayer(context);
            Object source = createExoSource(context, uri);
            return createExoPlayer(context, player, source);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static MediaPlayerCompat createExoPlayer27(Context context, Uri uri) {
        try {
            Object player = createExoPlayer(context);
            Class Util = forName("com.google.android.exoplayer2.util.Util");
            Object dataSourceFactory = forName("com.google.android.exoplayer2.upstream.DefaultDataSourceFactory").getConstructor(Context.class, String.class).newInstance(context, Util.getMethod("getUserAgent", Context.class, String.class).invoke(null, context, AboutPreferenceCompat.getApplicationName(context)));
            Class DataSource$Factory = forName("com.google.android.exoplayer2.upstream.DataSource$Factory");
            Class ExtractorMediaSource$Factory = forName("com.google.android.exoplayer2.source.ExtractorMediaSource$Factory");
            Object factory = ExtractorMediaSource$Factory.getConstructor(DataSource$Factory).newInstance(dataSourceFactory);
            Object source = ExtractorMediaSource$Factory.getDeclaredMethod("createMediaSource", Uri.class).invoke(factory, uri);
            return createExoPlayer(context, player, source);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static class PlayerView extends FrameLayout {
        public PlayerView(Context context) {
            super(context);
        }

        public PlayerView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public PlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @TargetApi(21)
        public PlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        public void setPlayer(MediaPlayerCompat c) {
            removeAllViews();
            addView(c.createView(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    public static class MovieDrawable extends AnimationDrawable {
        public static int STEP = 1000 / 24; // 24 frames per second
        public Movie m;
        public int frame = 0;
        public int duration;
        public boolean mRunning;
        public int w;
        public int h;

        public MovieDrawable(Movie m) {
            if (m == null)
                return;
            this.m = m;
            int d = m.duration();
            if (d == 0)
                d = 1000;
            duration = d / STEP;
            w = m.width();
            h = m.height();
        }

        public MovieDrawable(InputStream is) {
            this(Movie.decodeStream(is));
        }

        public MovieDrawable(String file) {
            this(Movie.decodeFile(file));
        }

        @Override
        public int getIntrinsicWidth() {
            return w;
        }

        @Override
        public int getIntrinsicHeight() {
            return h;
        }

        @Override
        public Drawable getFrame(int index) {
            return this;
        }

        @Override
        public int getDuration(int i) {
            return STEP;
        }

        @Override
        public void run() {
            frame++;
            if (frame > duration)
                frame = 0;
            invalidateSelf();
            schedule();
        }

        void schedule() {
            unscheduleSelf(this);
            scheduleSelf(this, SystemClock.uptimeMillis() + STEP);
        }

        @Override
        public int getNumberOfFrames() {
            return duration;
        }

        @Override
        public void draw(Canvas canvas) {
            m.setTime(frame * STEP);
            m.draw(canvas, 0, 0);
        }

        @Override
        public void start() {
            if (!isRunning()) {
                mRunning = true;
                schedule();
            }
        }

        @Override
        public void stop() {
            mRunning = false;
            unscheduleSelf(this);
        }

        @Override
        public boolean isRunning() {
            return mRunning;
        }
    }

    public static class MovieView extends AppCompatImageView {
        public Paint p = new Paint();

        public MovieView(Context context, Uri is) {
            this(context, new MediaPlayerCompat.MovieDrawable(openInputStream(context, is)));
        }

        public MovieView(Context context, InputStream is) {
            this(context, new MediaPlayerCompat.MovieDrawable(is));
        }

        public MovieView(Context context, String file) {
            this(context, new MediaPlayerCompat.MovieDrawable(file));
        }

        public MovieView(Context context, MediaPlayerCompat.MovieDrawable g) {
            super(context);
            setImageDrawable(g);
            g.start();
            if (Build.VERSION.SDK_INT >= 11)
                setLayerType(LAYER_TYPE_SOFTWARE, p);
        }
    }

    public static class UnrecognizedInputFormatException extends RuntimeException {
        public UnrecognizedInputFormatException(Exception e) {
            super(e);
        }
    }

    public static class Wrapper extends MediaPlayerCompat {
        public MediaPlayerCompat player;

        public Wrapper(MediaPlayerCompat mp) {
            player = mp;
        }

        @Override
        public void prepare() {
            player.prepare();
        }

        public long getCurrentPosition() {
            return player.getCurrentPosition();
        }

        public void seekTo(long pos) {
            player.seekTo(pos);
        }

        public void release() {
            player.release();
            player = null;
        }

        public boolean getPlayWhenReady() {
            return player.getPlayWhenReady();
        }

        public void setPlayWhenReady(boolean b) {
            player.setPlayWhenReady(b);
        }

        public long getDuration() {
            return player.getDuration();
        }

        public Bitmap getArtwork() {
            return player.getArtwork();
        }

        @TargetApi(21)
        public void setAudioAttributes(AudioAttributes audioAttributes) {
            player.setAudioAttributes(audioAttributes);
        }

        public void setAudioStreamType(int streamType) {
            player.setAudioStreamType(streamType);
        }

        public void addListener(Listener e) {
            listener = e;
        }

        public View createView() {
            return player.createView();
        }
    }

    public interface ExoListener extends Listener {
        void onTimelineChanged();

        void onTracksChanged();
    }


    public interface Listener {
        void onReady();

        void onEnd();

        void onError(Exception e);
    }

    public MediaPlayerCompat() {
    }

    public void prepare() {
    }

    public long getCurrentPosition() {
        return -1;
    }

    public void seekTo(long pos) {
    }

    public void release() {
    }

    public boolean getPlayWhenReady() {
        return false;
    }

    public void setPlayWhenReady(boolean b) {
    }

    public long getDuration() {
        return -1;
    }

    public Bitmap getArtwork() {
        return null;
    }

    @TargetApi(21)
    public void setAudioAttributes(AudioAttributes audioAttributes) {
    }

    public void setAudioStreamType(int streamType) {
    }

    public void addListener(Listener e) {
        listener = e;
    }

    public View createView() {
        return null;
    }
}
