package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.BuildBundle
import com.jht.updated.bundler.JHTFlavor
import org.gradle.api.Project
import java.io.File

val CONSOLE_APPS: BuildBundle
    get() {
    return object : BuildBundle("Apps", "63a74e6c-8380-4416-963a-2ee9d2357cc6", "chimera_mainapp") {
        override val requiresReboot: Boolean get() = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ProjectoneMatrixChimera,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> listOf(
                File(project.projectDir, "Chimera/build/outputs/apk/${flavor.flavorDir}/release/Chimera-${flavor.flavorAPK}-release.apk")
//                File(project.projectDir, "Chimera/build/outputs/apk/${flavor.flavorDir}/debug/Chimera-${flavor.flavorAPK}-debug.apk")
            )
        }

        private val versionMap = mutableMapOf(
            JHTFlavor.ChimeraMainMatrixChimera to "7.14.1.0",
            JHTFlavor.Vision600eMatrixParker to "1.2.0.10",
            JHTFlavor.ParkerMainMatrixParker to "1.9.1.13",
            JHTFlavor.T600xeMatrixParker to "1.2.0.9",
            JHTFlavor.ChimeraMainMatrixLcgui to "1.6.0.5",
            JHTFlavor.ProjectoneMatrixChimera to "2.9.1.0"
        )

        override fun version(flavor: JHTFlavor): String =
            versionMap[flavor] ?: error("No version defined for $flavor")

        override fun setVersion(flavor: JHTFlavor, newVersion: String) {
            versionMap[flavor] = newVersion
        }

        override fun usbOnly(flavor: JHTFlavor): Boolean = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera -> false
        }

        override fun buildTasks(flavor: JHTFlavor): List<String> = listOf(
            // fixme: This should include the clean?
            ":Chimera:assemble${flavor.flavorCapitalized}Release"
//            ":Chimera:assemble${flavor.flavorCapitalized}Debug"
        )

        // The console apps are the only bundle that can't be reused.
        override val allowReuse: Boolean get() = false

        // All flavors support the updater.
        override val supportedFlavors: List<JHTFlavor> = JHTFlavor.values().toList()
    }
}