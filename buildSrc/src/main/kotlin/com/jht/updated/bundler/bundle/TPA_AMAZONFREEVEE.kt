package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_AMAZONFREEVEE: TPABundle get() {
    return object : TPABundle("AmazonFreevee", "3996f8c1-12d0-4586-8f46-36d0d410fd5a") {
        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.amazon.imdb.tv.mobile.app")
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf("com.amazon.imdb.tv.mobile.app")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.amazon.imdb.tv.mobile.app")
            else -> unsupported()
        }

        override val allPackages: List<String> = listOf(
            "com.amazon.imdb.tv.mobile.app"
        )

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/AmazonFreevee/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/AmazonFreevee/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ChimeraMainMatrixLcgui -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/AmazonFreevee/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/AmazonFreevee/${version(flavor)}".replace("/", File.separator))
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(getAWSFile("${chimeraAWSBuildAssets}/Apps/AmazonFreevee/${version(flavor)}",
                "${chimeraBuildAssetsDir}/Apps/AmazonFreevee/${version(flavor)}".replace("/", File.separator))
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "1.16.0.0"
            JHTFlavor.ChimeraMainMatrixLcgui -> "1.10.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "1.16.0.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}