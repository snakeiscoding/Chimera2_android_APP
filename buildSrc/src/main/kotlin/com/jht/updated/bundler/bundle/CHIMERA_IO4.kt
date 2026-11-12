package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// VORTEX IO
val CHIMERA_IO4: Bundle
    get() {
        return object : Bundle("IO4", "26d2f55d-7286-40d1-afcd-a5d6dfe006c2", "chimera_firmware") {
            override val requiresReboot: Boolean = true

            override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera,
                JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Firmware/IO4/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Firmware/IO4/${version(flavor)}".replace("/", File.separator))
                )
                JHTFlavor.ParkerMainMatrixParker,
                JHTFlavor.Vision600eMatrixParker,
                JHTFlavor.T600xeMatrixParker,
                JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            }

            override fun version(flavor: JHTFlavor): String = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera,
                JHTFlavor.ProjectoneMatrixChimera -> "1.19.0.0"

                JHTFlavor.ParkerMainMatrixParker -> unsupported()
                JHTFlavor.Vision600eMatrixParker -> unsupported()
                JHTFlavor.T600xeMatrixParker -> unsupported()
                JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            }

            override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera -> false
                JHTFlavor.ParkerMainMatrixParker -> unsupported()
                JHTFlavor.Vision600eMatrixParker -> unsupported()
                JHTFlavor.T600xeMatrixParker -> unsupported()
                JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
                JHTFlavor.ProjectoneMatrixChimera -> false
            }

            override val supportedFlavors: List<JHTFlavor> = listOf(
                JHTFlavor.ChimeraMainMatrixChimera,
                JHTFlavor.ProjectoneMatrixChimera
            )
        }
    }