package com.jht.chimera.test.fixtures;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.mock.MockSerialPort;
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel;
import com.jht.serialport.peripheral.tuner.swift.test.US6Emulator;


/**
 * Useful for testing tuners.  Here we need to make sure the emulator and mocks are set correctly.
 * We also
 */
public class US6TunerTestFixture extends TunerTestFixture<SwiftChannel> {

    public US6TunerTestFixture() {
        super(
                new MockSerialPort(TVKeys.EVENT_US6_TUNER_HOST_OUTPUT_STREAM, TVKeys.EVENT_US6_TUNER_HOST_INPUT_STREAM),
                new US6Emulator(),
                SettingKeys.TVSource.US6.toString());

    }

    public <A extends Activity> US6TunerTestFixture(@NonNull Class<A> activityClass) {
        this();
        startActivity(activityClass);

    }
}
