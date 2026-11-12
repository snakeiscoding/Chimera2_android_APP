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
 * This is a dummy bundle for the old Third Party Apps bundle that existed before third party apps
 * were broken out. The purpose of this package is to give the updater a version of the old bundle
 * higher than any potential version that could be in the field. We need this dummy package to
 * prevent a console from mistakenly downgrading it's third party apps if it moves to an
 * environment where the old bundle still exists. This will cause the updater to get in a strange
 * state where it downgrades the updates, but believes the new broken out versions are installed.
 * This would prevent the console from being able to re-update the third party apps without
 * clearing the updater service
 **/
val TPA_DUMMY_TPA_ZIP = object : Bundle(
        "Third Party Apps",
        "fd9193ca-3ab3-43e9-829f-b41e83fc915f",
        packageGroup = THIRD_PARTY_PACKAGE_GROUP
    ) {
    override fun getSources(project: Project, flavor: JHTFlavor): List<File> {
        // Ensure the required update.sh file is generated.
        createUpdateSh(project, flavor)

        // return the required list of files.
        return listOf(getUpdateSh(project, flavor).parentFile)
    }

    /** Third Party Apps bundle version. */
    override fun version(flavor: JHTFlavor): String = "9.9.9.9"

    override fun usbOnly(flavor: JHTFlavor): Boolean { return false; }

    override val supportedFlavors: List<JHTFlavor>
        get() = listOf(
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
        project.projectDir.get("Deploy/generated_dummy_zip_${flavor.flavorCapitalized}/update.sh")


    /**
     * creates empty update.sh file as needed for the dummy bundle to believe to be installed
     */
    private fun createUpdateSh(project: Project, flavor: JHTFlavor) {
        val updateShPath = getUpdateSh(project, flavor)
        val fileContent = StringBuilder("#!/bin/bash\n\n")

        // Write the file to the correct location.
        println("Creating the required file.")
        updateShPath.createFile(fileContent.toString())
    }
}
