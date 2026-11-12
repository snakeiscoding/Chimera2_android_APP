package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_KINDLELITE: TPABundle get() {
    return object : TPABundle("KindleLite", "13041fd1-526f-43e0-b9d5-5049d64af0cf") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.Vision600eMatrixParker -> listOf("com.amazon.klite")
            JHTFlavor.T600xeMatrixParker -> listOf("com.amazon.klite")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.amazon.klite")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/KindleLite/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/KindleLite/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/KindleLite/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/KindleLite/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.Vision600eMatrixParker -> "1.15.0.0"
            JHTFlavor.T600xeMatrixParker -> "1.15.0.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker
        )
    }
}