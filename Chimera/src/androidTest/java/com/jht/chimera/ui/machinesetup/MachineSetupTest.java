package com.jht.chimera.ui.machinesetup;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.NonNull;
import androidx.test.espresso.contrib.RecyclerViewActions;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.fixtures.UITestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
public class MachineSetupTest {

    @Parameterized.Parameter()
    public FrameSerialTestCase frameSerialTestCase;

    @Parameterized.Parameters
    public static Collection<FrameSerialTestCase> initParameters() {
        List<FrameSerialTestCase> testCases = new ArrayList<>();
        HashMap<FrameConfigEnum, List<String>> machinePrefixes = getPrefixMap();
        for(Map.Entry<Integer, List<MachineSerialPrefixMap>> machinesForLCBTestCase : getLCBMap().entrySet()) {
            List<MachineSerialPrefixMap> machines = machinesForLCBTestCase.getValue();
            for(int i = 0; i < machines.size(); i++) {
                List<String> prefixes = machinePrefixes.get(machines.get(i).machine());
                if(prefixes != null) {
                    for (String prefix : prefixes) {
                        testCases.add(new FrameSerialTestCase(machinesForLCBTestCase, i, prefix));
                    }
                }
            }
        }
        return testCases;
    }

    @NonNull
    private static HashMap<FrameConfigEnum, List<String>> getPrefixMap() {
        HashMap<FrameConfigEnum, List<String>> machinePrefixes = new HashMap<>();
        machinePrefixes.put(FrameConfigEnum.Treadmill_Lifestyle, Arrays.asList("FTM759", "FTM785"));
        machinePrefixes.put(FrameConfigEnum.Treadmill_Endurance, Arrays.asList("FTM747", "FTM784"));
        machinePrefixes.put(FrameConfigEnum.Treadmill_Performance, Arrays.asList("FTM538", "FTM549"));
        return machinePrefixes;
    }

    @NonNull
    private static HashMap<Integer, List<MachineSerialPrefixMap>> getLCBMap() {
        HashMap<Integer, List<MachineSerialPrefixMap>> lcbMap = new HashMap<>();
        lcbMap.put(0xA1, Arrays.asList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Endurance), new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Performance)));
        lcbMap.put(0xC3, Arrays.asList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Performance), new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Lifestyle)));
        lcbMap.put(0xC4, Collections.singletonList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Performance)));
        lcbMap.put(0xC5, Collections.singletonList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Performance)));
        lcbMap.put(0xC7, Arrays.asList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Endurance), new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Performance), new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Lifestyle)));
        lcbMap.put(0xC8, Arrays.asList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Endurance), new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Performance)));
        lcbMap.put(0xA3, Collections.singletonList(new MachineSerialPrefixMap(FrameConfigEnum.Treadmill_Lifestyle)));
        return lcbMap;
    }


    @Test
    @Emulator
    @Chimera
    @com.jht.chimera.test.annotations.runner.MachineSetup
    public void testFrameSerialNumber() {
        try(UITestFixture ignored = new UITestFixture(MachineSetup.class)) {
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, "");
            frameSerialTestCase.initTestCase();
            navigateToMachineSelection();
            frameSerialTestCase.selectMachine();
            frameSerialTestCase.validate();
        }
    }

    private void navigateToMachineSelection() {
        onView(withId(R.id.language_selector)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.machine_setup_next_button)).perform(click());
        onView(withId(R.id.machine_setup_next_button)).perform(click());
        onView(withId(R.id.machine_setup_next_button)).perform(click());
    }

}
