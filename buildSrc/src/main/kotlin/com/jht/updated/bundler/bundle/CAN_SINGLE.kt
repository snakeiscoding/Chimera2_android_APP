package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//CAN_SINGLE, the toggle/LCD device for NexGen models of Treadmill and Ascent Trainer, which communicate using the CAN 2.0 protocol with ISO-TP layer
val CAN_SINGLE: Bundle
    get() {
    return object : Bundle("CAN_SINGLE", "4d5890bb-3cd8-480b-99de-41c568db17aa", "chimera_firmware") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                // NOTE: this is from BuildAssets, not the Deploy folder in the repo
                getAWSFile("$chimeraAWSBuildAssets/Firmware/CAN/SINGLE/${version(flavor)}",
                "$chimeraBuildAssetsDir/Firmware/CAN/SINGLE/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        //TODO: I think only main matrix supports CAN, but verify
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