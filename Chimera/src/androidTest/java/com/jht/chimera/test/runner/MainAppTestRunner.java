package com.jht.chimera.test.runner;

import android.os.Bundle;

import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Runs tests in the mainapp process with the instrumentation flag set for the whole run.
 *
 * Starting instrumentation force-stops the package, so splash relaunches as home and its
 * dropPreviousSession() kills mainapp. The manifest also attaches this runner to the splash
 * process, so onCreate sets the flag there before splash logic reads it. Tests only run in mainapp,
 * the first target process.
 */
public class MainAppTestRunner extends JHTTestRunner {

    @Override
    public void onCreate(Bundle bundle) {
        DataMatrix.putBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL, true);
        super.onCreate(bundle);
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        DataMatrix.putBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL, false);
        super.finish(resultCode, results);
    }
}
