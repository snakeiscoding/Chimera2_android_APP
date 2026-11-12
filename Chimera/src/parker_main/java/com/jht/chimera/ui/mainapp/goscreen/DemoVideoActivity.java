package com.jht.chimera.ui.mainapp.goscreen;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.FeaturedWorkoutKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;
import java.io.FileFilter;

public class DemoVideoActivity extends RestringActivity {

    private final String TAG = "DemoVideoActivity";
    private VideoView mVideoView;
    private int mVideoIndex = 0;
    private File[] mVideoList;
    private DataMatrix mDataMatrix = new DataMatrix();
    private static final String NORTH_AMERICA_VIDEO_FILE_IDENTIFIER = "Master_US";
    private static final String GLOBAL_VIDEO_FILE_IDENTIFIER = "Master_Global";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.demo_video_layout);

        File videoFolder = new File("/sdcard/jht/demo");

        if (videoFolder.exists()) {
            mVideoList = videoFolder.listFiles();

            if (mVideoList != null) {

                mVideoList = videoFolder.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        Log.d(TAG, "pathname: "+pathname.getName());
                        if (DataMatrix.getBoolean(FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL, false)) {
                            boolean isNorthAmericaVideo = pathname.getName().contains(NORTH_AMERICA_VIDEO_FILE_IDENTIFIER);
                            return isNorthAmericaVideo && pathname.getName().matches(".*\\.(mp4|mov)"); //NON-NLS
                        } else {
                            boolean isGlobalVideo = pathname.getName().contains(GLOBAL_VIDEO_FILE_IDENTIFIER);
                            return isGlobalVideo && pathname.getName().matches(".*\\.(mp4|mov)"); //NON-NLS
                        }
                    }
                });

            }
        }

        Log.d(TAG, "video list length: "+mVideoList.length);

        mVideoView = findViewById(R.id.video_view);
        findViewById(R.id.cancel).setOnClickListener(v -> LaunchAppUtil.launchSplashScreen());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataMatrix.finish();
        stopVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startVideo();
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> LaunchAppUtil.launchSplashScreen());
    }

    public void startVideo() {

        if (mVideoList == null) return;
        if (mVideoList.length == 0) return;

        try {
            File file = mVideoList[mVideoIndex];
            if (file.exists()) {
                mVideoView.setVideoURI(Uri.fromFile(file));
                mVideoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    mp.setVolume(0.0f, 0.0f); // TODO
                });
                mVideoView.setOnCompletionListener(mp -> {
                    mVideoIndex++;
                    if (mVideoIndex >= mVideoList.length) {
                        mVideoIndex = 0;
                    }
                    startVideo();
                });
                mVideoView.start();
            } else {
                mVideoIndex++;
                if (mVideoIndex >= mVideoList.length) {
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

}