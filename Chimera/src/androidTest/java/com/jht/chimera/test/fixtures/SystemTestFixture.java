package com.jht.chimera.test.fixtures;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.performance.SystemStats;
import com.jht.chimera.test.util.LogMonitor;
import com.jht.datamatrixlib.DataMatrix;


/**
 * Here we want to make sure to start the system.  Sometimes we may want to set the frame too.
 * This class helps simplify the tests.
 */
public class SystemTestFixture extends UITestFixture {

    private final SystemStats stats;
    private LogMonitor logMonitor;

    public SystemTestFixture() {
        this(true);
    }

    public SystemTestFixture(boolean startServices) {
        LaunchAppUtil.launchAndroidLauncher();
        if(startServices) {
            startServices(new DataMatrix());
        }
        DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        stats = new SystemStats(InstrumentationRegistry.getInstrumentation().getContext());
        stats.startRecording(1000);
    }

    public SystemTestFixture(FrameConfigEnum frame) {
        this();
        setFrame(frame);
    }
    public <A extends Activity> SystemTestFixture(FrameConfigEnum frame, @NonNull Class<A> activityClass) {
        this(frame);
        startActivity(activityClass);
    }

    public void monitorLogForCrashes() {
        logMonitor = new LogMonitor();
    }


    public void monitorLogForCrashes(String[] expectedEvents) {
        logMonitor = new LogMonitor(expectedEvents);
    }

    @Override
    public void close() {
        super.close();
        stats.stopRecording();
        //Scripts key in off this log for IFIT testing, do not modify.
        Log.d("JHTTestTiming", stats.analyzeCurrentData());

        if(logMonitor != null) {
            logMonitor.stop();
            logMonitor.assertLogsAreClean();
        }
    }

}
