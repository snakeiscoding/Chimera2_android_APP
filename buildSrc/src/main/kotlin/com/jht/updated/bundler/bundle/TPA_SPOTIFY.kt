package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

val TPA_SPOTIFY: TPABundle get() {
    return object : TPABundle("Spotify", "6d75fdea-c3df-41b6-8811-e6cd8c19d1ef") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.spotify.music")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.spotify.music")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.spotify.music")
            JHTFlavor.T600xeMatrixParker -> listOf("com.spotify.music")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.spotify.music")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.spotify.music")
        }

        override val allPackages: List<String>
            get() = listOf("com.spotify.music")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Spotify/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Spotify/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Spotify/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Spotify/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Spotify/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Spotify/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Spotify/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Spotify/${version(flavor)}".replace("/", File.separator))
                )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Spotify/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Spotify/${version(flavor)}".replace("/", File.separator))
                )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Spotify/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Spotify/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "8.9.76.538"
            JHTFlavor.ParkerMainMatrixParker -> "9.0.28.123"
            JHTFlavor.Vision600eMatrixParker -> "8.9.30.433"
            JHTFlavor.T600xeMatrixParker -> "8.9.30.433"
            JHTFlavor.ChimeraMainMatrixLcgui -> "9.0.28.123"
            JHTFlavor.ProjectoneMatrixChimera -> "8.9.76.538"
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