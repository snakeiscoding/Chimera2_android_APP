package com.jht.updated.bundler.bundle

import com.jht.updated.bundler.Bundle
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.TPABundle
import com.jht.updated.createFile
import com.jht.updated.bundler.THIRD_PARTY_PACKAGE_GROUP
import com.jht.updated.get
import org.gradle.api.Project
import java.io.File

/**
 * This is being used for the automatic generation of the Third Party App cleanup task. This
 * consumes the versions that are used by everything else.
 *
 * @param allThirdPartyAppBundles The list of all third party app bundles that are available.
 */
fun TPA_CLEANUP(allThirdPartyAppBundles: List<TPABundle>): Bundle {
    return object : Bundle("Third Party App - Cleanup", "838ffdb8-63a0-471e-963c-6d63ee18f4a4", packageGroup = THIRD_PARTY_PACKAGE_GROUP) {
        override fun getSources(project: Project, flavor: JHTFlavor): List<File> {
            // Ensure the required update.sh file is generated.
            createUpdateSh(project, flavor)

            // return the required list of files.
            return listOf(getUpdateSh(project, flavor).parentFile)
        }

        /**
         * Only update the version if the Third Party Apps are being updated. If none are updated,
         * this does not need to be updated.
         */
        override fun version(flavor: JHTFlavor): String = when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> "0.0.0.6"
            JHTFlavor.ParkerMainMatrixParker -> "0.0.0.4"
            JHTFlavor.Vision600eMatrixParker -> "0.0.0.2"
            JHTFlavor.T600xeMatrixParker -> "0.0.0.2"
            JHTFlavor.ChimeraMainMatrixLcgui -> "0.0.0.6"
            JHTFlavor.ProjectoneMatrixChimera -> "0.0.0.6"
        }

        /** @return false, we don't want any app to be USB only. */
        override fun usbOnly(flavor: JHTFlavor): Boolean = false

        override val supportedFlavors: List<JHTFlavor> get() = listOf(
            JHTFlavor.ChimeraMainMatrixChimera,
            JHTFlavor.ParkerMainMatrixParker,
            JHTFlavor.Vision600eMatrixParker,
            JHTFlavor.T600xeMatrixParker,
            JHTFlavor.ChimeraMainMatrixLcgui,
            JHTFlavor.ProjectoneMatrixChimera
        )

        /**
         * Return the path to the update.sh file to generate, or generate, for the given flavor.
         *
         * @param project The given project.
         * @param flavor The given flavor.
         * @return The absolute path to the update SH file.
         */
        private fun getUpdateSh(project: Project, flavor: JHTFlavor): File =
            project.projectDir.get("Deploy/generated_cleanup_${flavor.flavorCapitalized}/update.sh")

        /**
         * Responsible for ensuring the update.sh file is created properly. Issues could occur if
         * this is called more than one time.
         *
         * FIXME: This needs to be updated so that it isn't called more than once. This isn't
         *  extremely time-consuming; however, that doesn't mean we can just consume this time for
         *  no reason.
         */
        private fun createUpdateSh(project: Project, flavor: JHTFlavor) {
            val updateShPath = getUpdateSh(project, flavor)
            val fileContent = StringBuilder("#!/bin/bash\n\n")

            // Currently, it is possible for two bundles to have overlapping package classpaths;
            // specifically, iFit and iFit commercial. To address this issue, it is necessary to
            // gather the list of all supported package classpaths. This is checked before
            // outputting the removal script.
            val supportedPackages = mutableListOf<String>()
            allThirdPartyAppBundles.forEach { bundle ->
                if (bundle.supportedFlavors.contains(flavor)) {
                    supportedPackages.addAll(bundle.currentPackages(flavor))
                }
            }

            println("getUpdateSh: ${updateShPath.absolutePath}")
            allThirdPartyAppBundles.forEach { bundle ->
                if (!bundle.supportedFlavors.contains(flavor)) {
                    bundle.allPackages.forEach { p ->
                        // Only add packages that are not listed in the supported packages list.
                        if (!supportedPackages.contains(p)) {
                            fileContent
                                .append("# Uninstall $p if installed.\n")
                                .append("pm list packages $p | grep :$p$ >/dev/null\n")
                                .append("if [[ \"$?\" = \"0\" ]]; then\n")
                                .append("  pm disable $p\n")
                                .append("  echo \"Uninstalling $p\"\n")
                                .append("  pm uninstall $p\n")
                                .append("fi\n")
                                .append("\n")
                        }
                    }
                }
            }

            // Write the file to the correct location.
            println("Creating the required file.")
            updateShPath.createFile(fileContent.toString())
        }
    }
}