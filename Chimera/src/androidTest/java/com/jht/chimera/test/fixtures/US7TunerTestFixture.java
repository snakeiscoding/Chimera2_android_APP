package com.jht.chimera.test.fixtures;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.mock.MockSerialPort;
import com.jht.serialport.peripheral.tuner.swift.SwiftProIdiomChannel;
import com.jht.serialport.peripheral.tuner.swift.test.US7Emulator;


/**
 * Useful for testing tuners.  Here we need to make sure the emulator and mocks are set correctly.
 *
 */
public class US7TunerTestFixture extends TunerTestFixture<SwiftProIdiomChannel> {

    public US7TunerTestFixture() {
        super(
                new MockSerialPort(TVKeys.EVENT_US7_TUNER_HOST_OUTPUT_STREAM, TVKeys.EVENT_US7_TUNER_HOST_INPUT_STREAM),
                new US7Emulator(),
                SettingKeys.TVSource.US7.toString());

    }

    public <A extends Activity > US7TunerTestFixture(@NonNull Class<A> activityClass) {
        this();
        startActivity(activityClass);

    }
}
