package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

/** CloudMusic (NetEase) app. */
val TPA_CLOUDMUSIC: TPABundle get() {
    return object : TPABundle("cloudmusic", "c21f7829-9aca-4ab8-8b40-a31caf74f8a3") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.netease.cloudmusic")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.netease.cloudmusic")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/cloudmusic/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/cloudmusic/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> "8.9.50.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(JHTFlavor.ParkerMainMatrixParker)
    }
}