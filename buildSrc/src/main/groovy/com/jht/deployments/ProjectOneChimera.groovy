package com.jht.deployments

import com.jht.JHTConfiguration
import com.jht.JHTFlavor
import com.jht.JHTFlavors

/**
 * This is the deployment for ProjectOne running on Chimera.
 */
class ProjectOneChimera extends FlavoredDeployBase {
    // Package to Manifest map.
    private static final JHTPackages[][] packagesInManifest = [
            [
                    JHTPackages.UPDATER, JHTPackages.CONSOLE_APPS, JHTPackages.CHIMERA_OS_K2_2, JHTPackages.ASSETS, JHTPackages.CHIMERA_TPA,
                    JHTPackages.CHIMERA_IO2, JHTPackages.SALUTRON, JHTPackages.WLT_BLUETOOTH,
                    JHTPackages.CHIMERA_US5, JHTPackages.CHIMERA_US6, JHTPackages.CHIMERA_KM5,
                    JHTPackages.P1_BUMP_SENSOR, JHTPackages.P1_TOGGLE_TOUCH_FIRMWARE, JHTPackages.RFID2, JHTPackages.RFID3
            ],
            [
                    JHTPackages.UPDATER, JHTPackages.CONSOLE_APPS, JHTPackages.CHIMERA_OS_K2_2, JHTPackages.ASSETS, JHTPackages.CHIMERA_TPA,
                    JHTPackages.CHIMERA_IO2, JHTPackages.SALUTRON, JHTPackages.WLT_BLUETOOTH,
                    JHTPackages.CHIMERA_US5, JHTPackages.CHIMERA_US6, JHTPackages.CHIMERA_KM5,
                    JHTPackages.CHIMERA_U_BOOT, JHTPackages.CHIMERA_LCB1, JHTPackages.CHIMERA_LCB2,
                    JHTPackages.CHIMERA_LCB_Climbmill, JHTPackages.CHIMERA_LCBA,
                    JHTPackages.CHIMERA_TOUCH_PANEL, JHTPackages.P1_BUMP_SENSOR, JHTPackages.P1_TOGGLE_TOUCH_FIRMWARE, JHTPackages.RFID2, JHTPackages.RFID3
            ],
            [JHTPackages.CHIMERA_TOUCH_PANEL],
            [JHTPackages.P1_BUMP_SENSOR],
            [JHTPackages.P1_TOGGLE_TOUCH_FIRMWARE],
            [JHTPackages.CHIMERA_US5, JHTPackages.CHIMERA_US6, JHTPackages.CHIMERA_KM5]
    ]

    /**
     * Default constructor.
     *
     * @param configuration The configuration object with links to folders so we can find the source
     * files.
     */
    ProjectOneChimera(JHTConfiguration configuration) {
        this(JHTFlavor.flavors[JHTFlavors.PROJECTONE_MATRIX_CHIMERA.ordinal()], configuration)
    }

    /**
     * Used by customizations that run on the Chimera platform.
     *
     * @param flavor        The flavor for this deployment.
     * @param configuration The configuration object with links to folders so we can find the source
     * files.
     */
    protected ProjectOneChimera(JHTFlavor flavor, JHTConfiguration configuration) {
        super(flavor, configuration, ["", ".all", ".chimera_tp", ".p1_bump_sensor", ".p1_toggle_touch", ".tuner"], packagesInManifest)
    }

