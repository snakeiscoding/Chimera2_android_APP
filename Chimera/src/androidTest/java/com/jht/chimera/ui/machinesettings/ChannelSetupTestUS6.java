package com.jht.chimera.ui.machinesettings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.jht.chimera.test.espresso.options.OptionSelectorChecks.selectorHasOptions;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.canDeleteChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.canEditChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.canEnableChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.channelEnabled;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.channelName;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.editChannel;
import static com.jht.chimera.test.espresso.recyclerview.RecyclerViewOperations.viewHolderExists;
import static com.jht.chimera.test.espresso.userinput.UserInputActions.setUserInputText;
import static com.jht.chimera.test.fixtures.BaseTestFixture.sleepIgnoreInterruption;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.US6Tuner;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MachineSettings;
import com.jht.chimera.test.fixtures.US6TunerTestFixture;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ChannelSetupTestUS6 {

    @Test @Emulator @Chimera @MachineSettings
    public void testChannelSetupLaunch() {

        try(US6TunerTestFixture testFixture = new US6TunerTestFixture(TVChannelSetupActivityNew.class)) {
            onView(withId(R.id.channel_setup_mode_1)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.channel_setup_mode_2)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

            onView(withId(R.id.tv_channel_setup_keypad)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.tv_channel_setup_extended_controls)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.tv_channel_setup_color)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

            onView(withId(R.id.eng_tv_control_expanded_upper_left)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_up)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_expanded_upper_right)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_left)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_confirm)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_right)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_expanded_bottom_left)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.eng_tv_control_down)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));


            onView(withId(R.id.tv_channel_setup_operation)).check(selectorHasOptions(new String[]{
                    testFixture.getTargetString(R.string.import_channel_list_from_usb_tc),
                    testFixture.getTargetString(R.string.export_channel_list_from_usb_tc),
                    testFixture.getTargetString(R.string.import_channels_from_tuner_tc),
                    testFixture.getTargetString(R.string.export_channels_to_tuner),
                    testFixture.getTargetString(R.string.update_tuner_firmware_tc)
            }));

        }
    }


    /**
     * This test relies heavily on the com.jht.chimera.test.espresso.recyclerview.ChannelList class
     * to perform actions and check the channel list items.
     */
    @Test @Emulator @Chimera @MachineSettings
    public void testChannelList() {

        try(US6TunerTestFixture testFixture = new US6TunerTestFixture()) {
            testFixture.copyAssetToFlash("tuner/us6/settings/ATSC_channels.bin", "settings/swift/ATSC_channels.bin");
            testFixture.copyAssetToFlash("tuner/us6/settings/TVChannel.json", "settings/swift/TVChannel.json");
            testFixture.copyAssetToFlash("tuner/us6/settings/TVChannelNames.txt", "settings/swift/TVChannelNames.txt");
            testFixture.startActivity(TVChannelSetupActivityNew.class);
            sleepIgnoreInterruption(500);

            US6Tuner tuner = new US6Tuner(testFixture.getTargetContext());
            for (int i = 0; i < tuner.tuner().getChannelList(true).size(); i++) {
                onView(withId(R.id.eng_tv_channel_list)).
                        perform(viewHolderExists(i)).
                        check(channelName(i, tuner.tuner().getChannelList(true).get(i).displayName(null))).
                        check(canEnableChannel(i, false)).
                        check(channelEnabled(i, tuner.tuner().getChannelList(true).get(i).enabled())).
                        check(canDeleteChannel(i, false)).
                        check(canEditChannel(i, true));
            }

            // Modify item added earlier.
            onView(withId(R.id.eng_tv_channel_list)).check(canEditChannel(0, true)).perform(RecyclerViewActions.actionOnItemAtPosition(0, editChannel()));
            onView(withId(R.id.tv_channel_name)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("test name"));
            onView(withId(R.id.message_dialog_ok)).perform(click());
            onView(withId(R.id.eng_tv_channel_list)).check(channelName(0, tuner.tuner().getChannelList(true).get(0).number() + " - test name"));

            // Modify item added earlier.
            onView(withId(R.id.eng_tv_channel_list)).check(canEditChannel(0, true)).perform(RecyclerViewActions.actionOnItemAtPosition(0, editChannel()));
            onView(withId(R.id.tv_channel_name)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("test afaewea"));
            onView(withId(R.id.message_dialog_cancel)).perform(click());
            onView(withId(R.id.eng_tv_channel_list)).check(channelName(0, tuner.tuner().getChannelList(true).get(0).number() + " - test name"));
        }
    }
}