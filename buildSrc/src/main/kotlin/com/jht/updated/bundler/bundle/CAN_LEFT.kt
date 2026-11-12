package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//CAN_LEFT, the toggle/LCD device for NexGen models of Climb Mill and Stationary Bikes, which communicate using the CAN 2.0 protocol with ISO-TP layer
val CAN_LEFT: Bundle
    get() {
    return object : Bundle("CAN_LEFT", "86f5f0cd-dc6e-4f6c-b5ba-3d7c9df02f86", "chimera_firmware") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                // NOTE: this is from BuildAssets, not the Deploy folder in the repo
                getAWSFile("$chimeraAWSBuildAssets/Firmware/CAN/LEFT/${version(flavor)}",
                "$chimeraBuildAssetsDir/Firmware/CAN/LEFT/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "0.2.5.0"
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