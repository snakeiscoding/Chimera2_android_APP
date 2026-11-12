package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_WEIBO: TPABundle get() {
    return object : TPABundle("Weibo", "57055f3e-e800-4848-b1c3-f3de059fcac3") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.weico.international")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.weico.international")
            JHTFlavor.T600xeMatrixParker -> listOf("com.weico.international")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.weico.international")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Weibo/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Weibo/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Weibo/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Weibo/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Weibo/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Weibo/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> "3.6.2.0"
            JHTFlavor.Vision600eMatrixParker -> "3.6.2.0"
            JHTFlavor.T600xeMatrixParker -> "3.6.2.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker
        )
    }
}