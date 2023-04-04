package com.github.topol777.androidlibrary.sound;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import com.github.topol777.androidlibrary.preferences.SilencePreferenceCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Sound {
    public static final int DEFAULT_AUDIOFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_RATE = 16000;

    public static int[] RATES = new int[]{8000, 11025, 16000, 22050, 32000, 44100, 48000, 96000, 144000, 192000};

    public static final String ZEN_MODE = "zen_mode";
    public static final int ZEN_MODE_OFF = 0;
    public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;
    public static final int ZEN_MODE_NO_INTERRUPTIONS = 2;
    public static final int ZEN_MODE_ALARMS = 3;

    public static final int ENCODING_PCM_24BIT_PACKED = 0x00000015;
    public static final int ENCODING_PCM_32BIT = 0x00000016;

    public static final long LAST = 1000; // last delay

    public Context context;
    public Handler handler = new Handler();
    public int soundMode = -1;
    public long last; // last change, prevent spam
    public Runnable delayed;
    public Set<Runnable> dones = new HashSet<>(); // valid done list, in case sound was canceled during play done will not be present
    public Set<Runnable> exits = new HashSet<>(); // run when all done

    public static void beep() {
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    public static int getValidRecordRate(int format, int in, int rate) {
        int i = Arrays.binarySearch(RATES, rate);
        if (i < 0)
            i = -i - 2;
        for (; i >= 0; i--) {
            int r = RATES[i];
            int bufferSize = AudioRecord.getMinBufferSize(r, in, format);
            if (bufferSize > 0)
                return r;
        }
        return -1;
    }

    public static int getValidAudioRate(int format, int out, int rate) {
        int i = Arrays.binarySearch(RATES, rate);
        if (i < 0)
            i = -i - 2;
        for (; i >= 0; i--) {
            int r = RATES[i];
            int bufferSize = AudioTrack.getMinBufferSize(r, out, format);
            if (bufferSize > 0)
                return r;
        }
        return -1;
    }

    public static int getChannelCount(int channelConfig) {
        switch (channelConfig) {
            case AudioFormat.CHANNEL_OUT_DEFAULT: //AudioFormat.CHANNEL_CONFIGURATION_DEFAULT
            case AudioFormat.CHANNEL_OUT_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                return 1;
            case AudioFormat.CHANNEL_OUT_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
                return 2;
            default:
                if (channelConfig == AudioFormat.CHANNEL_INVALID)
                    return 0;
                return Integer.bitCount(channelConfig);
        }
    }

    public static class Channel {
        public static Channel NORMAL = new Channel(AudioManager.STREAM_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION, AudioAttributes.CONTENT_TYPE_SONIFICATION);
        public static Channel ALARM = new Channel(AudioManager.STREAM_ALARM, AudioAttributes.USAGE_ALARM, AudioAttributes.CONTENT_TYPE_SONIFICATION);

        public int streamType; // AudioManager.STREAM_* == AudioSystem.STREAM_*
        public int usage; // AudioAttributes.USAGE_*
        public int ct; // AudioAttributes.CONTENT_TYPE_*

        public Channel() {
        }

        public Channel(int t, int u, int c) {
            streamType = t;
            usage = u;
            ct = c;
        }
    }

    public Sound(Context context) {
        this.context = context;
    }

    public static float log1(float v, float m) {
        float log1 = (float) (Math.log(m - v) / Math.log(m));
        return 1 - log1;
    }

    public static float log1(float v) {
        return log1(v, 2);
    }

    boolean delaying(Runnable r) {
        long next = last + LAST;
        long last = System.currentTimeMillis();
        if (next > last) {
            handler.removeCallbacks(delayed);
            delayed = r;
            handler.postDelayed(delayed, next - last);
            return true;
        }
        this.last = last;
        return false;
    }

    public void silent() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!SilencePreferenceCompat.isNotificationPolicyAccessGranted(context))
                return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                silent();
            }
        };
        if (delaying(r))
            return;

        if (soundMode != -1)
            return; // already silensed

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        soundMode = am.getRingerMode();

        if (soundMode == AudioManager.RINGER_MODE_SILENT) { // we already in SILENT mode. keep all unchanged.
            soundMode = -1;
            return;
        }

        am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    public void unsilent() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!SilencePreferenceCompat.isNotificationPolicyAccessGranted(context))
                return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                unsilent();
            }
        };
        if (delaying(r))
            return;

        if (soundMode == -1)
            return; // already unsilensed

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int soundMode = am.getRingerMode();
        if (soundMode == AudioManager.RINGER_MODE_SILENT) {
            am.setRingerMode(this.soundMode);
            am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        }

        this.soundMode = -1;
    }

    @TargetApi(17)
    public int getDNDMode() {
        ContentResolver resolver = context.getContentResolver();
        try {
            return Settings.Global.getInt(resolver, ZEN_MODE);
        } catch (Settings.SettingNotFoundException e) {
            return ZEN_MODE_OFF;
        }
    }

    public Channel getSoundChannel() {
        return Channel.NORMAL;
    }

    public float getVolume() {
        return 1f;
    }

    public float reduce(float vol) {
        return (float) Math.pow(vol, 3);
    }

    public float unreduce(float vol) {
        return (float) Math.exp(Math.log(vol) / 3);
    }

    public void done(Runnable done) {
        if (done != null && dones.contains(done)) {
            dones.remove(done); // remove before run
            done.run();
            removed();
            return;
        }
        remove(done);
    }

    public void after(Runnable done) {
        if (dones.isEmpty())
            done.run();
        else
            exits.add(done);
    }

    public void remove(Runnable done) {
        dones.remove(done);
        handler.removeCallbacks(done);
        removed();
    }

    public void removed() {
        if (dones.isEmpty()) {
            for (Runnable r : exits)
                r.run();
            exits.clear();
        }
    }
}
