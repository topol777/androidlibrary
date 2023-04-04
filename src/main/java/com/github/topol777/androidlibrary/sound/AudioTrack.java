package com.github.topol777.androidlibrary.sound;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

public class AudioTrack extends android.media.AudioTrack {
    public static String TAG = AudioTrack.class.getSimpleName();

    public static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;
    public static final int FLOAT_SIZE = Float.SIZE / Byte.SIZE;

    public long playStart = 0; // start time, reset on end
    public int len; // len in frames (stereo frames = len * 2)
    public int frames; // frames written to audiotrack (including zeros, stereo frames = frames)

    Handler playbackHandler = new Handler();
    Runnable playbackUpdate;
    OnPlaybackPositionUpdateListener userListener;
    OnPlaybackPositionUpdateListener systemListener = new OnPlaybackPositionUpdateListener() {
        @Override
        public void onMarkerReached(android.media.AudioTrack track) {
            Log.d(TAG, "onMarkerReached");
            if (playStart <= 0)
                return;
            playStart = 0;
            if (userListener != null)
                userListener.onMarkerReached(track);
        }

        @Override
        public void onPeriodicNotification(android.media.AudioTrack track) {
            Log.d(TAG, "onPeriodicNotification");
            if (userListener != null)
                userListener.onPeriodicNotification(track);
        }
    };

    int markerInFrames = -1;
    int periodInFrames = 1000;

    // AudioTrack unable to play shorter then 'min' size of data, fill it with zeros
    public static int getMinSize(int sampleRate, int c, int audioFormat, int b) {
        int min = android.media.AudioTrack.getMinBufferSize(sampleRate, c, audioFormat);
        if (b < min)
            b = min;
        return b;
    }

    // streamType AudioManager#STREAM_MUSIC
    // usage AudioAttributes#USAGE_MEDIA
    // ct AudioAttributes#CONTENT_TYPE_MUSIC
    public static AudioTrack create(int streamType, int usage, int ct, AudioBuffer buffer) {
        AudioTrack t = create(streamType, usage, ct, buffer, buffer.getBytesMin());
        t.write(buffer);
        return t;
    }

