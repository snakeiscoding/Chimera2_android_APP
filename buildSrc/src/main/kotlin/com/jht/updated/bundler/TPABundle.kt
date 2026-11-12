package com.jht.updated.bundler

/**
 * A Third Party Apps bundle. Version information, apk name, and supported flavors for the given app.
 */
abstract class TPABundle(packageName: String, id: String, baseFolder: String = "chimera_tpa") : Bundle("Third Party App - $packageName", id, baseFolder, UpdateType.General, THIRD_PARTY_PACKAGE_GROUP) {
    /**
     * Third party apps **must not** be USB only. The business would like these to eventually be
     * released directly though CORE.
     */
    override fun usbOnly(flavor: JHTFlavor): Boolean = false

    /**
     * Return the list of all current packages associated with the given flavor.
     */
    abstract fun currentPackages(flavor: JHTFlavor): List<String>

    /**
     * The list of all know packages associated with this bundle. These are used during the
     * uninstall process.
     */
    abstract val allPackages: List<String>
}