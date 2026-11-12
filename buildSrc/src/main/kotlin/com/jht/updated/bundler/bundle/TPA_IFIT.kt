package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

/** Retail only */
val TPA_IFIT: TPABundle get() {
    return object : TPABundle("ifit", "1a91da36-6db0-4b8e-89cf-574813cc085f") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.ifit.standalone")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.ifit.standalone")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/ifit/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/ifit/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> "2.8.3.4778"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(JHTFlavor.ParkerMainMatrixParker)
    }
}