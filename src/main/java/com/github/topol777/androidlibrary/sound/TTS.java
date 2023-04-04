package com.github.topol777.androidlibrary.sound;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.AndroidRuntimeException;
import android.util.Log;

import com.github.topol777.androidlibrary.app.AlarmManager;
import com.github.topol777.androidlibrary.preferences.OptimizationPreferenceCompat;
import com.github.topol777.androidlibrary.widgets.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

public class TTS extends Sound {
    public static final String TAG = TTS.class.getSimpleName();

    public static String TTS_WAIT = "Waiting for TTS";
    public static String TTS_FAILED_AGAIN = "Failed TTS again, skipping";
    public static String TTS_FAILED_RESTART = "Failed TTS again, restarting";
    public static String TTS_LANG_FAILED = "TTS language failed %s";

    public static final int DELAYED_DELAY = 5 * AlarmManager.SEC1;
    public static final String TTS_INIT = TTS.class.getCanonicalName() + ".TTS_INIT";

    public TextToSpeech tts;
    public Runnable delayedTTS; // delayedSpeach. tts may not be initalized, on init done, run delayed.run()
    public int restart; // restart tts once if failed. on apk upgrade tts always failed.
    public Runnable onInit; // once
    public TreeMap<String, Runnable> utterance = new TreeMap<>();

    public static void startTTSInstall(Context context) {
        try {
            Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (OptimizationPreferenceCompat.isCallable(context, intent))
                context.startActivity(intent);
        } catch (AndroidRuntimeException e) {
            Log.d(TAG, "Unable to load TTS", e);
            startTTSCheck(context);
        }
    }

    public static void startTTSCheck(Context context) {
        try {
            Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (OptimizationPreferenceCompat.isCallable(context, intent))
                context.startActivity(intent);
        } catch (AndroidRuntimeException e1) {
            Log.d(TAG, "Unable to load TTS", e1);
        }
    }

    public static String ttsLangError(Locale locale, int d) {
        String str = "";
        switch (d) {
            case TextToSpeech.LANG_AVAILABLE:
                break;
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                break;
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                break;
            case TextToSpeech.LANG_NOT_SUPPORTED:
                str = String.format(TTS_LANG_FAILED, locale) + " (not supported)";
                break;
            case TextToSpeech.LANG_MISSING_DATA:
                str = String.format(TTS_LANG_FAILED, locale) + " (missing data)";
                break;
            default:
                str = String.format(TTS_LANG_FAILED, locale) + String.format(" (unknown error: %d)", d);
        }
        return str;
    }

    public static class Speak {
        public Locale locale;
        public String text;

        public Speak(Locale l, String t) {
            locale = l;
            text = t;
        }

        @Override
        public String toString() {
            return text + " (" + locale + ")";
        }
    }

    public static class PreferedLocales implements Comparator<Locale> {
        ArrayList<Locale> pp;

        public PreferedLocales(Locale userSelection, Locale systemLanguage) {
            pp = new ArrayList<>(Arrays.asList(userSelection, systemLanguage,
                    Locale.US, Locale.ENGLISH));
        }

        public int indexOf(Locale l) {
            for (int i = 0; i < pp.size(); i++) {
                if (pp.get(i).equals(l))
                    return i;
            }
            for (int i = 0; i < pp.size(); i++) {
                if (pp.get(i).getLanguage().equals(l.getLanguage()))
                    return i;
            }
            return -1;
        }

        @Override
        public int compare(Locale o1, Locale o2) {
            int i1 = indexOf(o1);
            int i2 = indexOf(o2);
            if (i1 != -1 && i2 != -1) {
                int r = Integer.compare(i1, i2);
                if (r != 0)
                    return r;
                return o1.toString().compareTo(o2.toString());
            }
            if (i1 != -1)
                return -1;
            if (i2 != -1)
                return 1;
            return o1.toString().compareTo(o2.toString());
        }
    }

    public TTS(Context context) {
        super(context);
    }

