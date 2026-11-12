package com.jht.chimera.ui.machinesetup;

import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class MachineSetupPageData {
    private static final String TAG = "MachineSetupPageData";

    /**
     * Required prefix for Bridge Serial Number.
     */
    private static final String PREFIX_BRIDGE_SN = "B";

    public ImageButton nextButton;
    public ImageButton previousButton;
    public ImageButton confirmButton;
    public TextView title;
    public JHTCommonTypes.MachineType machineTypeSelection;

    public String consoleSerialNumberEntered = "";
    public String frameSerialNumberEntered = "";
    public String bridgeSerialNumberEntered = "";

    public String consoleSerialNumberConfirm = "";
    public String frameSerialNumberConfirm = "";
    public String bridgeSerialNumberConfirm = "";

    public String consolePrefix = "";
    public String framePrefix = "";

    public int clubIdEntered = 0;
    public int clubIdConfirm = 0;
    public boolean useSafetyPin = false;
    public String safetyPin = null;
    public Boolean wasCalibrationDone = null;

    public void loadSerialNumbers() {
        // Make sure the frame config is updated.
        FrameConfigManager.getInstance().updateSettings();

        String consoleSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        String frameSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        int clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);

        consolePrefix = ProductConfig.getConsoleType(machineTypeSelection).getConsolePrefix();
        framePrefix = FrameConfigManager.getInstance().getFramePrefix();

        Log.d(TAG, "Serial numbers " + consolePrefix + " " + consoleSerial + " " + machineTypeSelection);
        consoleSerialNumberEntered = consoleSerial;
        consoleSerialNumberConfirm = consoleSerial;

        if (FrameConfigManager.getInstance().framePrefixValid(frameSerial)) {
            frameSerialNumberEntered = frameSerial;
            frameSerialNumberConfirm = frameSerial;
        } else {
            frameSerialNumberEntered = framePrefix;
            frameSerialNumberConfirm = framePrefix;
        }

        if (BuildConfig.HAS_BRIDGE_SN) {
            String bridgeSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING);
            if (!bridgeSerial.startsWith(PREFIX_BRIDGE_SN)) {
                bridgeSerialNumberEntered = PREFIX_BRIDGE_SN;
                bridgeSerialNumberConfirm = PREFIX_BRIDGE_SN;
            } else {
                bridgeSerialNumberEntered = bridgeSerial;
                bridgeSerialNumberConfirm = bridgeSerial;
            }
        }

        if (clubId != 0) {
            clubIdEntered = clubId;
            clubIdConfirm = clubId;
        }
    }
}
