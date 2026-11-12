package com.jht.chimera.ui.machinesetup;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.jht.chimera.test.espresso.userinput.UserInputActions.checkUserInputText;
import static org.hamcrest.CoreMatchers.allOf;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;
import java.util.Map;

class FrameSerialTestCase {
    private final MachineTestCase machineTestCase;
    private final String testSerialNumber;
    private final String expectedSerialNumber;

    FrameSerialTestCase(Map.Entry<Integer, List<MachineSerialPrefixMap>> machineMapEntry, int machineIndex, String prefix) {
        machineTestCase = new MachineTestCase(machineMapEntry, machineIndex);
        testSerialNumber = prefix + "TESTING1234";
        expectedSerialNumber = prefix + "TESTING1234";
    }

    void initTestCase() {
        machineTestCase.initTestCase();
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, testSerialNumber);
        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, false);
    }

    void selectMachine() {
        machineTestCase.selectMachine();
    }

    void validate() {
        onView(allOf(withId(R.id.machine_setup_frame_serial_number), isDisplayed())).check(checkUserInputText(expectedSerialNumber));
    }

}
