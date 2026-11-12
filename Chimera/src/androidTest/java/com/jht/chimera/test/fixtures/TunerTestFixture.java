package com.jht.chimera.test.fixtures;

import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.mock.MockSerialPort;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.TestTrace;
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel;
import com.jht.serialport.peripheral.tuner.swift.test.SwiftBaseEmulator;


/**
 * Useful for testing tuners.  Here we need to make sure the emulator and mocks are set correctly.
 * We also
 */
public class TunerTestFixture <E extends SwiftChannel> extends UITestFixture {
    private final MockSerialPort mockSerialPort;
    private final SwiftBaseEmulator<E> tunerEmulator;

    TunerTestFixture(MockSerialPort mockSerialPort, SwiftBaseEmulator<E> tunerEmulator, String tuner) {
        this.tunerEmulator = tunerEmulator;
        this.mockSerialPort = mockSerialPort;
        TestTrace.instance().setInTest();
        DataMatrix.putBoolean(TVKeys.DATA_USE_TUNER_EMULATOR_BOOL, true);
        ExecuteAsRoot.run("rm -rf /data/data/com.jht.chimera/files/settings/us7/*");
        ExecuteAsRoot.run("rm -rf /data/data/com.jht.chimera/files/settings/swift/*");
        mockSerialPort.open();
        tunerEmulator.startTest(mockSerialPort.getInputStream(), mockSerialPort.getOutputStream());
        tunerEmulator.channels().clear();
        tunerEmulator.iptvChannelList().clear();
        DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, tuner);

    }

    public SwiftBaseEmulator<E> tunerEmulator() { return tunerEmulator; }

    @Override
    public void close() {
        super.close();
        tunerEmulator.stopTest();
        mockSerialPort.close();
        DataMatrix.putBoolean(TVKeys.DATA_USE_TUNER_EMULATOR_BOOL, false);

    }
}
