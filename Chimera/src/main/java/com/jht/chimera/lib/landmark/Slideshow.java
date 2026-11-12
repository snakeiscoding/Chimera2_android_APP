package com.jht.chimera.lib.landmark;

import java.util.Timer;
import java.util.TimerTask;

public class Slideshow {
    private final static long DEFAULT_TIMEOUT = 10000; // 10 seconds

    private Timer _timer; // change the photo every 10 seconds

    private int _framesNum = 0;
    private int _index = 0; // current index of frame
    private long _timeout = 0;

    private Callback _callback;

    public interface Callback {
        void onTimeout(final int index);
    }

    public Slideshow(Callback callback) {
        this(callback, DEFAULT_TIMEOUT);
    }

    public Slideshow(Callback callback, long timeout) {
        _callback = callback;
        _timeout = timeout;
    }

    public void resume() {
        start(_index, _framesNum);
    }

    public void pause() {
        stop();
    }

    public void start(int index, final int framesNum) {
        if (null != _timer) {
            return;
        }

        _index = index;
        _framesNum = framesNum;

        _timer = new Timer();
        _timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (_index >= framesNum) {
                    _index = 0;
                }

                _callback.onTimeout(_index++);
            }

        }, _timeout, _timeout);
    }

    public void restart(int index, final int framesNum) {
        stop();
        start(index, framesNum);
    }

    public void stop() {
        if (null != _timer) {
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }
    }
}
