package com.jht.chimera.service.systemsettings;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class ImportExportSettingsProtocol {

    public static final int NEEDS_CLIENT_CONSOLE_SERIAL_NUMBER = 0;
    public static final int GET_IP = 1;
    public static final int CONNECTED = 2;
    public static final int READY_TO_BEGIN_TRANSFER = 3;
    public static final int READY_TO_SEND_ASSETS = 4;
    public static final int DONE_EXPORTING = 5;

    private int state = NEEDS_CLIENT_CONSOLE_SERIAL_NUMBER;

    public String processInput(String input) {
        String output = "";
        if(state == NEEDS_CLIENT_CONSOLE_SERIAL_NUMBER) {
            if(input == null) {
//                output = "RequestConsoleSerialNumber";
                output = "RequestConsoleSerialNumber:" + DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, "SOME SERIAL NUMBER"); //NON-NLS
            } else {
                state = CONNECTED;
                output = input;
            }
        } else if(state == GET_IP) {
            output = input;
            state = CONNECTED;
        } else if(state == CONNECTED) {
            if(input.equalsIgnoreCase("readyToReceiveSettings")) {
                state = READY_TO_BEGIN_TRANSFER;
            }
        } else if(state == READY_TO_BEGIN_TRANSFER) {
            if(input.equalsIgnoreCase("model_settings_received")) {
                state = READY_TO_SEND_ASSETS;
            }
        } else if(state == READY_TO_SEND_ASSETS) {
            if(input.equalsIgnoreCase("Export_Done")) {
                state = DONE_EXPORTING;
            }
        }
        return output;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
