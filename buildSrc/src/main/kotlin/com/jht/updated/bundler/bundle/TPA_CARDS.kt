package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

/** Legacy Solitaire Suite app. */
val TPA_CARDS: TPABundle get() {
    return object : TPABundle("Cards", "06effdce-fed4-4f29-bbe6-8e15b569072b") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.rikkigames.solsuite")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.rikkigames.solsuite")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.rikkigames.solsuite")
            JHTFlavor.T600xeMatrixParker -> listOf("com.rikkigames.solsuite")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.rikkigames.solsuite")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.rikkigames.solsuite")
        }

        override val allPackages: List<String> = listOf("com.rikkigames.solsuite")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Cards/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Cards/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Cards/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Cards/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Cards/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Cards/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Cards/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Cards/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Cards/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Cards/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Cards/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Cards/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "5.15.0.1"
            JHTFlavor.ParkerMainMatrixParker -> "5.15.0.0"
            JHTFlavor.Vision600eMatrixParker -> "5.11.0.0"
            JHTFlavor.T600xeMatrixParker -> "5.11.0.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "5.15.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "5.15.0.1"
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