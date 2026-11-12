package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

val TPA_HELIBOARD: TPABundle get() {

    return object : TPABundle("HeliBoard", "550e8400-e29b-41d4-a716-446655440000") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = listOf("helium314.keyboard")

        override val allPackages: List<String> get() = listOf("helium314.keyboard")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(
                getAWSFile(
                    "${chimeraAWSBuildAssets}/Apps/HeliBoard/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/HeliBoard/${version(flavor)}".replace("/", File.separator)
                )
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> "3.6.0.3"
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