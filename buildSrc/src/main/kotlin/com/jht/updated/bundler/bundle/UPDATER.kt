package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.BuildBundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.UpdateType.SoftwareUpdater
import com.jht.updated.md5
import org.gradle.api.Project
import java.io.File

val UPDATER: BuildBundle
    get() {
    return object : BuildBundle("Updater", "d9a76997-7a2d-4f47-b429-6273de4bcfeb", "chimera_updater", SoftwareUpdater) {
        override val requiresReboot: Boolean get() = true

        override fun getSources(project: Project, flavor: JHTFlavor): List<File> = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker -> listOf(
                File(project.projectDir, "UpdateService/build/outputs/apk/${flavor.flavorDir}/release/UpdateService-${flavor.flavorAPK}-release.apk")
            )
        }

        override fun publishBundle(project: Project, flavor: JHTFlavor, sourceMD5: String) {
            val updaterMD5 = File(project.projectDir.absolutePath + File.separator + "UpdateService").md5
            super.publishBundle(project, flavor, updaterMD5)
        }

        override fun publishNightlyBundle(project: Project, flavor: JHTFlavor, sourceMD5: String) {
            val updaterMD5 = File(project.projectDir.absolutePath + File.separator + "UpdateService").md5
            super.publishNightlyBundle(project, flavor, updaterMD5)
        }

        private val versionMap = mutableMapOf(
            JHTFlavor.ChimeraMainMatrixChimera to "4.10.0.12",
            JHTFlavor.Vision600eMatrixParker to "1.2.0.4",
            JHTFlavor.ParkerMainMatrixParker to "1.9.1.11",
            JHTFlavor.T600xeMatrixParker to "1.2.0.4",
            JHTFlavor.ChimeraMainMatrixLcgui to "1.6.0.11",
            JHTFlavor.ProjectoneMatrixChimera to "2.5.0.12"
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
            ":UpdateService:assemble${flavor.flavorCapitalized}Release"
        )

        /** All flavors support the updater. */
        override val supportedFlavors: List<JHTFlavor> = listOf(
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera,

            //TODO: For testing purposes these should be commented out.
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker
        )
    }
}