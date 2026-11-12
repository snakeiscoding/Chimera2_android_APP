package com.jht.chimera.lib.common.process;

import android.content.pm.PackageManager;
import android.util.Log;

import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.context.ContextUtil;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class KillProcessHelper {

    ArrayList<String> mPackagesToKill = new ArrayList<String>();
    ArrayList<String> mPackagesToWipe = new ArrayList<String>();
    ArrayList<String> mPackagesToStop = new ArrayList<String>();
    ArrayList<String> mPackagesToClear = new ArrayList<String>();
    ArrayList<String> mPackagesToDisable = new ArrayList<String>();
    HashMap<String, Boolean> mAsynchronousList = new HashMap<>();

    ExecuteAsRoot mExecuteAsRoot = new ExecuteAsRoot();

    public void addPackageToKill(@NonNls String packageName, boolean asynchronous) {
        if (!mPackagesToKill.contains(packageName)) {
            mPackagesToKill.add(packageName);
            mAsynchronousList.put(packageName, asynchronous);
        }
    }

    public void addPackageToDisable(@NonNls String packageName, boolean asynchronous) {
        if (!mPackagesToDisable.contains(packageName)) {
            mPackagesToDisable.add(packageName);
            mAsynchronousList.put(packageName, asynchronous);
        }
    }

    public void addPackageToWipe(@NonNls String packageName, boolean asynchronous) {
        if (!mPackagesToWipe.contains(packageName)) {
            mPackagesToWipe.add(packageName);
            mAsynchronousList.put(packageName, asynchronous);
        }
    }

    public void addPackagesToStop(@NonNls String packageName, boolean asynchronous) {
        if (!mPackagesToStop.contains(packageName)) {
            mPackagesToStop.add(packageName);
            mAsynchronousList.put(packageName, asynchronous);
        }
    }

    public void addPackagesToClear(@NonNls String packageName, boolean asynchronous) {
        if (!mPackagesToClear.contains(packageName)) {
            mPackagesToClear.add(packageName);
            mAsynchronousList.put(packageName, asynchronous);
        }
    }

    public void kill() {
        mExecuteAsRoot.setCommandList(getCommandsToExecute());
        mExecuteAsRoot.execute(false);
    }

    String getSuffix(@NonNls String packageName) {
        Boolean isAsynchronous = mAsynchronousList.get(packageName);
        if (isAsynchronous != null) {
            if (isAsynchronous) {
                return " &";
            }
        }
        return "";
    }

    ArrayList<String> getCommandsToExecute() {
        ArrayList<String> commandList = new ArrayList<String>();

        commandList.add("log -p d -t process_killer \"Started.\""); //NON-NLS

        // run disable first since that will also stop the app process
        for (String p : mPackagesToDisable) {
            // Use the PackageManager setApplicationEnabledSetting to disable the app instead of 'pm disable
            // to helps with the issue where installing an app while try to enable it causes it to crash
            // TODO we probably should move this out this function - tech debt :)
            try {
                PackageManager packageManager = ContextUtil.getApplicationContext().getPackageManager();
                packageManager.setApplicationEnabledSetting(p, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception ignored) {}
        }

        // if we are stopping and killing the same package, stop first
        for (String p : mPackagesToStop) {
            //commandList.add("log -p d -t process_killer \"Stopping " + p + "\"");
            commandList.add("am force-stop " + p + getSuffix(p)); //NON-NLS
            //commandList.add("log -p d -t process_killer \"Stopped " + p + "\"");
        }

        for (String p : mPackagesToKill) {
            //String cmd = "am force-stop " + p; // this should work honeycomb and newer? is it fast enough?
            String cmd = "kill -9 $(pidof " + p + ")" + getSuffix(p); //NON-NLS
            commandList.add(cmd);
            //commandList.add("log -p d -t process_killer \"Killed " + p + "\"");
        }

        for (String p : mPackagesToWipe) {
            File file = new File("/data/data/" + p + "");
            if (file.exists() && file.isDirectory()) {
                //commandList.add("log -p d -t process_killer \"Wiping data directory for " + p + "\"");
                commandList.add("kill -9 " + p + getSuffix(p)); //NON-NLS
                commandList.add("rm -rf /data/data/" + p + "/!(lib)" + getSuffix(p)); //NON-NLS NON-NLS
                //commandList.add("log -p d -t process_killer \"Wiping data directory for " + p + "\"");
            }
        }

        for (String p : mPackagesToClear) {
            //commandList.add("log -p d -t process_killer \"Clearing " + p + "\"");
            commandList.add("pm clear " + p + getSuffix(p)); //NON-NLS
            //commandList.add("log -p d -t process_killer \"Cleared " + p + "\"");
        }

        commandList.add("wait"); //NON-NLS
        commandList.add("log -p d -t process_killer \"Finished.\""); //NON-NLS
        return commandList;
    }

    public static void stopThread(Thread thd) {
        if (thd != null) {
            thd.interrupt();
            try {thd.join(500);} catch (Exception ex) {}
        }
    }
}
