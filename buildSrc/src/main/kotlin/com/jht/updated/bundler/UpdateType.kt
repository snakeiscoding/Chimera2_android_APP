package com.jht.updated.bundler

/**
 * The values supported by `type` in the update manifest.
 */
enum class UpdateType(val text: String) {
    /** For bundles with no special behavior. */
    General("general"),
    /** For an OS package. ***WARNING:** Not meant for all OS packages.* */
    OS("os"),
    /** For the software updater package. */
    SoftwareUpdater("software_updater"),
    /** For packages that reboot the console on their own, not using required reboot. */
    Rebooting("rebooting");

    override fun toString(): String = text
}