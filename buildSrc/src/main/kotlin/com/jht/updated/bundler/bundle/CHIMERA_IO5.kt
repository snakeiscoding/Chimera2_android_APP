package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// CHIMERA3 IO
val CHIMERA_IO5: Bundle
    get() {
        return object : Bundle("IO5", "6b90c9a5-2b75-5a65-66de-12a0a06717f5", "chimera_firmware") {
            override val requiresReboot: Boolean = true

            override fun getSources(project: Project, flavor: JHTFlavor): List<File> = listOf(getAWSFile("${chimeraAWSBuildAssets}/Firmware/IO5/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Firmware/IO5/${version(flavor)}".replace("/", File.separator))
            )

            override fun version(flavor: JHTFlavor): String = when (flavor) {
                // Assuming no P1 yet
                JHTFlavor.ChimeraMainMatrixChimera -> "0.17.0.0"
                else -> unsupported()
            }

            override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
                // Assuming no P1 yet
                JHTFlavor.ChimeraMainMatrixChimera -> false
                else -> unsupported()
            }

            override val supportedFlavors: List<JHTFlavor> = listOf(
                // Assuming no P1 yet
                JHTFlavor.ChimeraMainMatrixChimera,
            )
        }
    }