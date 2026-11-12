package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_PLUTOTV: TPABundle get() {
    return object : TPABundle("PlutoTV", "6d96d850-6c12-405e-b1c9-b8acf71524eb") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf("tv.pluto.android")
            else -> unsupported()
        }

        override val allPackages: List<String>
            get() = listOf("tv.pluto.android")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PlutoTV/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PlutoTV/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera -> "5.56.0.0"
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui -> "5.51.1.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}