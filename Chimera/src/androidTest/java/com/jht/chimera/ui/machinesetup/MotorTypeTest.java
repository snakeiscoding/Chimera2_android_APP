package com.jht.chimera.ui.machinesetup;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.SplashApp;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RunWith(Parameterized.class)
public class MotorTypeTest {

    static class MotorTestCase {
        final int lcbType;
        final String frameSerialNumber;
        final FrameConfigEnum machineType;
        final int expectedMotorType;

        MotorTestCase(int lcbType, String frameSerialNumber, FrameConfigEnum machineType, int expectedMotorType) {
            this.lcbType = lcbType;
            this.frameSerialNumber = frameSerialNumber;
            this.machineType = machineType;
            this.expectedMotorType = expectedMotorType;
        }
        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "0x%02x %s %s %d", lcbType, frameSerialNumber, machineType.toString(), expectedMotorType);
        }
    }
    @Parameterized.Parameter()
    public MotorTestCase motorTestCase;


    @Parameterized.Parameters
    public static Collection<MotorTestCase> initParameters() {
        List<MotorTestCase> testCases = new ArrayList<>();

        HashMap<Integer, List<FrameConfigEnum>> lcbMap = new HashMap<>();
        lcbMap.put(0xA1, Arrays.asList(FrameConfigEnum.Treadmill_Endurance, FrameConfigEnum.Treadmill_Performance));
        lcbMap.put(0xC3, Arrays.asList(FrameConfigEnum.Treadmill_Performance, FrameConfigEnum.Treadmill_Lifestyle));
        lcbMap.put(0xC4, Collections.singletonList(FrameConfigEnum.Treadmill_Performance));
        lcbMap.put(0xC5, Collections.singletonList(FrameConfigEnum.Treadmill_Performance));
        lcbMap.put(0xC7, Arrays.asList(FrameConfigEnum.Treadmill_Endurance, FrameConfigEnum.Treadmill_Performance, FrameConfigEnum.Treadmill_Lifestyle));
        lcbMap.put(0xC8, Arrays.asList(FrameConfigEnum.Treadmill_Endurance, FrameConfigEnum.Treadmill_Performance));
        lcbMap.put(0xA3, Collections.singletonList(FrameConfigEnum.Treadmill_Lifestyle));
        int[] lcbsToTest = new int[] { 0xA1, 0xC3, 0xC4, 0xC5, 0xC7, 0xC8, 0xA3};
        HashMap<FrameConfigEnum, String> ie2TestNumbers = new HashMap<>();
        ie2TestNumbers.put(FrameConfigEnum.Treadmill_Lifestyle, "FTM759testing123");
        ie2TestNumbers.put(FrameConfigEnum.Treadmill_Endurance, "FTM747testing123");
        ie2TestNumbers.put(FrameConfigEnum.Treadmill_Performance, "FTM538testing123");
        HashMap<FrameConfigEnum, String> ie3TestNumbers = new HashMap<>();
        ie3TestNumbers.put(FrameConfigEnum.Treadmill_Lifestyle, "FTM785testing123");
        ie3TestNumbers.put(FrameConfigEnum.Treadmill_Endurance, "FTM784testing123");
        ie3TestNumbers.put(FrameConfigEnum.Treadmill_Performance, "FTM549testing123");

        for(int lcb : lcbsToTest) {
            for(FrameConfigEnum frame : Objects.requireNonNull(lcbMap.get(lcb))) {
                String serialNumber = lcb == 0xA1 || lcb == 0xA3 ? ie3TestNumbers.get(frame) : ie2TestNumbers.get(frame);
                int motorType = frame.equals(FrameConfigEnum.Treadmill_Lifestyle) && lcb != 0xA3 ? 4 : 0;
                testCases.add(new MotorTestCase(lcb, serialNumber, frame, motorType));
            }
        }

        return testCases;
    }

    @Test
    @Emulator
    @Chimera
    @SplashApp
    public void testMotorType() {
        try(BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(motorTestCase.machineType);
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, motorTestCase.frameSerialNumber);
            DataMatrix.putInt(HALKeys.DATA_LCB_TYPE_INT, motorTestCase.lcbType);
            Assert.assertEquals(motorTestCase.expectedMotorType, FrameConfigManager.getInstance().getMotorType());
        }
    }


}
