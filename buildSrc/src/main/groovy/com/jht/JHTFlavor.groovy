package com.jht

import com.jht.deployments.FlavoredDeployBase

/**
 * Defines the various flavors. Use this to get properties for the current active flavor.
 */
class JHTFlavor {
    /*
     *
     * FIXME(ARR) - MUST CHANGE TO A MAP BECAUSE THIS IS BEING ACCESSED BY ORDINAL WHICH IS ACTUALLY
     *  TERRIFYING!!!
     */
    static final JHTFlavor[] flavors = [
            new JHTFlavor(JHTFlavors.CHIMERA_MAIN_MATRIX_CHIMERA, "chimera_main", "matrix", "chimera", "chimera", "ReleaseDirectory/Chimera", [1963], "chimera", "chimera", FlavoredDeployBase.Chimera_Main_Chimera, "reporting/Chimera/CI"),
            new JHTFlavor(JHTFlavors.PARKER_MAIN_MATRIX_PARKER, "parker_main", "matrix", "parker", "parker", "ReleaseDirectory/Parker", [1963], "parker", "parker", FlavoredDeployBase.Parker_Main_Parker, "reporting/Parker/CI"),
            new JHTFlavor(JHTFlavors.VISION600E_MATRIX_PARKER, "vision600e", "matrix", "parker", "vision600e", "ReleaseDirectory/Vision600e", [], "vision600e", "vision600e", FlavoredDeployBase.Vision_Parker, "reporting/Vision600/CI"),
            new JHTFlavor(JHTFlavors.T600XE_MATRIX_PARKER, "t600xe", "matrix", "parker", "t600xe", "ReleaseDirectory/T600xe", [], "t600xe", "t600xe", FlavoredDeployBase.T600_Parker, "reporting/T600XE/CI"),
            new JHTFlavor(JHTFlavors.CHIMERA_MAIN_MATRIX_LCGUI, "chimera_main", "matrix", "lcgui", "lowcostgui", "ReleaseDirectory/LowCostGui",[1963], "lcgui", "lowcostgui", FlavoredDeployBase.Chimera_Main_LCGUI, "reporting/TouchBase/CI"),
            new JHTFlavor(JHTFlavors.PROJECTONE_MATRIX_CHIMERA, "projectone", "matrix", "chimera", "projectone", "ReleaseDirectory/ProjectOne",[1963],"projectone", "projectone", FlavoredDeployBase.ProjectOne_Chimera, "reporting/Chimera/CI"),
    ]

    private String platform
    private String ui
    private String product
    private String equipment
    private String releaseDir
    private JHTFlavors flavor
    private List<Integer> defaultRSCUClub
    private String softwareType
    private String softwareConfigurationClass
    private String sdc
    private String ftpLocation

    /**
     * Default constructor
     * @param flavor     The flavor enum.
     * @param product       The product dimension
     * @param ui            The UI dimension
     * @param platform      The platform dimension
     * @param equipment     The string used for 'equipment' tag in manifest (some products share platform but identify as different equipment)
     * @param releaseDir    The location fo the release directory on the Z drive.
     */
    private JHTFlavor(JHTFlavors flavor, String product, String ui, String platform, String equipment, String releaseDir, List<Integer> defaultRSCUClub, String softwareType, String softwareConfigurationClass, String sdc, String ftpLocation) {
        this.platform = platform
        this.ui = ui
        this.product = product
        this.equipment = equipment
        this.releaseDir = releaseDir
        this.flavor = flavor
        this.defaultRSCUClub = defaultRSCUClub
        this.softwareType = softwareType
        this.softwareConfigurationClass = softwareConfigurationClass
        this.sdc = filterSdc(sdc)
        this.ftpLocation = ftpLocation
    }

    private static String filterSdc(String version) {
        String[] fields = version.split("\\.", 3)
        String sdc = fields[0]
        if (fields.length > 1) sdc = sdc + "." + fields[1]
        return sdc
    }

    String platfrom() {
        return platform
    }

    String ui() {
        return ui
    }

    String product() {
        return product
    }

    String sdc() {
        return sdc
    }

    String flavor() {
        return "${product}${ui}${platform}"
    }

    String flavorCapitalized() {
        return "${product.capitalize()}${ui.capitalize()}${platform.capitalize()}"
    }

    String flavorDir() {
        return "${product()}${ui().capitalize()}${platfrom().capitalize()}"
    }

    String flavorAPK() {
        return "${product()}-${ui()}-${platfrom()}"
    }

    String releaseDir() {
        return releaseDir
    }

    String equipment() {
        return equipment
    }

    JHTFlavors flavorType() {
        return flavor
    }

    int[] defaultRSCUClub() { return defaultRSCUClub }
    String softwareType() { return softwareType }
    String softwareConfigurationClass() { return softwareConfigurationClass }

    /**
     * TODO - just use "equipment" ? could releaseDir be replaced with path+equipment or does that
     *  need to remain flexible between flavors?
     */
    String getSubFolder() {
        return releaseDir.split("/").last()
    }
    String getAwsSubFolder() {
        return getSubFolder().toLowerCase()
    }

    String ftpLocation() { return ftpLocation }
}
