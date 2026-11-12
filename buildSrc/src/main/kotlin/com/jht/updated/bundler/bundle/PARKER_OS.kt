package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.UpdateType.OS
import com.jht.updated.bundler.parkerAWSBuildAssets
import com.jht.updated.bundler.parkerBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// PARKER_OS
val PARKER_OS: Bundle
    get() {
    // same UUID as CHIMERA_OS_K2_2
    return object : Bundle("OS", "2ec6a147-375f-4a61-ad53-6050c17c27bb", "chimera_os", OS) {
        override val requiresReboot: Boolean get() = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${parkerAWSBuildAssets}/Software/os/ota/${version(flavor)}",
                "${parkerBuildAssetsDir}/Software/os/ota/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${parkerAWSBuildAssets}/Software/os/ota/${version(flavor)}",
                "${parkerBuildAssetsDir}/Software/os/ota/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> "1.0.6.0" // done
            JHTFlavor.Vision600eMatrixParker -> "1.0.5.2"
            JHTFlavor.T600xeMatrixParker -> "1.0.5.2" // done
            JHTFlavor.ChimeraMainMatrixLcgui -> "2.0.5.3"
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> false
            JHTFlavor.Vision600eMatrixParker -> false
            JHTFlavor.T600xeMatrixParker -> false
            JHTFlavor.ChimeraMainMatrixLcgui -> false
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui
        )
    }
}