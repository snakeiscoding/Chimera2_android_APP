package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_MOJIWEATHER: TPABundle get() {
    return object : TPABundle("MojiWeather", "0a01b701-bd39-4207-b419-cdeb7db8b813") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.moji.mjweather")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.moji.mjweather")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.moji.mjweather")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                getAWSFile("${chimeraAWSBuildAssets}/Apps/MojiWeather/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/MojiWeather/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(
                getAWSFile("${chimeraAWSBuildAssets}/Apps/MojiWeather/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/MojiWeather/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "8.0506.02.0"
            JHTFlavor.ProjectoneMatrixChimera -> "8.0506.02.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}