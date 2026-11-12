package com.jht.chimera.lib.common.config;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.RealScreen;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.hardware.Driver;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


/**
 * This class maintains several enums that help us configure the software based on the product
 * (flavor) we built and the hardware it is running on.  This is a layer on top of the build config
 * along with some auto detection logic for hardware and machine configuration.
 * <p>
 * Note the console type can change based on the frame type and services may check this on boot.
 * Thus we have to provide a function to get the console type which needs to see if the frame type
 * has changed.
 */
public class ProductConfig {
    // would it reduce the amount of functions if we created a EnumSet of feature flags?

    private static final String TAG = ProductConfig.class.getName();

    // For apps that require geolocation, do we allow use of weather location as override?
    public static final boolean allowWeatherLocationOverride = true;

    // These values are cached for easy access to the current system configuration.
    public final static Product product = Product.detect();
    public final static UI ui = UI.detect();
    public final static Platform platform = Platform.detect();
    public final static BuildFlavor flavor = BuildFlavor.detect();
    public final static ConsoleHardware hardware = ConsoleHardware.detect();
    public final static PortMappings portMappings = PortMappings.detect();
    public final static Market market = Market.detect();


    static private ConsoleType sConsoleConfig = null;
    static private JHTCommonTypes.MachineType sMachineType = null;

    /**
     * The product part of the build flavor.
     */
    public enum Product {
        Chimera("chimera_main"),
        Parker("parker_main"),
        Vision600E("vision600e"),
        T600XE("t600xe"),
        ProjectOne("projectone"),
        UNKNOWN("");

        Product(String name) {
            this.name = name;
        }

        private final String name;
        private static Product detect() {
            for(Product p : Product.values()) {
                if(p.name.equalsIgnoreCase(BuildConfig.FLAVOR_product))
                    return p;
            }
            return UNKNOWN;
        }
    }

    /**
     * The UI part of the build flavor.  For now we have the standard matrix UI and the Planet Fitness
     * UI.
     */
    public enum UI {
        Matrix("matrix"),
        UNKNOWN("");

        UI(String name) {
            this.name = name;
        }

        private final String name;
        private static UI detect() {
            for(UI ui : UI.values()) {
                if(ui.name.equalsIgnoreCase(BuildConfig.FLAVOR_ui))
                    return ui;
            }
            return UNKNOWN;
        }
    }

    /**
     * The platform part of the build flavor.  In general this ist the target hardware though
     * we have split the low cost GUI from Parker because that has some different hardware
     * configurations.
     */
    public enum Platform {
        Chimera("chimera"),
        Parker("parker"),
        LCGUI("lcgui"),
        UNKNOWN("");

        Platform(String name) {
            this.name = name;
        }

        private final String name;
        private static Platform detect() {
            for(Platform ui : Platform.values()) {
                if(ui.name.equalsIgnoreCase(BuildConfig.FLAVOR_platform))
                    return ui;
            }
            return UNKNOWN;
        }
    }

    /**
     * The flavors that we build for teh software which combines the product, ui, and platform.
     */
    public enum BuildFlavor {
        // THESE SHOULD MATCH THE GRADLE PRODUCT FLAVOR STRINGS !! (Case can be ignored, for now)
        Chimera(Product.Chimera, UI.Matrix, Platform.Chimera),
        Parker(Product.Parker, UI.Matrix, Platform.Parker),
        Vision600E(Product.Vision600E, UI.Matrix, Platform.Parker),
        T600XE(Product.T600XE, UI.Matrix, Platform.Parker),
        LCGUI(Product.Chimera, UI.Matrix, Platform.LCGUI),
        ProjectOne(Product.ProjectOne, UI.Matrix, Platform.Chimera),
        Unknown(Product.UNKNOWN, UI.UNKNOWN, Platform.UNKNOWN);

        private final Platform platform;
        private final UI ui;
        private final Product product;

        BuildFlavor(Product product, UI ui, Platform platform) {
            this.platform = platform;
            this.ui = ui;
            this.product = product;
        }

        private static BuildFlavor detect() {
            for (BuildFlavor b : BuildFlavor.values()) {
                if (b.product.equals(ProductConfig.product) && b.ui.equals(ProductConfig.ui)
                        && b.platform.equals(ProductConfig.platform)) {
                    return b;
                }
            }
            return Unknown;
        }
    }

    /**
     * This is used to detect the console hardware we are running on.
     */
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

