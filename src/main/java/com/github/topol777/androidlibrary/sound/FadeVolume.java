package com.github.topol777.androidlibrary.sound;

import android.os.Handler;

public class FadeVolume implements Runnable {
    public Handler handler;

    public int dur;
    public int step = 0;
    public int steps = 50;
    public int delay = 100;

    public FadeVolume(Handler handler, int sec) {
        this.dur = sec;
        this.handler = handler;
        steps = (sec / delay);
        if (steps < 1)
            steps = 1;
    }

    public FadeVolume(int sec) {
        this(new Handler(), sec);
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        handler.removeCallbacks(this);

        float vol = Sound.log1(step, steps);

        if (!step(vol))
            return;

        step++;

        if (step >= steps) {
            // should be clear anyway
            handler.removeCallbacks(this);
            done();
            return;
        }

        handler.postDelayed(this, delay);
    }

    public boolean step(float vol) {
        return true;
    }

    public void done() {
    }
}
