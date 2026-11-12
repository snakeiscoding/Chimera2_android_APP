package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_YMCA360: TPABundle get() {
    return object : TPABundle("YMCA360", "9475f2da-3478-4405-b95a-2f276503a91e") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("org.ymca360.omni")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("org.ymca360.omni")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("org.ymca360.omni")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("org.ymca360", "org.ymca360.omni")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/YMCA360/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/YMCA360/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/YMCA360/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/YMCA360/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/YMCA360/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/YMCA360/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "2.16.5.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "2.16.5.0"
            JHTFlavor.ProjectoneMatrixChimera -> "2.16.5.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}