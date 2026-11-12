package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

/** In-house JHT Cards app. NOTE: Until we fix the signing, this app gets its own update.sh. Once
 *  fixed, we could use misc/custom.sh for the AOT compilation line.
 */
val TPA_CARDSJHT: TPABundle get() {
    return object : TPABundle("CardsJHT", "48f27795-cdf1-46af-bb2d-18fb3a8798b5") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            else -> listOf("com.jht.solitaire")
        }

        override val allPackages: List<String> = listOf("com.jht.solitaire")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ParkerMainMatrixParker -> unsupported()
            else -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/CardsJHT/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/CardsJHT/${version(flavor)}".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = "1.0.4.5"

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}