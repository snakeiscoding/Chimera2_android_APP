package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_DISNEYPLUS: TPABundle get() {
    return object : TPABundle("DisneyPlus", "1399ddce-80a8-4edf-8666-75c5c7cc4dcc") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.disney.disneyplus")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.disney.disneyplus")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/DisneyPlus/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/DisneyPlus/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera -> "4.7.1.2"
            JHTFlavor.ChimeraMainMatrixLcgui -> "4.7.1.2"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}