    public void ttsCreate() {
        Log.d(TAG, "tts create");
        handler.removeCallbacks(onInit);
        onInit = new Runnable() {
            @Override
            public void run() {
                onInit();
            }
        };
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(final int status) {
                if (status != TextToSpeech.SUCCESS)
                    return;
                handler.post(onInit);
            }
        });
    }

    public void onInit() {
        if (Build.VERSION.SDK_INT >= 21) {
            Channel c = getSoundChannel();
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(c.usage)
                    .setContentType(c.ct)
                    .build());
        }

        if (Build.VERSION.SDK_INT < 15) {
            tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(final String utteranceId) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TTS.this.onDone(utteranceId);
                        }
                    });
                }
            });
        } else {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(final String utteranceId) { // tts start speaking
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TTS.this.onStart(utteranceId);
                        }
                    });
                }

                public void onRangeStart(final String utteranceId, final int start, final int end, final int frame) { // API26
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TTS.this.onRangeStart(utteranceId, start, end, frame);
                        }
                    });
                }

                @Override
                public void onDone(final String utteranceId) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TTS.this.onDone(utteranceId);
                        }
                    });
                }

                @Override
                public void onError(final String utteranceId) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TTS.this.onError(utteranceId);
                        }
                    });
                }
            });
        }

        handler.removeCallbacks(onInit);
        onInit = null;

        done(delayedTTS);
        handler.removeCallbacks(delayedTTS);
        delayedTTS = null;
    }

    public void close() { // external close
        closeTTS();
    }

    public void closeTTS() { // internal close
        Log.d(TAG, "closeTTS()");
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        handler.removeCallbacks(onInit);
        onInit = null;
        dones.remove(delayedTTS);
        handler.removeCallbacks(delayedTTS);
        delayedTTS = null;
    }

    public void playSpeech(final Speak speak, final Runnable done) {
        dones.add(done);

        dones.remove(delayedTTS);
        handler.removeCallbacks(delayedTTS);
        delayedTTS = null;

        if (tts == null)
            ttsCreate();

        // clear delayed(), sound just played
        final Runnable clear = new Runnable() {
            @Override
            public void run() {
                dones.remove(delayedTTS);
                handler.removeCallbacks(delayedTTS);
                delayedTTS = null;
                done(done);
            }
        };
        dones.add(clear);
        utterance.put(Integer.toString(speak.hashCode()), clear);

        // TTS may say failed, but play sounds successfully. we need regardless or failed do not
        // play speech twice if clear.run() was called.
        if (!playSpeech(speak)) {
            Log.d(TAG, TTS_WAIT);
            Toast.makeText(context, TTS_WAIT, Toast.LENGTH_SHORT).show();
            dones.remove(delayedTTS);
            handler.removeCallbacks(delayedTTS);
            delayedTTS = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "delayed run()");
                    if (!playSpeech(speak)) {
                        closeTTS();
                        if (restart >= 1) {
                            Log.d(TAG, TTS_FAILED_AGAIN);
                            Toast.makeText(context, TTS_FAILED_AGAIN, Toast.LENGTH_SHORT).show();
                            clear.run();
                        } else {
                            Log.d(TAG, TTS_FAILED_RESTART);
                            restart++;
                            Toast.makeText(context, TTS_FAILED_RESTART, Toast.LENGTH_SHORT).show();
                            dones.remove(delayedTTS);
                            handler.removeCallbacks(delayedTTS);
                            delayedTTS = new Runnable() {
                                @Override
                                public void run() {
                                    playSpeech(speak, done);
                                }
                            };
                            dones.add(delayedTTS);
                            handler.postDelayed(delayedTTS, DELAYED_DELAY);
                        }
                    }
                }
            };
            dones.add(delayedTTS);
            handler.postDelayed(delayedTTS, DELAYED_DELAY);
        }
    }

    public Locale getUserLocale() { // overided with user selection
        return Locale.getDefault();
    }

    public Locale getTTSLocale() {
        Locale locale = getUserLocale();

        if (tts == null)
            return locale;

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
            String lang = locale.getLanguage();
            locale = new Locale(lang);
        }

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) { // user selection not supported.
            locale = null;
            if (Build.VERSION.SDK_INT >= 21) {
                Voice v = tts.getDefaultVoice();
                if (v != null)
                    locale = v.getLocale();
            }
            if (locale == null) {
                if (Build.VERSION.SDK_INT >= 18)
                    locale = tts.getDefaultLanguage();
                else
                    locale = tts.getLanguage();
            }
            if (locale == null)
                locale = Locale.getDefault();
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                String lang = locale.getLanguage(); // default tts voice not supported. use 'lang' "ru" of "ru_RU"
                locale = new Locale(lang);
            }
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                locale = Locale.getDefault(); // default 'lang' tts voice not supported. use 'system default lang'
                String lang = locale.getLanguage();
                locale = new Locale(lang);
            }
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED)
                locale = Locale.US; // 'system default lang' tts voice not supported. use 'en_US'
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED)
                locale = Locale.ENGLISH; // 'system default lang' tts voice not supported. use 'en'
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED)
                return null; // 'en' not supported? do not speak
        }

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_MISSING_DATA) {
            startTTSInstall(context);
            return null;
        }

        return locale;
    }

    public boolean playSpeech(Speak speak) {
        if (onInit != null)
            return false;
        setLanguage(speak.locale); // live
        String u = Integer.toString(speak.hashCode());
        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, getSoundChannel().streamType);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, getVolume());
            if (tts.speak(speak.text, TextToSpeech.QUEUE_FLUSH, params, u) != TextToSpeech.SUCCESS)
                return false;
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(getSoundChannel().streamType));
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, Float.toString(getVolume()));
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, u);
            if (tts.speak(speak.text, TextToSpeech.QUEUE_FLUSH, params) != TextToSpeech.SUCCESS)
                return false;
        }
        restart = 0;
        return true;
    }

    public void sort(ArrayList<Locale> ll) {
        Collections.sort(ll, new PreferedLocales(getUserLocale(), Locale.getDefault()));
    }

    public Locale setLanguage(Locale locale) {
        Locale d = locale;
        int l = tts.setLanguage(d);
        if (l < 0) {
            Toast.makeText(context, ttsLangError(d, l), Toast.LENGTH_LONG).show();
            ArrayList<Locale> ll = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= 21) {
                Set<Voice> vv = tts.getVoices();
                if (vv != null) {
                    for (Voice v : vv)
                        ll.add(v.getLocale());
                }
            }
            sort(ll);
            for (Locale k : ll) {
                k = ll.get(0);
                l = tts.setLanguage(k);
                if (l == 0)
                    return k;
            }
            for (Locale k : ll) {
                k = ll.get(0);
                l = tts.setLanguage(k);
                if (l >= 0)
                    return k;
            }
            if (l < 0) {
                d = Locale.getDefault();
                l = tts.setLanguage(d);
                if (l < 0) {
                    d = Locale.US;
                    l = tts.setLanguage(d);
                    if (l < 0) {
                        d = Locale.ENGLISH;
                        tts.setLanguage(d);
                    }
                }
            }
        }
        return d;
    }

    public void onStart(String utteranceId) {
    }

    public void onRangeStart(String utteranceId, int start, int end, int frame) {
    }

    public void onDone(String utteranceId) {
        Log.d(TAG, "onDone " + utteranceId);
        Runnable r = utterance.get(utteranceId);
        if (r != null) {
            done(r);
            utterance.remove(utteranceId);
        }
    }

    public void onError(String utteranceId) {
        Log.d(TAG, "onError " + utteranceId);
        Runnable r = utterance.get(utteranceId);
        if (r != null) {
            done(r);
            utterance.remove(utteranceId);
        }
    }
}
