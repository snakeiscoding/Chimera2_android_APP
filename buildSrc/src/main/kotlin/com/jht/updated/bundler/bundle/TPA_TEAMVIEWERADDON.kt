package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_TEAMVIEWERADDON: TPABundle get() {
    return object : TPABundle("TeamViewerAddon", "5427b75e-706a-42bd-91da-4a0134e36465") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.teamviewer.quicksupport.addon.aosp4")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.teamviewer.quicksupport.addon.aosp14")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.teamviewer.quicksupport.addon.aosp4")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.teamviewer.quicksupport.addon.aosp4")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.teamviewer.quicksupport.addon.aosp4")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewerAddOn/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewerAddOn/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewerAddOn/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewerAddOn/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewerAddOn/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewerAddOn/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewerAddOn/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewerAddOn/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "14.2.0.0"
            JHTFlavor.ParkerMainMatrixParker -> "14.3.0.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "14.2.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "14.2.0.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            /* CGC-20971
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera*/
        )
    }
}