package com.jht.chimera.lib.common.config;

import android.util.Log;

import com.jht.androidcommonalgorithms.util.ExecuteAsRoot;
import com.jht.androidcommonalgorithms.util.SystemCommandResult;

import org.jetbrains.annotations.NonNls;

public class TouchPanel {
    @NonNls public static final String TAG = "TouchPanel";

    public static String getFWVersionChimera1() {

        if(ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera)) {
            SystemCommandResult result = ExecuteAsRoot.executeCommand("eGalaxInfo");
            String[] lines = result.output().split("\n");
            for (String line : lines) {
                if (line.contains("FW Version")) {
                    String[] version = line.split(":\\s+");
                    return version.length >= 2 ? version[1] : "N/A"; // NON-NLS
                }
            }
        }
        return "N/A"; // NON-NLS

    }

    public static String getFWVersionChimera2() {

        if(ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)){
            SystemCommandResult result_command = ExecuteAsRoot.executeCommand("ls /vendor/bin | grep Linux_Daemon_ITS");
            String command = result_command.output().trim();
            if (command.equals("")){
                Log.e(TAG,"touch firmware tool Linux_Daemon_ITS not found!!");
                return "N/A";
            }
            SystemCommandResult result = ExecuteAsRoot.executeCommand(command + " PanelInfor USB V3 null null --no-reset --hidraw");
            String[] lines = result.output().split("\n");
            for (String line : lines) {
                if (line.contains("fw-version-tag:") || line.contains("fw version:")) {
                    String[] version = line.split(": ")[1].replace("[", "").replace("]", "").split("\\.");
                    return Integer.valueOf(version[0]) + "." + Integer.valueOf(version[1]) + "." + Integer.valueOf(version[2]) + "." + Integer.valueOf(version[3]);
                }
            }
        }
        return "N/A"; // NON-NLS

    }
}
