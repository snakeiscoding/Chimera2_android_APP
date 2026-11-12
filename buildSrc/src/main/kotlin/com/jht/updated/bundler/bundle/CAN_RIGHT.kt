package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//CAN_RIGHT, the toggle/LCD device for NexGen models of Climb Mill and Stationary Bikes, which communicate using the CAN 2.0 protocol with ISO-TP layer
val CAN_RIGHT: Bundle
    get() {
    return object : Bundle("CAN_RIGHT", "65664e6d-0128-4ba7-b6c0-c9dd01a38ed3", "chimera_firmware") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                // NOTE: this is from BuildAssets, not the Deploy folder in the repo
                getAWSFile("$chimeraAWSBuildAssets/Firmware/CAN/RIGHT/${version(flavor)}",
                "$chimeraBuildAssetsDir/Firmware/CAN/RIGHT/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "0.2.4.0"
            else -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> false
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ChimeraMainMatrixChimera
        )
    }
}