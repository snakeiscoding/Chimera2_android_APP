package com.jht.chimera.lib.network.ethernet;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.usb.StorageUtils;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.lang.reflect.Method;

public class NetworkSettingsImportExport {
    private static final String TAG = "ImportWiFiSettings";
    @NonNls private static final String wifiBackupFilePath = "WifiBackup.bin";
    @NonNls private static final String wifiConfigStoreFilePath = "/WifiConfigStore.xml";
    @NonNls private static final String wifiConfigAndroidPath = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ?
            "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml" :
            "/data/misc/wifi/WifiConfigStore.xml";

    /*
     * NOTE: The exporting and importing the WifiConfigStore.xml requires an OS modification to load the config
     * via broadcast 'com.jht.import.wifi.settings'. The exporting and importing using the WiFiManager
     * reflection doesn't need any OS modification (it's also a binary so the password is not exposed).
     */

    public static boolean usbExportWifiConfig() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return usbExportWifiBackup(StorageUtils.getUSBPath());
        } else {
            return usbExportWifiConfigStoreXml(StorageUtils.getUSBPath());
        }
    }

    public static boolean usbImportWifiConfig() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return usbImportWifiBackup();
        } else {
            return usbImportWifiConfigStoreXml();
        }
    }

    private static boolean usbExportWifiConfigStoreXml(File location) {
        File wifiFile = new File(location, wifiConfigStoreFilePath);
        File wifiMd5File = new File(location, "WifiConfigStore.md5");
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("cp " + wifiConfigAndroidPath + " " + wifiFile.getAbsolutePath());
        executeAsRoot.addCommand("echo `md5sum " + wifiConfigAndroidPath + " | cut -d \" \" -f 1` > "  + wifiMd5File.getAbsolutePath());
        executeAsRoot.addCommand("chmod 777 " + wifiFile.getAbsolutePath());
        executeAsRoot.addCommand("chmod 777 " + wifiMd5File.getAbsolutePath());
        executeAsRoot.addCommand("sync");
        executeAsRoot.execute();
        return new File(StorageUtils.getUSBPath().getAbsolutePath() + wifiConfigStoreFilePath).exists();
    }

    private static boolean usbImportWifiConfigStoreXml() {
        File wifiFile = new File(StorageUtils.getUSBPath(), wifiConfigStoreFilePath);
        File wifiMd5File = new File(StorageUtils.getUSBPath(), "WifiConfigStore.md5");
        if(FileUtils.checkMD5(wifiFile, wifiMd5File)) {
            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("chmod 777 " + wifiConfigAndroidPath);
            executeAsRoot.addCommand("cp " + StorageUtils.getUSBPath().getAbsolutePath() + wifiConfigStoreFilePath + " " + wifiConfigAndroidPath);
            executeAsRoot.addCommand("chmod 777 " + wifiConfigAndroidPath);
            executeAsRoot.execute();
            return true;
        }
        Log.w(TAG, "Import failed because MD5 was corrupt.");
        return false;
    }

    private static boolean usbImportWifiBackup() {
        try {
            // compute the file location
            File wifiFile = new File(StorageUtils.getUSBPath(), wifiBackupFilePath);
            File wifiMd5File = new File(StorageUtils.getUSBPath(), wifiBackupFilePath + ".md5");

            if(FileUtils.checkMD5(wifiFile, wifiMd5File)) {

                // get the data from the binary file
                byte[] data = java.nio.file.Files.readAllBytes(wifiFile.toPath());

                Context context = ContextUtil.getApplicationContext();
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                // call the restoreBackupData function through reflection
                Method method = wifiManager.getClass().getDeclaredMethod("restoreBackupData", byte[].class);
                method.setAccessible(true);
                method.invoke(wifiManager, (Object) data);

                return true;
            }
            Log.w(TAG, "usbImportWifiBackup failed because MD5 was corrupt.");
        } catch (Exception e) {
            Log.e(TAG, "usbImportWifiBackup failed", e);
        }
        return false;
    }

    private static boolean usbExportWifiBackup(File location) {

        try {
            Context context = ContextUtil.getApplicationContext();
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            // call the retrieveBackupData function through reflection
            Method method = wifiManager.getClass().getDeclaredMethod("retrieveBackupData");
            method.setAccessible(true);
            Object data = method.invoke(wifiManager);

            // get the wifi backup data has hex string
            if (data == null) return false;
            String hexData = Strings.bytesToHex((byte[]) data);

            // compute the file location
            File wifiFile = new File(location, wifiBackupFilePath);
            File wifiMd5File = new File(location, wifiBackupFilePath + ".md5");

            // create the Wifi binary file
            if (wifiFile.exists()) wifiFile.delete();
            wifiFile.createNewFile();
            java.nio.file.Files.write(wifiFile.toPath(), (byte[]) data);

            // save md5 checksum to Wifi binary file
            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("echo `md5sum " + wifiFile.getAbsolutePath() + " | cut -d \" \" -f 1` > "  + wifiMd5File.getAbsolutePath());
            executeAsRoot.addCommand("chmod 777 " + wifiFile.getAbsolutePath());
            executeAsRoot.addCommand("chmod 777 " + wifiMd5File.getAbsolutePath());
            executeAsRoot.addCommand("sync");
            executeAsRoot.execute();

            // return if the file exists
            return wifiFile.exists();
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return false;
        }
    }

}
