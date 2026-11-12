package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.parkerAWSBuildAssets
import com.jht.updated.bundler.parkerBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val PARKER_IO3: Bundle
    get() {
        return object : Bundle("IO3", "6b3d43a2-46a4-48e9-9c0d-bdae7af28f2b", "chimera_firmware") {
            override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
                JHTFlavor.Vision600eMatrixParker -> unsupported()
                JHTFlavor.T600xeMatrixParker -> unsupported()
                JHTFlavor.ProjectoneMatrixChimera -> unsupported()
                JHTFlavor.ParkerMainMatrixParker,
                JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${parkerAWSBuildAssets}/Firmware/IO3/${version(flavor)}",
                    "${parkerBuildAssetsDir}/Firmware/IO3/${version(flavor)}".replace("/", File.separator))
                )
            }

            override fun version(flavor: JHTFlavor): String = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
                JHTFlavor.ProjectoneMatrixChimera -> unsupported()
                JHTFlavor.Vision600eMatrixParker -> unsupported()
                JHTFlavor.T600xeMatrixParker -> unsupported()
                JHTFlavor.ParkerMainMatrixParker -> "1.6.7.0"
                JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            }

            override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
                JHTFlavor.ParkerMainMatrixParker -> false
                JHTFlavor.Vision600eMatrixParker -> false
                JHTFlavor.T600xeMatrixParker -> false
                JHTFlavor.ChimeraMainMatrixLcgui -> false
                JHTFlavor.ProjectoneMatrixChimera -> unsupported()
            }

            override val supportedFlavors: List<JHTFlavor> get() = listOf(
                JHTFlavor.ParkerMainMatrixParker
            )
        }
    }