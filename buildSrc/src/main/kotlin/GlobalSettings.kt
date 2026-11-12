import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/***************************************************************************************************
 *
 * GLOBAL SETTINGS
 *
 * This file contains the global settings needed in `buildSrc` and the build files for the project.
 *
 **************************************************************************************************/


/**
 * Whether to use traditional release candidate version numbers vs local dated versions.
 * Favor false so developer defaults to local versions unless explicitly enables it.
 */
fun USE_RC_VERSIONS(project: Project): Boolean {
    val properties = Properties()
    val propertiesFile = File(project.rootDir, "local.properties")
    if (!propertiesFile.exists()) return false
    val fs = FileInputStream(propertiesFile)
    properties.load(fs)
    return properties.getProperty("USE_RC_VERSIONS") == "true"
}
