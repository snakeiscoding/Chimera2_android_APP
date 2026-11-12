package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

val TPA_IQIYI: TPABundle get() {
    return object : TPABundle("IQIYI", "32a9915e-2259-4ea2-a72f-4014506c46c2") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = listOf("com.iqiyi.i18n")

        override val allPackages: List<String>
            get() = listOf("com.iqiyi.i18n")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/iqiyi/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/iqiyi/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> "7.3.6.0"
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker
        )
    }
}