        // TODO Why are we doing the same thing in ConsoleType?
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
            else if(Build.PRODUCT.contains("rk3588_jht")) {
                // Vortex K2 "rk3588_jht_gen2", Vortex Lite "rk3588_jht_l"
                if (Build.PRODUCT.contains("rk3588_jht_l")) return VortexLite;
                return Vortex;
            }
            else if(Build.PRODUCT.contains("chimera_3")) {
                return Chimera_3;
            }
            else if(SystemDetection.isEmulator()) {
                return Emulator;
            }
            return Unknown;
        }
    }

    /**
     * This is used to detect which vendor provided the UCB of the current platform
     */
    public static String detectUCBVendor() {
        switch (ConsoleHardware.detect()) {
            case Chimera:
                return "1.0";
            case Chimera_2:
                return "2.0";
            case Chimera_3:
                return "3.0";
            case Parker:
                return "P 1.0";
            case Parker_3:
                return "P 3.0";
            case UltraThin:
                return "UT 1.0";
            case Vortex:
                return "Vortex 1.0"; // TODO VORTEX What name do we want to return?
            case VortexLite:
                return "Vortex Lite 1.0";
            case Emulator:
                return "Emulator";
            default:
                return "Unknown";
        }
    }

    /**
     * This is used to detect the factory of origin we are running on.
     */
    public enum Factory {
        JIS,    // China
        JIV,    // Vietnam ("JIV" is the official, not "JVN")
        Unknown;

        public static Factory detect() {
            switch (ProductConfig.hardware) {
                case Parker:
                case Parker_3:
                    // TODO Verify Parker 3.0
                    File marker = new File("/mnt/backup/factory_jvn"); //NON-NLS

                    // Since some JIV consoles got out without a marker we cannot assume JIS if omitted
                    return marker.exists() ? JIV : Unknown;
                default:
                    // or can we assume JIS as default?
                    return Unknown;
            }
        }
    }

    /**
     * Port mappings which are based on the hardware and product. This helps us determine where to
     * find things like the IO and LCB.
     */
    public enum PortMappings {
        Chimera_1_0("/dev/ttyS4", "/dev/ttymxc2", "/dev/ttyS6", "/dev/ttyS1", "/dev/ttymxc3", "/dev/ttyS3", "/dev/ttyS0", "/dev/block/sdc1", "/dev/ttyS2", "/dev/ttyUSB1", null, null, null),
        UltraThin("/dev/ttyS4", null, null, null, null, null, null, null, null, null, null, null, null),
        Parker("/dev/ttyS3", "/dev/ttyS1", null, null, null, null, null, null, null, null, null, "/dev/i2c-3", null),
        Parker_3_0("/dev/ttyUSB0", "/dev/ttyS1", null, null, null, null, null, null, null, null, null, "/dev/i2c-6", null),
        LCGUI("/dev/ttyS3", "/dev/ttyS1", "/dev/ttyS4", "/dev/ttyXRUSB1", "/dev/ttyXRUSB2", "/dev/ttyXRUSB0", "/dev/ttyXRUSB4", "/dev/block/sdb1", null, null, "/dev/ttyXRUSB3", "/dev/i2c-3", null),
        Vision_600("/dev/ttyS3", "/dev/ttyS1", null, "/dev/ttyXRUSB2", "/dev/ttyXRUSB3", null, "/dev/ttyXRUSB0", "/dev/block/sdb1", null, null, null, null, null),
        T_600("/dev/ttyS3", "/dev/ttyS1", null, "/dev/ttyXRUSB2", "/dev/ttyXRUSB3", null, "/dev/ttyXRUSB0", "/dev/block/sdb1", null, null, null, null, null),
        Chimera_2_0("/dev/ttyS5", "/dev/ttymxc3", "/dev/ttyS7", "/dev/ttyS2", "/dev/ttymxc1", "/dev/ttyS6", "/dev/ttyS4", "/dev/block/sdb1", "/dev/ttyS3", "/dev/ttyS1", null, null, null),
        Emulator(null, null, null, null, null, null, null, null, null, null, null, null, null),
        ProjectOne_1_0("/dev/ttyS4", "/dev/ttymxc2", "/dev/ttyS6", "/dev/ttyS1", "/dev/ttymxc3", "/dev/ttyS3", "/dev/ttyS0", "/dev/block/sdc1", "/dev/ttyS2", "/dev/ttyUSB1", null, null, "/dev/ttyUSB0"),
        ProjectOne_2_0("/dev/ttyS5", "/dev/ttymxc3", "/dev/ttyS7", "/dev/ttyS2", "/dev/ttymxc1", "/dev/ttyS6", "/dev/ttyS4", "/dev/block/sdb1", "/dev/ttyS3", "/dev/ttyS1", null, null, "/dev/ttyUSB1"),
        Vortex("/dev/ttyWCH0", "/dev/ttyS3", null, "/dev/ttyWCH1", null /*"/dev/ttyWCH2"*/, null, null, null, "/dev/ttyS7", "/dev/ttyWCH3", "/dev/lirc0", null, null),
        Unknown(null, null, null, null, null, null, null, null, null, null, null, null, null);
        private final String lcbPath;
        private final String ioPath;
        private final String salutronPath;
        private final String csafePath;
        private final String swiftPath;
        private final String cabPath;
        private final String bluetoothSinkPath;
        private final String vividMetricsPath;
        private final String rfidPath;
        private final String us4Path;

        /** if handled by separate mcu from io mcu, otherwise leave null */
        private final String irPath;

        private final String hdmiI2CPath;
        private final String bumpSensorPath;

        PortMappings(String lcb, String io, String salutron, String csafe, String swift, String cab, String bluetoothSink, String vividMetrics, String rfidPath, String us4Path, String irPath, String hdmiI2CPath, String bumpSensorPath) {
            lcbPath = lcb;
            ioPath = io;
            salutronPath = salutron;
            csafePath = csafe;
            swiftPath = swift;
            cabPath = cab;
            bluetoothSinkPath = bluetoothSink;
            vividMetricsPath = vividMetrics;
            this.rfidPath = rfidPath;
            this.us4Path = us4Path;
            this.irPath = irPath;
            this.bumpSensorPath = bumpSensorPath;
            this.hdmiI2CPath = hdmiI2CPath;
        }
        public String lcbPath() { return lcbPath; }
        public String ioPath() { return ioPath; }
        public String salutronPath() {
            return salutronPath;
        }
        public String csafePath() { return csafePath; }
        public String swiftPath() { return swiftPath; }
        public String cabPath() { return cabPath; }
        public String bluetoothSinkPath() {
            if (platform.equals(Platform.LCGUI)) {
                return DataMatrix.getString(HALKeys.DATA_BT_SINK_PORT_STRING, bluetoothSinkPath);
            } else {
                return bluetoothSinkPath;
            }
        }

        public String bumpSensorPath() {
            return bumpSensorPath;
        }

        /** This is apple co-processor, not sure if it counts as part of gymkit or its own component */
        public String authCp3Path() {
            return "/dev/i2c-2";
        }

        public String vividMetricsPath() {
            return vividMetricsPath;
        }

        public String rfidPath() {
            if (platform.equals(Platform.LCGUI)) {
                return DataMatrix.getString(HALKeys.DATA_RFID_MANAGER_SERIAL_PORT_STRING, rfidPath);
            } else {
                return rfidPath;
            }
        }

        public String us4Path() {
            return us4Path;
        }

        @Nullable
        public String irSerialPath() {
            // this *should* work for the typical case of serial paths having "tty"
            if (irPath != null && irPath.contains("tty")) return irPath;
            return null;
        }
        @Nullable
        public String irLircPath() {
            // this *should* work for the typical case of linux IR controller paths having "lirc"
            if (irPath != null && irPath.contains("lirc")) return irPath;
            return null;
        }

        public String hdmiI2CPath() {
            return hdmiI2CPath;
        }

        private static PortMappings detect() {
            switch (ProductConfig.hardware) {
                case Chimera:
                    if (product.equals(Product.ProjectOne)) {
                        return ProjectOne_1_0;
                    }
                    return Chimera_1_0;
                case Chimera_2:
                case Chimera_3:
                    if (product.equals(Product.ProjectOne)) {
                        return ProjectOne_2_0;
                    }
                    return Chimera_2_0;
                case Vortex:
                case VortexLite:
                    return Vortex;
                case UltraThin:
                    return UltraThin;
                case Parker:
                    switch (ProductConfig.product) {
                        case Parker:
                            return Parker;
                        case Vision600E:
                            return Vision_600;
                        case T600XE:
                            return T_600;
                        case Chimera:
                            return LCGUI;
                        case UNKNOWN:
                            return Unknown;
                    }
                case Parker_3:
                    return Parker_3_0;
                case Emulator:
                    return Emulator;
                case Unknown:
                default:
                    return Unknown;
            }
        }
    }

    /**
     * The target market for the build.
     */
    public enum Market {
        /** Parker */
        Retail,

        /** Vision */
        LightCommercial,

        /** Chimera, Project One, and Low Cost GUI */
        Commercial,
        Unknown;

        private static Market detect() {
            if (BuildConfig.MARKET_RETAIL) {
                return Retail;
            } else if (BuildConfig.MARKET_LIGHT_COMMERCIAL) {
                return LightCommercial;
            } else if (BuildConfig.MARKET_COMMERCIAL) {
                return Commercial;
            }
            return Unknown;
        }
    }

    public static boolean isRetail() {
        return market.equals(Market.Retail);
    }

    public static boolean isLightCommercial() {
        return market.equals(Market.LightCommercial);
    }

    public static boolean isCommercial() {
        return market.equals(Market.Commercial);
    }

    /** Return true if we are not on an emulator and we are running on known hardware. */
    public static boolean isRealDevice() {
        return !hardware.equals(ConsoleHardware.Emulator)
                && !hardware.equals(ConsoleHardware.Unknown);
    }

    public static boolean isParker() {
        return product.equals(Product.Parker);
    }

    public static boolean isProjectOne() {
        return product.equals(Product.ProjectOne);
    }

    public static boolean hasIO() {
        return portMappings.ioPath != null;
    }

    public static boolean hasSeparateIoForIr() {
        return portMappings.irSerialPath() != null;
    }

    public static boolean hasBluetoothManager() {
        return isRealDevice();
    }

    public static boolean hasSalutron() {
        return portMappings.salutronPath != null;
    }

    public static boolean hasGymkit() {
        return BuildConfig.HAS_GYMKIT && portMappings.bluetoothSinkPath() != null;
    }



    public static boolean useCANHeartRate(){
        return FrameConfigManager.getInstance().getFrameConfig().USE_CAN_HEART_RATE;
    }

    public static boolean hasMFI() {
        switch (ProductConfig.hardware) {
            case Parker:
            case UltraThin:
                return false;
            default:
                return true;
        }
    }

    public static boolean hasRFID() {
        return portMappings.rfidPath() != null;
    }

    public static boolean hasBTSink() {
        return portMappings.bluetoothSinkPath() != null;
    }

    public static boolean hasCSAFE() {
        return portMappings.csafePath != null;
    }

    public static boolean hasBumpSensor() {
        // Bump sensor could be on serial port or through IO passthrough
        return portMappings.bumpSensorPath() != null
                || (hardware.isVortexBase() && isProjectOne());
    }

    /**
     * Parker has hdmi-in except for the 10 inch due to cost
     *
     * @return
     */
    public static boolean hasHDMIInput() {
        return product.equals(Product.Parker) && ProductConfig.getConsoleType().inches > 10;
    }

    public static boolean hasAuxAudioInput() {
        if (!product.equals(Product.Parker)) {

            // LCGUI is parker hw, but does not expose audio input
            return false;
        }
        // this should be hardware check as audio input jack is part of the fully assembled console
        return hardware.equals(ConsoleHardware.Parker) || hardware.equals(ConsoleHardware.Parker_3);
    }

    public static boolean hasFan() {
        // maybe this should be part of a console type configuration?
        ConsoleType consoleType = ProductConfig.getConsoleType();
        return product.equals(Product.Chimera) &&
                !consoleType.equals(ConsoleType.vortex_lite_16_1080p) &&
                !consoleType.equals(ConsoleType.vortex_lite_23_1080p) &&
                !consoleType.equals(ConsoleType.projectOne1) &&
                !consoleType.equals(ConsoleType.projectOne2) &&
                !consoleType.equals(ConsoleType.athena) &&
                !consoleType.equals(ConsoleType.athena2) &&
                !consoleType.equals(ConsoleType.tcv_24) &&
                !consoleType.equals(ConsoleType.lcgui16) &&
                !consoleType.equals(ConsoleType.lcgui22);
    }

    public static boolean hasRfidLed() {
        // only Chimera Vortex (Touch Pro) and Chimera Vortex Lite (Touch Essential) has RFID LED
        return product.equals(Product.Chimera) && (hardware.equals(ConsoleHardware.Vortex) || hardware.equals(ConsoleHardware.VortexLite));
    }

    public static boolean needToPurgeUserData() {
        return market.equals(Market.Commercial) || market.equals(Market.LightCommercial);
    }

    public static boolean showGymkitUI() {
        return product.equals(Product.Chimera) || product.equals(Product.ProjectOne);
    }

    public static boolean hasUserProfiles() {
        return market.equals(Market.Retail);
    }

    /** At present only full commercial supports PTP */
    public static boolean supportsPtp() {
        return BuildConfig.MARKET_COMMERCIAL;
    }

    // TODO - fill in with actual conditions, the DM key is ONLY FOR INITIAL TESTING
    public static boolean hasPremiumUi() { return !SettingKeys.UiSystem.isLegacy(DataMatrix.getInt(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT)); }

    public static boolean hasPowerLimits() {
        return hardware.equals(ConsoleHardware.Parker) || hardware.equals(ConsoleHardware.Parker_3);
    }

    /** Return true if external power needs to be switched on but mainboard does not use it
     * <a href="https://jhtnaee.atlassian.net/wiki/spaces/ICPS/pages/3894411482/0x0003+Power">IO power command</a>
     * <a href="https://bitbucket.org/johnsonhealthtech/chimera2/pull-requests/6694">More discussion</a>
     * */
    public static boolean needsExtPowerSwitch() {
        if (requiresExtPower()) {
            // The chimera hardware still uses standard power for mainboard, but display is fed by external power.
            // Other hardware such as vortex will use the full external power.
            return hardware.equals(ConsoleHardware.Chimera);
        }
        return false;
    }
    /** Returns true if external power is required and IO should enable it. */
    public static boolean requiresExtPower() {
        // currently P1/onyx requires external power
        return product.equals(Product.ProjectOne);
    }

    public static boolean hasPausePlayButton() {
        return product.equals(Product.Parker);
    }

    public static boolean forcesCustomWorkoutsToHaveCooldowns() {
        return !market.equals(Market.Retail);
    }

    /**
     * String to represent console branding. Can be passed to launched apps, so keep names standard
     * as possible.
     */
    public static String getBrandingName() {
        //TODO: We could do this per flavor or per console type?
        switch (ProductConfig.product) {
            case Vision600E:
            case T600XE:
                return "vision";
            case ProjectOne:
                return "onyx";
            default:
                return "matrix";
        }
    }

    /**
     * Calling getType with null can cause redundant computation, instead provide means to use last
     * known machine type
     * @return The console type detected.
     */
    public static ConsoleType getConsoleType() {
        return getConsoleType(FrameConfigManager.getInstance().getFrameConfig().machineType);
    }

    /**
     * Returns the console type given the current machine type. Note this is public because machine
     * settings needs this as it will use this to get the console serial number prefix before we
     * actually set the machine type.
     *
     * @param machineType The current machine type (may be selected).
     * @return The console type based on the machine type.
     */
    public static ConsoleType getConsoleType(JHTCommonTypes.MachineType machineType) {
        if (sConsoleConfig == null || sMachineType == null || !sMachineType.equals(machineType)) {
            sMachineType = machineType;
            sConsoleConfig = detectConsoleType(machineType);
            Log.v(TAG, "Console Type Detected " + sConsoleConfig);
        }
        return sConsoleConfig;
    }

    /**
     * For most consoles we can auto detect based on the screen width and product flavor. This is
     * the default fallback.
     *
     * @param machineType The current machine type.
     * @return The console type based on screen width.
     */
    private static @NonNull ConsoleType getConsoleTypeFromWidth(JHTCommonTypes.MachineType machineType) {
        int widthPx = Resources.getSystem().getDisplayMetrics().widthPixels;
        ConsoleType[] sConsoleList = ConsoleType.values();
        for (ConsoleType consoleType : sConsoleList) {
            if (consoleType.productFlavorEnum != ProductConfig.flavor) {
                continue;
            }
            if (widthPx == consoleType.widthPx) {
                if (consoleType.equals(ConsoleType.chimera_22)) {
                    // See if this is Athena.
                    if(JHTCommonTypes.MachineType.CardioSpinBike.equals(machineType)) {
                        return ConsoleType.athena;
                    }
                }
                return consoleType;
            }
        }

        return ConsoleType.chimera2_16;
    }

    /**
     * Returns the current console type based on the detected hardware, screen resolution, and
     * machine type.
     * @param machineType   The current machine type.
     * @return              The console type.
     */
    private static @NonNull ConsoleType detectConsoleType(JHTCommonTypes.MachineType machineType) {
        // should we be passing the general classification type instead of specific machine type? then we could just check for MachineClassificationType.SpinBike
        boolean isSpinBikeConsole = machineType.equals(JHTCommonTypes.MachineType.CardioSpinBike) || machineType.equals(JHTCommonTypes.MachineType.ManualResistanceSpinBike);
        switch(ProductConfig.hardware) {
            case Chimera:
                if (ProductConfig.isProjectOne()) {//ProjectOne flavor
                    String chimera_panelId = SystemPropertyUtil.get("ro.boot.panel_id");
                    Log.v(TAG, "Getting panel id " + chimera_panelId);
                    switch (chimera_panelId) {
                        case "4": //ProjectOne 32"
                            return ConsoleType.projectOne1;
                        case "2": //ProjectOne 22"
                        case "0": //ProjectOne 22" panel reverse same Chimera 22"
                            return ConsoleType.projectOne2;
                        case "1":
                        default:
                            return getConsoleTypeFromWidth(machineType);
                    }
                }
                return getConsoleTypeFromWidth(machineType);

            case Chimera_2:
            case Chimera_3:
                String panelId = SystemPropertyUtil.get("ro.boot.panel_id");
                Log.v(TAG, "Getting panel id " + panelId);
                switch (panelId) {
                    case "14":
                        if (ProductConfig.isProjectOne()) return ConsoleType.projectOne1; //32 inch P1
                    case "9":
                    case "10": //the second source for chimera2.0
                    case "13":
                    case "15":
                        if (ProductConfig.isProjectOne()) return ConsoleType.projectOne2; //22 inch P1
                        return isSpinBikeConsole ? ConsoleType.athena2 : ConsoleType.chimera2_22;
                    case "5":
                    case "7":
                    case "8":
                    case "11":
                    default:
                        return ConsoleType.chimera2_16;
                }
            case Vortex:
            case VortexLite:
                // TODO VORTEX should be using ro.boot.panel_id to return panel type
                panelId = SystemPropertyUtil.get("ro.boot.panel_id");
                Log.v(TAG, "Vortex getting panel id " + panelId);
                switch (panelId) {
                    case "3":
                        return ConsoleType.vortex_32_4k;
                    case "2":
                        return ConsoleType.vortex_23_4k;
                    case "1":
                        return isSpinBikeConsole ? ConsoleType.tcv_24 : ProductConfig.hardware.equals(ConsoleHardware.VortexLite) ? ConsoleType.vortex_lite_23_1080p : ConsoleType.vortex_23_1080p;
                    case "0":
                    default:
                        return ProductConfig.hardware.equals(ConsoleHardware.VortexLite) ? ConsoleType.vortex_lite_16_1080p : ConsoleType.vortex_16_1080p;
                }
            case UltraThin:
                return ConsoleType.ultraThin;
            case Parker:
            case Parker_3:
                Integer screen = Driver.GpioWrapper.getScreen();
                if (screen != null) {
                    ConsoleType[] sConsoleList = ConsoleType.values();
                    for (ConsoleType consoleType : sConsoleList) {
                        if (consoleType.productFlavorEnum != ProductConfig.flavor) continue;
                        if (consoleType.inches == screen) {
                            // got to be a better way to do this?
                            switch (consoleType) {
                                case xer2:
                                    if (isIfitBlocked()) return ConsoleType.xer2_no_ifit;
                                    break;
                                case xir2:
                                    if (isIfitBlocked()) return ConsoleType.xir2_no_ifit;
                                    break;
                                case xur:
                                    if (isIfitBlocked()) return ConsoleType.xur_no_ifit;
                                    break;
                            }
                            return consoleType;
                        }
                    }
                }
                return getConsoleTypeFromWidth(machineType);
            case Emulator:
                try {
                    @SuppressLint("SdCardPath") File consoleTypeOverride = new File("/sdcard/consoleTypeOverride");
                    String consoleTypeName = FileUtils.readFileToString(consoleTypeOverride);
                    ConsoleType type = ConsoleType.valueOf(consoleTypeName.trim());
                    if (type.productFlavorEnum != ProductConfig.flavor) throw new Exception("Unsupported console type");
                    return type;
                } catch (Exception ignored) { return getConsoleTypeFromWidth(machineType); }
            default:
                return getConsoleTypeFromWidth(machineType);
        }
    }

    /**
     * For Parker we need to force the density based on the real metrics and reported display metrics.
     */
    public static void checkForceResolution() {
        ConsoleType consoleType = getConsoleType(); // we only need to know expected resolution
        if (consoleType == ConsoleType.unknown) return; // don't risk this on an unknown console

        DisplayMetrics effectiveMetrics = Resources.getSystem().getDisplayMetrics();
        Log.i(TAG, "EFFECTIVE " + effectiveMetrics.widthPixels + "x" + effectiveMetrics.heightPixels + " dpi=" + effectiveMetrics.densityDpi);
        Log.i(TAG, "REAL " + RealScreen.widthPixels + "x" + RealScreen.heightPixels + " dpi=" + RealScreen.densityDpi);
        Log.i(TAG, "ReadWidth: "+RealScreen.widthPixels+" consoleWidth: "+consoleType.widthPx +" divide: "+(float)RealScreen.widthPixels/consoleType.widthPx);
        float density = ((float) RealScreen.widthPixels / consoleType.widthPx);
        int densityDpi = (int)Math.floor(density * 160);


        // TODO VORTEX we need to figure since logic above doesn't work correctly
        // hardcoding for now
        if (ProductConfig.hardware.isVortexBase()) {
            switch (consoleType) {
                case vortex_16_1080p:
                    densityDpi = 224;
                    break;
                case vortex_23_1080p:
                case tcv_24:
                    densityDpi = 160;
                    break;
                case vortex_23_4k:
                case vortex_32_4k:
                    densityDpi = 320;
                    break;
            }
        }

        Log.i(TAG, "Needed density " + densityDpi);

        if (densityDpi != effectiveMetrics.densityDpi) {
            // Try to do this via API or commandline?
            com.jht.androidcommonalgorithms.util.ExecuteAsRoot.executeCommand("wm density " + densityDpi);
        }
    }

    public static HdmiResolutionConfig getHDMIResolutionConfig() {
        switch (product) {
            case ProjectOne:
                if (hardware == ConsoleHardware.Vortex) {
                    return new HdmiResolutionConfig(
                            SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_2160p,
                            SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_1080p,
                            SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_720p
                    );
                }
                return new HdmiResolutionConfig(
                        SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_1080p,
                        SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_720p
                );
            default:
                if (hardware == ConsoleHardware.Vortex) {
                    return new HdmiResolutionConfig(
                            SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_1080p,
                            SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_720p
                    );
                }
                return new HdmiResolutionConfig(
                        SettingKeys.TVHDMIOutputResolution.OUTPUT_RESOLUTION_720p
                );
        }
    }

    /** IP address of the Vortex internal router on the isolated ethernet network. */
    public static final String VORTEX_INTERNAL_IP = "192.168.3.1";

    /** true if there is an isolated ethernet network in the console for internal networked devices */
    public static boolean hasInternalEthernetNetwork() {
        return hardware.isVortexBase();
    }

    /** true if console supports casting */
    public static boolean supportsCasting(SettingKeys.TVSource source) {
        return hardware.isVortexBase() && source != null && source.supportsCasting();
    }

    /** true if app needs to perform manual HDMI switching (maybe we replace this with something that returns the needed HDMI input or -1 if NA?) */
    public static boolean supportsHdmiSwitching() {
        switch (hardware) {
            case Parker:
            case Parker_3:
            case Vortex:
            case VortexLite: // technically has only one HDMI
                return true;
        }
        return false;
    }

    /** Get the HDMI input for the given source. Pass null to indicate user input. */
    public static int getHdmiInput(@Nullable SettingKeys.TVSource source) {
        // input numbering should really be formalized somewhere
        if (!supportsHdmiSwitching()) return -1;
        // Vortex mainly just uses one HDMI input, but might have 2
        if (hardware.isVortexBase()) return 1;
        // parker hardware reserves #1 for external HDMI input, and use #2 for tuners?
        return source == null ? 1 : 2;
    }

    @NonNull
    public static SettingKeys.TVSource getDefaultTuner() {
        if (hardware == ConsoleHardware.VortexLite) return SettingKeys.TVSource.STB;
        if (hardware == ConsoleHardware.Vortex) return SettingKeys.TVSource.US9;
        return SettingKeys.TVSource.US6;
    }

    public static List<SettingKeys.TVSource> getAvailableTuners() {
        // wonder if this list should become part of a console type configuration class?
        List<SettingKeys.TVSource> sources = new ArrayList<>();
        if (!BuildConfig.HAS_TV) {
            // no TV, no tuners
        } else if (hardware == ConsoleHardware.VortexLite) {
            sources.add(SettingKeys.TVSource.STB);
            sources.add(SettingKeys.TVSource.IPTV);
        } else if (hardware == ConsoleHardware.Vortex) {
            sources.add(SettingKeys.TVSource.US9);
            sources.add(SettingKeys.TVSource.KM7);
            sources.add(SettingKeys.TVSource.BR3);
            sources.add(SettingKeys.TVSource.JP1);
            sources.add(SettingKeys.TVSource.CAB);
            sources.add(SettingKeys.TVSource.STB);
        } else if (BuildConfig.MARKET_LIGHT_COMMERCIAL){ // vision values may need confirmation?
            sources.add(SettingKeys.TVSource.BR2);
            sources.add(SettingKeys.TVSource.JP0);
            sources.add(SettingKeys.TVSource.KM4);
            sources.add(SettingKeys.TVSource.KM5);
            sources.add(SettingKeys.TVSource.KM6);
            sources.add(SettingKeys.TVSource.US5);
            sources.add(SettingKeys.TVSource.US6);
        } else {
            sources.add(SettingKeys.TVSource.BR2);
            sources.add(SettingKeys.TVSource.JP0);
            sources.add(SettingKeys.TVSource.KM4);
            sources.add(SettingKeys.TVSource.KM5);
            sources.add(SettingKeys.TVSource.KM6);
            sources.add(SettingKeys.TVSource.STB);
            sources.add(SettingKeys.TVSource.CAB);
            sources.add(SettingKeys.TVSource.IPTV);
            if (ProductConfig.platform != ProductConfig.Platform.LCGUI) sources.add(SettingKeys.TVSource.US4);
            sources.add(SettingKeys.TVSource.US5);
            sources.add(SettingKeys.TVSource.US6);
            sources.add(SettingKeys.TVSource.US8);
        }
        return sources;
    }

    public static int getTunerDefaultVolume(SettingKeys.TVSource source) {
        switch(hardware) {
            case Chimera:
            case Chimera_2:
            case Chimera_3:
            case Vortex:
            case VortexLite:
                return isProjectOne() ? getProjectOneTunerDefaultVolume(source) : getChimeraTunerDefaultVolume(source);
            case Parker:
                return getParkerTunerDefaultVolume(source);
        }
        return 0;
    }

    private static int getProjectOneTunerDefaultVolume(SettingKeys.TVSource source) {
        switch(source) {
            case US4:
                return 51;
            case US6:
                return 75;
            case US5:
            case BR2:
                return 87;
            case US8:
            case US9:
            case KM6:
            case KM7:
            case BR3:
            case JP1:
                return 80;
            case KM4:
                return 78;
            case KM5:
                return 29;
            case JP0:
                return 20;
            case IPTV:
            case STB:
            case CAB:
                return 0;
        }
        return 0;
    }

    private static int getChimeraTunerDefaultVolume(SettingKeys.TVSource source) {
        switch(source) {
            case US4:
                return 51;
            case US5:
            case US6:
            case BR2:
                return 87;
            case US7:
                return 32;
            case US8:
            case US9:
            case KM6:
            case KM7:
            case BR3:
            case JP1:
                return 80;
            case KM4:
                return 78;
            case KM5:
                return 29;
            case JP0:
                return 20;
            case IPTV:
            case STB:
            case CAB:
                return 0;
        }
        return 0;
    }

    private static int getParkerTunerDefaultVolume(SettingKeys.TVSource source) {
        switch(source) {
            case US4:
                return 51;
            case US5:
                return 90;
            case US6:
                return 88;
            case BR2:
                return 87;
            case US7:
                return 33;
            case KM4:
                return 80;
            case KM5:
                return 31;
            case JP0:
                return 20;
            case IPTV:
            case STB:
            case CAB:
                return 0;
        }
        return 0;
    }

    // "marker files" provide a way to leave an indicator/setting on a console that is safe from being altered by updates and factory resets
    @SuppressLint("SdCardPath")
    public static String getMarkerFilePath() { // path must end in path delimiter for easy use in commandline
        switch (hardware) {
            case Emulator:
                return "/sdcard/backup/"; // emulator should retain changes here
            default:
                return "/mnt/backup/"; // all retail consoles have this location setup (parker1, UT, parker2), not sure about the other consoles
        }
    }
    public static String getMarkerFilePath(String filename) {
        return getMarkerFilePath() + filename; // could add an actual path combiner, but for now keep it simple
    }

    /**
     * The LCGUIBuildConfig has been updated as of CGC-15581 (6/25/2024), and so the check for Chimera 2 hardware has
     * been removed. This key overrides the hardware config for IFIT. This does <strong>not</strong> enable the app.
     * That still needs to come from CORE (console config).
     *
     * @return      true if the product supports iFIT.
     */
    public static boolean hasIFIT() {
        return BuildConfig.HAS_IFIT;
    }

    /**
     * Per contract (see CGC-15581), iFIT must be enabled for Touch Base via CORE. All other platforms have iFIT by
     * default.
     *
     * @return      true if specified platform does NOT require being enabled via CORE
     */
    public static boolean ifitByDefault() {
        return !platform.equals(Platform.LCGUI);
    }

    // keep to retail for now? (costco retail cannot have ifit due to non-compete concerns)
    public static boolean supportsIfitBlocking() { return isRetail(); }
    public static boolean isIfitBlocked() { // return true if marker is found indicating ifit is BLOCKED
        if (supportsIfitBlocking()) {
            // java API seems to be able to do this ok, otherwise might have had to do a native file existence function
            // is java.nio.file.Files.exists() faster since nothing is created?
            File marker = new File(ProductConfig.getMarkerFilePathIfitBlocked());
            return marker.exists();
        } else {
            return false; // if platform does not support blocking, always return false
        }
    }
    public static String getMarkerFilePathIfitBlocked() { return getMarkerFilePath("block_ifit"); } // do method, or string constant to use with getMarkerFilePath?

    /**
     * Checks if iFit functionality is blocked outside North America.
     * @return {@code true} if marker file is located indicating iFit is blocked outside North America,
     * {@code false} if iFit blocking is not supported or the marker file does not exist.
     */
    public static boolean isIfitBlockedOutsideNorthAmerica() {
        File marker = new File(ProductConfig.getMarkerFilePathIfitBlockedOutsideNA());
        return supportsIfitBlocking() && marker.exists();
    }

    /**
     * Retrieves the file path for the marker file that indicates iFit is blocked outside North America.
     * @return The file path for the block iFit outside North America marker file.
     */
    public static String getMarkerFilePathIfitBlockedOutsideNA() { return getMarkerFilePath("block_ifit_outside_north_america"); }

    /**
     * Appstore is supported on:
     * - commercial GUIs
     * - retail GUISs
     *
     * @return      true if the product supports AppStore.
     */
    public static boolean hasAppStore() {
        return isCommercial() || isRetail();
    }

    /**
     * Returns true if the console supports SleepMode. Currently for commercial, only Vortex-based console
     * supports SleepMode. Retail supports SleepMode.
     *
     * @return      true if the console supports SleepMode.
     */
    public static boolean hasSleepMode() {
        return hardware.isVortexBase() || isRetail();
    }
}

