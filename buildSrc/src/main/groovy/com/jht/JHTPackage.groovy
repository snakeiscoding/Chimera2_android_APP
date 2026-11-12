package com.jht

import com.jht.filemanagement.AWSHelper
import com.jht.filemanagement.AWSPackageObject
import com.jht.filemanagement.FileManagement

class JHTPackage {
    private String name
    private String id
    private String version
    private boolean requiresReboot
    private boolean manualReboot
    private String packageShortName
    private ArrayList<File> srcFolder
    private ArrayList<File> codeFolder
    private File releaseFolder
    private String updateType
    private int order
    private long installSize
    private boolean force
    private boolean forceDowngrade
    private boolean forceRecreate
    public String targetConsole = null
    private JHTConfiguration configuration
    private int type
    private String sourceMD5
    private String packageMD5

    JHTPackage(int type, String name, String id, boolean requiresReboot, boolean manualReboot, ArrayList<File> srcFolder, String updateType, JHTConfiguration configuration) {
        this.name = name
        this.packageShortName = name.replace(" ", "_").toLowerCase()
        this.id = id
        this.requiresReboot = requiresReboot
        this.manualReboot = manualReboot
        this.srcFolder = srcFolder
        this.codeFolder = null
        this.configuration = configuration
        this.updateType = updateType
        this.type = type
    }

    String getName() {
        return name
    }

    String getShortName() {
        return packageShortName
    }

    String getId() {
        return id
    }

    String getVersion() {
        return version
    }

    boolean getRequiresReboot() {
        return requiresReboot
    }
    void setRequiresReboot(boolean needsReboot) {
        requiresReboot = needsReboot
    }

    void addCodeFolder(File root, List<String> codePath) {
        if (codePath != null) {
            if (this.codeFolder == null) this.codeFolder = new ArrayList<>()
            for (String p : codePath) {
                this.codeFolder.add(new File(root, p))
            }
        }
    }

    int type() {
        return type
    }

    void setForceRecreate(boolean forceRecreate) {
        this.forceRecreate = forceRecreate
    }

    void setForce(boolean force) {
        this.force = force
    }

    void setForceDowngrade(boolean force) {
        this.forceDowngrade = force
    }

    String computeSrcMD5() {
        sourceMD5 = codeFolder != null ? FileManagement.getMD5(codeFolder) : FileManagement.getMD5(srcFolder)
        return sourceMD5
    }

    void setSourceMD5(String md5) {
        sourceMD5 = md5
    }

    void setVersion(String version) {
        this.version = version
        this.releaseFolder = new File(configuration.releaseDir, "${packageShortName}/${version}")
    }

    void setReleaseFolder(File folder) {
        releaseFolder = folder
    }

    void setInstallSize(long size) {
        installSize = size
    }

    void order(int order) {
        this.order = order
    }

    static boolean isComputedVersion(String version) {
        // should this be anything less than 4, or should there be a special flag of some kind
        return (version.split("\\.").length < 4)
    }

    long computeInstallSize(boolean throwOnEmpty) {
        installSize = 0
        for (File src : srcFolder) {
            if (throwOnEmpty) {
                if (!src.exists() || (src.isDirectory() && src.listFiles().length == 0)) {
                    println("Package ${name} has an empty source folder $src")
                    throw new Exception("Package ${name} has an empty source folder $src")
                }
            }
            installSize += FileManagement.getFolderSize(src)
        }
        return installSize
    }

    void validatePackage() {
        for(File src: srcFolder) {
            println("Checking Package $name ${src.name}")
            if(!src.exists() || (src.isDirectory() && src.listFiles().length == 0)) {
                println("Package ${name} has an empty source folder $src")
                throw new Exception("Package ${name} has an empty source folder $src")
            }
        }
    }

    void createPackage(String platform, boolean isNightly = false, String zipPassword = null) {
        String AWSBucket = ""
        if (isNightly) {
            AWSBucket = "jht-nightly-builds"
        } else {
            AWSBucket = "jhtsoftware"
        }

        //Create AWS package for uploading later
        AWSPackageObject awsObject = null
        println("## createPackage " + shortName + " sourceMD5=" + sourceMD5 + " installSize=" + installSize)
        if (!configuration.disableAWS) {
            awsObject = AWSPackageObject.getInfoIfExists(AWSBucket, getAWSKey(platform))
            if (awsObject != null) {
                println(shortName + " AWS check " + awsObject.getMetaString())
                // we no longer get a source MD5 from previous to compare and putObjectIfNotExists will not upload new zip if old one exists anyway unless forced
//                if (!forceRecreate && awsObject.sourceMd5Match(sourceMD5)) {
                if (!forceRecreate) {
                    // No need to do anything.  Package has already been created and exists.
//                    println(shortName + " not uploading as source MD5 matches")
                    packageMD5 = awsObject.packageMD5()
                    println(shortName + " not uploading as it already exists, reuse " + packageMD5)
                    return
                }
            }
        }

        File zipFile = new File(releaseFolder, "${this.packageShortName}.zip")
        if (forceRecreate || !zipFile.exists()) {
            println(shortName + " createPackage needs to build zip file")
            computeInstallSize(true)

            File stagingFolder = new File(releaseFolder, "staging")
            stagingFolder.mkdirs()

            for (File src : srcFolder) {
                if (src.isFile()) {
                    FileManagement.copy(src, new File(stagingFolder, src.name))
                } else {
                    FileManagement.copy(src, stagingFolder)
                }
            }

            FileManagement.zipFiles(stagingFolder, zipFile.absolutePath, zipPassword)
            if (configuration.deleteStagingDir) {
                stagingFolder.deleteDir()
            }
        } else if (installSize <= 0) {
            println(shortName + " createPackage needs to recompute size")
            computeInstallSize(true)
        }

        if (forceRecreate && awsObject != null && !awsObject.md5Match(FileManagement.getMD5(zipFile))) {
            AWSHelper.putObject(AWSBucket, getAWSKey(platform), zipFile, sourceMD5, installSize, configuration.disableAWS)
        } else {
            AWSHelper.putObjectIfNotExists(AWSBucket, getAWSKey(platform), zipFile, sourceMD5, installSize, configuration.disableAWS)
        }
    }

