package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_TEAMVIEWER: TPABundle get() {
    return object : TPABundle("TeamViewer", "6f94e2a1-711b-4080-9cc1-49c3ebc59bea") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.teamviewer.quicksupport.market")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.teamviewer.quicksupport.market")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.teamviewer.quicksupport.market")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.teamviewer.quicksupport.market")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.teamviewer.quicksupport.market")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewer/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewer/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewer/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewer/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewer/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewer/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/TeamViewer/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/TeamViewer/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "15.9.128.0"
            JHTFlavor.ParkerMainMatrixParker -> "15.9.128.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "15.9.128.0"
            JHTFlavor.ProjectoneMatrixChimera -> "15.9.128.0"
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