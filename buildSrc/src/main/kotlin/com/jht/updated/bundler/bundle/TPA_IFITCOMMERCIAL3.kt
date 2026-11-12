package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_IFITCOMMERCIAL3: TPABundle get() {
    return object : TPABundle("ifit_commercial3", "d09c8555-d67a-4939-870b-9b313f65c0f8") {
        // NOTE: we need a way to install iFit2 on legacy console and iFit3 on Vortex console which is done with target_console
        override val targetConsole: String = "rk3588_jht_gen3,rk3588_jht_l_gen2" // vortex,vortex_lite

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            // No LCGUI? JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.ifit.standalone")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.ifit.standalone")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            // No LCGUI? JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/ifit_commercial/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/ifit_commercial/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            // No LCGUI? JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> "3.2.0.5168"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}