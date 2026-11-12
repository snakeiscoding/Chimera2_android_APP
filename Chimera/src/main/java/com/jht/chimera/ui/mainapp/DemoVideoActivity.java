package com.jht.chimera.ui.mainapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.ArrayList;

public class DemoVideoActivity extends RestringActivity {
   @NonNls private final String TAG = "DemoVideoActivity";
    private VideoView mVideoView;
    private int mVideoIndex = 0;
    private ArrayList<String> mVideoList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.demo_video_layout);

        //noinspection HardCodedStringLiteral
        mVideoList.add("/sdcard/jht/media/MX16_VIDEO_Retail_Brand.mp4");
        mVideoList.add("/sdcard/jht/media/Sprint8-Promo.mp4"); //NON-NLS

        mVideoView = findViewById(R.id.video_view);
        findViewById(R.id.cancel).setOnClickListener(v -> LaunchAppUtil.launchSplashScreen());

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startVideo();
    }

    public void startVideo() {
        try {
            File file = new File(mVideoList.get(mVideoIndex));
            if (file.exists()) {
                mVideoView.setVideoURI(Uri.fromFile(file));
                mVideoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    mp.setVolume(0.0f, 0.0f); // TODO
                });
                mVideoView.setOnCompletionListener(mp -> {
                    mVideoIndex++;
                    if (mVideoIndex >= mVideoList.size()) {
                        mVideoIndex = 0;
                    }
                    startVideo();
                });
                mVideoView.start();
            } else {
                mVideoIndex++;
                if (mVideoIndex >= mVideoList.size()) {
                    mVideoIndex = 0;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void stopVideo() {
        try {
            mVideoView.stopPlayback();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LaunchAppUtil.launchSplashScreen();
        return super.onKeyDown(keyCode, event);
    }
}
