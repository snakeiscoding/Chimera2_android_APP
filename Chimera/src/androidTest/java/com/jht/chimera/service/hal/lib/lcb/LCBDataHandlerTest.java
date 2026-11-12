package com.jht.chimera.service.hal.lib.lcb;

import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.machine.Machine;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This contains tests for the LCM.  Currently the tests are as follows.
 *
 * Test serial number automation.  Make sure the LCB service functions handle this correctly.
 *      For this test we run through several scenarios as the LCB may have the serial numbers and
 *      it may not.  Similarly the console may have serial numbers it may not.  This ensures the rules
 *      are followed properly.  Basically for firmware serial number the LCB is the source of truth
 *      and for console serial number the console is the source of truth when they both have a value.
 *      It also tests the situations where we read the serial number from a file.
 */
public class LCBDataHandlerTest {
    private static final String TAG = "LCBDataHandlerTest";
    String consoleSN;
    String frameSN;

    @Before
    public void setUp() {
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation().getContext());
    }


    /**
     * Run a test.
     *
     * @param serialNumber          Data from the serial number file.  Can be blank.  Invalid numbers handled.
     * @param consoleSNOriginal     The original console serial number.
     * @param frameSNOriginal       The original frame serial number.
     * @param consoleSNExpected     Expected result of the console serial number.
     * @param frameSNExpected       Expected result of the frame serial number.
     * @param updateLCBEvent        True if the test should result in the LCB being updated.
     * @param consoleSNEvent        True if the test should result in the console serial number being updated.
     * @param frameSNEvent          True if the test should result in the console serial number being updated.
     */
    private void runSNTest(String serialNumber, String consoleSNOriginal, String frameSNOriginal, String consoleSNExpected, String frameSNExpected, boolean updateLCBEvent, boolean consoleSNEvent, boolean frameSNEvent) {
        try(BaseTestFixture testFixture = new BaseTestFixture()) {

            JHTNotification updateLCBNotification = new JHTNotification();
            JHTNotification consoleSerialNumberNotification = new JHTNotification();
            JHTNotification frameSerialNumberNotification = new JHTNotification();

            LCBDataHandler dataHandler = new LCBDataHandler(testFixture.getTargetContext());
            consoleSN = consoleSNOriginal;
            frameSN = frameSNOriginal;

            DataMatrix dataMatrix = new DataMatrix();

            DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, consoleSNOriginal);
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, frameSNOriginal);
            dataMatrix.createEventAdapter(HALKeys.ACTION_UPDATE_LCB_SERIAL_NUMBERS, action -> updateLCBNotification.setNotification());

            dataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, sn -> {
                consoleSN = sn;
                consoleSerialNumberNotification.setNotification();
            }, false);

            dataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, sn -> {
                frameSN = sn;
                frameSerialNumberNotification.setNotification();
            }, false);
            dataHandler.dataHandlerParseLcbSN(serialNumber);

            Assert.assertEquals(updateLCBEvent, updateLCBNotification.waitForNotification(2000));
            Assert.assertEquals(consoleSNEvent, consoleSerialNumberNotification.waitForNotification(2000));
            Assert.assertEquals(frameSNEvent, frameSerialNumberNotification.waitForNotification(2000));

            dataMatrix.finish();

            Assert.assertEquals(consoleSNExpected, consoleSN);
            Assert.assertEquals(frameSNExpected, frameSN);
        }
    }

    /**
     * Test case 1.  Console is blank and LCB has both serial numbers.  Expectation is that we will
     * get the console and frame serial number events from the data matrix, but there is no action
     * to update the LCB.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN1() {
        runSNTest("CTM784testserial1/FTM538testserial1", "", "", "CTM784testserial1","FTM538testserial1", false, true, true);
    }

    /**
     * Test case 2.  Console is blank and LCB has frame serial number.  Expectation is that we will
     * get the frame serial number event only.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN2() {
        runSNTest("FTM785testserial1", "", "", "","FTM785testserial1", false, false, true);
    }

    /**
     * Test case 3. Console is blank and LCB has a single serial number string.
     * Expectation: a single string is always retroactively assigned to the Frame
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN3() {
        runSNTest("CTM756testserial1", "", "", "","CTM756testserial1", false, false, true);
    }

    /**
     * Test case 4.  Console is has serial numbers and LCB is blank.  Expectation is that we will
     * get the update LCB only.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN4() {
        runSNTest("", "CTM756testserial1", "FTM538testserial1", "CTM756testserial1","FTM538testserial1", true, false, false);
    }

    /**
     * Test case 5.  Console is has serial numbers and LCB has serial numbers.  They disagree.  Console wins for console
     * and LCB wins for frame.  LCB is updated in the end.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN5() {
        runSNTest("CTM756testserial2/FTM538testserial2", "CTM756testserial1", "FTM538testserial1", "CTM756testserial1","FTM538testserial2", true, false, true);
    }

    /**
     * Test case 6.  Console is has serial numbers and LCB has serial numbers.  They disagree with console.
     * Only update LCB.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN6() {
        runSNTest("CTM756testserial2/FTM538testserial1", "CTM756testserial1", "FTM538testserial1", "CTM756testserial1","FTM538testserial1", true, false, false);
    }

    /**
     * Test case 7.  Console is has serial numbers and LCB has serial numbers.  They disagree with frame.
     * LCB does not get updated, but get the frame serial number event.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN7() {
        runSNTest("CTM756testserial1/FTM538testserial2", "CTM756testserial1", "FTM538testserial1", "CTM756testserial1","FTM538testserial2", false, false, true);
    }

    /**
     * Test case 8.  Blank all around.  No events should occur.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN8() {
        runSNTest("", "", "", "","", false, false, false);
    }

    /**
     * Test case 9.  LCB has data, but it is not a serial number. No events should occur.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN9() {
        runSNTest("bad sn", "", "", "","", false, false, false);
    }

    /**
     * Test case 10. Modern Multi-Serial Parsing.
     * Expectation: LCB sends prefix-less strings. Index 0 is Console, Index 1 is Frame.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN10() {
        runSNTest("TM784testserial1/TM538testserial1", "", "", "TM784testserial1","TM538testserial1", false, true, true);
    }

    /**
     * Test case 11. Modern Single Frame Parsing.
     * Expectation: LCB sends a single prefix-less string. It is assigned to the Frame (Similiar to test 3).
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN11() {
        runSNTest("TM785testserial1", "", "", "","TM785testserial1", false, false, true);
    }

    /**
     * Test case 12. Manufacturing Test Run (TR) Parsing.
     * Expectation: LCB sends strings prefixed with 'TR'.
     */
    @Test @Emulator @Default @Chimera
    public void testAutomatedSN12() {
        runSNTest("TRTM784testserial1/TRTM538testserial1", "", "", "TRTM784testserial1","TRTM538testserial1", false, true, true);
    }

    @Test @Emulator @Default @Chimera
    public void testTreadmillRPM() {
        try(SystemTestFixture ignored = new SystemTestFixture(FrameConfigEnum.Treadmill_Performance_Plus)) {
            for(int i = 0; i <= (int)(FrameConfigManager.getInstance().getFrameConfig().speedKph.getMaxModel() * 10); i++) {
                Log.d(TAG, "KPH " + i + " RPM " + Machine.kphToRpm(i / 10.0f));
            }
        }
    }
}