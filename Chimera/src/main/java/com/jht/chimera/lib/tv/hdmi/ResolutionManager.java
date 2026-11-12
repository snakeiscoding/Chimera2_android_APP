package com.jht.chimera.lib.tv.hdmi;

import static com.jht.chimera.service.logger.LoggerService.TAG;

import android.util.Log;

import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.hardware.Driver;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

public class ResolutionManager {
    private final List<HDMIResolution> supportedResolutions;
    ResolutionManager() {

        // Note this may need to be platform dependent.
        supportedResolutions = new ArrayList<>();
        if(ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera)) {
            supportedResolutions.add(new HDMIResolution(720, 480, 48000, true));
            supportedResolutions.add(new HDMIResolution(1280, 720, 48000, true));
            supportedResolutions.add(new HDMIResolution(1920, 1080, 48000, true));
            supportedResolutions.add(new HDMIResolution(1500, 848, 48000, true));
        }
    }

    ResolutionManager(List<HDMIResolution> supportedResolutions) {
        this.supportedResolutions = supportedResolutions;
    }
    boolean isResolutionValid() {
        HDMIResolution resolution = readResolution();
        Log.d("HdmiDelayCamera2", "HdmiDelayCamera2, isResolutionValid checking resolution " + resolution);

        if(resolution != null) {
            DataMatrix.putInt(SettingKeys.Experimental.DATA_HDMI_WIDTH_CHIMERA2_INT, resolution.getHdmiInputInfo().getWidth());
            DataMatrix.putInt(SettingKeys.Experimental.DATA_HDMI_HEIGHT_CHIMERA2_INT, resolution.getHdmiInputInfo().getHeight());
        }

        if(!supportedResolutions.isEmpty()) {
            if(resolution == null) {
                return true;
            }
            for (HDMIResolution r : supportedResolutions) {
                if (!r.isDifferent(resolution)) {
                    return true;
                }
            }
            return false;
        }
        return resolution != null;

    }
    public static HDMIResolution readResolution() {
        if(ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera)) {
            ExecuteAsRoot checkResolution = new ExecuteAsRoot();
            checkResolution.addCommand("cat /proc/tc35874x/read_resolution");
            //checkResolution.addCommand("cat /proc/tc35874x/entry");
            StringBuilder resolutionOuput = new StringBuilder();
            checkResolution.execute(false, true, resolutionOuput);
            return new HDMIResolution(resolutionOuput.toString());
        }
        else if(ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3)) {
            ExecuteAsRoot checkResolution = new ExecuteAsRoot();
            checkResolution.addCommand("cat /sys/devices/platform/soc@0/30800000.bus/30a30000.i2c/i2c-1/1-002b/read_resolution");
            StringBuilder resolutionOuput = new StringBuilder();
            checkResolution.execute(false, true, resolutionOuput);
            return new HDMIResolution(resolutionOuput.toString());
        }
        else { // Parker, Parker2, UltraThin
            try {
                String formatString = Driver.getFormat();
                Log.d("getFormat", "readResolution: " + formatString);
                if (formatString != null && !formatString.isEmpty()) {
                    HDMIResolution parsedResolution = HDMIResolution.fromFormat(formatString);
                    if (!parsedResolution.equals(HDMIResolution.UNKNOWN)) {
                        return parsedResolution;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "ResolutionManager: Failed to call Driver.getFormat() - " + e.getMessage());
            }

            // Fallback to individual driver calls
            try {
                String resolutionString = Driver.getResolution();
                if (resolutionString == null || !resolutionString.contains("x")) {
                    return HDMIResolution.UNKNOWN;
                }

                String[] parts = resolutionString.split("x");
                int width = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
                int height = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

                if (width == 0 || height == 0) {
                    return HDMIResolution.UNKNOWN;
                }

                int frameRate = Driver.getFrameRate();
                int sampleRate = Driver.getSampleRate();
                int audioChannels = Driver.getAudioChannel();
                boolean interlaced = Driver.isInterlaced() == 1;
                boolean connected = width > 0 && height > 0;

                return new HDMIResolution(new HdmiInputInfo(width, height, sampleRate, frameRate, connected, audioChannels, interlaced));
            } catch (Exception e) {
                Log.e(TAG, "ResolutionManager: Exception with individual getters - " + e.getMessage());
            }

            return HDMIResolution.UNKNOWN;
        }
    }
}
