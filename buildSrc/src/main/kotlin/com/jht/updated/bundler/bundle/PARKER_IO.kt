package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.parkerAWSBuildAssets
import com.jht.updated.bundler.parkerBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val PARKER_IO: Bundle
    get() {
    return object : Bundle("IO", "fb50aaa6-64bc-4217-a2aa-fdeda79684b0", "chimera_firmware") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()

            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${parkerAWSBuildAssets}/Firmware/IO/${version(flavor)}",
                "${parkerBuildAssetsDir}/Firmware/IO/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()

            JHTFlavor.ParkerMainMatrixParker -> "4.11.0.0"
            JHTFlavor.Vision600eMatrixParker -> "4.9.0.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "4.11.0.0"
            JHTFlavor.T600xeMatrixParker -> "4.9.0.0"
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> false
            JHTFlavor.Vision600eMatrixParker -> false
            JHTFlavor.T600xeMatrixParker -> false
            JHTFlavor.ChimeraMainMatrixLcgui -> false
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.T600xeMatrixParker
        )
    }
}