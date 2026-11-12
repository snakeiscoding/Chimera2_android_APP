package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_BSJLIVETV: TPABundle
    get() {
    return object : TPABundle("BSJLiveTV", "00f9be8c-deba-4fa2-9eea-7e20058c6eab") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.bsjlive.tv")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.bsjlive.tv")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.bsjlive.tv")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                getAWSFile("$chimeraAWSBuildAssets/Apps/BSJLiveTV/${version(flavor)}",
                    "$chimeraBuildAssetsDir/Apps/BSJLiveTV/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(
                getAWSFile("$chimeraAWSBuildAssets/Apps/BSJLiveTV/${version(flavor)}",
                    "$chimeraBuildAssetsDir/Apps/BSJLiveTV/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "6.6.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "6.6.0.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            /* CGC-20971
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera*/
        )
    }
}