package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//SALUTRON
val SALUTRON: Bundle
    get() {
    return object : Bundle("Salutron", "acd5713e-3e57-4b15-98f9-37716a3440a6", "chimera_salutron") {
        // fixme: I might just want to provide a default implementation to save more time.
        override fun getSources(project: Project, flavor: JHTFlavor): List<File>? = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> null // No additional files are needed.

            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
        }

        // 4504C2230 is an example version, 4504 is the firmware version, 2230 is bootloader version
        // example "4552C2233" is 4552.2233.0.0
        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "4552.2233.0.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "4552.2233.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "4552.2233.0.0"
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> false
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> false
            JHTFlavor.ProjectoneMatrixChimera -> false
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}