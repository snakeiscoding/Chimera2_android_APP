package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

/**
 * There is no use of this on our consoles, however its still located in many other files, so not
 *  completely removing at this time (Samantha, 4/28/25).
 */
val TPA_SNAPCHAT: TPABundle get() {
    return object : TPABundle("Snapchat", "b03ef1a4-7701-4e6b-a557-7c82e5c9aefe") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> listOf("com.snapchat.android")
            JHTFlavor.T600xeMatrixParker -> listOf("com.snapchat.android")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.snapchat.android")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Snapchat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Snapchat/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Snapchat/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/Snapchat/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> "10.89.0.66"
            JHTFlavor.T600xeMatrixParker -> "10.89.0.66"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker
        )
    }
}