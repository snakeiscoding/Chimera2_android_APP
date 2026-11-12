package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

val TPA_CHINESETRADITIONALKEYBOARD: TPABundle get() {
    return object : TPABundle("ChineseTraditionalKeyboard", "a09bcbc8-3314-448d-a53a-874d683f6062") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = listOf("com.googlecode.tcime.unofficial")

        override val allPackages: List<String> = listOf("com.googlecode.tcime.unofficial")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/ChineseTraditionalKeyboard/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/ChineseTraditionalKeyboard/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> "1.0.0.0"
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