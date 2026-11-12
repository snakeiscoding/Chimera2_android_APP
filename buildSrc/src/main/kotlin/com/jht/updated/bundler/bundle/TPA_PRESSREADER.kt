package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

val TPA_PRESSREADER: TPABundle get() {
    return object : TPABundle("PressReader", "fb213164-ecf9-46f7-bcd3-520485541889") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.newspaperdirect.pressreader.android")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.newspaperdirect.pressreader.android")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.newspaperdirect.pressreader.android")
            JHTFlavor.T600xeMatrixParker -> listOf("com.newspaperdirect.pressreader.android")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.newspaperdirect.pressreader.android")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.newspaperdirect.pressreader.android")
        }

        override val allPackages: List<String>
            get() = listOf("com.newspaperdirect.pressreader.android")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PressReader/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PressReader/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PressReader/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PressReader/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PressReader/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PressReader/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PressReader/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PressReader/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PressReader/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PressReader/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/PressReader/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/PressReader/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "7.2.250930.0"
            JHTFlavor.ParkerMainMatrixParker -> "7.2.250930.0"
            JHTFlavor.Vision600eMatrixParker -> "7.2.250930.0"
            JHTFlavor.T600xeMatrixParker -> "7.2.250930.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "7.2.250930.0"
            JHTFlavor.ProjectoneMatrixChimera -> "7.2.250930.0"
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}