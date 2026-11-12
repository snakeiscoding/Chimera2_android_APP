package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_MYZONE: TPABundle
    get() {
    return object : TPABundle("MyZone", "6d0154b7-663e-4a96-ad34-44c71900e16f") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.myzone.myzoneble.console_service")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.myzone.myzoneble.console_service")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.myzone.myzoneble.console_service")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                getAWSFile("$chimeraAWSBuildAssets/Apps/MyZone/${version(flavor)}",
                    "$chimeraBuildAssetsDir/Apps/MyZone/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(
                getAWSFile("$chimeraAWSBuildAssets/Apps/MyZone/${version(flavor)}",
                    "$chimeraBuildAssetsDir/Apps/MyZone/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "0.5.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "0.5.0.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}