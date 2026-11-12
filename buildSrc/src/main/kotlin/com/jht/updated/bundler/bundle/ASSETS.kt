package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.parkerAWSBuildAssets
import com.jht.updated.bundler.parkerBuildAssetsDir
import org.gradle.api.Project
import java.io.File

/**
 * The assets package bundle
 */
val ASSETS: Bundle
    get() {
    return object : Bundle("Assets", "f5a6a28a-dacc-44a5-9949-697fc379873d", "chimera_assets") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File>? = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> null
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${parkerAWSBuildAssets}/Software/assets",
                "${parkerBuildAssetsDir}/Software/assets".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${parkerAWSBuildAssets}/Software/assets",
                "${parkerBuildAssetsDir}/Software/assets".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> null
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${parkerAWSBuildAssets}/Software/assets",
                "${parkerBuildAssetsDir}/Software/assets".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> null
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "4.10.3.13"
            JHTFlavor.Vision600eMatrixParker -> "1.2.0.1"
            JHTFlavor.ParkerMainMatrixParker -> "1.9.1.11"
            JHTFlavor.T600xeMatrixParker -> "1.2.0.1"
            JHTFlavor.ChimeraMainMatrixLcgui -> "1.6.3.9"
            JHTFlavor.ProjectoneMatrixChimera -> "2.5.3.13"
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> false
            JHTFlavor.ParkerMainMatrixParker -> false
            JHTFlavor.Vision600eMatrixParker -> false
            JHTFlavor.T600xeMatrixParker -> false
            JHTFlavor.ChimeraMainMatrixLcgui -> false
            JHTFlavor.ProjectoneMatrixChimera -> false
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}