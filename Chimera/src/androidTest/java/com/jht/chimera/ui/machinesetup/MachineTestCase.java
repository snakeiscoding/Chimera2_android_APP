package com.jht.chimera.ui.machinesetup;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.test.espresso.recyclerview.ModelList;
import com.jht.chimera.test.espresso.recyclerview.RecyclerViewOperations;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;
import java.util.Map;

class MachineTestCase {
    private final int lcb;
    private final FrameConfigEnum[] machines;
    private final int machineIndex;


    MachineTestCase(Map.Entry<Integer, List<MachineSerialPrefixMap>> machineMapEntry, int machineIndex) {
        lcb  = machineMapEntry.getKey();
        machines = new FrameConfigEnum[machineMapEntry.getValue().size()];
        for(int i = 0; i < machineMapEntry.getValue().size(); i++) {
            machines[i] = machineMapEntry.getValue().get(i).machine();
        }
        this.machineIndex = machineIndex;
    }

    // testCases.add(new FrameSerialTestCase(new MachineTestCase(machinesForLCBTestCase, i), prefix + "TESTING1234", prefix + "TESTING1234"));

    void initTestCase() {
        DataMatrix.putInt(HALKeys.DATA_LCB_TYPE_INT, lcb);
    }

    void selectMachine() {
        if (machines.length > 1) {
            ViewInteraction interaction = onView(allOf(withId(R.id.machine_setup_machine_selection), isDisplayed()))
                    .check(RecyclerViewOperations.viewHasNItems(machines.length));
            for (int i = 0; i < machines.length; i++) {
                interaction.check(ModelList.isModel(i, machines[i]));
            }
            interaction.perform(RecyclerViewActions.actionOnItemAtPosition(machineIndex, click()));
            onView(withId(R.id.machine_setup_next_button)).perform(click());
        }
    }
}
