package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val VORTEX_LITE_OS: Bundle
    get() {
    return object : Bundle("OS3LITE", "ace5c5f9-73d8-4efc-b0bc-4950b617186e", "chimera_os") {
        override val requiresReboot: Boolean = true
        override val targetConsole: String = "rk3588_jht_l_gen2" // vortex_lite

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/OS/VantronLite/V${version(flavor)}/ota_update",
                "${chimeraBuildAssetsDir}/OS/VantronLite/V${version(flavor)}/ota_update".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "1.1.0.0"
            else -> unsupported()
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> false
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
        )
    }
}
