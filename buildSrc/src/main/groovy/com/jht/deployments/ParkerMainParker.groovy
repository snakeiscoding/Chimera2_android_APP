package com.jht.deployments

import com.jht.JHTConfiguration
import com.jht.JHTFlavor
import com.jht.JHTFlavors
import com.jht.TpaList

/**
 * This is the deployment for "Matrix_retail_gui" which covers Parker, and UltraThin/ICR.
 * TODO:  This file is obsolete. Deploys are setup through AWS in buildSrc/src/main/kotlin now. @Deprecated
 */
class ParkerMainParker extends FlavoredDeployBase {
    @Override
    String[] getTrackedReleasePrefixes() {
        return [
                "1.0.2.8", "1.0.2.12",
                "1.1.1.13", "1.1.1.14", "1.1.1.16", "1.1.1.18",
                "1.2.2.4", "1.2.2.6",
                "1.3.0.12",
                "1.4.0.19", "1.4.0.20", "1.4.0.21", "1.4.0.22", "1.4.0.23",
                "1.5.0.27",
                "1.6.0.14", "1.6.0.26",
                "1.7",
        ]
    }


    // Package to Manifest map.
    private static final JHTPackages[][] packagesInManifest = [
            // everything for RSCU
            [
                    JHTPackages.UPDATER, JHTPackages.CONSOLE_APPS, JHTPackages.ASSETS,
                    JHTPackages.CHIMERA_TPA, JHTPackages.PARKER_IO,
                    JHTPackages.PARKER_OS, JHTPackages.PARKER3_OS, JHTPackages.PARKER3_IO,
                    //JHTPackages.UT_OS, //JHTPackages.ICR70_LCB,
            ],
            // All Updates (Includes LCB updates)
            [
                    JHTPackages.UPDATER, JHTPackages.CONSOLE_APPS, JHTPackages.ASSETS,
                    JHTPackages.CHIMERA_TPA, JHTPackages.PARKER_IO,
                    JHTPackages.PARKER_OS, JHTPackages.PARKER3_IO, JHTPackages.PARKER3_OS, //JHTPackages.UT_OS,
                    JHTPackages.PARKER_LCB_RETAIL_CLIMBMILL
            ],
    ]

    /**
     * Default constructor.
     *
     * @param configuration The configuration object with links to folders so we can find the source
     * files.
     */
    ParkerMainParker(JHTConfiguration configuration) {
        this(JHTFlavor.flavors[JHTFlavors.PARKER_MAIN_MATRIX_PARKER.ordinal()], configuration, ["", ".all"], packagesInManifest)
        //this(JHTFlavor.flavors[JHTFlavors.PARKER_MAIN_MATRIX_PARKER.ordinal()], configuration, ["", ".parker", ".ut"], packagesInManifest)
    }

    /**
     * Used by customizations that run on the Parker platform.
     *
     * @param flavor             The flavor for this deployment.
     * @param configuration      The configuration object with links to folders so we can find the
     * source files.
     * @param manifests          The manifest files for this deployment.
     * @param packagesInManifest Mapping of packages to the manifest file.
     */
    protected ParkerMainParker(JHTFlavor flavor, JHTConfiguration configuration, String[] manifests, JHTPackages[][] packagesInManifests) {
        super(flavor, configuration, manifests, packagesInManifests)
    }

    /*@Override
    protected boolean needsDowngrade(JHTPackages pack) {
        return (pack == JHTPackages.PARKER_OS) // we are downgrading from 1.0.3.2 to 1.0.2.7
    }*/