    String getManifestXML(String awsSubFolder, boolean ota, boolean isNightly) {
        File zipFile = new File(releaseFolder, "${this.packageShortName}.zip")
        // FIXME(ARR) - Convert to using split(".")
        String[] versionNumbers = this.version.tokenize(".")
        String md5 = zipFile.exists() ? FileManagement.getMD5(zipFile) : packageMD5
        String reboot = this.requiresReboot ? "true" : "false"
        String manualReboot = this.manualReboot ? "true" : "false"

        StringBuilder strTags = new StringBuilder()
        strTags.append("order=\"${this.order}\" ")
        strTags.append("name=\"${this.name}\" ")
        strTags.append("file=\"${packageShortName}.zip\" ")

        if (ota) {
            strTags.append("file-url=\"${getAWSURL(awsSubFolder, isNightly)}\" ")
        }

        if (force) {
            strTags.append("force=\"true\" ") // always installs the package
        } else if (forceDowngrade) {
            strTags.append("force_downgrade=\"true\" ") // only installs if versions are different
        }

        strTags.append("install_size=\"${this.installSize}\" ")
        strTags.append("major=\"${versionNumbers[0]}\" ")
        strTags.append("minor=\"${versionNumbers[1]}\" ")
        strTags.append("patch=\"${versionNumbers[2]}\" ")
        strTags.append("build=\"${versionNumbers[3]}\" ")
        strTags.append("update_id=\"${this.id}\" ")
        strTags.append("md5=\"$md5\" ")
        strTags.append("type=\"$updateType\" ")
        strTags.append("requires_reboot=\"$reboot\" ")
        strTags.append("manual_reboot=\"$manualReboot\" ")

        if (targetConsole != null) {
            strTags.append("target_console=\"$targetConsole\" ")
        }

        return "<update ${strTags.toString()}/>"
    }

    String getJSON() {
        StringBuilder json = new StringBuilder()
        json.append("\t\t\t\t{\n")
        json.append("\t\t\t\t\t\"packageName\": \"").append(name).append("\",\n")
        json.append("\t\t\t\t\t\"packageUUID\": \"").append(id).append("\",\n")
        json.append("\t\t\t\t\t\"packageVersion\": \"").append(version).append("\"\n")
        json.append("\t\t\t\t}")
        return json.toString()
    }

    String getAWSURL(String awsSubFolder, boolean isNightly) {
        if (isNightly) {
            return "https://jht-nightly-builds.s3.us-east-2.amazonaws.com/${getAWSKey(awsSubFolder)}"
        }
        return "https://core.aws-jhtna.net/${getAWSKey(awsSubFolder)}"
    }

    String getAWSKey(String equipment) {
        return "${equipment}/${packageShortName}/${version}/${packageShortName}.zip"
    }

    File getPackageLocation() {
        return new File(releaseFolder, "${this.packageShortName}.zip")
    }

    String getDownloadShellScript(String awsSubFolder, boolean isNightly) {
        String awsURL = this.getAWSURL(awsSubFolder, isNightly)
        StringBuilder script = new StringBuilder()
        script.append("\tif [ -z \"\$Z_DRIVE\" ]; then\n")
        script.append("\t\techo \"Downloading ${awsURL}\"\n")
        script.append("\t\twget -P . ${awsURL}\n")
        script.append("\telse\n")
        script.append("\t\tcp -v \"\${Z_DRIVE}/${configuration.flavor.releaseDir()}/${this.packageShortName}/${this.version}/${this.packageShortName}.zip\" .\n")
        script.append("\tfi\n")
        return script.toString()
    }


    String getDownloadBatchScript(String awsSubFolder, boolean isNightly) {
        StringBuilder script = new StringBuilder()
        script.append("if defined Z_DRIVE (\r\n")
        script.append("\tcopy \"%Z_DRIVE%\\${configuration.flavor.releaseDir().replace('/', '\\')}\\${this.packageShortName}\\${this.version}\\${this.packageShortName}.zip\" .\r\n")
        script.append(") else (\r\n")
        script.append("\techo Downloading ${this.getAWSURL(awsSubFolder, isNightly)}\r\n")
        script.append("\twget -P . ${this.getAWSURL(awsSubFolder, isNightly)}\r\n")
        script.append(")\r\n")
        return script.toString()
    }

    List<File> getSourceFiles() {
        return srcFolder
    }
}
