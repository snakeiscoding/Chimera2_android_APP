package com.jht.chimera.lib.tv;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.tv.tuner.US6Tuner;
import com.jht.chimera.mock.MockSerialPort;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.runner.SplashApp;
import com.jht.chimera.test.fixtures.US6TunerTestFixture;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TunerDetectionTest {

    @Test
    @Chimera
    @SplashApp
    public void testTunerDetection() {
        try( US6TunerTestFixture testFixture = new US6TunerTestFixture()) {
            US6Tuner us6Tuner = new US6Tuner(testFixture.getTargetContext(), new MockSerialPort(TVKeys.EVENT_US6_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_US6_TUNER_HOST_OUTPUT_STREAM));

            testFixture.tunerEmulator().setHDMIResolution(TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_720p);
            testFixture.tunerEmulator().setOutputResolution(TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_1080i);
            testFixture.tunerEmulator().setSoftwareVersion(1, 0);
            testFixture.tunerEmulator().setManufactureCode((byte)3);
            us6Tuner.tuner().setTunerVolume(50);
            us6Tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.MUTE_OFF);

            for(SettingKeys.TVSource source : SettingKeys.TVSource.values()) {
                testTunerDetection(testFixture, us6Tuner, (byte) 1, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.US6 : source );
                testTunerDetection(testFixture, us6Tuner, (byte) 2, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.US5 : source );
                testTunerDetection(testFixture, us6Tuner, (byte) 3, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.BR2 : source );
                testTunerDetection(testFixture, us6Tuner, (byte) 4, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.JP0 : source );
                testTunerDetection(testFixture, us6Tuner, (byte) 5, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.US7 : source );
                testTunerDetection(testFixture, us6Tuner, (byte) 6, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.KM5 : source );
            }

            testFixture.tunerEmulator().setHDMIResolution(TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_UNKNOWN);
            testFixture.tunerEmulator().setOutputResolution(TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_UNKNOWN);
            testFixture.tunerEmulator().setSoftwareVersion(0, 0);
            testFixture.tunerEmulator().setManufactureCode((byte)0);
            for(SettingKeys.TVSource source : SettingKeys.TVSource.values()) {
                testTunerDetection(testFixture, us6Tuner, (byte) 7, source, source.followsSwiftProtocol() ? SettingKeys.TVSource.KM4 : source );
            }
        }
    }

    private void testTunerDetection(US6TunerTestFixture testFixture, US6Tuner us6Tuner, byte tunerType, SettingKeys.TVSource originalSource, SettingKeys.TVSource expectedResult) {
        String tunerTest = "Testing original " + originalSource + " tuner type " + (int)tunerType + " expected result " + expectedResult;
        testFixture.tunerEmulator().setTunerType(tunerType);
        DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, originalSource.toString());
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        us6Tuner.ensureTunerMatchesDetectedHardware(originalSource, true);
        Assert.assertEquals(tunerTest, expectedResult, SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, "US6")));
        Assert.assertEquals(tunerTest, (int)tunerType, DataMatrix.getInt(TVKeys.DATA_TV_TUNER_TYPE_DETECTED_INT));
    }
}
