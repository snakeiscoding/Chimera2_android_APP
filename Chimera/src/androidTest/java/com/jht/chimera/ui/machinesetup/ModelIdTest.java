package com.jht.chimera.ui.machinesetup;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@RunWith(Parameterized.class)
public class ModelIdTest {

    static class ModelIdTestCase {
        final String frameSerialNumber;
        final FrameConfigEnum machineType;
        final int expectedModelId;

        ModelIdTestCase(String frameSerialNumber, FrameConfigEnum machineType, int expectedModelId) {
            this.frameSerialNumber = frameSerialNumber;
            this.machineType = machineType;
            this.expectedModelId = expectedModelId;
        }
        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, " %s %s %d", frameSerialNumber, machineType.toString(), expectedModelId);
        }
    }
    @Parameterized.Parameter()
    public ModelIdTestCase modelIdTestCase;


    @Parameterized.Parameters
    public static Collection<ModelIdTestCase> initParameters() {
        List<ModelIdTestCase> testCases = new ArrayList<>();

        testCases.add(new ModelIdTestCase("FTM549testing123", FrameConfigEnum.Treadmill_Performance, 116));
        testCases.add(new ModelIdTestCase("FTM538testing123", FrameConfigEnum.Treadmill_Performance, 92));
        testCases.add(new ModelIdTestCase("FTM784testing123", FrameConfigEnum.Treadmill_Endurance, 117));
        testCases.add(new ModelIdTestCase("FTM747testing123", FrameConfigEnum.Treadmill_Endurance, 91));
        testCases.add(new ModelIdTestCase("FTM776testing123", FrameConfigEnum.Treadmill_Endurance, 130));
        testCases.add(new ModelIdTestCase("FTM785testing123", FrameConfigEnum.Treadmill_Lifestyle, 118));
        testCases.add(new ModelIdTestCase("FTM759testing123", FrameConfigEnum.Treadmill_Lifestyle, 106));
        testCases.add(new ModelIdTestCase("FTM543testing123", FrameConfigEnum.Treadmill_Performance_Plus, 93));
        testCases.add(new ModelIdTestCase("FTM548testing123", FrameConfigEnum.Treadmill_Performance_Plus, 140));

        return testCases;
    }

    @Test
    @Emulator
    @Chimera
    @SplashApp
    public void testModelId() {
        try(BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(modelIdTestCase.machineType);
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, modelIdTestCase.frameSerialNumber);
            Assert.assertEquals(modelIdTestCase.toString(), modelIdTestCase.expectedModelId, FrameConfigManager.getInstance().getModelId());
        }
    }


}
