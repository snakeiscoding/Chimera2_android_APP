package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_WECHAT: TPABundle get() {
    return object : TPABundle("WeChat", "35b3cd5a-a34b-4e37-b4f9-fa87242cdf48") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.tencent.mm")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.tencent.mm")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.tencent.mm")
            JHTFlavor.T600xeMatrixParker -> listOf("com.tencent.mm")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.tencent.mm")
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.tencent.mm")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/WeChat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/WeChat/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/WeChat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/WeChat/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/WeChat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/WeChat/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/WeChat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/WeChat/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/WeChat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/WeChat/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "8.0.60.0"
            JHTFlavor.ParkerMainMatrixParker -> "8.0.55.0"
            JHTFlavor.Vision600eMatrixParker -> "8.0.55.0"
            JHTFlavor.T600xeMatrixParker -> "8.0.55.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "8.0.55.0"
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui
        )
    }
}