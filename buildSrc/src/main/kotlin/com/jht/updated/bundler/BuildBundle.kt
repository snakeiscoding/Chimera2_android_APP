package com.jht.updated.bundler

/**
 * BuildBundle extends the standard bundle class and is used for bundles that are not just downloaded from S3 or the Z drive
 * Provides extra function to properly build and verify if they have already been staged to speed up build times.
 */
abstract class BuildBundle(packageName: String, id: String, baseFolder: String, updateType: UpdateType = UpdateType.General) : Bundle(packageName, id, baseFolder, updateType) {
    /**
     * Creates build extra build tasks required for
     * this bundle as it is a BuildBundle that needs to be built instead of just downloaded
     */
    abstract fun buildTasks(flavor: JHTFlavor): List<String>

}