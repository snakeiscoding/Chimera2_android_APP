package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

// CHIMERA_IO
// fixme: This could be removed. This is not actually used anywhere apparently.
val CHIMERA_IO: Bundle
    get() {
    return object : Bundle("IO", "fb50aaa6-64bc-4217-a2aa-fdeda79684b0", "chimera_firmware") {
        override val requiresReboot: Boolean = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Firmware/IO/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Firmware/IO/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
        }

        // fixme: Need to figure out how to reduce the list here. It should be easier than this is
        //  implying.
        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> unsupported()
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            JHTFlavor.Vision600eMatrixParker -> unsupported()
            JHTFlavor.T600xeMatrixParker -> unsupported()
            JHTFlavor.ChimeraMainMatrixLcgui -> unsupported()
            JHTFlavor.ProjectoneMatrixChimera -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf()
    }
}