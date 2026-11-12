package com.jht

/**
 * Helper class to manage the package version in the local properties file.
 */
class PackageVersionHelper {
    private String version = ""
    private String build = ""
    private String prefix = ""
    private String md5 = ""
    private String installSize = ""

    private String versionField
    private String buildField
    private String prefixField
    private String md5Field
    private String installSizeField

    PackageVersionHelper(JHTFlavor f, String SDC, JHTPackage thePackage, Properties properties) {
        versionField = "${f.flavor()}_${SDC}_${thePackage.getShortName()}"
        prefixField = "${f.flavor()}_${SDC}_${thePackage.getShortName()}_prefix"
        buildField = "${f.flavor()}_${SDC}_${thePackage.getShortName()}_build"
        md5Field = "${f.flavor()}_${SDC}_${thePackage.getShortName()}_md5"
        installSizeField = "${f.flavor()}_${SDC}_${thePackage.getShortName()}_size"
        load(properties)
    }

    void load(Properties properties) {
        version = properties.getProperty(versionField)
        prefix = properties.getProperty(prefixField)
        build = properties.getProperty(buildField)
        md5 = properties.getProperty(md5Field)
        installSize = properties.getProperty(installSizeField)

        if (version == null) version = "0.0.0.0"
        if (prefix == null) prefix = "1.0.0"
        if (build == null) build = ""
        if (md5 == null) md5 = ""
        if (installSize == null) installSize = ""
    }

    void save(Properties properties) {
        if (prefix != "" && build != "") version = "${prefix}.${build}"
        properties.setProperty(versionField, version)
        properties.setProperty(prefixField, prefix)
        properties.setProperty(buildField, build)
        properties.setProperty(md5Field, md5)
        properties.setProperty(installSizeField, installSize)
    }

    void saveIgnoreEmpty(Properties properties) {
        if (prefix != null && !prefix.isEmpty() && build != null && !build.isEmpty()) {
            version = "${prefix}.${build}"
            properties.setProperty(prefixField, prefix)
            properties.setProperty(buildField, build)
        }
        if (version != null && !version.isEmpty()) properties.setProperty(versionField, version)
        if (md5 != null && !md5.isEmpty()) properties.setProperty(md5Field, md5)
        if (installSize != null && !installSize.isEmpty()) properties.setProperty(installSizeField, installSize)
    }

    long installSize() {
        try { return Long.parseLong(installSize) }
        catch (Exception ignored) { return 0 }
    }

    String md5() {
        return md5
    }

    String version() {
        return version == null ? "0.0.0.0" : version
    }

    String prefix() {
        return prefix
    }

    String build() {
        return build
    }

    void installSize(Long installSize) {
        this.installSize = (installSize == null ? "" : installSize.toString())
    }

    void md5(String md5) {
        this.md5 = md5
    }

    void version(String version) {
        this.version = version
    }

    void prefix(String prefix) {
        this.prefix = prefix
    }

    void build(String build) {
        this.build = build
    }
}