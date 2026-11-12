package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// CHIMERA_IO3 - Chimera2 IO
val CHIMERA_IO3: Bundle
    get() {
    return object : Bundle("IO3", "e4640af0-abf9-11ec-b909-0242ac120002", "chimera_firmware") {
        override val requiresReboot: Boolean get() = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Firmware/IO3/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Firmware/IO3/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "2.35.0.0"

            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> false
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