package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//CHIMERA_LCBAthena
val CHIMERA_LCBAthena: Bundle
    get() {
    return object : Bundle("LCBAthena", "eb7359d0-68e8-4293-ada3-2c42392b62c8", "chimera_firmware") {
        override val requiresReboot: Boolean get() = true
        override val manualReboot: Boolean get() = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Firmware/LCB/LCBAthena/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Firmware/LCB/LCBAthena/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "1.17.0.0"
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> true
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ChimeraMainMatrixChimera
        )
    }
}