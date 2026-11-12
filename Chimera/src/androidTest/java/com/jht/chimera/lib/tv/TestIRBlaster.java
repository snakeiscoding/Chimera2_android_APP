package com.jht.chimera.lib.tv;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxController;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.test.annotations.hardware.NotAutomated;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.irlibrary.learner.CommandFusion;
import com.jht.irlibrary.learner.HarcHardwareException;
import com.jht.irlibrary.learner.InvalidArgumentException;
import com.jht.irlibrary.learner.IrCoreUtils;
import com.jht.irlibrary.learner.ModulatedIrSequence;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class TestIRBlaster {
    private static final String TAG = "TestIRBlaster";
    @Test
    @NotAutomated
    public void testHotwire() {
        testSTB("Fision Arris VIP5602W");
    }

    @Test
    @NotAutomated
    public void testDishWally() {
        testSTB("Dish Echostar");
    }

    @Test
    @NotAutomated
    public void testTDS1() {
        testSTB("TDS TV+ Arris VIP6102W");
    }

    public void testSTB(String stb) {
        DataMatrix dataMatrix = new DataMatrix();
        int frequency;
        StringBuilder testResults = new StringBuilder();
        StringBuilder testResult;
        BaseTestFixture.startServicesBlocking(dataMatrix);
        dataMatrix.finish();
        String originalSTB = DataMatrix.getString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING);
        SetTopBoxController setTopBoxController = null;
        try {
            DataMatrix.putString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING, stb);
            testResults.append("Set Top Box - ").append(stb);
            Log.d(TAG, "Set Top Box - " + stb);
            setTopBoxController = new SetTopBoxController(InstrumentationRegistry.getInstrumentation().getTargetContext());
            setTopBoxController.open();

            frequency = setTopBoxController.getCurrentSTB().frequencyKhz * 1000;

            try (CommandFusion commandFusion = new CommandFusion()) {
                JHTNotification notification = new JHTNotification();
                commandFusion.open(InstrumentationRegistry.getInstrumentation().getTargetContext(), (device, ready) -> notification.setNotification(), null);

                Assert.assertTrue(notification.waitForNotification(7000));

                try {
                    commandFusion.fetchVersion();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (HarcHardwareException e) {
                    e.printStackTrace();
                }
                String version = commandFusion.getVersion();
                Log.d(TAG, "getVersion " + version);

                for (Map.Entry<String, SetTopBoxSetting.RemoteButton> button : setTopBoxController.getCurrentSTB().remoteButtonMap.entrySet()) {
                    testResult = new StringBuilder();
                    List<Double> expectedValues = new ArrayList<>();

                    for(int i = 0; i < button.getValue().pulseSequences.length; i++) {
                        for(int j = 0; j <= setTopBoxController.getCurrentSTB().repeats; j++) {
                            for(int k = 0; k < button.getValue().pulseSequences[i].sequence.length; k++) {
                                expectedValues.add((double) button.getValue().pulseSequences[i].sequence[k]);
                            }
                        }
                    }
                    try {
                        commandFusion.startCapture();
                    } catch (IOException ignored) {
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                    setTopBoxController.executeSTBCommand(button.getValue());
                    ModulatedIrSequence irSequence = null;
                    try {
                        irSequence = commandFusion.capture(3000);
                    } catch (IOException | InvalidArgumentException ignored) {
                    }

                    testResult.append("\tTesting ").append(button.getValue().buttonName).append(" ... ");
                    if(irSequence == null) {
                        testResult.append("Failed - Did not get IR signal");
                    }
                    else if(Math.abs(irSequence.getFrequency() - frequency) > IrCoreUtils.DEFAULT_FREQUENCY_TOLERANCE) {
                        testResult.append("Failed - Frequency out of range expected ").append(frequency).append(" but got " ).append(irSequence.getFrequency());
                    }
                    else {
                        double[] data = irSequence.toDoubles();
                        boolean success = expectedValues.size() == data.length;
                        if(!success) {
                            testResult.append("Failed - IR data length doesn't match.  Expected ").append(expectedValues.size()).append(" but got ").append(data.length);
                        }
                        for (int i = 0; i < expectedValues.size() - 1 && i < data.length; i++) {
                            if(
                                Math.abs(expectedValues.get(i) - data[i]) > IrCoreUtils.DEFAULT_RELATIVE_TOLERANCE * expectedValues.get(i) &&
                                Math.abs(expectedValues.get(i) - data[i]) > IrCoreUtils.DEFAULT_ABSOLUTE_TOLERANCE) {
                                if(success) {
                                    success = false;
                                    testResult.append("Failed");
                                }
                                //if(i % 2 == 0) {
                                //    testResult.append("\n\t\tPulse ").append(i/2).append(" out of range expected ").append(expectedValues.get(i)).append(" but got ").append(data[i]);
                                //}
                                //else {
                                //    testResult.append("\n\t\tSpace ").append(i/2).append(" out of range expected ").append(expectedValues.get(i)).append(" but got ").append(data[i]);
                                //}
                            }
                        }
                        if(success) {
                            testResult.append("Success");
                        } else {
                            testResult.append("\n\t\tExpected: ");
                            for(Double d : expectedValues) {
                                testResult.append(d.intValue()).append(" ");
                            }
                            testResult.append("\n\t\tReceived: ");
                            for(double d : data) {
                                testResult.append((int)d).append(" ");
                            }
                        }
                    }
                    Log.d(TAG, testResult.toString());
                    testResults.append("\n").append(testResult.toString());
                }

                Assert.assertFalse(testResults.toString(), testResults.toString().contains("Failed"));


            }
        }
        finally {
            DataMatrix.putString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING, originalSTB);
            if(setTopBoxController != null) {
                setTopBoxController.close();
            }
        }
    }
}
