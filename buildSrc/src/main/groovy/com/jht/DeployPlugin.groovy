package com.jht

import com.jht.rscu.RSCU
import com.jht.rscu.RSCUAssociation
import com.jht.rscu.RSCUSoftwarePackage
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.TaskProvider

class DeployPlugin implements Plugin<Project> {
    private static final String GROUP_BUILD = "deploy - build"
    private static final String GROUP_PUBLISH = "deploy - publish"
    private static final String GROUP_VERSION = "deploy - version"
    private static final String GROUP_TPA_MANIFEST = "deploy - tpa manifest"
    private static final String GROUP_CLEAN_APP = "deploy - clean app"
    private static final String GROUP_BUILD_APP = "deploy - build app"
    private static final String GROUP_CLEAN_UPDATER = "deploy - clean updater"
    private static final String GROUP_BUILD_UPDATER = "deploy - build updater"
    private static final String GROUP_THIRD_PARTY_APPS = "deploy - third party apps"
    private static final String GROUP_ASSETS = "deploy - assets"
    private static final String GROUP_INSTALL = "deploy - install"
    private static final String GROUP_FORCE_INSTALL = "deploy - forceInstall"
    private static final String GROUP_SETUP = "deploy - setup"

    private List<String> getCleanCmd(String projectName) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return ["cmd", "/c", "gradlew.bat", "${projectName}:clean"]
        }
        return ["./gradlew", "${projectName}:clean"]
    }

    private List<String> getAssembleCmd(String projectName, JHTFlavor flavor) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return ["cmd", "/c", "gradlew.bat", "${projectName}:assemble${flavor.flavorCapitalized()}Release"]
        }
        return ["./gradlew", "${projectName}:assemble${flavor.flavorCapitalized()}Release"]
    }

    private List<String> getAssembleCmd2(String projectName, JHTFlavor flavor) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return ["cmd", "/c", "gradlew.bat", "${projectName}:assemble${flavor.flavorCapitalized()}Debug"]
        }
        return ["./gradlew", "${projectName}:assemble${flavor.flavorCapitalized()}Debug"]
    }

    private List<String> getAssembleCmd3(String projectName, JHTFlavor flavor) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return ["cmd", "/c", "gradlew.bat", "${projectName}:assemble${flavor.flavorCapitalized()}DebugAndroidTest"]
        }
        return ["./gradlew", "${projectName}:assemble${flavor.flavorCapitalized()}DebugAndroidTest"]
    }

    private static final List<String> TASKS_BUILD_APP = new ArrayList<String>() {{
        add("Chimera:clean")
        add("Chimera:assembleRelease")
    }}
    private static final List<String> TASKS_BUILD_UPDATER = new ArrayList<String>() {{
        add("UpdateService:clean")
        add("UpdateService:assembleRelease")
    }}
    private static final List<String> TASKS_BUILD_ALL = new ArrayList<String>() {{
        addAll(TASKS_BUILD_APP)
        addAll(TASKS_BUILD_UPDATER)
    }}

    static String getFixVersion(Project project) {
        if (project.properties.containsKey("version_to_fix")) return project.properties["version_to_fix"]
        if (System.getenv().containsKey("version_to_fix")) return System.getenv("version_to_fix")
        return ""
    }
    static String getRSCUUser(Project project) {
        if (project.properties.containsKey("RSCU_USER")) return project.properties["RSCU_USER"]
        if (System.getenv().containsKey("RSCU_USER")) return System.getenv("RSCU_USER")
        return "No User"
    }

    static String getRSCUPassword(Project project)  {
        if (project.properties.containsKey("RSCU_PASSWORD")) return project.properties["RSCU_PASSWORD"]
        if (System.getenv().containsKey("RSCU_PASSWORD")) return System.getenv("RSCU_PASSWORD")
        return "No User"
    }

    @Override
    void apply(Project project) {
        if (GlobalSettings.USE_UPDATED_DEPLOY_TASKS(project)) {
            println("Skipping the deploy tasks in favor of the new bundling tasks.")
            return
        } else {
            println("Using legacy tasks")
        }

        project.tasks.register("rscuTest", GradleBuild.class) {
            description = "Test RSCU deploy"
            group = "utilities"
            doLast {
                // Note if you want to test this out locally you will need to go into the RSCULogin.groovy
                // file and manually put in your user name and password for AM4 or have it in the environment variable.
                RSCU rscu = new RSCU(getRSCUUser(project), getRSCUPassword(project))
                List<RSCUAssociation> associations = new ArrayList<>()
                associations.add(new RSCUAssociation(1963, true, false, false))
                rscu.addPackage(new RSCUSoftwarePackage("https://s3-us-west-2.amazonaws.com/jhtsoftware/chimera/manifest/1.5.2.6/update.xml", "307cf5d9efc8b94d192bfa92c98c78e2", "chimera", "1.5.2.6_temp", "", ""))
                rscu.deploy("chimera", "0.0.1.6", associations, "chimera")
            }
        }

        project.tasks.register("rscuLoginTest", GradleBuild.class) {
            description = "Test RSCU Login"
            group = "utilities"
            doLast {
                // Note if you want to test this out locally you will need to go into the RSCULogin.groovy
                // file and manually put in your user name and password for AM4 or have it in the environment variable.
                RSCU rscu = new RSCU(getRSCUUser(project), getRSCUPassword(project))
                rscu.login()
            }
        }

        project.tasks.register("buildApp", GradleBuild.class) {
            description = "Builds all release flavors of the Chimera app."
            group = GROUP_BUILD
            tasks = TASKS_BUILD_APP
        }

        project.tasks.register("buildUpdater", GradleBuild.class) {
            description = "Builds all release flavors of the updater app."
            group = GROUP_BUILD
            tasks = TASKS_BUILD_UPDATER
        }

        project.tasks.register("buildAll", GradleBuild.class) {
            description = "Builds all release flavors of the Chimera and Updater App."
            group = GROUP_BUILD
            tasks = TASKS_BUILD_ALL
        }

        project.tasks.register("buildTests", GradleBuild.class) {
            group = GROUP_BUILD
            description = "Builds tests for the Chimera App."
            tasks = ["Chimera:assembleAndroidTest"]

            doLast {
                String apkTestLocation = "${project.projectDir}\\out_tests"
                project.delete(apkTestLocation)

                println("COPY Test APKS")
                FileTree testApkTree = project.fileTree(".").include("**/*androidTest*.apk")
                testApkTree.each { File file ->
                    if (file.size() > 20000) {
                        println(file)
                        project.copy {
                            from file
                            into apkTestLocation
                        }
                    }
                }
            }
        }

        /*
         * Create tasks for each flavor to compute the version and publish the release. This will
         * add the tasks to the overall publishRelease and computeVersion tasks above.
         *
         * FIXME(ARR) - Remove this once I finally finish cleaning up this project.
         */
        JHTFlavor.flavors.each { JHTFlavor flavor ->
            project.tasks.register("printTpaManifest-${flavor.flavorCapitalized()}") {
                description = "Print the flavored TPA Manifest task in the console."
                group = GROUP_TPA_MANIFEST
                doLast {
                    try {
                        TpaList tpas = new ReleaseSoftware(flavor, project.projectDir).deployment().getTpaList()
                        println("=== printTpaManifest-${flavor.flavorCapitalized()} ===")
                        println()

                        if (tpas == null) {
                            println("!! ${flavor.flavorCapitalized()} does NOT support TPA manifests !!")
                        } else {
                            println(tpas.toJson())
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw new TaskExecutionException(this, ex)
                    }
                }
            }

            project.tasks.register("verifyTpaManifest-${flavor.flavorCapitalized()}") {
                description = "Verify current TPAs against the flavored TPA manifest task."
                group = GROUP_TPA_MANIFEST
                doLast {
                    try {
                        TpaList tpas = new ReleaseSoftware(flavor, project.projectDir).deployment().getTpaList()
                        if (tpas == null) {
                            println("!! ${flavor.flavorCapitalized()} does NOT support TPA manifests !!")
                        } else {
                            tpas.validateTpaSources()
                            println("<< TPA manifest verification complete >>")
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw new TaskExecutionException(this, ex)
                    }
                }
            }

            project.tasks.register("getVersion${flavor.flavorCapitalized()}") {
                description = "Create the flavored compute version task."
                group = GROUP_VERSION
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().printVersions()
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            TaskProvider computeTask = project.tasks.register("computeVersion${flavor.flavorCapitalized()}") {
                description = "Create the flavored compute version task."
                group = GROUP_VERSION
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().computePackageVersions()
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            TaskProvider cleanAppTask = project.tasks.register("cleanApp${flavor.flavorCapitalized()}", Exec.class) {
                group = GROUP_CLEAN_APP
                commandLine = getCleanCmd("Chimera")
                mustRunAfter(computeTask)
            }

            TaskProvider buildAppTask = project.tasks.register("buildApp${flavor.flavorCapitalized()}", Exec.class) {
                group = GROUP_BUILD_APP
                commandLine = getAssembleCmd("Chimera", flavor)
                mustRunAfter(cleanAppTask)
            }

            TaskProvider buildDebugAppTask = project.tasks.register("buildDebugApp${flavor.flavorCapitalized()}", Exec.class) {
                group = GROUP_BUILD_APP
                commandLine = getAssembleCmd2("Chimera", flavor)
                mustRunAfter(buildAppTask)
            }

            TaskProvider buildDebugAppAndroidTestTask = project.tasks.register("buildDebugAppAndroidTestApp${flavor.flavorCapitalized()}", Exec.class) {
                group = GROUP_BUILD_APP
                commandLine = getAssembleCmd3("Chimera", flavor)
                mustRunAfter(buildDebugAppTask)
            }

            TaskProvider cleanUpdaterTask = project.tasks.register("cleanUpdater${flavor.flavorCapitalized()}", Exec.class) {
                group = GROUP_CLEAN_UPDATER
                commandLine = getCleanCmd("UpdateService")
                mustRunAfter(computeTask)
            }

            TaskProvider buildUpdaterTask = project.tasks.register("buildUpdater${flavor.flavorCapitalized()}", Exec.class) {
                group = GROUP_BUILD_UPDATER
                commandLine = getAssembleCmd("UpdateService", flavor)
                mustRunAfter(cleanUpdaterTask)
            }

            project.tasks.register("build${flavor.flavorCapitalized()}") {
                group = GROUP_BUILD
                dependsOn([cleanAppTask, buildAppTask, buildDebugAppTask, buildDebugAppAndroidTestTask, cleanUpdaterTask, buildUpdaterTask])
            }

            TaskProvider publishTask = project.tasks.register("publishRelease${flavor.flavorCapitalized()}") {
                description = "Create the flavored publish task."
                group = GROUP_PUBLISH
                mustRunAfter(buildAppTask, buildUpdaterTask)
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().releaseSoftware(getRSCUUser(project), getRSCUPassword(project), false)
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            TaskProvider publishNightlyTask = project.tasks.register("publishNightlyRelease${flavor.flavorCapitalized()}") {
                description = "Release the flavored nightly build."
                group = GROUP_PUBLISH
                mustRunAfter(buildAppTask, buildUpdaterTask)
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().releaseSoftware("", "", true)
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            TaskProvider pullDependenciesTask = project.tasks.register("pullDependencies${flavor.flavorCapitalized()}") {
                description = "Download build assets from AWS S3 bucket."
                group = GROUP_SETUP
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().pullDependencies("jht-build-assets")
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("fixVersion" + flavor.flavorCapitalized()) {
                description = "Fix deployed version md5's"
                group = GROUP_VERSION
                doLast {
                    // Surround in the try and catch so you can see a stack trace for any errors.
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().fixVersion(getFixVersion(project), getRSCUUser(project), getRSCUPassword(project))
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("fixAllVersions" + flavor.flavorCapitalized()) {
                description = "Fix all deployed version md5's"
                group = GROUP_VERSION
                doLast {
                    // Surround in the try and catch so you can see a stack trace for any errors.
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().fixAllVersions(getRSCUUser(project), getRSCUPassword(project))
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("updateToLatest${flavor.flavorCapitalized()}") {
                description = "Update console to latest version."
                group = GROUP_VERSION
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().updateToLatest()
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("installThirdPartyApps${flavor.flavorCapitalized()}") {
                group = GROUP_THIRD_PARTY_APPS
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().installThirdPartyApps()
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("installAssets${flavor.flavorCapitalized()}") {
                group = GROUP_ASSETS
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().installAssets()
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("install${flavor.flavorCapitalized()}") {
                group = GROUP_INSTALL
                mustRunAfter(publishTask)
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().adbInstall(false)
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        throw ex
                    }
                }
            }

            project.tasks.register("forceInstall${flavor.flavorCapitalized()}") {
                group = GROUP_FORCE_INSTALL
                mustRunAfter(publishTask)
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().adbInstall(true)
                    } catch (Exception e) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }

            project.tasks.register("installLastRelease${flavor.flavorCapitalized()}") {
                group = GROUP_INSTALL
                mustRunAfter(publishTask)
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().installLastRelease(false)
                    } catch (Exception e) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }

            project.tasks.register("forceInstallLastRelease${flavor.flavorCapitalized()}") {
                group = GROUP_FORCE_INSTALL
                mustRunAfter(publishTask)
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().installLastRelease(true)
                    } catch (Exception e) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }

            project.tasks.register("flavorInfo${flavor.flavorCapitalized()}") {
                group = "documentation"
                doLast {
                    try {
                        new ReleaseSoftware(flavor, project.projectDir).deployment().printFlavorDebugInfo()
                    } catch (Exception e) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }
        }
    }
}
