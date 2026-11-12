package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// CHIMERA_LCB1
val CHIMERA_LCB1: Bundle
    get() {
    return object : Bundle("LCB1", "100ec386-af52-4ee5-ab62-326388ea36e1", "chimera_firmware") {
        override val requiresReboot: Boolean get() = true
        override val manualReboot: Boolean get() = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Firmware/LCB/LCB_1/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Firmware/LCB/LCB_1/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "10.0.0.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "10.0.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "10.0.0.0"

            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> true
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> true
            JHTFlavor.ProjectoneMatrixChimera -> true
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}