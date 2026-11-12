package com.jht.chimera.ui.mainapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;

public class BlankActivity extends RestringActivity {

    // What is the max time before assuming the transition is not going to happen?
    protected JHTTimer timer = new JHTTimer(15000,() -> {
        // if we are in a workout, should we bail to main workout activity? but what if we are in a third party integration? splash just safer since it is known good point
        Log.e(BlankActivity.class.getSimpleName(), "BlankActivity timed out! Bail to splash screen!");
        //finish();
        LaunchAppUtil.launchSplashScreen();
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blank_layout);
    }

    @Override
    public void onResume() {
        super.onResume();
        timer.reset();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.stop();
    }
}
