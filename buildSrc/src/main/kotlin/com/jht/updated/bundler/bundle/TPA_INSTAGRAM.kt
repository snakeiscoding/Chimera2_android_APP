package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import org.gradle.api.Project
import java.io.File

@Deprecated("Using web page since 2019.")
/**
 * We now use the web page instead of the apk. Please reference Parker release notes from version
 * 1.6.0.9 created in 2022.
 * @see <a href="https://jhtnaee.atlassian.net/wiki/spaces/CPRNS/pages/1239941327/Matrix+Retail+GUI+formerly+Parker+Console+App+Release+Notes#:~:text=Remove%20unused%20instagram%20apk%20from%20TPA%20package%20since%20we%27ve%20been%20using%20the%20webpage%20since%20Nov%202019.">1.6.0.9 notes</a>
 */
val TPA_INSTAGRAM: TPABundle get() {
    return object : TPABundle("Instagram", "a622de85-005f-4b3a-b3cc-5de4e20b4e90") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.instagram.android")
            JHTFlavor.ParkerMainMatrixParker -> listOf("com.instagram.android")
            JHTFlavor.Vision600eMatrixParker -> listOf("com.instagram.android")
            JHTFlavor.T600xeMatrixParker -> listOf("com.instagram.android")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.instagram.android")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.instagram.android")
        }

        override val allPackages: List<String> get() = listOf("com.instagram.android")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Instagram/164.0.0.46.123",
                "${chimeraBuildAssetsDir}/Apps/Instagram/164.0.0.46.123".replace("/", File.separator))
            )
            JHTFlavor.ParkerMainMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Instagram/164.0.0.46.123",
                    "${chimeraBuildAssetsDir}/Apps/Instagram/164.0.0.46.123".replace("/", File.separator))
                )
            JHTFlavor.Vision600eMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Instagram/119.0.0.33.147",
                "${chimeraBuildAssetsDir}/Apps/Instagram/119.0.0.33.147".replace("/", File.separator))
            )
            JHTFlavor.T600xeMatrixParker -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Instagram/119.0.0.33.147",
                "${chimeraBuildAssetsDir}/Apps/Instagram/119.0.0.33.147".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Instagram/164.0.0.46.123",
                "${chimeraBuildAssetsDir}/Apps/Instagram/164.0.0.46.123".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/Instagram/164.0.0.46.123",
                "${chimeraBuildAssetsDir}/Apps/Instagram/164.0.0.46.123".replace("/", File.separator))
            )
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "164.0.0.46" //.123
            JHTFlavor.ParkerMainMatrixParker -> "164.0.0.46" //.123
            JHTFlavor.Vision600eMatrixParker -> "119.0.0.33" //.147
            JHTFlavor.T600xeMatrixParker -> "119.0.0.33" //.147
            JHTFlavor.ChimeraMainMatrixLcgui -> "164.0.0.46" //.123
            JHTFlavor.ProjectoneMatrixChimera -> "164.0.0.46" //.123
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf()
    }
}