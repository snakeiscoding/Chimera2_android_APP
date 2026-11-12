package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

//CHIMERA_TOUCH_PANEL
// fixme: need to rename this bundle after migration.
val CHIMERA_TOUCH_PANEL: Bundle
    get() {
    return object : Bundle("Touch Panel Firmware", "41595365-8fce-438a-98ff-5e089c1a01eb", "chimera_touch_panel") {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File>? = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera -> null // no additional files needed.

            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "0.6.0.1"
            JHTFlavor.ProjectoneMatrixChimera -> "0.6.0.1"
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> true
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> true
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}