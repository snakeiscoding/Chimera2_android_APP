package com.jht.chimera.lib.common;

import android.content.Context;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.TouchPanel;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import updatepackages.Version;

public class VersionHelper {
    @NonNls
    private static final String TAG = "VersionHelper";

    /** get string value for given key in Android "Settings.Secure" */
    private static @Nullable String getAndroidSettingString(@NonNls String key) {
        Context context = ContextUtil.getApplicationContext();
        if (context == null) return null;
        return AndroidSetting.getString(context.getContentResolver(), key);
    }

    public static @Nullable String getCurrentBundleVersion() {
        return getAndroidSettingString("com.jht.updateservice.bundleVersion");
    }

    public static String getOsVersion() {
        String osVersion = SystemPropertyUtil.get("ro.product.os"); //NON-NLS
        if (osVersion.isEmpty()) { //  try alternative property
            osVersion = SystemPropertyUtil.get("ro.jht.product.version"); //NON-NLS
        }
        if (osVersion.length() > 0) {
            // regex will remove the front non-decimal
            osVersion = osVersion.replaceAll( "^[^0-9]*", "" ); //NON-NLS
        }
        return osVersion;
    }

    public static String getBootVersion() {
        String version = SystemPropertyUtil.get("ro.boot.uboot_ver"); //NON-NLS
        if (version.isEmpty()) { //  try alternative property
            version = SystemPropertyUtil.get("ro.boot.version"); //NON-NLS
        }
        return version;
    }

    public static String getIoVersion() {
        return DataMatrix.getString(HALKeys.DATA_VERSION_IO_STRING);
    }

