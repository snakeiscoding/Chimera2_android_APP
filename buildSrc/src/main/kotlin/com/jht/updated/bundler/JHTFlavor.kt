package com.jht.updated.bundler

import com.jht.updated.bundler.CORE.CORE
import com.jht.updated.bundler.CORE.COREAssociation
import com.jht.updated.bundler.CORE.CORECollection
import com.jht.updated.bundler.CORE.CORESoftwareItem
import com.jht.updated.bundler.bundle.CONSOLE_APPS
import com.jht.updated.copyFile
import com.jht.updated.copyFileInto
import com.jht.updated.createDirectory
import com.jht.updated.get
import com.jht.updated.largeFileMD5
import com.jht.updated.md5
import com.jht.updated.remove
import com.jht.updated.zipFile
import com.jht.updated.zipFiles
import com.jht.aws.AwsSecrets.getSecret
import getGitInfo
import getLocalVersion
import org.gradle.api.Project
import runCommand
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The available JHT Flavors and relevant information.
 *
 * @constructor Default constructor.
 * @param product The product dimension.
 * @param ui The UI dimension.
 * @param platform The platform dimension.
 * @param equipment The string used for 'equipment' tag in manifest (some products share platform
 * but identify as different equipment).
 * @param releaseDir The relative path of the release directory for this flavor.
 * @param bundleVersion The latest (last or next) bundle version for this flavor.
 */