    /**
     * This allows deployments to add source files and folders to a package.
     *
     * @param pType The package.
     * @return The additional sources for the package.
     */
    @Override
    ArrayList<File> getSources(JHTPackages pType) {
        ArrayList<File> sources = new ArrayList<>()
        sources.add(new File(configuration.rootDir, "Deploy/" + pType.baseFolder()))
        switch (pType) {
            case JHTPackages.UPDATER:
                sources.add(new File(configuration.rootDir, "UpdateService/build/outputs/apk/${flavor.flavorDir()}/release/UpdateService-${flavor.flavorAPK()}-release.apk"))
                break
            case JHTPackages.CONSOLE_APPS:
                sources.add(new File(configuration.rootDir, "Chimera/build/outputs/apk/${flavor.flavorDir()}/release/Chimera-${flavor.flavorAPK()}-release.apk"))
                break
            case JHTPackages.CHIMERA_OS_K2_2:
                sources.add(new File(configuration.chimeraRootNetworkDir, "/OS/Nexcom/V${getVersion(JHTPackages.CHIMERA_OS_K2_2)}/ota_update"))
                break
            case JHTPackages.CHIMERA_TPA:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/CardsJHT/1.0.2"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/ChineseTraditionalKeyboard/1.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/Flipboard/4.3.25"))
                //sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/Instagram/164.0.0.46.123"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/Netflix/8.114.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/DisneyPlus/3.1.3"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/PressReader/6.6.240217"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/Spotify/8.9.30.433"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/TeamViewer/15.9.128"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/TeamViewerAddOn/14.v2"))
                //sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/WeChat/8.0.55.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/ifit_commercial/2.8.2.4779"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/MyWeather/0.3.10"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/MojiWeather/8.0506.02"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/Kindle/8.98.0.100"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/MyZone/0.5.0.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/BSJLiveTV/6.6"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/YMCA360/2.16.5"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/HangulKeyboard/1.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/MatrixTestApp/1.2"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/AmazonFreevee/1.10.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/PlutoTV/5.51.1.0"))
                sources.add(new File(configuration.chimeraRootNetworkDir, "Apps/iqiyi/6.4.5"))
                break
            case JHTPackages.CHIMERA_IO:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/IO/${getVersion(JHTPackages.CHIMERA_IO)}"))
                break
            case JHTPackages.CHIMERA_IO2:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/IO/${getVersion(JHTPackages.CHIMERA_IO2)}"))
                break
            case JHTPackages.CHIMERA_LCB1:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/LCB/LCB_1/${getVersion(JHTPackages.CHIMERA_LCB1)}"))
                break
            case JHTPackages.CHIMERA_LCB2:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/LCB/LCB_2/${getVersion(JHTPackages.CHIMERA_LCB2)}"))
                break
            case JHTPackages.CHIMERA_LCB_Climbmill:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/LCB/LCBClimbmill/${getVersion(JHTPackages.CHIMERA_LCB_Climbmill)}"))
                break
            case JHTPackages.CHIMERA_LCBA:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/LCB/LCBA/${getVersion(JHTPackages.CHIMERA_LCBA)}"))
                break
            case JHTPackages.WLT_BLUETOOTH:
                sources.add(new File(configuration.rootDir, "Deploy/chimera_bt"))
                break
            case JHTPackages.RFID2:
                sources.add(new File(configuration.rootDir, "Deploy/chimera_rfid2"))
                break
            case JHTPackages.RFID3:
                sources.add(new File(configuration.rootDir, "Deploy/chimera_rfid3"))
                break
            case JHTPackages.CHIMERA_US5:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/TV/US5/${getVersion(JHTPackages.CHIMERA_US5)}"))
                break
            case JHTPackages.CHIMERA_US6:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/TV/US6/${getVersion(JHTPackages.CHIMERA_US6)}"))
                break
            case JHTPackages.CHIMERA_KM5:
                sources.add(new File(configuration.chimeraRootNetworkDir, "Firmware/TV/KM5/${getVersion(JHTPackages.CHIMERA_KM5)}"))
                break
            case JHTPackages.P1_BUMP_SENSOR:
                sources.add(new File(configuration.projectOneNetworkDir, "Firmware/BumpSensor/${getVersion(JHTPackages.P1_BUMP_SENSOR)}"))
                break
            case JHTPackages.P1_TOGGLE_TOUCH_FIRMWARE:
                sources.add(new File(configuration.projectOneNetworkDir, "Firmware/LCDTouch/${getVersion(JHTPackages.P1_TOGGLE_TOUCH_FIRMWARE)}"))
        }

        return sources
    }

    /**
     * Get a version for the given package. Deployments need to override this function.
     * @param pType The package enum
     * @return The version. Can be NO_VERSION if the package is not in the deployment.
     */
    @Override
    String getVersion(JHTPackages p) {
        switch(p) {
            case JHTPackages.UPDATER:
            case JHTPackages.CONSOLE_APPS:
            case JHTPackages.CONSOLE_APPS_DEBUG:
                return ProjectOne_Chimera
            case JHTPackages.ASSETS:
                return P1_Assets
            case JHTPackages.CHIMERA_OS_K2_2:
                return CHIMERA1_OS_VERSION_4GB
            case JHTPackages.CHIMERA_TPA:
                return P1_TPA
            case JHTPackages.CHIMERA_IO:
                return IO_VERSION
            case JHTPackages.CHIMERA_IO2:
                return IO_2_VERSION
            case JHTPackages.CHIMERA_LCB1:
                return LCB1
            case JHTPackages.CHIMERA_LCB2:
                return LCB2
            case JHTPackages.CHIMERA_LCB_Climbmill:
                return LCBClimbmill
            case JHTPackages.CHIMERA_LCBA:
                return LCBA
            case JHTPackages.SALUTRON:
                return SALUTRON_VERSION
            case JHTPackages.WLT_BLUETOOTH:
                return BLUETOOTH_VERSION
            case JHTPackages.RFID2:
                return RFID2_VERSION
            case JHTPackages.RFID3:
                return RFID3_VERSION
            case JHTPackages.CHIMERA_U_BOOT:
                return U_BOOT_VERSION
            case JHTPackages.CHIMERA_US5:
                return US5_VERSION
            case JHTPackages.CHIMERA_US6:
                return US6_VERSION
            case JHTPackages.CHIMERA_KM5:
                return KM5_VERSION
            case JHTPackages.P1_BUMP_SENSOR:
                return P1_BUMP_SENSOR_VERSION
            case JHTPackages.P1_TOGGLE_TOUCH_FIRMWARE:
                return P1_TOGGLE_FIRMWARE_VERSION
            case JHTPackages.CHIMERA_TOUCH_PANEL:
                return TOUCH_PANEL_VERSION
        }

        return NO_VERSION
    }
}
