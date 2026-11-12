package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.bundler.chimeraAWSBuildAssets
import com.jht.updated.bundler.chimeraBuildAssetsDir
import com.jht.updated.bundler.unsupported
import org.gradle.api.Project
import java.io.File

val TPA_MATRIXTESTAPP: TPABundle get() {
    return object : TPABundle("MatrixTestApp", "7bc74c8d-224c-47f8-bd48-82ff8fea2032") {

        override fun currentPackages(flavor: JHTFlavor): List<String> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf("com.jht.matrixcomm.testapp")
            JHTFlavor.ProjectoneMatrixChimera -> listOf("com.jht.matrixcomm.testapp")
            else -> unsupported()
        }

        override val allPackages: List<String> get() = listOf("com.jht.matrixcomm.testapp")

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> listOf(
                getAWSFile("${chimeraAWSBuildAssets}/Apps/MatrixTestApp/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/MatrixTestApp/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            JHTFlavor.ProjectoneMatrixChimera -> listOf(
                getAWSFile("${chimeraAWSBuildAssets}/Apps/MatrixTestApp/${version(flavor)}",
                    "${chimeraBuildAssetsDir}/Apps/MatrixTestApp/${version(flavor)}"
                        .replace("/", File.separator)
                )
            )
            else -> unsupported()
        }

        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "1.3.0.0"
            JHTFlavor.ProjectoneMatrixChimera -> "1.3.0.0"
            else -> unsupported()
        }

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ProjectoneMatrixChimera
        )
    }
}