enum class JHTFlavor(val product: String, val ui: String, val platform: String, val equipment: String, val softwareType: String, val softwareConfigurationClass: String, val releaseDir: String,
                     val bundleVersion: String, val defaultCOREClub: List<Int>) {
    ChimeraMainMatrixChimera("chimera_main", "matrix", "chimera", "chimera", "chimera", "chimera", "Software-Releases/Console/Chimera",
        "7.14.1.0", listOf(1963)),
    Vision600eMatrixParker("vision600e", "matrix", "parker", "vision600e", "vision600e", "vision600e","Software-Releases/Console/Vision600e",
        "1.0.2", listOf()),
    ParkerMainMatrixParker("parker_main", "matrix", "parker", "parker", "parker", "parker","Software-Releases/Console/Parker",
        "1.9.1.13", listOf(1963)),
    T600xeMatrixParker("t600xe", "matrix", "parker", "t600xe", "t600xe", "t600xe","Software-Releases/Console/T600xe",
        "1.0.2", listOf(1963)),
    ChimeraMainMatrixLcgui("chimera_main", "matrix", "lcgui", "lowcostgui", "lcgui", "lowcostgui","Software-Releases/Console/LowCostGui",
        "1.6.0.5", listOf(1963)),
    ProjectoneMatrixChimera("projectone", "matrix", "chimera","projectone", "projectone", "projectone","Software-Releases/Console/ProjectOne",
        "2.9.1.0", listOf(1963));

    val flavor: String = "$product$ui$platform"
    val flavorCapitalized: String = "${product.capitalize()}${ui.capitalize()}${platform.capitalize()}"
    val flavorDir: String = "$product${ui.capitalize()}${platform.capitalize()}"
    val flavorAPK: String = "$product-$ui-$platform"

    /**
     * TODO: Just use "equipment"? Could release dir be replaced with path+equipment or does that
     *  need to remain flexible between flavors?
     */
    val subFolder: String = releaseDir.split("/").last()
    val awsSubFolder: String = subFolder.toLowerCase()

    private var nightlyVersion: String? = null

    /**
     * If nightlyVersion is null sets the nightly version to local version (e.x. YYYY.MM.DD.0.GIT_BRANCH.GIT_COMMIT).
     * This needs to be run before the nightly manifests are generated.
     */
    fun setNightlyVersion(rootDir: File) {
        if (nightlyVersion == null) {
            nightlyVersion = getLocalVersion(getGitInfo(rootDir))
        }
    }

    /**
     * The staging directory for the bundle release of this flavor.
     */
    fun bundleReleaseStagingDirectory(p: Project, isNightly: Boolean): File {
        if (isNightly && nightlyVersion != null) {
            return File("${p.buildDir.get(releaseDir)}/${nightlyVersion}")
        } else {
            return p.buildDir.get(releaseDir).get(bundleVersion)
        }
    }

    //Checks if a production release for this version already exists before building
    open fun preBuildCheck(rootDir: File, coreUser : String, corePassword: String) {
        println("Checking for existing release files in production.")
        val core = CORE(coreUser, corePassword)
        val swItem = CORESoftwareItem("","", softwareConfigurationClass, bundleVersion, "", "")
        val swCollection = CORECollection(softwareConfigurationClass, bundleVersion)
        core.login()

        when {
            CONSOLE_APPS.isVersionPublished(this) ->
                throw error("First Party Apps for ${bundleVersion} already published at ${CONSOLE_APPS.getAWSURL(this)}.")
            /* these might not be new versions between builds
            UPDATER.isVersionPublished(this) ->
                throw error("Updater for ${bundleVersion} already published at ${UPDATER.getAWSURL(this)}.")
            ASSETS.isVersionPublished(this) ->
                throw error("Assets for ${bundleVersion} already published at ${ASSETS.getAWSURL(this)}.")*/
            publishedOnAWS(AWS_CORE_BUCKET, "${awsSubFolder}/manifest/${bundleVersion}/update_ota.xml",AWS_PROD_REGION) ->
                throw error("Manifest for ${bundleVersion} already published at ${AWS_CORE_BUCKET}/${awsSubFolder}/manifest/${bundleVersion}/update_ota.xml.")
            publishedOnAWS(AWS_CORE_BUCKET, "${awsSubFolder}/mappings/${bundleVersion}/mapping.txt", AWS_PROD_REGION) ->
                throw error("Mapping file for ${bundleVersion} already published at ${AWS_CORE_BUCKET}/${awsSubFolder}/mappings/${bundleVersion}/mapping.txt.")
            ("git tag -l ${softwareConfigurationClass}_${bundleVersion}".runCommand(workingDir = rootDir) == "${softwareConfigurationClass}_${bundleVersion}") ->
                throw error("Git tag for ${softwareConfigurationClass}_${bundleVersion} already tagged.")
            //check for the capitalized version of the scc too
            ("git tag -l ${subFolder}_${bundleVersion}".runCommand(workingDir = rootDir) == "${subFolder}_${bundleVersion}") ->
                throw error("Git tag for ${subFolder}_${bundleVersion} already tagged.")
            (core.getReleasedSoftwareManifest(softwareConfigurationClass, defaultCOREClub[0], "prod") != null) ->
                throw error("Software deploy already published in CORE for ${softwareConfigurationClass} ${defaultCOREClub[0]} prod.")
            (CORECollection.checkExists(core, swCollection)) ->
                throw error("Software Collection already published in CORE for $softwareConfigurationClass $bundleVersion")
            (CORESoftwareItem.checkExists(core, swItem)) ->
                throw error("Software item already published in CORE for ${softwareConfigurationClass} ${bundleVersion}")
        }
        println("No files in production for ${softwareConfigurationClass} ${bundleVersion} Continuing build.")
    }

    /**
     * Uploads the manifest in the build/releaseDir/bundleVersion to the specified S3 bucket
     *
     * @param rootDir project root directory
     * @param s3Bucket S3 bucket for the file to be uploaded to, typically jhtsoftware or jht-nightly-builds
     * @param TPAVersionName new paramater defaulted to null used to be able to reuse this function to publish the TPA manifest
     * @param AWSRegion what AWS Region our bucket is in. Defaulted to region of JHTSoftware bucket
     */
    open fun publishManifest(rootDir : File, s3Bucket: String, isNightly: Boolean = false, TPAVersionName: String? = null, AWSRegion: String = AWS_PROD_REGION) {
        val installSize = 0
        val manifestVersion =
            if (isNightly && nightlyVersion != null) {
                nightlyVersion!!
            }
            else if (TPAVersionName != null) {
                TPAVersionName
            }
            else {
                this.bundleVersion
            }

        val manifestDir = File("${rootDir}/build/${this.releaseDir}/${manifestVersion}/")

        //iterate through the manifest directory
        for (file in manifestDir.walkTopDown()) {
            if (!file.isDirectory) {
                val manifestShortName = file.absolutePath.split(File.separator).last()
                val manifestKey = "${this.awsSubFolder}/manifest/${manifestVersion}/${manifestShortName}"
                val manifestMD5 = file.md5
                val metadata = "Content-MD5=${manifestMD5},Install-Size=${installSize},SourceMD5= "
                publishToAWS(s3Bucket, manifestKey, file, metadata = metadata, isNightly = isNightly, AWSRegion)
            }
        }
    }

    /**
     * Zips all bundles from bundles and the update.xml then uploads to S3 under the manifest folder
     *
     * @param rootDir project directory
     * @param bundles list of bundles to be zipped
     * @param s3Bucket S3 bucket to upload USB zip
     */
    open fun publishZip(rootDir: File, bundles: List<Bundle>, s3Bucket: String) {
        val zipPassword = getSecret("UPDATE_ZIP_PASSWORD")

        //zip up all bundles
        val flavorDir = File("${rootDir}/build/${this.releaseDir}".replace("/", File.separator))
        val USBDir = File("${flavorDir}/USB".replace("/", File.separator))
        USBDir.createDirectory()

        bundles.forEach {
            val bundleZip = File("${flavorDir}/${it.packageShortName}/${it.version(this)}/${it.packageShortName}.zip".replace("/", File.separator))
            if (bundleZip.exists()) {
                bundleZip.copyFile(File("${USBDir}/${it.packageShortName}.zip".replace("/", File.separator)))
            } else {
                throw error("publishZip: Missing ${bundleZip.path}")
            }
        }

        val manifestFile = File("${flavorDir}/${this.bundleVersion}/update.xml")
        if (manifestFile.exists()) {
            manifestFile.copyFileInto(File("${USBDir}"))
        } else {
            throw error("publishZip: Missing ${manifestFile.path}")
        }

        val USBFile = File("${flavorDir}/${this.bundleVersion}/USB.zip".replace("/", File.separator))
        USBDir.zipFiles(USBFile, zipPassword)

        //check on S3 for file
        val AWSFile = "${this.awsSubFolder}/manifest/${this.bundleVersion}/USB.zip"
        val AWSKey = "s3://${s3Bucket}/${AWSFile}"
        val installSize = USBFile.length()
        val USBMD5 = USBFile.largeFileMD5
        if (!publishedOnAWS(s3Bucket, AWSFile, AWS_PROD_REGION)) {
            //upload to S3Bucket
            println("""
                        Uploading USB Zip to S3
                        bucket:         ${s3Bucket}
                        USBZipKey:      ${AWSFile}
                        USBZipMD5:      ${USBMD5}
                        USBZipSize:     ${installSize}
                        USBFile:        ${USBFile.absolutePath}
                    """.trimIndent())
            Runtime.getRuntime()
                .exec(
                    "aws s3 cp ${USBFile.absolutePath} ${AWSKey} --acl public-read " +
                            "--metadata Content-MD5=${USBMD5},Install-Size=${installSize},SourceMD5= "
                )
                .run {
                    waitFor(10, TimeUnit.SECONDS)
                    inputStream.bufferedReader().use { it.readText() }
                }
        } else {
            throw error("publishZip: CONFLICT ${s3Bucket}/${this.awsSubFolder}/manifest/${this.bundleVersion}/USB.zip exists")
        }
    }


    /**
     * Method to publish up the proguard mapping.txt file for the main app
     *
     * @param rootDir Projects root directory used for creating file paths
     * @param s3Bucket bucket to upload to
     * @param isNightly bool for logic changes for nightly uploads
     * @param AWSRegion what AWS Region we our bucket is in. Defaulted to region of JHTSoftware bucket
     */
    fun publishMapping(rootDir : File, s3Bucket: String, isNightly: Boolean = false, AWSRegion: String = AWS_PROD_REGION) {
        val zipPassword = getSecret("UPDATE_ZIP_PASSWORD")
        val mappingFile = File("${rootDir}/Chimera/build/outputs/mapping/${this.flavorDir}Release/mapping.txt")
        val zippedMapping = File("${rootDir}/build/${this.releaseDir}/mapping/", "mapping.zip")

        if (isNightly && mappingFile.length() == 0L) {
            println("No usable mapping file at $mappingFile, skipping the mapping publish.")
            return
        }

        mappingFile.zipFile(dest = zippedMapping, password = zipPassword)

        //Create nightly versioning for s3Key
        val version = if (isNightly && nightlyVersion != null) {
            nightlyVersion!!
        } else {
            this.bundleVersion
        }
        val s3Key = "${this.awsSubFolder}/mappings/${version}/mapping.zip"

        if(zippedMapping.exists()) {
            println("Uploading mapping: ${zippedMapping} to S3 at $s3Key")
            publishToAWS(
                s3Bucket,
                s3Key,
                zippedMapping,
                isNightly = isNightly,
                isPublic = false,
                region = AWSRegion
            )
        }
        else {
            println("Mapping file not found at: $zippedMapping")
            throw Exception("No Mapping file");
        }
    }

    //TODO: This method should be moved outside JHTFlavor to a util class to standardize calls from both bundles and manifests to S3
    /**
     * Method to upload files to S3, returns if the file is successfully uploaded to S3 or not
     *
     * @param s3Bucket bucket to upload to
     * @param s3Key key for file to upload
     * @param file file to upload
     * @param metadata any metadata flags that are wanted to be added to the upload
     * @param isNightly
     * @param region what AWS region the bucket is in
     * @param isPublic should this item have public access from inside the bucket
     */
    private fun publishToAWS(s3Bucket: String, s3Key: String, file: File, metadata: String = "", isNightly: Boolean, region: String, isPublic: Boolean = true) : Boolean {
        if (!file.isDirectory) {
            //publish manifest if not in s3 bucket
            //or publish to the nightly bucket where we overwrite the previous release
            if (!publishedOnAWS(s3Bucket, s3Key, region) or (isNightly)) {
                println(
                    """
                        Uploading to S3
                        bucket:         ${s3Bucket}
                        S3Key:          ${s3Key}
                        metadata:       ${metadata}
                        upload:         ${file.absolutePath}
                    """.trimIndent()
                )
                println("File size: ${file.length()}")
                println("File exists: ${file.exists()}")

                val command = mutableListOf<String>("aws", "s3api", "put-object", "--bucket", "$s3Bucket",
                    "--key", "$s3Key", "--body", file.absolutePath, "--region", region)
                if(metadata != "") {
                    command.add("--metadata")
                    command.add("$metadata")
                }
                if(isPublic) {
                    command.add("--acl")
                    command.add("public-read")
                }

                command.forEach { i ->
                    print(i)
                    print(" ")
                }
                println()

                //upload to S3Bucket
                val result = ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor()
                println("exit: $result")

                return true
            } else {
                println("${s3Key} already in ${s3Bucket}, not uploading.")
            }
        }
        return false
    }

    /**
     * Return if a file exists on AWS
     *
     * @param s3Bucket The bucket where the object may exists
     * @param s3Key The key in the S3 bucket for the object
     * @param region the AWS region to look for if the object exists
     * @return true if the response from AWS is not empty
     */
    private fun publishedOnAWS(s3Bucket: String, s3Key: String, region: String): Boolean{
        val command = arrayOf("aws", "s3api", "head-object", "--bucket", s3Bucket, "--key", s3Key, "--region", region)
        val fileExists = Runtime.getRuntime()
                .exec(command)
                .run {
                    waitFor()
                    inputStream.bufferedReader().use { it.readText() }
                }.isEmpty()
        return !fileExists
    }


    /**
     * Publishes the Software Item for the given flavor as well as a Software Collection that is
     * deployed to all listed clubs in defaultCOREClub for a given flavor
     * @param coreUser user name for CORE build user
     * @param corePassword password for CORE build user
     * @param rootDir rootDir of project
     * @param TPAVersionName optional parameter to override the bundle manifest publication with the TPA bundle version. should only be provided with a TPA update
     */
    open fun publishCOREAssociation(coreUser: String, corePassword: String, rootDir: File, TPAVersionName: String? = null) {
        //If TPAVersionName is not null override the bundleVersion to publish the TPA manifest to CORE instead
        val bundleVersion = TPAVersionName ?: this.bundleVersion
        val manifestFile =  File("${rootDir}/build/${this.releaseDir}/${bundleVersion}/update_ota.xml")
        if(!manifestFile.exists()) {
            println("Manifest File Does Not Exist")
            throw FileNotFoundException("Manifest File ${manifestFile.absolutePath} Does Not Exist")
        }
        val associations: MutableList<COREAssociation> = mutableListOf()
        val s3URL = "https://core.aws-jhtna.net/"
        val s3Path = "${this.awsSubFolder}/manifest/${bundleVersion}/update_ota.xml"
        //Do not want to associate any clubs be default with a TPA Manifest at this time
        if(TPAVersionName == null) {
            for (club in defaultCOREClub) {
                associations.add(COREAssociation(club, true, false, false))
            }
        }
        val core = CORE(coreUser, corePassword)
        val softwareItem = CORESoftwareItem(s3URL + s3Path, manifestFile.md5, softwareType, bundleVersion, "", "") //Copying from FlavoredDeployBase.groovy should this last 2 be empty?
        core.deploy(softwareConfigurationClass, bundleVersion, associations, softwareItem)
    }

    /**
     * Method to grab manifest from CORE for the provided clubID and DapiEnv removing existing Third Party Apps and
     * Adding in the current third party apps on this branch. Will create this modified manifest,
     * publish it to S3 and create a CORE Association
     */
    fun createAndPublishTPAManifest(
        coreUser: String,
        corePassword: String,
        TPABundles: List<Bundle>,
        project: Project,
        clubID: Int = -1,
        dapiEnv: String = "prod"
    ) {
        println("clubID: $clubID, dapiEnv: $dapiEnv")
        val core = CORE(coreUser, corePassword)
        var releasedManifest = core.getReleasedSoftwareManifest(softwareConfigurationClass, clubID, dapiEnv)

        if(releasedManifest != null) {
            println("Manifest Found on CORE")
            println(releasedManifest)
            //Remove current Third Party Packages
            releasedManifest = ManifestMaker.removeTPAPackagesFromManifest(releasedManifest)


            //Add all TPABundles
            releasedManifest = ManifestMaker.addBundleGroupsToManifest(releasedManifest, ManifestMaker.getBundleGroups(TPABundles), project, this, ota = true)
            println("Manifest with new Third Party Packages")
            println(releasedManifest)

            val date = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyMMdd")
            val manifestVersion = "${getBundleVersionFromManifest("Apps", releasedManifest)}-TPA${date.format(formatter)}"
            releasedManifest = releasedManifest.replace("<$THIRD_PARTY_PACKAGE_GROUP>", "<$THIRD_PARTY_PACKAGE_GROUP version=\"$manifestVersion\">")
            createManifestFile(project, manifestVersion, releasedManifest)

            //Publish to S3
            publishManifest(project.rootDir, "jhtsoftware", TPAVersionName = manifestVersion)
            //Publish to CORE
            publishCOREAssociation(coreUser, corePassword, project.rootDir, manifestVersion)
        }
        else {
            println("No release found for the provided software type: $softwareType, club id: $clubID, and dapi: $dapiEnv")
            throw Exception("No release found for the provided software type: $softwareType, club id: $clubID, and dapi: $dapiEnv")
        }

    }

    private fun createManifestFile(project: Project, manifestVersion: String, releasedManifest: String): File {
        val manifestFileString = "${project.buildDir.get(this.releaseDir)}/$manifestVersion/update_ota.xml".replace("/", File.separator)

        println("file path: $manifestFileString")
        val manifestFile = File(manifestFileString)
        if(manifestFile.exists()) {
            manifestFile.remove()
        }
        manifestFile.parentFile?.mkdirs()
        manifestFile.createNewFile()
        manifestFile.writeText(releasedManifest)
        return manifestFile
    }

    /**
     * Parses A String XML Manifest and returns the combined version number as a string for the provided bundleName
     * @param bundleName bundle to get version for
     * @param manifest manifest to search for
     */
    private fun getBundleVersionFromManifest(bundleName: String, manifest: String): String {
        var lines = manifest.split('\n') as MutableList
        var ret = "Version_Not_Found"
        for (line in lines) {
            val name = line.substringAfter("name=\"").substringBefore('\"').lowercase(Locale.ROOT)
            if (name == bundleName.lowercase(Locale.ROOT)) {
                println("Bundle found")
                val major = line.substringAfter("major=\"").substringBefore('\"').lowercase(Locale.ROOT)
                val minor = line.substringAfter("minor=\"").substringBefore('\"').lowercase(Locale.ROOT)
                val patch = line.substringAfter("patch=\"").substringBefore('\"').lowercase(Locale.ROOT)
                val build = line.substringAfter("build=\"").substringBefore('\"').lowercase(Locale.ROOT)
                ret = "$major.$minor.$patch.$build"
            }
        }
        return ret
    }

    /**
     * Debugging string formatter for XML modifications used in TPT release
     */
    private fun publishTPAManifest(manifest: String) {

    }

    companion object {
        /**
         * Return whether a flavor exists with the given combination of dimensions.
         *
         * @param product The product dimension.
         * @param ui The UI dimension.
         * @param platform The platform dimension.
         * @return `true` if the combination exists, else `false`.
         */
        @Deprecated("Instead just check for non-null flavor.")
        fun exists(product: String, ui: String, platform: String): Boolean {
//            return values().firstOrNull {
//                // fixme: Not sure this should be ignoring case.
//                it.product.equals(product, true) && it.ui.equals(ui, true) && it.platform.equals(platform, true)
//            } != null
            return flavor(product, ui, platform) != null
        }

        /**
         * Return the flavor matching the combination of given dimensions. If no flavor exists then
         * return `null`.
         *
         * @param product The product dimension.
         * @param ui The UI dimension.
         * @param platform The platform dimension.
         * @return The matching flavor, else `null`.
         */
        fun flavor(product: String, ui: String, platform: String): JHTFlavor? =
            values().firstOrNull {
                // fixme: Not sure this should be ignoring case.
                it.product.equals(product, true) && it.ui.equals(ui, true) && it.platform.equals(platform, true)
            }

        /**
         * Used for Allure Reporting to find out what flavor is being built.
         * If unknown defaults to ChimeraMainMatrixChimera
         */
        fun getJHTFlavorFromFlavorCapitalized(flavor: String) : JHTFlavor {
            JHTFlavor.values().forEach { jhtFlavor ->
                if(flavor == jhtFlavor.flavorCapitalized)
                    return jhtFlavor
            }
            return ChimeraMainMatrixChimera
        }
    }
}