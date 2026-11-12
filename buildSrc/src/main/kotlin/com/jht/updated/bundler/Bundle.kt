package com.jht.updated.bundler

import com.jht.updated.copyInto
import com.jht.updated.createFile
import com.jht.updated.download
import com.jht.updated.exists
import com.jht.updated.get
import com.jht.updated.md5
import com.jht.updated.numBytes
import com.jht.updated.remove
import com.jht.updated.toURL
import com.jht.updated.unzipFile
import com.jht.updated.zipFiles
import com.jht.aws.AwsSecrets.getSecret
import org.gradle.api.Project
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Base class for a bundle. Each Individual bundle (apps, assets, os, etc) all implement this base class
 * to share basic bundle logic for downloading/staging, manifest generation, and publication
 *
 * @constructor The default constructor.
 * @param packageName The human-readable name to display for this package during installation.
 * @param id The UUID for the package being bundled here. The system associates the versions to this
 * number. **Change this value with caution.**
 * @param baseFolder The base folder from Deploy containing the code to include during deployment.
 * This should only be `null` for the custom TPA cleanup task that generates all required sources.
 * @param updateType The value to provide for `type` in the update manifest (see [UpdateType] for
 * more information). Default value is [UpdateType.General].
 */
abstract class Bundle(
    private val packageName: String,
    private val id: String,
    val baseFolder: String? = null,
    private val updateType: UpdateType = UpdateType.General,
    private val packageGroup: String = CORE_PACKAGE_GROUP,
    var isForced: Boolean = false,
    var isForcedDowngrade: Boolean = false
) {
    /**
     * The value to provide for `requires_reboot` in the update manifest.
     *
     * `requires_reboot` &ndash; Reboots the console after the package has been installed.
     *
     * Can be either `true` or `false`. Defaults to `false`.
     */
    open val requiresReboot: Boolean = false

    /**
     * The value to provide for `manual_reboot` in the update manifest.
     *
     * `manual_reboot` &ndash; Tells the console to require a manual power cycle of the console.
     * This if used for LCB updates that require a power cycle to correctly work.
     *
     * Can be either `true` or `false`. Defaults to `false`.
     */
    open val manualReboot: Boolean = false

    /**
     * The value to provide for `target_console` in the update manifest.
     *
     * `target_console` &ndash; Can be used to only allow the update package to be installed
     *  when the target console type matches.
     *
     * An empty string or value "all" will allow the update package to be installed on all consoles.
     * Any other values will be checked against the console type list defined getConsoleType()
     * implementation.
     */
    open val targetConsole: String = ""

    /**
     * This is also referred to just as the shortName.
     */
    val packageShortName = packageName.replace(" ", "_").lowercase()


    fun getPackageGroup(): String { return packageGroup; }

    /**
     * The parent directory for the bundle and release staging directories.
     *
     * @param p The given project.
     * @param f The given flavor.
     * @return The staging directory.
     */
    fun stagingDirectory(p: Project, f: JHTFlavor): File = p.buildDir.get(f.releaseDir).get(packageShortName)

    /** The staging directory for the content to release. */
    fun releaseStagingDir(p: Project, f: JHTFlavor): File = stagingDirectory(p, f).get(version(f))

    /**
     * The staging directory for the bundle content used to create the release staging. This is
     * what can be deleted.
     */
    fun bundleStagingDir(p: Project, f: JHTFlavor): File = stagingDirectory(p, f).get("staging")

    /** The staging directory for the manifest to combine with the release. */
    fun manifestStagingDir(p: Project, f: JHTFlavor): File = stagingDirectory(p, f).get("manifest")

    fun stagingZipFile(project: Project, flavor: JHTFlavor): File =
        File(releaseStagingDir(project, flavor), "$packageShortName.zip")

    /**
     * Return the installation size in bytes. The installation size is the sum of all files in the
     * staging directory for a given bundle.
     */
    fun computeInstallSize(project: Project, flavor: JHTFlavor, errorOnEmptyDir: Boolean = true): Long {
        return bundleStagingDir(project, flavor).listFiles()?.asList()?.numBytes(errorOnEmptyDir) ?: error("unexpected file size.")
    }

    /**
     * Whether this bundle must always be installed or not. By default this is `false` for all
     * flavors.
     */
    fun force(): Boolean {
        return isForced
    }

    open fun forceDowngrade(flavor: JHTFlavor): Boolean {
        return isForcedDowngrade
    }

    /**
     * Output the information about this specific update type.
     */
    fun outputInformation(project: Project, flavor: JHTFlavor) {
        println("""
            Outputting task information:
            =============================================================
            name:               $packageName
            shortName:          $packageShortName
            uuid:               $id
            base_folder:        ${baseFolder ?: "null"}
            updateType:         $updateType
            reboot:             $requiresReboot
            manualReboot:       $manualReboot
            targetConsole:      $targetConsole

            Calculated values:
            -------------------------------------------------------------
            stagingDir:         ${stagingDirectory(project, flavor).absolutePath}
            bundleStagingDir:   ${bundleStagingDir(project, flavor).absolutePath}
            releaseStagingDir:  ${releaseStagingDir(project, flavor).absolutePath}
            zipFile:            ${stagingZipFile(project, flavor).absolutePath}
            installSize:        ${computeInstallSize(project, flavor, true)}
            sources:            ${getAllSources(project, flavor).joinToString("\n") { it.absolutePath }}
        """.trimIndent())
    }

    /**
     * Whether a previously published version of this Bundle can be used. If `false`, an exception
     * needs to be thrown if there is already a published version. If `true`, staging must use the
     * existing published version to prevent issues during updating and to ensure the manifests are
     * accurately maintained.
     *
     * The default behavior is to allow re-use. If not allowed, this needs to be set to false.
     */
    open val allowReuse: Boolean get() = true

    /**
     * Verify that this bundle is published on AWS in JHTSoftware for the given flavor
     * @return true if exists, else false
     */
    private fun publishedOnAWS(f: JHTFlavor): Boolean = getAWSURL(f).toURL().exists()

    /**
     * Return whether the release zip file exists for this version of this bundle for this flavor.
     * Checks AWS for an existing publication
     */
    fun isVersionPublished(f: JHTFlavor): Boolean {
        return publishedOnAWS(f)
    }

    /**
     * Moves needed files for a release into the build/$releaseDir folder.
     * If a file is already published this will download the release from S3.
     * If file is not in S3 this will move the source files provided by the getAllSources() function.
     *
     * @param forceStageFromSource boolean that forces the staging task to use the source files (this is helpful for bundles like CONSOLE_APPS)
     */
    fun stageBundle(project: Project, flavor: JHTFlavor, forceStageFromSource: Boolean = false) {
        println("Staging the bundle for $packageShortName")

        // If fully staged, then skip the remaining steps.
        if (fullyStaged(project, flavor)) {
            println("> $packageShortName already fully staged.")
            return
        }

        // Remove the previously published bundle staging directory.
        println("> Removing the previous staging directory")
        bundleStagingDir(project, flavor).remove()

        if (!forceStageFromSource && isVersionPublished(flavor)) {
            stageBundleFromReleaseFiles(project, flavor)
        } else {
            stageBundleFromSource(project, flavor)
        }
    }

    /**
     * Stage the bundle using the released files.
     */
    private fun stageBundleFromReleaseFiles(project: Project, flavor: JHTFlavor) {
        println("Staging using a released bundle.")

        //Prioritize AWS Released files
        if (publishedOnAWS(flavor)) {
            println("> Downloading existing release zip file from AWS...")
            getAWSURL(flavor).toURL().download(stagingZipFile(project, flavor))
        }
        else {
            throw error("Staging bundle failed; existing release zip file not found in AWS.")
        }

        println("> Fetching zip password...")
        val zipPassword = getSecret("UPDATE_ZIP_PASSWORD")

        println("> Extracting the release file...")
        stagingZipFile(project, flavor).unzipFile(
            dest = bundleStagingDir(project, flavor),
            password = zipPassword
        )

        println("> Create the manifest files...")
        createManifest(project, flavor, false)
        createManifest(project, flavor, true)

        if (DELETE_STAGING_DIRECTORIES) {
            println("> Deleting the staging directories...")
            bundleStagingDir(project, flavor).remove()
        }
    }

    /**
     * Stage the bundle using the source files.
     */
    private fun stageBundleFromSource(project: Project, flavor: JHTFlavor) {
        println("> Fetching zip password...")
        val zipPassword = getSecret("UPDATE_ZIP_PASSWORD")

        println("> Copying the source files...")
        getAllSources(project, flavor).copyInto(bundleStagingDir(project, flavor))

        println("> Creating the release zip file...")
        bundleStagingDir(project, flavor).zipFiles(stagingZipFile(project, flavor), zipPassword)
        
        println("> Creating the manifest files...")
        createManifest(project, flavor, false)
        createManifest(project, flavor, true)

        if (DELETE_STAGING_DIRECTORIES) {
            println("> Deleting the staging directories...")
            bundleStagingDir(project, flavor).remove()
        }
    }

    /**
     * Return all sources for a bundle. **Every** bundle has the files from it's base directory. All
     * other files must be specified in [getSources].
     */
    fun getAllSources(project: Project, flavor: JHTFlavor): List<File> {
        val result = if (baseFolder != null) mutableListOf(project.projectDir.get("Deploy/$baseFolder")) else mutableListOf()
        getSources(project, flavor)?.forEach { result.add(it) }
        return result
    }

    /**
     * This is not meant to be called by anything other than [getAllSources]. Protected abstract
     * function each bundle must implement to retrieve all sources required for the bundle when staging.
     * Returns null if no sources needed to be downloaded for the given bundle for the provided flavor
     */
    protected abstract fun getSources(project: Project, flavor: JHTFlavor): List<File>?

    /**
     * Uploads bundle based off Project and Flavor to the S3 bucket jhtsoftware
     * Bundle needs to be staged before upload
     * Will NOT override an existing file in S3, if already published will do nothing
     *
     * @param project the module/bundle that is going to be uploaded
     * @param flavor the flavor of the bundle that is going to be uploaded
     * @param sourceMD5 metadata for built modules such as the updater, left blank by default
     */
    @JvmOverloads
    open fun publishBundle(project: Project, flavor: JHTFlavor, sourceMD5: String = "") {
        val s3Bucket = "jhtsoftware"
        val s3Key = getAWSKey(flavor)
        val zipFile = stagingZipFile(project, flavor)
        val md5 = zipFile.md5
        val installSize = computeInstallSize(project, flavor)

        println("""
            Uploading to S3
            bucket:         ${s3Bucket}
            key:            ${s3Key}
            md5:            ${md5}
            source-md5:     ${sourceMD5}
            install-size:   ${installSize}
            file:           ${zipFile.absolutePath}
        """.trimIndent())

        if (!publishedOnAWS(flavor)) {
            Runtime.getRuntime()
                .exec(
                    "aws s3api put-object --bucket ${s3Bucket} --key ${s3Key} --acl public-read " +
                            "--metadata Content-MD5=${md5},Install-Size=${installSize},SourceMD5=${sourceMD5} " +
                            "--body ${zipFile.absolutePath}"
                )
                .run {
                    waitFor()
                    inputStream.bufferedReader().use { it.readText() }
                }
            println("Upload complete!")
        }
        else {
            println("bundle: ${this.packageShortName} already exists on S3 at $s3Bucket/$s3Key")
        }

    }

    /**
     * Uploads bundle based off Project and Flavor to the S3 bucket jht-nightly-builds
     * Bundle needs to be staged before upload
     *
     * @param project the module/bundle that is going to be uploaded
     * @param flavor the flavor of the bundle that is going to be uploaded
     * @param sourceMD5 metadata for built modules such as the updater, left blank by default
     */
    @JvmOverloads
    open fun publishNightlyBundle(project: Project, flavor: JHTFlavor, sourceMD5: String = "") {
        val s3Bucket = "jht-nightly-builds"
        val s3Key = getAWSKey(flavor)
        val zipFile = stagingZipFile(project, flavor)
        val md5 = zipFile.md5
        val installSize = computeInstallSize(project, flavor)

        val md5Match = Runtime.getRuntime()
            .exec("aws s3api head-object --bucket ${s3Bucket} --key ${s3Key}")
            .run {
                waitFor()
                inputStream.bufferedReader().use { it.readText() }
            }.contains(md5)

        if (!md5Match) {
            println("""
            Uploading to S3
            bucket:         ${s3Bucket}
            key:            ${s3Key}
            md5:            ${md5}
            source-md5:     ${sourceMD5}
            install-size:   ${installSize}
            file:           ${zipFile.absolutePath}
            """.trimIndent())

            Runtime.getRuntime()
                .exec(
                    "aws s3api put-object --bucket ${s3Bucket} --key ${s3Key} --acl public-read " +
                            "--metadata Content-MD5=${md5},Install-Size=${installSize},SourceMD5=${sourceMD5} " +
                            "--body ${zipFile.absolutePath}"
                )
                .run {
                    waitFor()
                    inputStream.bufferedReader().use { it.readText() }
                }
            println("Upload complete!")
        } else {
            println("MD5 match in ${s3Bucket}/${s3Key}, skipping upload.")
        }
    }

    /**
     * Get the version of this bundle for this specific flavor.
     */
    abstract fun version(flavor: JHTFlavor): String

    /**
     * Overrides the version of a bundle.
     * This function needs to be overridden in the individual bundle
     */
    open fun setVersion(flavor: JHTFlavor, newVersion: String) { }

    /**
     * Return whether this bundle is supported for USB only or not. This is used during manifest
     * creation only.
     */
    abstract fun usbOnly(flavor: JHTFlavor): Boolean

    /**
     * The list of flavors that need to support this bundle.
     */
    abstract val supportedFlavors: List<JHTFlavor>

    /**
     * Return the file pointer to the manifest file for the given inputs.
     */
    private fun getManifestFile(project: Project, flavor: JHTFlavor, ota: Boolean): File =
        manifestStagingDir(project, flavor).get(if (ota) "update_ota.xml" else "update.xml")

    /**
     * Create a manifest file for this bundle.
     * @param flavor the flavor for this bundle.
     * @param ota `true` if we should add ota links.
     * @return return the created manifest file.
     */
    fun createManifest(project: Project, flavor: JHTFlavor, ota: Boolean): File {
        val manifestFile = getManifestFile(project, flavor, ota)
        manifestFile.createFile(ManifestMaker.createManifest(project, flavor, this, ota))
        return manifestFile
    }

    /**
     * Return whether this bundle is fully staged. The general logic for determining whether this
     * bundle is fully staged is whether the manifest files have been created. These files are
     * created last, which requires all stages that could fail to have completed successfully.
     *
     * **NOTE:** For a build bundle, this must always be `false` to ensure the build is constantly
     * being re-run (provided the build version hasn't already been published).
     *
     * @param project The project.
     * @param flavor The current flavor.
     * @return `true` if fully staged, else `false`.
     */
    open fun fullyStaged(project: Project, flavor: JHTFlavor): Boolean =
        getManifestFile(project, flavor, false).isFile &&
                getManifestFile(project, flavor, true).isFile


    /**
     * Method to generate the proper update XML string for this bundle to be added to an update manifest.
     */
    fun getManifestXML(project: Project, flavor: JHTFlavor, order: Int, ota: Boolean, isNightly: Boolean): String {
        val zipFile = stagingZipFile(project, flavor)
        val fileMD5 = zipFile.md5
        val versionNumbers = version(flavor).split(".")

        val strTags = StringBuilder()
        strTags.append("order=\"$order\" ")
        strTags.append("name=\"$packageName\" ")
        strTags.append("file=\"$packageShortName.zip\" ")

        //file-url is only added to update_ota.xml, not update.xml files
        if (ota)
            strTags.append("file-url=\"${getAWSURL(flavor, isNightly)}\" ")

        //Only add the force and force_downgrade tags if they are set true
        if (force()) {
            strTags.append("force=\"true\" ") // always installs the bundle.
        } else if (forceDowngrade(flavor)) {
            strTags.append("force_downgrade=\"true\" ") // only installs if versions are different.
        }

        strTags.append("install_size=\"${computeInstallSize(project, flavor)}\" ")
        strTags.append("major=\"${versionNumbers[0]}\" ")
        strTags.append("minor=\"${versionNumbers[1]}\" ")
        strTags.append("patch=\"${versionNumbers[2]}\" ")
        strTags.append("build=\"${versionNumbers[3]}\" ")
        strTags.append("update_id=\"$id\" ")
        strTags.append("md5=\"${fileMD5}\" ")
        strTags.append("type=\"$updateType\" ")
        strTags.append("requires_reboot=\"${if (requiresReboot) "true" else "false"}\" ")
        if (!targetConsole.isEmpty()) strTags.append("target_console=\"$targetConsole\" ")
        strTags.append("manual_reboot=\"${if (manualReboot) "true" else "false"}\" ")

        return "<update $strTags/>"
    }

    /**
     * The URL where the file exists at.
     */
    fun getAWSURL(flavor: JHTFlavor, isNightly: Boolean = false): String {
        if (isNightly) {
             return "https://doore.aws-jhtna.net/${getAWSKey(flavor)}"
        } else {
            return "https://core.aws-jhtna.net/${getAWSKey(flavor)}"
        }
    }

    fun getAWSKey(flavor: JHTFlavor): String =
        "${flavor.awsSubFolder}/$packageShortName/${version(flavor)}/$packageShortName.zip"

    /**
     * Downloads file from key if s3 sync is successful
     * Checks if download already exists at outfile before downloading
     * throws Exception if key is not found on S3
     *
     * @param key S3 URI
     * @param outfile local directory for storing file
     * @return File from S3 sync command
     */
    fun getAWSFile(key: String, outfile: String): File {
        val fileExists = Runtime.getRuntime().exec("aws s3 ls ${key}").run {waitFor()
            inputStream.bufferedReader().use { it.readText() }}.isNotEmpty()
        if (fileExists) {
            println("Downloading ${key} to ${outfile}")
            Runtime.getRuntime().exec("aws s3 sync ${key} ${outfile}").run {
                waitFor(10, TimeUnit.SECONDS)
                inputStream.bufferedReader().use { it.readText() }
            }
            return File(outfile)
        } else {
            throw Exception("${key} not found")
        }
    }
}