package com.jht.updateservice;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;

import com.jht.androidcommonalgorithms.util.SystemDetection;

public class ConsoleType {
    public static String getConsoleType(Context context) {
        String console_type = Settings.Secure.getString(context.getContentResolver(), "console_type"); // NON-NLS
        if (console_type != null) return console_type;

        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        String consoleType = "unknown"; // NON-NLS
        String platform = BuildConfig.FLAVOR_platform.toLowerCase();

        if (platform.equals("chimera")) { // NON-NLS
            return "chimera"; // NON-NLS
        } else if (platform.equals("parker")) { // NON-NLS
            String product = BuildConfig.FLAVOR_product.toLowerCase();
            if(product.equals("parker_main")) { // NON-NLS
                if (width >= 1920) return "xur"; // NON-NLS
                if (width >= 1366) return "xir2"; // the new 15.6 IPS panel is 1920x1080 // NON-NLS
                if (width >= 1024) return "xer2"; // NON-NLS
            } else if (product.equals("vision600e")) { // NON-NLS
                if (width >= 1920) return "vision22"; // NON-NLS
                if (width >= 1366) return "vision15"; // NON-NLS
            }
        }

        return consoleType;
    }

    public final static ConsoleHardware hardware = ConsoleHardware.detect();
    public enum ConsoleHardware {
        Chimera,
        Chimera_2,
        Chimera_3,
        Parker,
        Parker_3,
        UltraThin,
        Vortex,
        VortexLite,
        Emulator,
        Unknown;
        /** There are a couple Vortex variants. Lite is mostly just stripped down Vortex. */
        public boolean isVortexBase() { return this == Vortex || this == VortexLite; } // probably should break this into hw feature designations?

        private static ConsoleHardware detect() {
            if (Build.PRODUCT.contains("jht_8mq")) {
                return Chimera;
            } else if (Build.PRODUCT.contains("ngp2") && Build.PRODUCT.contains("rk3399")) {
                return Parker_3;
            } else if (Build.PRODUCT.contains("rk3399")) {
                return Parker;
            } else if (Build.PRODUCT.contains("3399_2B") || Build.PRODUCT.contains("UltraThin")) {
                // OS 1.0.1.0 used 3399_2B, changed to UltraThin in OS 1.0.1.1
                return UltraThin;
            } else if (Build.PRODUCT.contains("leo_8mq")) {
                return Chimera_2;
            }
            else if(Build.PRODUCT.contains("chimera_3")) {
                return Chimera_3;
            }
            else if(Build.PRODUCT.contains("rk3588_jht")) {
                // Vortex K2 "rk3588_jht_gen2", Vortex Lite "rk3588_jht_l"
                if (Build.PRODUCT.contains("rk3588_jht_l")) return VortexLite;
                return Vortex;
            }
            else if(SystemDetection.isEmulator()) {
                return Emulator;
            }
            return Unknown;
        }
    }
}
