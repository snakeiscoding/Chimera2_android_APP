package com.jht

import com.jht.deployments.*

/**
 * This class creates a IFlavorConfiguration object for managing the software release for a given
 * flavor. The configuration object allows you to get the package versions, compute new versions,
 * and deploy the packages. See that class for more info.
 */
class ReleaseSoftware {
    private FlavoredDeployBase deployment

    /**
     * Create a release instance.
     *
     * @param flavor  The flavor we are deploying
     * @param rootDir The project root dir.
     */
    ReleaseSoftware(JHTFlavor flavor, File rootDir) {
        JHTConfiguration configuration = new JHTConfiguration(flavor, rootDir)
        
        switch (flavor.flavorType()) {
            case JHTFlavors.CHIMERA_MAIN_MATRIX_CHIMERA:
                deployment = new ChimeraMainChimera(configuration)
                break
            case JHTFlavors.PARKER_MAIN_MATRIX_PARKER:
                deployment = new ParkerMainParker(configuration)
                break
            case JHTFlavors.VISION600E_MATRIX_PARKER:
                deployment = new Vision600EParker(configuration)
                break
            case JHTFlavors.T600XE_MATRIX_PARKER:
                deployment = new T600XEParker(configuration)
                break
            case JHTFlavors.CHIMERA_MAIN_MATRIX_LCGUI:
                deployment = new ChimeraMainMatrixLcgui(configuration)
                break
            case JHTFlavors.PROJECTONE_MATRIX_CHIMERA:
                deployment = new ProjectOneChimera(configuration)
                break
            default:
                println("!! ReleaseSoftware factory missing construction for ${flavor.flavorType()} !!")
        }
    }

    FlavoredDeployBase deployment() {
        return deployment
    }
}
