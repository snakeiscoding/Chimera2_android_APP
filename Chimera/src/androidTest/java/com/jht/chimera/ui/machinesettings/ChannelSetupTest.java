package com.jht.chimera.ui.machinesettings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.jht.chimera.test.espresso.dialogs.ProgressDialogChecks.findProgressDialog;
import static com.jht.chimera.test.espresso.dialogs.ProgressDialogChecks.monitorProgressDialog;
import static com.jht.chimera.test.espresso.options.OptionSelectorChecks.selectOption;
import static com.jht.chimera.test.espresso.options.OptionSelectorChecks.selectorHasOptions;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.canDeleteChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.canEditChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.canEnableChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.channelEnabled;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.channelName;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.deleteChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.editChannel;
import static com.jht.chimera.test.espresso.recyclerview.ChannelList.toggleChannelEnable;
import static com.jht.chimera.test.espresso.recyclerview.RecyclerViewOperations.viewHolderExists;
import static com.jht.chimera.test.espresso.userinput.UserInputActions.setUserInputText;
import static com.jht.chimera.test.espresso.view.ViewChecks.isActive;
import static com.jht.chimera.test.fixtures.BaseTestFixture.sleepIgnoreInterruption;

import android.util.Log;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.US7Tuner;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MachineSettings;
import com.jht.chimera.test.fixtures.US7TunerTestFixture;
import com.jht.serialport.peripheral.tuner.swift.SwiftProIdiomChannel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ChannelSetupTest {
    private static final String TAG = "ChannelSetupTest";



    @Test @Emulator @Chimera @MachineSettings
    public void testChannelSetupLaunch() {
        try(US7TunerTestFixture testFixture = new US7TunerTestFixture(TVChannelSetupActivityNew.class)) {
            onView(withId(R.id.channel_setup_mode_1)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(true));
            onView(withId(R.id.channel_setup_mode_2)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(false));

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
                    testFixture.getTargetString(R.string.import_ip_list_from_usb_tc),
                    testFixture.getTargetString(R.string.export_ip_list_to_tuner_tc),
                    testFixture.getTargetString(R.string.update_tuner_firmware_tc)
            }));

            onView(withId(R.id.channel_setup_mode_2)).perform(click()).check(isActive(true));
            onView(withId(R.id.channel_setup_mode_1)).check(isActive(false));

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
    public void testIPList() {
        try(US7TunerTestFixture testFixture = new US7TunerTestFixture(TVChannelSetupActivityNew.class)) {
            onView(withId(R.id.channel_setup_mode_1)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(true));
            onView(withId(R.id.channel_setup_mode_2)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(false));

            // Add an address
            onView(withId(R.id.eng_tv_channel_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
            onView(withId(R.id.iptv_channel_port)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("5678"));
            onView(withId(R.id.iptv_channel_address)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("224.1.2.3"));
            onView(withId(R.id.message_dialog_ok)).perform(click());
            onView(withId(R.id.eng_tv_channel_list)).check(channelName(0, "224.1.2.3 5678"));

            // Check the tuner.
            US7Tuner tuner = new US7Tuner(testFixture.getTargetContext());
            Assert.assertEquals(1, tuner.tuner().getIPTVList().size());
            Assert.assertEquals("224.1.2.3", tuner.tuner().getIPTVList().get(0).ip());
            Assert.assertEquals("5678", tuner.tuner().getIPTVList().get(0).port());

            // Try to add another, but change mind.
            onView(withId(R.id.eng_tv_channel_list)).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
            onView(withId(R.id.iptv_channel_port)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("1234"));
            onView(withId(R.id.iptv_channel_address)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("224.4.5.6"));
            onView(withId(R.id.message_dialog_cancel)).perform(click());
            onView(withId(R.id.eng_tv_channel_list)).check(channelName(0, "224.1.2.3 5678"));

            // Check the tuner.
            sleepIgnoreInterruption(100);
            tuner.tuner().loadSettings();
            Assert.assertEquals(1, tuner.tuner().getIPTVList().size());
            Assert.assertEquals("224.1.2.3", tuner.tuner().getIPTVList().get(0).ip());
            Assert.assertEquals("5678", tuner.tuner().getIPTVList().get(0).port());

            // Modify item added earlier.
            onView(withId(R.id.eng_tv_channel_list)).check(canEditChannel(0, true)).perform(RecyclerViewActions.actionOnItemAtPosition(0, editChannel()));
            onView(withId(R.id.iptv_channel_port)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("9876"));
            onView(withId(R.id.iptv_channel_address)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("224.7.8.9"));
            onView(withId(R.id.message_dialog_ok)).perform(click());
            onView(withId(R.id.eng_tv_channel_list)).check(channelName(0, "224.7.8.9 9876"));

            // Check the tuner.
            sleepIgnoreInterruption(100);
            tuner.tuner().loadSettings();
            Assert.assertEquals(1, tuner.tuner().getIPTVList().size());
            Assert.assertEquals("224.7.8.9", tuner.tuner().getIPTVList().get(0).ip());
            Assert.assertEquals("9876", tuner.tuner().getIPTVList().get(0).port());

            // Delete channel.
            onView(withId(R.id.eng_tv_channel_list)).check(canDeleteChannel(0, true)).perform(RecyclerViewActions.actionOnItemAtPosition(0, deleteChannel()));
            sleepIgnoreInterruption(100);
            tuner.tuner().loadSettings();
            Assert.assertEquals(0, tuner.tuner().getIPTVList().size());

            // Destroy the tuner.
            tuner.destroy();
        }
    }


    /**
     * This test relies heavily on the com.jht.chimera.test.espresso.recyclerview.ChannelList class
     * to perform actions and check the channel list items.
     */
    @Test @Emulator @Chimera @MachineSettings
    public void testChannelList() {

        try(US7TunerTestFixture testFixture = new US7TunerTestFixture()) {
            testFixture.copyAssetToFlash("tuner/us7/settings/ATSC_channels.bin", "settings/us7/ATSC_channels.bin");
            testFixture.copyAssetToFlash("tuner/us7/settings/consoleChannelInfo.txt", "settings/us7/consoleChannelInfo.txt");
            testFixture.copyAssetToFlash("tuner/us7/settings/us7_ip_scan.xml", "settings/us7/us7_ip_scan.xml");

            testFixture.startActivity(TVChannelSetupActivityNew.class);
            onView(withId(R.id.channel_setup_mode_1)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(false));
            onView(withId(R.id.channel_setup_mode_2)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(true));

            sleepIgnoreInterruption(500);

            US7Tuner tuner = new US7Tuner(testFixture.getTargetContext());
            for (int i = 0; i < tuner.tuner().getChannelList(true).size(); i++) {
                onView(withId(R.id.eng_tv_channel_list)).
                        perform(viewHolderExists(i)).
                        check(channelName(i, tuner.tuner().getChannelList(true).get(i).displayName(null))).
                        check(canEnableChannel(i, true)).
                        check(channelEnabled(i, tuner.tuner().getChannelList(true).get(i).enabled())).
                        check(canDeleteChannel(i, false)).
                        check(canEditChannel(i, false));
            }
            onView(withId(R.id.eng_tv_channel_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, toggleChannelEnable()));
            sleepIgnoreInterruption(100);
            tuner.tuner().loadSettings();
            onView(withId(R.id.eng_tv_channel_list)).check(channelEnabled(0, tuner.tuner().getChannelList(true).get(0).enabled()));

            onView(withId(R.id.channel_setup_mode_1)).perform(click()).check(isActive(true));
            onView(withId(R.id.channel_setup_mode_2)).check(isActive(false));
            sleepIgnoreInterruption(500);
            for (int i = 0; i < tuner.tuner().getIPTVList().size(); i++) {
                onView(withId(R.id.eng_tv_channel_list)).
                        perform(viewHolderExists(i)).
                        check(channelName(i, tuner.tuner().getIPTVList().get(i).displayName(null))).
                        check(canDeleteChannel(i, true)).
                        check(canEnableChannel(i, false)).
                        check(canEditChannel(i, true));
            }
        }
    }

    /**
     * Push the IP list to the tuner emulator to make sure the progress dialog works as expected.
     */
    @Test @Emulator @Chimera @MachineSettings
    public void testPushIPList() {

        try(US7TunerTestFixture testFixture = new US7TunerTestFixture(TVChannelSetupActivityNew.class)) {
            onView(withId(R.id.channel_setup_mode_1)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(true));
            onView(withId(R.id.channel_setup_mode_2)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(false));

            // Add an address
            onView(withId(R.id.eng_tv_channel_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
            onView(withId(R.id.iptv_channel_port)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("5678"));
            onView(withId(R.id.iptv_channel_address)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(setUserInputText("224.1.2.3"));
            onView(withId(R.id.message_dialog_ok)).perform(click());
            onView(withId(R.id.eng_tv_channel_list)).check(channelName(0, "224.1.2.3 5678"));

            try {
                testFixture.tunerEmulator().channels().add(new SwiftProIdiomChannel(InetAddress.getByName("224.1.2.3"), 5678, "1", "1", "test"));
                testFixture.tunerEmulator().channels().add(new SwiftProIdiomChannel(InetAddress.getByName("224.1.2.3"), 5678, "1", "2", "test 2"));
            } catch (UnknownHostException e) {
                Log.w(TAG, e);
            }
            onView(withId(R.id.tv_channel_setup_operation)).perform(selectOption(1));
            onView(withId(R.id.tv_channel_setup_operation_button)).perform(click());

            List<String> expectedMessages = new ArrayList<>();
            expectedMessages.add(testFixture.getTargetString(R.string.import_channels_from_tuner_tc));
            expectedMessages.add(testFixture.getTargetString(R.string.wait_for_scan_pg));
            expectedMessages.add(testFixture.getTargetString(R.string.import_channels_from_tuner_tc));
            expectedMessages.add(testFixture.getTargetString(R.string.export_channels_success_tc));
            onView(findProgressDialog()).perform(monitorProgressDialog(expectedMessages, 30000, true));

            // Need to make sure the import completes.
            sleepIgnoreInterruption(1000);

            onView(withId(R.id.channel_setup_mode_2)).perform(click()).check(isActive(true));
            sleepIgnoreInterruption(500);
            for (int i = 0; i < testFixture.tunerEmulator().channels().size(); i++) {
                onView(withId(R.id.eng_tv_channel_list)).
                        perform(viewHolderExists(i)).
                        check(channelName(i, testFixture.tunerEmulator().channels().get(i).displayName(null))).
                        check(canDeleteChannel(i, false)).
                        check(canEnableChannel(i, true)).
                        check(canEditChannel(i, false));
            }
        }

    }
    /**
     * Test the import and export channels with the emulator.
     */
    @Test @Emulator @Chimera @MachineSettings
    public void testImportExportChannels() {

        try(US7TunerTestFixture testFixture = new US7TunerTestFixture(TVChannelSetupActivityNew.class)) {
            onView(withId(R.id.channel_setup_mode_1)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(true));
            onView(withId(R.id.channel_setup_mode_2)).
                    check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).
                    check(isActive(false));

            try {
                testFixture.tunerEmulator().channels().add(new SwiftProIdiomChannel(InetAddress.getByName("224.1.2.3"), 5678, "1", "1", "test 3"));
                testFixture.tunerEmulator().channels().add(new SwiftProIdiomChannel(InetAddress.getByName("224.1.2.3"), 5678, "1", "2", "test 4"));
            } catch (UnknownHostException e) {
                Log.w(TAG, e);
            }

            onView(withId(R.id.channel_setup_mode_2)).perform(click()).check(isActive(true));


            onView(withId(R.id.tv_channel_setup_operation)).perform(selectOption(2));
            onView(withId(R.id.tv_channel_setup_operation_button)).perform(click());

            List<String> expectedMessages = new ArrayList<>();
            expectedMessages.add(testFixture.getTargetString(R.string.import_channels_success_tc));
            onView(findProgressDialog()).perform(monitorProgressDialog(expectedMessages, 30000, true));

            // Need to make sure the import completes.
            sleepIgnoreInterruption(1000);
            onView(withId(R.id.channel_setup_mode_1)).perform(click());
            sleepIgnoreInterruption(500);
            onView(withId(R.id.channel_setup_mode_2)).perform(click());
            sleepIgnoreInterruption(500);

            for (int i = 0; i < testFixture.tunerEmulator().channels().size(); i++) {
                onView(withId(R.id.eng_tv_channel_list)).perform(viewHolderExists(i)).
                        check(channelName(i, testFixture.tunerEmulator().channels().get(i).displayName(null))).
                        check(canDeleteChannel(i, false)).
                        check(canEnableChannel(i, true)).
                        check(canEditChannel(i, false));
            }

            testFixture.tunerEmulator().channels().clear();

            onView(withId(R.id.tv_channel_setup_operation)).perform(selectOption(3));
            onView(withId(R.id.tv_channel_setup_operation_button)).perform(click());

            expectedMessages = new ArrayList<>();
            expectedMessages.add(testFixture.getTargetString(R.string.import_channels_success_tc));

            onView(findProgressDialog()).perform(monitorProgressDialog(expectedMessages, 30000, true));

        }
    }
}