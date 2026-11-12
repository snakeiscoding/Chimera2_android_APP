package com.jht

/**
 * Helper class with links to a bunch of folders that contain source files for the deployment
 * packages. Also it contains the properties of the deployment.
 */
class JHTConfiguration {
    private static boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key)

        if (value != null && !value.isEmpty()) {
            return value.equalsIgnoreCase("true")
        }

        return defaultValue
    }

    private static final String chimeraLocationOnZDrive = "BuildAssets/Chimera"
    private static final String parkerLocationOnZDrive = "BuildAssets/Parker"
    private static final String maxHeadroomLocationOnZDrive = "BuildAssets/MaxHeadroom"
    private static final String projectOneLocationOnZDrive = "BuildAssets/projectone"
    private static final String propertiesFileName = "local.properties.versions"

    final File chimeraRootNetworkDir
    final File parkerRootNetworkDir
    final File maxHeadroomRootNetworkDir
    final File projectOneNetworkDir

    final File propertiesFile
    final File releaseDir
    final File rootDir
    final File buildAssetsDir
    final File sdkDir
    final JHTFlavor flavor

    final Properties deploymentVersions = new Properties()
    final Properties localProperties = new Properties()
    boolean disableAWS = false
    boolean deleteStagingDir = true

    JHTConfiguration(JHTFlavor flavor, File rootDir) {
        this.flavor = flavor

        // if root dir is null, this is only an information only construction
        if (rootDir == null) {
            println("!! JHTConfiguration has rootDir null, information only construction")
            this.rootDir = new File("/fake/folder/is/not/used")
            chimeraRootNetworkDir = this.rootDir
            parkerRootNetworkDir = this.rootDir
            maxHeadroomRootNetworkDir = this.rootDir
            projectOneNetworkDir = this.rootDir
            propertiesFile = this.rootDir
            releaseDir = this.rootDir
            buildAssetsDir = this.rootDir
            sdkDir = this.rootDir
            return
        }
        this.rootDir = rootDir

        try {
            new File(rootDir, "local.properties").withDataInputStream {stream -> localProperties.load(stream) }
        } catch (Exception ex) {
            ex.printStackTrace()
        }

        buildAssetsDir = new File((String)localProperties.getProperty("buildAssets"))

        sdkDir = localProperties.getProperty("sdk.dir") == null ? null : new File(localProperties.getProperty("sdk.dir"))
        String releasePath = localProperties.getProperty("releasePath")

        if (releasePath != null && !releasePath.isEmpty()) {
            this.releaseDir = new File(new File(releasePath), flavor.releaseDir())
        } else {
            this.releaseDir = new File(buildAssetsDir, flavor.releaseDir())
        }

        if (!releaseDir.exists()) {
            releaseDir.mkdirs()
        }

        disableAWS = getBooleanProperty(localProperties, "disableAWS", disableAWS)
        deleteStagingDir = getBooleanProperty(localProperties, "deleteStagingDir", deleteStagingDir)

        chimeraRootNetworkDir = new File(buildAssetsDir, chimeraLocationOnZDrive)
        parkerRootNetworkDir = new File(buildAssetsDir, parkerLocationOnZDrive)
        maxHeadroomRootNetworkDir = new File(buildAssetsDir, maxHeadroomLocationOnZDrive)
        projectOneNetworkDir = new File(buildAssetsDir, projectOneLocationOnZDrive)
        propertiesFile = new File(releaseDir, propertiesFileName)

        if (propertiesFile.exists()) {
            try {
                propertiesFile.withDataInputStream {stream -> deploymentVersions.load(stream)}
            } catch (Exception ignored) {}
        } else {
            try {
                propertiesFile.createNewFile()
            } catch (Exception ignored) {}
        }
    }
}
