package com.jht.chimera.ui.machinesetup;

import static com.jht.androidcommonalgorithms.util.ThreadHelper.sleepIgnoreInterruption;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.lcb.LCBDataHandler;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.SplashAppConsoleSwap;
import com.jht.chimera.test.fixtures.UITestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class ConsoleSwapDetectionTest {

    static class ConsoleTestCase {
        final int originalLCBType;
        final int newLCBType;
        final String frameSerialNumber;
        final String lcbFrameSerialNumber;
        final FrameConfigEnum originalMachineType;
        final boolean serialNumberValid;
        final boolean machineValid;

        ConsoleTestCase(int originalLCBType, int newLCBType, String frameSerialNumber, String lcbFrameSerialNumber, FrameConfigEnum originalMachineType, boolean serialNumberValid, boolean machineValid) {
            this.originalLCBType = originalLCBType;
            this.newLCBType = newLCBType;
            this.frameSerialNumber = frameSerialNumber;
            this.lcbFrameSerialNumber = lcbFrameSerialNumber;
            this.originalMachineType = originalMachineType;
            this.serialNumberValid = serialNumberValid;
            this.machineValid = machineValid;
        }
        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "0x%02x => 0x%02x SN: %s LCB SN: %s Machine: %s SN Valid: %b Machine Valid: %b", originalLCBType, newLCBType, frameSerialNumber, lcbFrameSerialNumber, originalMachineType.toString(), serialNumberValid, machineValid);
        }
    }


    /**
     * Note a full test of all permutations takes over 2 hours.  We are going to randomly get test cases that test the 4 perumtations of the results.
     * @return  A list of test cases.
     */
    public List<ConsoleTestCase> getTestCases() {
        HashMap<Integer, List<FrameConfigEnum>> lcbFrameMap = new HashMap<>();
        HashMap<FrameConfigEnum, Set<String>> framePrefixMap = new HashMap<>();


        for(FrameConfigEnum frame : FrameConfigEnum.getSupportedFrames()) {
            Set<String> prefixes = new HashSet<>();
            FrameConfig frameConfig = frame.createFrameConfig();
            for(int lcb : frameConfig.supportLcbTypeList) {
                List<FrameConfigEnum> frames = lcbFrameMap.get(lcb);
                if(frames == null) {
                    frames = new ArrayList<>();
                    frames.add(frame);
                    lcbFrameMap.put(lcb, frames);
                }
                else {
                    frames.add(frame);
                }
                DataMatrix.putInt(HALKeys.DATA_LCB_TYPE_INT, lcb);
                prefixes.addAll(frameConfig.getFramePrefixes());
            }
            framePrefixMap.put(frame, prefixes);
        }



        List<ConsoleTestCase> testCases = new ArrayList<>();
        for(Integer fromLCB : lcbFrameMap.keySet()) {
            for(Integer toLCB : lcbFrameMap.keySet()) {
                for(FrameConfigEnum fromMachine : Objects.requireNonNull(lcbFrameMap.get(fromLCB))) {
                    for(String originalSerialNumber : Objects.requireNonNull(framePrefixMap.get(fromMachine))) {
                        for(FrameConfigEnum toMachine : Objects.requireNonNull(lcbFrameMap.get(toLCB))) {
                            for (String frameSerialNumber : Objects.requireNonNull(framePrefixMap.get(toMachine))) {
                                testCases.add(new ConsoleTestCase(fromLCB, toLCB, originalSerialNumber + "TESTING123", frameSerialNumber + "TESTING1234", fromMachine, fromMachine.equals(toMachine), Objects.requireNonNull(lcbFrameMap.get(toLCB)).contains(fromMachine)));
                            }
                        }
                    }
                }

            }
        }
        testCases.add(new ConsoleTestCase(0xC8, 0xC8, "FTM747TESTING123", "FTM799TESTING1234", FrameConfigEnum.Treadmill_Endurance, true, true));

        List<ConsoleTestCase> trimmedCases = new ArrayList<>();

        for(int i = 0; i < 4; i++) {
            ConsoleTestCase randomTestCase = getRandomTestCase(testCases, i % 2 == 0, i < 2);
            if(randomTestCase != null) {
                trimmedCases.add(randomTestCase);
            }
        }

        return trimmedCases;
    }

    ConsoleTestCase getRandomTestCase(List<ConsoleTestCase> testCases, boolean serialNumberValid, boolean frameValid) {
        Random random = new Random();
        for(int i = 0; i < testCases.size(); i++) {
            ConsoleTestCase testCase = testCases.get(random.nextInt(testCases.size()));
            if(testCase.machineValid == frameValid && testCase.serialNumberValid == serialNumberValid) {
                return testCase;
            }
        }
        return null;
    }

    @Test
    @Emulator
    @Chimera
    @SplashAppConsoleSwap
    public void testConsoleSwap() {
        try(UITestFixture testFixture = new UITestFixture()) {
            DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, 60);
            List<ConsoleTestCase> testCases = getTestCases();
            int i = 0;
            Log.d("bdc_console_swap", "Going through the test cases " + testCases.size());
            for(ConsoleTestCase consoleTestCase : getTestCases()) {
                Log.d("bdc_console_swap", "Test case " + i + " " + consoleTestCase);
                i++;
                LaunchAppUtil.launchAndroidLauncher();
                setupMachine(testFixture, consoleTestCase);
                LCBDataHandler dataHandler = new LCBDataHandler(testFixture.getTargetContext());
                if (consoleTestCase.machineValid) {
                    launchWelcomeScreen(testFixture, consoleTestCase);
                    dataHandler.dataHandlerParseLcbSN(consoleTestCase.lcbFrameSerialNumber, false);
                    sleepIgnoreInterruption(2000);
                    if (consoleTestCase.serialNumberValid) {
                        assertWelcomeScreenShown(testFixture, consoleTestCase);
                    } else {
                        assertMachineSetupShown(testFixture, consoleTestCase);
                    }
                } else {
                    dataHandler.onLcbType((byte) consoleTestCase.newLCBType, false);
                    sleepIgnoreInterruption(1000);
                    assertMachineSetupShown(testFixture, consoleTestCase);
                }
            }
        }
    }

    private void assertWelcomeScreenShown(UITestFixture testFixture, ConsoleTestCase consoleTestCase) {
        testFixture.waitForView("welcome_guest_button", 1, consoleTestCase.toString());
    }

    private void assertMachineSetupShown(UITestFixture testFixture, ConsoleTestCase consoleTestCase) {
        testFixture.waitForView("language_selector", 1, consoleTestCase.toString());
    }

    private void setupMachine(UITestFixture testFixture, ConsoleTestCase consoleTestCase) {
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        testFixture.setFrame(consoleTestCase.originalMachineType);
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, consoleTestCase.frameSerialNumber);
        DataMatrix.putInt(HALKeys.DATA_LCB_TYPE_INT, consoleTestCase.newLCBType);
    }

    private void launchWelcomeScreen(UITestFixture testFixture, ConsoleTestCase consoleTestCase) {
        LaunchAppUtil.launchWelcomeScreen();
        assertWelcomeScreenShown(testFixture, consoleTestCase);
    }

}