    @Override
    protected void createStrictTpaList() {
        strictTpas = new TpaList(configuration.chimeraRootNetworkDir)
        // MD5 hashes are an optional extra check, good if xapk is unzipped and you want to makes sure nothing happened to its files

        strictTpas.add("Apps/ChineseTraditionalKeyboard/1.0", "com.googlecode.tcime.unofficial", "1.0", "1", "1bec14d89c2509521e325bc725ac615a")
        strictTpas.add("Apps/Flipboard/4.3.25", "flipboard.app", "4.3.25", "5443", "fcaf3c25cc089926ab3b8c82c6a8a664")
        strictTpas.add("Apps/HangulKeyboard/1.0", "org.kandroid.app.hangulkeyboard", "1.0", "1", "ca3cf94348c211e28104100846a041e3")
        //strictTpas.add("Apps/Hulu/4.26.0.5673", "com.hulu.plus", "4.26.0+5673-google", "4005673", "ff935c680fccd8d5b11bce0c15fa45dc")
        //strictTpas.add("Apps/Instagram/164.0.0.46.123", "com.instagram.android", "164.0.0.46.123", "252055948", "ce022b21c7b2365a3d5fb9959a27b0fd")
        strictTpas.add("Apps/Kindle/8.98.0.100", "com.amazon.kindle", "8.98.0.100(2.0.11694.0)", "1285761010", "4c7244ec1193536f95fc1aea99aa2673")

        //TODO old version
        strictTpas.add("Apps/Netflix/8.114.0", "com.netflix.mediaclient", "8.114.0 build 14 50676", "50676", "e53964adcd298920e376250a7ae2559b")
        strictTpas.add("Apps/PressReader/6.1.201015", "com.newspaperdirect.pressreader.android", "6.1.201015", "437", "110611375a2941a0d2f4b44f9406e09c")
        strictTpas.add("Apps/Spotify/8.9.30.433", "com.spotify.music", "8.9.30.433", "112987095", "dc7a8a14614d95c05b27f79e84ec76c5")
        strictTpas.add("Apps/TeamViewer/15.9.128", "com.teamviewer.quicksupport.market", "15.9.128", "159128", "77d707c76039c258d0e4b994366fa926")
        strictTpas.add("Apps/TeamViewerAddOn/14.v3", "com.teamviewer.quicksupport.addon.aosp14", "11.0.5583", "5583", "165210b53e5e4a92cf50152efc87a3f6")
        strictTpas.add("Apps/cloudmusic/8.9.50", "com.netease.cloudmusic", "8.9.50", "8009050", "a846cc7a72f2159aabec30492cd2cc85")
        strictTpas.add("Apps/ifit/2.8.3.4778", "com.ifit.standalone", "2.8.3", "4778", "7c25ef79688303203a69fad0f7a4cae3")

        //TODO: This should be 8.0.60.0
        strictTpas.add("Apps/WeChat/8.0.55", "com.tencent.mm", "8.0.55", "2780", "7d4ed11083a73ed037d7214bb61db2af")
        strictTpas.add("Apps/Weibo/3.6.2", "com.weico.international", "3.6.2", "3620", "5157add58ca211d3455125d0b9a57995")
        strictTpas.add("Apps/PlutoTV/5.51.1.0", "tv.pluto.android", "5.51.1", "410400217", "ef415cee2f48daa9adada5e86aae450d")
        strictTpas.add("Apps/iqiyi/6.4.5", "com.iqiyi.i18n", "6.4.5", "698471", "46761549ff658fe40fa4ead5b523028a")
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
            case JHTPackages.CHIMERA_TPA:
                strictTpas.populateSources(sources)
                break
            case JHTPackages.PARKER_OS:
                sources.add(new File(configuration.parkerRootNetworkDir, "/Software/os/ota/${getVersion(JHTPackages.PARKER_OS)}"))
                break
            case JHTPackages.PARKER3_OS:
                sources.add(new File(configuration.parkerRootNetworkDir, "/Software/os3/ota/${getVersion(JHTPackages.PARKER3_OS)}"))
                break
            case JHTPackages.PARKER_IO:
                sources.add(new File(configuration.parkerRootNetworkDir, "Firmware/IO/${getVersion(JHTPackages.PARKER_IO)}"))
                break
            case JHTPackages.PARKER3_IO:
                sources.add(new File(configuration.parkerRootNetworkDir, "Firmware/IO3/${getVersion(JHTPackages.PARKER3_IO)}"))
                break
            case JHTPackages.ASSETS:
                sources.add(new File(configuration.parkerRootNetworkDir, "Software/assets"))
                break
            case JHTPackages.PARKER_LCB_RETAIL_CLIMBMILL:
                sources.add(new File(configuration.parkerRootNetworkDir, "Firmware/LCB_RetailClimbmill/${getVersion(JHTPackages.PARKER_LCB_RETAIL_CLIMBMILL)}"))
                break
            default:
                break
        }

        return sources
    }

    /**
     * Get a version for the given package. Deployments need to override this function.
     *
     * @param pType The package enum
     * @return The version. Can be NO_VERSION if the package is not in the deployment.
     */
    @Override
    String getVersion(JHTPackages p) {
        switch (p) {
            case JHTPackages.UPDATER:
            case JHTPackages.CONSOLE_APPS:
            case JHTPackages.CONSOLE_APPS_DEBUG:
                return Parker_Main_Parker
            case JHTPackages.ASSETS:
                return Parker_Assets
            case JHTPackages.CHIMERA_TPA:
                return Parker_TPA
            case JHTPackages.PARKER_OS:
                return Parker_OS
            case JHTPackages.PARKER3_OS:
                return Parker3_OS
            case JHTPackages.PARKER_IO:
                return Parker_IO
            case JHTPackages.PARKER3_IO:
                return Parker3_IO
            case JHTPackages.PARKER_LCB_RETAIL_CLIMBMILL:
                return LCBRetailClimbmill
            default:
                return NO_VERSION
        }
    }

    @Override
    boolean doesConsoleAppNeedReboot() {
        return true
    }

}