    public static AudioTrack create(int streamType, int usage, int ct, AudioParams buffer, int len) {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes.Builder b = new AudioAttributes.Builder();
            b.setUsage(usage);
            b.setContentType(ct);
            AudioAttributes a = b.build();
            return new AudioTrack(a, buffer, len);
        } else {
            return new AudioTrack(streamType, buffer, len);
        }
    }

    public static class SamplesBuffer {
        public int format; // AudioFormat.ENCODING_PCM_16BIT
        public int capacity; // samples count
        public int pos;
        public int limit;
        public byte[] bytes; // 8 bits
        public short[] shorts; // 16 bits
        public int[] ints; // 24-bits or 32-bits
        public float[] floats; // PCM float

        public SamplesBuffer(int format, int capacity) {
            this.format = format;
            this.capacity = capacity;
            switch (format) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    bytes = new byte[capacity];
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    shorts = new short[capacity];
                    break;
                case Sound.ENCODING_PCM_24BIT_PACKED:
                    ints = new int[capacity];
                    break;
                case Sound.ENCODING_PCM_32BIT:
                    ints = new int[capacity];
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    floats = new float[capacity];
                    break;
            }
            clear();
        }

        public void flip() {
            limit = pos;
            pos = 0;
        }

        public void rewind() {
            pos = 0;
        }

        public void clear() {
            pos = 0;
            limit = capacity;
        }

        public final int remaining() {
            int rem = limit - pos;
            return rem > 0 ? rem : 0;
        }

        public void limit(int newLimit) {
            if (newLimit > capacity | newLimit < 0)
                throw new RuntimeException("bad limit" + newLimit);
            limit = newLimit;
            if (pos > newLimit) pos = newLimit;
        }

        public void put(SamplesBuffer buf) { // put all samples from but into current buffer
            int tail = buf.limit - buf.pos;
            switch (format) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    System.arraycopy(buf.shorts, 0, shorts, this.pos, tail);
                    break;
                case Sound.ENCODING_PCM_24BIT_PACKED:
                    break;
                case Sound.ENCODING_PCM_32BIT:
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    System.arraycopy(buf.floats, 0, floats, this.pos, tail);
                    break;
            }
            pos += tail;
        }

        public void put(SamplesBuffer buf, int pos, int size) { // pos - target buffer
            switch (format) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    System.arraycopy(buf.shorts, pos, shorts, this.pos, size);
                    break;
                case Sound.ENCODING_PCM_24BIT_PACKED:
                    break;
                case Sound.ENCODING_PCM_32BIT:
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    System.arraycopy(buf.floats, pos, floats, this.pos, size);
                    break;
            }
            this.pos += size;
        }

        public int get(SamplesBuffer buf, int pos, int size) { // pos - target buffer
            switch (format) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    return -1;
                case AudioFormat.ENCODING_PCM_16BIT:
                    System.arraycopy(shorts, this.pos, buf.shorts, pos, size);
                    break;
                case Sound.ENCODING_PCM_24BIT_PACKED:
                    return -1;
                case Sound.ENCODING_PCM_32BIT:
                    return -1;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    System.arraycopy(floats, this.pos, buf.floats, pos, size);
                    break;
                default:
                    throw new RuntimeException("Unknown format");
            }
            this.pos += size;
            return size;
        }
    }

    public static class AudioParams {
        public int hz; // sample rate
        public int c; // AudioFormat.CHANNEL_OUT_MONO or AudioFormat.CHANNEL_OUT_STEREO
        public int a; // AudioFormat.ENCODING_PCM_16BIT

        public int getChannels() {
            switch (c) {
                case AudioFormat.CHANNEL_OUT_MONO:
                    return 1;
                case AudioFormat.CHANNEL_OUT_STEREO:
                    return 2;
                default:
                    throw new RuntimeException("unknown mode");
            }
        }

        @TargetApi(21)
        public AudioFormat getAudioFormat() {
            AudioFormat.Builder builder = new AudioFormat.Builder();
            builder.setEncoding(a);
            builder.setSampleRate(hz);
            builder.setChannelMask(c);
            return builder.build();
        }
    }

    public static class AudioBuffer extends AudioParams {
        public SamplesBuffer buffer; // buffer including zeros (to fill minimum size)
        public int len; // buffer samples length
        public int pos; // write AudioTrack pos

        public AudioBuffer(int sampleRate, int channelConfig, int audioFormat, SamplesBuffer buf, int len) {
            this.hz = sampleRate;
            this.c = channelConfig;
            this.a = audioFormat;
            this.buffer = buf;
            this.len = len;
        }

        public AudioBuffer(int sampleRate, int channelConfig, int audioFormat, int len) {
            this.hz = sampleRate;
            this.c = channelConfig;
            this.a = audioFormat;

            int b = len * SHORT_SIZE;
            b = getMinSize(sampleRate, channelConfig, audioFormat, b);
            if (b <= 0)
                throw new RuntimeException("unable to get min size");
            int blen = b / SHORT_SIZE;

            this.len = len;
            this.buffer = new SamplesBuffer(audioFormat, blen);
        }

        public AudioBuffer(int sampleRate, int channelConfig, int audioFormat) {
            this.hz = sampleRate;
            this.c = channelConfig;
            this.a = audioFormat;
            this.len = getMinSize(sampleRate, c, audioFormat, 0);
            if (len <= 0)
                throw new RuntimeException("unable to initialize audio");
            this.buffer = new SamplesBuffer(audioFormat, len);
        }

        public void write(short[] buf, int pos, int len) {
            System.arraycopy(buf, pos, buffer.shorts, 0, len);
        }

        public void write(int pos, short s) {
            buffer.shorts[pos] = s;
        }

        public void write(int pos, short s1, short s2) {
            buffer.shorts[pos] = s1;
            buffer.shorts[pos + 1] = s2;
        }

        public void write(int pos, short... ss) {
            System.arraycopy(ss, 0, buffer.shorts, pos, ss.length);
        }

        public void write(int pos, short s, int cn) {
            for (int i = 0; i < cn; i++)
                buffer.shorts[pos + i] = s;
        }

        public void write(int pos, float f) {
            buffer.floats[pos] = f;
        }

        public void write(int pos, float f, float f2) {
            buffer.floats[pos] = f;
            buffer.floats[pos + 1] = f2;
        }

        public void write(int pos, float s, int cn) {
            for (int i = 0; i < cn; i++)
                buffer.floats[pos + i] = s;
        }

        public void write(float[] buf, int pos, int len) {
            System.arraycopy(buf, pos, buffer.floats, 0, len);
        }

        public void reset() {
            pos = 0;
        }

        public int getBytesLen() {
            switch (a) {
                case AudioFormat.ENCODING_PCM_16BIT:
                    return buffer.capacity * SHORT_SIZE;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    return buffer.capacity * FLOAT_SIZE;
                default:
                    throw new RuntimeException("unknown format");
            }
        }

        public int getBytesMin() {
            return getMinSize(hz, c, a, getBytesLen());
        }
    }

    public static class NotInitializedException extends RuntimeException {
    }

    // old phones bug.
    // http://stackoverflow.com/questions/27602492
    //
    // with MODE_STATIC setNotificationMarkerPosition not called
    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) throws IllegalArgumentException {
        super(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, MODE_STREAM);
    }

    public AudioTrack(int streamType, AudioBuffer buffer) throws IllegalArgumentException {
        this(streamType, buffer, buffer.getBytesMin());
        write(buffer);
    }

    @TargetApi(21)
    public AudioTrack(AudioAttributes a, AudioBuffer buffer) throws IllegalArgumentException {
        this(a, buffer, buffer.getBytesMin());
        write(buffer);
    }

    public AudioTrack(int streamType, AudioParams buffer, int len) throws IllegalArgumentException {
        super(streamType, buffer.hz, buffer.c, buffer.a, len, MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (getState() != STATE_INITIALIZED)
            throw new NotInitializedException();
    }

    @TargetApi(21)
    public AudioTrack(AudioAttributes a, AudioParams buffer, int len) throws IllegalArgumentException {
        super(a, buffer.getAudioFormat(), len, MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (getState() != STATE_INITIALIZED)
            throw new NotInitializedException();
    }

    void playbackListenerUpdate() {
        if (markerInFrames >= 0 && userListener != null && playStart > 0) { // some old bugged phones unable to set markers
            playbackHandler.removeCallbacks(playbackUpdate);
            playbackUpdate = new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    if (markerInFrames >= 0) {
                        long playEnd = playStart + markerInFrames * 1000 / getSampleRate();
                        if (now >= playEnd) {
                            systemListener.onMarkerReached(AudioTrack.this);
                            return;
                        }
                    }
                    systemListener.onPeriodicNotification(AudioTrack.this);
                    long update = periodInFrames * 1000 / getSampleRate();

                    int len = getNativeFrameCount() * 1000 / getSampleRate(); // getNativeFrameCount() checking stereo fine
                    long end = len * 2 - (now - playStart);
                    if (update > end)
                        update = end;

                    playbackHandler.postDelayed(playbackUpdate, update);
                }
            };
            playbackUpdate.run();
        } else {
            playbackHandler.removeCallbacks(playbackUpdate);
            playbackUpdate = null;
        }
    }

    @Override
    public void release() {
        super.release();
        if (playbackUpdate != null) {
            playbackHandler.removeCallbacks(playbackUpdate);
            playbackUpdate = null;
        }
    }

    @Override
    public void play() throws IllegalStateException {
        super.play();
        playStart = System.currentTimeMillis();
        playbackListenerUpdate();
    }

    @Override
    public int setNotificationMarkerPosition(int markerInFrames) {  // do not check != AudioTrack.SUCCESS crash often
        this.markerInFrames = markerInFrames;
        return super.setNotificationMarkerPosition(markerInFrames);
    }

    @Override
    public int setPositionNotificationPeriod(int periodInFrames) {
        this.periodInFrames = periodInFrames;
        return super.setPositionNotificationPeriod(periodInFrames);
    }

    @Override
    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
        super.setPlaybackPositionUpdateListener(systemListener);
        this.userListener = listener;
        playbackListenerUpdate();
    }

    @Override
    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener, Handler handler) {
        super.setPlaybackPositionUpdateListener(systemListener, handler);
        this.userListener = listener;
        if (handler != null) {
            this.playbackHandler.removeCallbacks(playbackUpdate);
            this.playbackHandler = handler;
        }
        playbackListenerUpdate();
    }

    @Override
    public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
        int out = super.write(audioData, offsetInShorts, sizeInShorts);
        if (out > 0) {
            this.len += out / getChannelCount();
            this.frames += out;
        }
        return out;
    }

    public int write(AudioBuffer buf) {
        int out = write(buf, buf.pos, buf.buffer.limit - buf.pos); // use 'buffer.length' instead of 'len'
        if (out > 0) {
            buf.pos += out;
            this.len += out / getChannelCount();
            this.frames += out;
        }
        return out;
    }

    public int write(@NonNull float[] audioData, int offsetInFloats, int sizeInFloats) {
        return write(audioData, offsetInFloats, sizeInFloats, WRITE_BLOCKING);
    }

    @Override
    public int write(@NonNull float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode) {
        int out = super.write(audioData, offsetInFloats, sizeInFloats, writeMode);
        if (out > 0) {
            this.len += out / getChannelCount();
            this.frames += out;
        }
        return out;
    }

    public int write(AudioBuffer buffer, int pos, int len) {
        switch (buffer.buffer.format) {
            case AudioFormat.ENCODING_PCM_16BIT:
                return write(buffer.buffer.shorts, pos, len);
            case AudioFormat.ENCODING_PCM_FLOAT:
                return write(buffer.buffer.floats, pos, len);
            default:
                throw new RuntimeException("Unknown format");
        }
    }
}
