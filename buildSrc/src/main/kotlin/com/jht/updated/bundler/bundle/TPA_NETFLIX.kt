package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

val TPA_NETFLIX: TPABundle get() {
    return object : TPABundle("Netflix", "8ec1167e-0f84-4352-b3c7-bb60dd847643") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.netflix.mediaclient")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.netflix.mediaclient")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.netflix.mediaclient")
            JHTFlavor.T600xeMatrixParker -> listOf("com.netflix.mediaclient")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.netflix.mediaclient")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.netflix.mediaclient")
        }

        override val allPackages: List<String>
            get() = listOf("com.netflix.mediaclient")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Netflix/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Netflix/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Netflix/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Netflix/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Netflix/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Netflix/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Netflix/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/Netflix/${version(flavor)}".replace("/", File.separator))
                )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Netflix/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/Netflix/${version(flavor)}".replace("/", File.separator))
                )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Netflix/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Netflix/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "9.45.0.3"
            JHTFlavor.ParkerMainMatrixParker -> "9.45.0.3"
            JHTFlavor.Vision600eMatrixParker -> "8.114.0.0" // todo: should i handle this version differently?
            JHTFlavor.T600xeMatrixParker -> "8.114.0.0" // todo: should i handle this version differently?
            JHTFlavor.ChimeraMainMatrixLcgui -> "9.45.0.3"
            JHTFlavor.ProjectoneMatrixChimera -> "9.45.0.3"
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