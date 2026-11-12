package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.UpdateType
import com.jht.updated.bundler.UpdateType.OS
import com.jht.updated.bundler.parkerAWSBuildAssets
import com.jht.updated.bundler.parkerBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// PARKER_OS3
val PARKER_OS3: Bundle
    get() {

        return object : Bundle("OS3", "3a57fa29-173c-4644-a33c-4aaf9867b23a", "chimera_os", UpdateType.General) { //TODO: Does Base Folder even matter anymore
            override val requiresReboot: Boolean get() = true

            override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
                JHTFlavor.T600xeMatrixParker -> unsupported()
                JHTFlavor.Vision600eMatrixParker -> unsupported()
                JHTFlavor.ProjectoneMatrixChimera -> unsupported()
                JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
                JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${parkerAWSBuildAssets}/Software/os3/ota/${version(flavor)}",
                    "${parkerBuildAssetsDir}/Software/os3/ota/${version(flavor)}".replace("/", File.separator))
                )

            }

            override fun version(flavor: JHTFlavor): String = when (flavor) {
                JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
                JHTFlavor.ParkerMainMatrixParker -> "2.3.1.0"
                JHTFlavor.Vision600eMatrixParker -> unsupported()
                JHTFlavor.T600xeMatrixParker -> unsupported()
                JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
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
                JHTFlavor.ParkerMainMatrixParker
            )
        }
    }