package com.jht.chimera.test.runner;

import android.os.Bundle;

/**
 * Custom test runner so we can listen for test events and export a test report.  Others can
 * override this to provide a different test listener which is useful naming the test suite.
 */
public class JHTTestRunner extends androidx.test.runner.AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle bundle) {
        String testListeners = bundle.getString("listener");
        if(testListeners == null || testListeners.isEmpty()) {
            bundle.putString("listener", getListenerClass());
        }
        else {
            bundle.putString("listener", testListeners + "," + getListenerClass());
        }
        super.onCreate(bundle);
    }

    /**
     * Note you can override the default listener if you want to add properties or set test suite name
     * since we don't get that from the Android test runner.
     *
     * @return  The listener class to use to listen for events.
     */
    protected String getListenerClass() {
        return JHTTestRunListener.class.getName();
    }


}