    public static String getIrVersion() {
        if (ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)) {
            return getAndroidSettingString("parker_ir_version");
        }
        return null;
    }

    public static String getTunerApMacAddress() {
        if (ProductConfig.hasInternalEthernetNetwork()) {
            return SystemPropertyUtil.get("persist.jht.tuner.ap_mac"); //NON-NLS
        }
        return null;
    }

    public static String getLcbVersion() {
        return DataMatrix.getString(HALKeys.DATA_VERSION_LCB_STRING);
    }
    public static int getLcbType() {
        return DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
    }

    public static String getAudioVersion() {
        if (ProductConfig.platform.equals(ProductConfig.Platform.Chimera)) {
            return DataMatrix.getString(HALKeys.DATA_VERSION_AUDIO_STRING);
        }
        return null;
    }

    public static String getTouchVersion() {
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera)) {
            String osVersion = getOsVersion();
            if(Version.parseVersion("1.1.6.0").isNewer(Version.parseVersion(osVersion)) || Version.parseVersion(osVersion).equals(Version.parseVersion("1.1.6.0"))) {
                return TouchPanel.getFWVersionChimera1();
            }
        } else if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
            String lcdVersion = getLcdVersion();
            if (lcdVersion.equals("11") || lcdVersion.equals("15") || lcdVersion.equals("13")) {
                // do nothing as this is the 2.0 ucb 1.0 rest of hardware hybrid combination that freezes the console for ~8 seconds
                // https://jhtnaee.atlassian.net/browse/CGC-9854?focusedCommentId=182572
                return "HYBRID";
            } else {
                return TouchPanel.getFWVersionChimera2();
            }
        }
        return null;
    }

    public static String getUcbVersion() {
        return ProductConfig.detectUCBVendor();
    }

    /**
     * Touch Panel/LCD Version is Chimera-specific, hide from LCG SW.
     * @return LCD version string or null
     */
    public static String getLcdVersion() {
        if (ProductConfig.platform.equals(ProductConfig.Platform.Chimera)) {
            return SystemPropertyUtil.get("ro.boot.panel_id"); //NON-NLS
        }
        return null;
    }

    public static String getBumpVersion() {
        // Used to read it live, but cached is faster for this purpose. Bump manager will log it if we need it sooner.
        // Display if version string is set, otherwise omit from final list.
        String value = null;
        String versionString = getAndroidSettingString("chimera_bumpsensor_version");
        if (versionString != null) {
            Version version = Version.parseVersion(versionString);
            if (version != null) {
                value = "Bootloader Version: " + version.getMajor() + "." + version.getMinor()
                        + ", App Version: " + version.getPatch() + "." + version.getBuild();
            }
        }
        return value;
    }

    public static String getCanVersion() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (!frameConfig.HAS_CAN) {
            return null;
        }

        boolean isSingle = (frameConfig.machineType == JHTCommonTypes.MachineType.Treadmill
                || frameConfig.machineType == JHTCommonTypes.MachineType.Ascent);

        if (isSingle) {
            String single = getAndroidSettingString("can_single_version");
            return "CAN: " + (single != null ? single : "N/A");
        } else {
            String right = getAndroidSettingString("can_right_version");
            String left = getAndroidSettingString("can_left_version");

            return "Right CAN: " + (right != null ? right : "N/A") + "\n"
                    + "Left CAN: " + (left != null ? left : "N/A");
        }
    }

    public static String getLowerLCDsVersion() {
        if (ProductConfig.isProjectOne()) {
            return getAndroidSettingString("p1_lcd_toggle_version");
        }
        return null;
    }

    public static String getFirmwareVersions(String prefix, String suffix) {
        StringBuilder firmwareVersions = new StringBuilder();
        String osVersion = getOsVersion();
        String bootVersion = getBootVersion();
        String audioVersion = getAudioVersion();
        String irVersion = getIrVersion();
        String touchVersion = getTouchVersion();
        String lcdVersion = getLcdVersion();
        String ucbVersion = getUcbVersion();
        String lowerLCDsVersion = getLowerLCDsVersion();
        String bumpVersion = getBumpVersion();
        String tunerApMac = getTunerApMacAddress();
        String canVersion = getCanVersion();

        firmwareVersions.append(prefix).append(String.format("OS: %s", osVersion)).append(suffix); //NON-NLS
        if (ProductConfig.hasIO()) firmwareVersions.append(prefix).append("IO: ").append(getIoVersion()).append(suffix); //NON-NLS
        if (ProductConfig.hasSalutron()) firmwareVersions.append(prefix).append("Heart Rate: ").append(DataMatrix.getString(HALKeys.DATA_VERSION_SALUTRON_STRING)).append(suffix); //NON-NLS
        if (audioVersion != null) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "AUDIO: %s", audioVersion)).append(suffix); //NON-NLS
        firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "LCB: %s %x", getLcbVersion(), getLcbType())).append(suffix); //NON-NLS
        if (ProductConfig.hasRFID()) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "RFID: %s", DataMatrix.getString(HALKeys.DATA_VERSION_NFC_STRING))).append(suffix); //NON-NLS
        if (ProductConfig.hasBTSink()) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "BT Sink: %s", DataMatrix.getString(HALKeys.DATA_GYM_KIT_FIRMWARE_VERSION_STRING))).append(suffix); //NON-NLS
        if (BuildConfig.HAS_TV) {
            firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "TV: %s", DataMatrix.getString(TVKeys.DATA_TUNER_VERSION_STRING))).append(suffix); //NON-NLS
            if (tunerApMac != null) firmwareVersions.append(prefix).append("Tuner AP: ").append(tunerApMac).append(suffix); //NON-NLS
        }
        firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "VA: %s", com.jht.virtualactive.BuildConfig.VERSION_NAME)).append(suffix); //NON-NLS
        if (ProductConfig.supportsPtp()) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "PTPN: %s", com.jht.engage360.BuildConfig.VERSION_NAME)).append(suffix); //NON-NLS
        if (!bootVersion.isEmpty()) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "U-Boot: %s", bootVersion)).append(suffix); //NON-NLS
        if (touchVersion != null) firmwareVersions.append(prefix).append("TouchPanel: ").append(touchVersion).append(suffix); //NON-NLS
        if (ProductConfig.hasSeparateIoForIr()) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "IR: %s", irVersion == null ? "N/A" : irVersion)).append(suffix); //NON-NLS
        if (lcdVersion != null) firmwareVersions.append(prefix).append(String.format(Locale.ENGLISH, "LCD: %s", lcdVersion)).append(suffix); //NON-NLS
        firmwareVersions.append(prefix).append(String.format("UCB: %s", ucbVersion)).append(suffix); //NON-NLS
        if (bumpVersion != null) firmwareVersions.append(prefix).append(String.format("Bump Sensor: %s" , bumpVersion)).append(suffix);
        if (lowerLCDsVersion != null) firmwareVersions.append(prefix).append(String.format("2.4\" LCDs: %s" , lowerLCDsVersion)).append(suffix);
        firmwareVersions.append(prefix).append("CHW: ").append(ProductConfig.hardware).append(suffix); //NON-NLS
        if (canVersion != null) firmwareVersions.append(getCanVersion());

        return firmwareVersions.toString();
    }

    public static CompletableFuture<String> getFirmwareVersionsAsync(String prefix, String suffix) {
        return CompletableFuture.supplyAsync(() -> getFirmwareVersions(prefix, suffix));
    }
}
