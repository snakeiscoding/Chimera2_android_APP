package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.projectOneAWSBuildAssets
import com.jht.updated.bundler.projectOneBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//P1_TOGGLE_TOUCH_FIRMWARE
val P1_TOGGLE_TOUCH_FIRMWARE: Bundle
    get() {
    return object : Bundle("Toggle Touch Firmware", "7916ce3e-3223-11ee-be56-0242ac120002", "chimera_firmware") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${projectOneAWSBuildAssets}/Firmware/LCDTouch/${version(flavor)}",
                "${projectOneBuildAssetsDir}/Firmware/LCDTouch/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> "1.0.0.0"
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> false
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ProjectoneMatrixChimera,
        )
    }
}