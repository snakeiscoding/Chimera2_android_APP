package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_MYWEATHER: TPABundle get() {
    return object : TPABundle("MyWeather", "f6f003da-a524-4293-b7a2-1599956e90ec") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.clover.myweather")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.clover.myweather")
            else -> unsupported()
        }

        override val allPackages: List<String>
            get() = listOf("com.clover.myweather")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/MyWeather/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/MyWeather/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/MyWeather/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/MyWeather/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "0.5.5.0"
            JHTFlavor.ProjectoneMatrixChimera -> "0.5.5.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}