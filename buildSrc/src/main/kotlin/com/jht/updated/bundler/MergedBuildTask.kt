package com.jht.updated.bundler

import com.jht.updated.bundler.bundle.ASSETS
import com.jht.updated.bundler.bundle.CHIMERA2_OS
import com.jht.updated.bundler.bundle.CHIMERA2_TOUCH_PANEL
import com.jht.updated.bundler.bundle.CHIMERA3_OS
import com.jht.updated.bundler.bundle.CHIMERA_IO2
import com.jht.updated.bundler.bundle.CHIMERA_IO3
import com.jht.updated.bundler.bundle.CHIMERA_IO4
import com.jht.updated.bundler.bundle.CHIMERA_IO5
import com.jht.updated.bundler.bundle.CHIMERA_KM5
import com.jht.updated.bundler.bundle.CHIMERA_LCB1
import com.jht.updated.bundler.bundle.CHIMERA_LCB1xBike
import com.jht.updated.bundler.bundle.CHIMERA_LCB2
import com.jht.updated.bundler.bundle.CHIMERA_LCBA
import com.jht.updated.bundler.bundle.CHIMERA_LCBAthena
import com.jht.updated.bundler.bundle.CHIMERA_LCB_Climbmill
import com.jht.updated.bundler.bundle.CHIMERA_OS_K2_2
import com.jht.updated.bundler.bundle.CHIMERA_TOUCH_PANEL
import com.jht.updated.bundler.bundle.CHIMERA_TOUCH_PANEL_ATHENA
import com.jht.updated.bundler.bundle.CHIMERA_US5
import com.jht.updated.bundler.bundle.CHIMERA_US6
import com.jht.updated.bundler.bundle.CHIMERA_US7
import com.jht.updated.bundler.bundle.CHIMERA_U_BOOT
import com.jht.updated.bundler.bundle.CONSOLE_APPS
import com.jht.updated.bundler.bundle.P1_BUMP_SENSOR
import com.jht.updated.bundler.bundle.P1_TOGGLE_TOUCH_FIRMWARE
import com.jht.updated.bundler.bundle.PARKER_IO
import com.jht.updated.bundler.bundle.PARKER_IO3
import com.jht.updated.bundler.bundle.PARKER_IR
import com.jht.updated.bundler.bundle.PARKER_OS
import com.jht.updated.bundler.bundle.PARKER_OS3
import com.jht.updated.bundler.bundle.RFID2
import com.jht.updated.bundler.bundle.RFID3
import com.jht.updated.bundler.bundle.RFID4
import com.jht.updated.bundler.bundle.SALUTRON
import com.jht.updated.bundler.bundle.TPA_AMAZONFREEVEE
import com.jht.updated.bundler.bundle.TPA_BSJLIVETV
import com.jht.updated.bundler.bundle.TPA_CARDS
import com.jht.updated.bundler.bundle.TPA_CARDSJHT
import com.jht.updated.bundler.bundle.TPA_CHINESEPINYINKEYBOARD
import com.jht.updated.bundler.bundle.TPA_CHINESETRADITIONALKEYBOARD
import com.jht.updated.bundler.bundle.TPA_CHINESETRADITIONALKEYBOARD_2026
import com.jht.updated.bundler.bundle.TPA_CLEANUP
import com.jht.updated.bundler.bundle.TPA_CLOUDMUSIC
import com.jht.updated.bundler.bundle.TPA_DISNEYPLUS
import com.jht.updated.bundler.bundle.TPA_DUMMY_TPA_ZIP
import com.jht.updated.bundler.bundle.TPA_FLIPBOARD
import com.jht.updated.bundler.bundle.TPA_HANGULKEYBOARD
import com.jht.updated.bundler.bundle.TPA_HELIBOARD
import com.jht.updated.bundler.bundle.TPA_IFIT
import com.jht.updated.bundler.bundle.TPA_IFITCOMMERCIAL
import com.jht.updated.bundler.bundle.TPA_IFITCOMMERCIAL3
import com.jht.updated.bundler.bundle.TPA_INSTAGRAM
import com.jht.updated.bundler.bundle.TPA_IQIYI
import com.jht.updated.bundler.bundle.TPA_KINDLE
import com.jht.updated.bundler.bundle.TPA_KINDLELITE
import com.jht.updated.bundler.bundle.TPA_MATRIXTESTAPP
import com.jht.updated.bundler.bundle.TPA_MOJIWEATHER
import com.jht.updated.bundler.bundle.TPA_MYWEATHER
import com.jht.updated.bundler.bundle.TPA_MYZONE
import com.jht.updated.bundler.bundle.TPA_NETFLIX
import com.jht.updated.bundler.bundle.TPA_OPENWNN
import com.jht.updated.bundler.bundle.TPA_PLUTOTV
import com.jht.updated.bundler.bundle.TPA_PRESSREADER
import com.jht.updated.bundler.bundle.TPA_SNAPCHAT
import com.jht.updated.bundler.bundle.TPA_SPOTIFY
import com.jht.updated.bundler.bundle.TPA_TEAMVIEWER
import com.jht.updated.bundler.bundle.TPA_TEAMVIEWERADDON
import com.jht.updated.bundler.bundle.TPA_WECHAT
import com.jht.updated.bundler.bundle.TPA_WEIBO
import com.jht.updated.bundler.bundle.TPA_YMCA360
import com.jht.updated.bundler.bundle.UPDATER
import com.jht.updated.bundler.bundle.VISION_AD_BOARD
import com.jht.updated.bundler.bundle.VORTEX_LITE_OS
import com.jht.updated.bundler.bundle.VORTEX_OS
import com.jht.updated.bundler.bundle.WLT_BLUETOOTH
import com.jht.updated.createFile
import com.jht.updated.get
import com.jht.updated.zipFiles
import com.jht.aws.AwsSecrets.getSecret
import com.jht.updated.bundler.bundle.CAN_LEFT
import com.jht.updated.bundler.bundle.CAN_RIGHT
import com.jht.updated.bundler.bundle.CAN_SINGLE
import getGitInfo
import getLocalVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import runCommand

enum class GradleGroupsEnum (val gradleGroupName : String) {
    bundle_all("bundle - all"), bundle_all_nightly("bundle - all - nightly"), aws_nightly("aws - nightly")
}

class MergedBuildTask: Plugin<Project> {
    //
    // Include all Third Party Apps individually. There needs to be a better way of ensuring all
    // apps that are not installed are being correctly uninstalled, including previous versions.
    //
    private val allThirdPartyAppBundles = listOf(
        TPA_CARDS,
        TPA_CARDSJHT,
        TPA_CHINESETRADITIONALKEYBOARD,//Legacy Android 13 and below
        TPA_CHINESETRADITIONALKEYBOARD_2026,//Android 14+
        TPA_CHINESEPINYINKEYBOARD,//Android 14+
        TPA_FLIPBOARD,
        TPA_NETFLIX,
        TPA_DISNEYPLUS,
        TPA_PRESSREADER,
        TPA_SPOTIFY,
        TPA_TEAMVIEWER,
        TPA_TEAMVIEWERADDON,
        TPA_WECHAT,
        TPA_HANGULKEYBOARD,//Legacy Android 13 and below Korean keyboard
        TPA_HELIBOARD,//Android 14+ Korean keyboard
        TPA_OPENWNN,//Android 14+ Japanese keyboard
        TPA_KINDLE,
        TPA_KINDLELITE,
        TPA_WEIBO,
        TPA_SNAPCHAT,
        TPA_INSTAGRAM,
        TPA_IFIT,
        TPA_IFITCOMMERCIAL,
        TPA_IFITCOMMERCIAL3,
        TPA_YMCA360,
        TPA_AMAZONFREEVEE,
        TPA_PLUTOTV,
        TPA_CLOUDMUSIC,
        TPA_MYWEATHER,
        TPA_MOJIWEATHER,
        TPA_MYZONE,
        TPA_BSJLIVETV,
        TPA_MATRIXTESTAPP,
        TPA_IQIYI
    )
    private val TPA_CLEANUP = TPA_CLEANUP(allThirdPartyAppBundles)

    private val allBundles = mutableListOf<Bundle>().apply {
        // Add the bundles that must appear first.
        //..........................................................................................
        add(UPDATER)
        add(CONSOLE_APPS)
        add(ASSETS)

        // Add the third party app bundles.
        //..........................................................................................
        add(TPA_CLEANUP) // Add the custom cleanup task.
        add(TPA_DUMMY_TPA_ZIP) //Adds dummy package with same update ID of old third party zip package
        allThirdPartyAppBundles.forEach { add(it) } // Add all the third party app bundles.

        // Add all the firmware bundles.
        //..........................................................................................
        add(WLT_BLUETOOTH)
        add(PARKER_IO)
        add(PARKER_IO3)
        add(PARKER_IR)
        add(P1_BUMP_SENSOR)
        add(P1_TOGGLE_TOUCH_FIRMWARE)
        add(VISION_AD_BOARD)
        add(CHIMERA_IO2)
        add(CHIMERA_IO3)
        add(CHIMERA_IO4)
        add(CHIMERA_IO5)
        add(CHIMERA_KM5)
        add(CHIMERA_LCB1)
        add(CHIMERA_LCB2)
        add(CHIMERA_LCB_Climbmill)
        // SUS-1615 Missing ParkerLcbClimbmillRetail. Add when Parker gets merged back in anyway.
        add(CHIMERA_LCBA)
        add(CHIMERA_LCB1xBike)
        add(CHIMERA_LCBAthena)
        add(PARKER_OS)
        add(PARKER_OS3)
        add(CHIMERA_OS_K2_2)
        add(CHIMERA2_OS)
        add(CHIMERA3_OS)
        add(VORTEX_OS)
        add(VORTEX_LITE_OS)
        add(RFID2)
        add(RFID3)
        add(RFID4)
        add(CAN_SINGLE)
        add(CAN_RIGHT)
        add(CAN_LEFT)
        add(SALUTRON)
        add(CHIMERA2_TOUCH_PANEL)
        add(CHIMERA_TOUCH_PANEL_ATHENA)
        add(CHIMERA_TOUCH_PANEL)
        add(CHIMERA_U_BOOT)
        add(CHIMERA_US5)
        add(CHIMERA_US6)
        add(CHIMERA_US7)
    }.toList()

    /**
     * If you only want to build a couple bundles, this is where you need to list all those bundles.
     * If empty no tasks will be added.
     */
    private val customBundles = mutableListOf<Bundle>().apply {
        // Add the third party app bundles.
        //..........................................................................................
        add(TPA_CLEANUP(allThirdPartyAppBundles)) // Add the custom cleanup task.
        allThirdPartyAppBundles.forEach { add(it) } // Add all the third party app bundles.
    }.toList()

    // shell command for running nightly builds Gradle tasks
    private val gitBranchCommand = "git branch --show-current"

    // strings for build output
    private val awsBuildFoundHere = "AWS BUILD FOUND HERE:\n============================================================================================================="

    companion object {
        fun getCOREUser(project: Project) : String
        {
            return when {
                project.properties.containsKey("CORE_USER") -> project.properties.get("CORE_USER") as String
                System.getenv().containsKey("CORE_USER") -> System.getenv().get("CORE_USER") as String
                else -> "No User"
            }
        }

        fun getCOREPassword(project: Project) : String
        {
            return when {
                project.properties.containsKey("CORE_PASSWORD") -> project.properties.get("CORE_PASSWORD") as String
                System.getenv().containsKey("CORE_PASSWORD") -> System.getenv().get("CORE_PASSWORD") as String
                else -> "No Password"
            }
        }
    }

    override fun apply(target: Project) {

        target.tasks.let { tasks ->

            // Adding Vortex and Vortex-Onyx manually for nightly builds
            tasks.register("RunNightlyVortex") {
                description = "Run nightly job for Vortex with the current working branch."
                group = GradleGroupsEnum.aws_nightly.gradleGroupName
                val sourceVersion = gitBranchCommand.runCommand()
                val awsNightlyProjectName = "Develop_Vortex_Nightly"
                val startBuildCommand = "aws codebuild start-build --region us-east-2 --source-version ${sourceVersion} --project-name ${awsNightlyProjectName} --query build.id --output text"
                doLast {
                    logger.warn("Be sure to push your changes to ${sourceVersion} before running!")
                    println(startBuildCommand)
                    val awsBuildID = startBuildCommand.runCommand().trim().replace(":", "%3A")
                    println(awsBuildFoundHere)
                    println("https://console.aws.amazon.com/codesuite/codebuild/651468908752/projects/${awsNightlyProjectName}/build/${awsBuildID}/?region=us-east-2")
                }
            }

            tasks.register("RunNightlyVortexOnyx") {
                description = "Run nightly job for Vortex Onyx with the current working branch."
                group = GradleGroupsEnum.aws_nightly.gradleGroupName
                val sourceVersion = gitBranchCommand.runCommand()
                val awsNightlyProjectName = "Develop_Vortex_Onyx_Nightly"
                val startBuildCommand = "aws codebuild start-build --region us-east-2 --source-version ${sourceVersion} --project-name ${awsNightlyProjectName} --query build.id --output text"
                doLast {
                    logger.warn("Be sure to push your changes to ${sourceVersion} before running!")
                    println(startBuildCommand)
                    val awsBuildID = startBuildCommand.runCommand().trim().replace(":", "%3A")
                    println(awsBuildFoundHere)
                    println("https://console.aws.amazon.com/codesuite/codebuild/651468908752/projects/${awsNightlyProjectName}/build/${awsBuildID}/?region=us-east-2")
                }
            }

            for (flavor in JHTFlavor.values()) {
                val allStageTasks = mutableSetOf<TaskProvider<*>>()
                val allNightlyStageTasks = mutableSetOf<TaskProvider<*>>()
                val allPublishTasks = mutableSetOf<TaskProvider<*>>()
                val allPublishTPATasks = mutableSetOf<TaskProvider<*>>()
                val allPublishNightlyTasks = mutableSetOf<TaskProvider<*>>()
                val allSupportedBundles = allBundles.filter { it.supportedFlavors.contains(flavor) }
                val allSupportedTPABundles = allBundles.filter { it is TPABundle && it.supportedFlavors.contains(flavor) }
                val coreSupportedBundles = allSupportedBundles.filter { !it.usbOnly(flavor) }

                // SUS-1616 Should this throw an exception if the list is empty?
                for (bundle in allSupportedBundles) {
                    val stageTask: TaskProvider<*>
                    val nightlyStageTask: TaskProvider<*>
                    if (bundle is BuildBundle) {
                        // SUS-1617 I don't really think I am going to be able to bypass this without
                        //  actually knowing what is happening. Should I remove this ahead of time.
                        //  I need to adjust the build tasks. I can skip them if not actually using the output.
                        val buildTask = tasks.register("build${flavor.flavorCapitalized}${bundle.packageShortName}") {//}, GradleBuild::class.java) {
                            dependsOn(bundle.buildTasks(flavor).map { tasks.findByPath(it) })
                        }

                        stageTask = tasks.register("stage${flavor.flavorCapitalized}${bundle.packageShortName}") {
                            doLast {
                                bundle.stageBundle(target, flavor, forceStageFromSource = false)
                            }
                        }

                        nightlyStageTask = tasks.register("nightlyStage${flavor.flavorCapitalized}${bundle.packageShortName}") {
                            doLast {
                                bundle.stageBundle(target, flavor, forceStageFromSource = true)
                            }
                        }

                    } else {
                        stageTask = tasks.register("stage${flavor.flavorCapitalized}${bundle.packageShortName}") {
                            doLast { bundle.stageBundle(target, flavor) }
                        }
                        //does the same to prevent issues with releases
                        nightlyStageTask = tasks.register("nightlyStage${flavor.flavorCapitalized}${bundle.packageShortName}") {
                            doLast { bundle.stageBundle(target, flavor) }
                        }
                    }

                    // Add the stage task to allStageTasks
                    allStageTasks.add(stageTask)
                    allNightlyStageTasks.add(nightlyStageTask)


                    // Create all the publish tasks.
                    tasks.register("publish${flavor.flavorCapitalized}${bundle.packageShortName}") {
                        dependsOn(stageTask)
                        doLast { bundle.publishBundle(target, flavor) }
                    }.also {
                        // Ensure the publish task is added to the all publish task.
                        allPublishTasks.add(it)
                        if (bundle is TPABundle || bundle == TPA_CLEANUP || bundle == TPA_DUMMY_TPA_ZIP) {
                            allPublishTPATasks.add(it)
                        }
                    }

                    tasks.register("publishNightly${flavor.flavorCapitalized}${bundle.packageShortName}") {
                        dependsOn(stageTask)
                        doLast { bundle.publishNightlyBundle(target, flavor) }
                    }.also {
                        // Ensure the publish nightly task is added to the all publish nightly task.
                        allPublishNightlyTasks.add(it)
                    }
                }

                //Check if a production release for this version already exists before building
                tasks.register("preBuildCheck${flavor.flavorCapitalized}") {
                    group = "pre-build"
                    doLast {
                        flavor.preBuildCheck(target.projectDir, getCOREUser(project), getCOREPassword(project))
                    }
                }

                // Create the task for staging all tasks.
                tasks.register("stage${flavor.flavorCapitalized}") {
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    dependsOn(allStageTasks)
                    doLast {
                        println("Creating the manifest files for the full bundle")
                        val bundleStagingDir = flavor.bundleReleaseStagingDirectory(target, false)
                        val manifestText = ManifestMaker.createManifest(target, flavor, coreSupportedBundles)
                        val manifestTextOTA = ManifestMaker.createManifest(target, flavor, coreSupportedBundles, true)
                        val manifestTextALL = ManifestMaker.createManifest(target, flavor, allSupportedBundles)
                        val manifestTextOTAALL = ManifestMaker.createManifest(target, flavor, allSupportedBundles, true)

                        bundleStagingDir.get("update.xml").createFile(manifestText)
                        bundleStagingDir.get("update_ota.xml").createFile(manifestTextOTA)
                        bundleStagingDir.get("update.all.xml").createFile(manifestTextALL)
                        bundleStagingDir.get("update_ota.all.xml").createFile(manifestTextOTAALL)
                    }
                }

                // Create the nightly task for staging all tasks.
                val stageNightly = tasks.register("stageNightly${flavor.flavorCapitalized}") {
                    group = GradleGroupsEnum.bundle_all_nightly.gradleGroupName
                    dependsOn(allNightlyStageTasks)
                    doLast {
                        flavor.setNightlyVersion(project.rootDir)
                        //set forced to true for all bundles for nightly updates
                        for (bundle in allSupportedBundles) {
                            if (bundle.packageShortName == "apps" || bundle.packageShortName == "updater") {
                                bundle.isForced = true
                                bundle.setVersion(flavor, getLocalVersion(getGitInfo(project.rootDir)))
                                val zipPassword = getSecret("UPDATE_ZIP_PASSWORD")
                                bundle.bundleStagingDir(project, flavor).zipFiles(
                                    dest = bundle.stagingZipFile(project, flavor),
                                    password = zipPassword
                                )
                            } else if (bundle.packageShortName == "assets") {
                                bundle.isForcedDowngrade = true
                            }
                        }
                        println("Creating the nightly manifest files for the full bundle")
                        val bundleStagingDir = flavor.bundleReleaseStagingDirectory(target, true)
                        val manifestText = ManifestMaker.createManifest(target, flavor, coreSupportedBundles)
                        val manifestOTAText = ManifestMaker.createManifest(target, flavor, coreSupportedBundles, true, true)
                        val manifestAllText = ManifestMaker.createManifest(target, flavor, allSupportedBundles)
                        val manifestAllOTAText = ManifestMaker.createManifest(target, flavor, allSupportedBundles, true, true)

                        bundleStagingDir.get("update.xml").createFile(manifestText)
                        bundleStagingDir.get("update_ota.xml").createFile(manifestOTAText)
                        bundleStagingDir.get("update.all.xml").createFile(manifestAllText)
                        bundleStagingDir.get("update_ota.all.xml").createFile(manifestAllOTAText)
                    }
                }

                // Create the task responsible for outputting the list of all supported packages
                // for each flavor.
                tasks.register("listSupportedPackages${flavor.flavorCapitalized}") {
                    description = "Output the list of supported packages for ${flavor.flavorCapitalized}."
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    doLast {
                        println("Supported packages for ${flavor.flavorCapitalized}:")
                        println("=================================================================")
                        allSupportedBundles.forEach { bundle ->
                            println("- ${bundle.packageShortName}")
                        }
                    }
                }

                tasks.register("listSupportedThirdPartyApps${flavor.flavorCapitalized}") {
                    description = "Output the list of supported third party apps packages for ${flavor.flavorCapitalized}"
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    doLast {
                        println("Supported Third Party Apps for ${flavor.flavorCapitalized}:")
                        println("=================================================================")
                        allSupportedTPABundles.forEach { bundle ->
                            println("- ${bundle.packageShortName}")
                        }
                    }
                }

                // Create the full publish task.
                val publishFlavor = tasks.register("publish${flavor.flavorCapitalized}") {
                    description = "Publish ${flavor.flavorCapitalized} to CORE"
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    dependsOn(allPublishTasks)
                    doLast {
                        println("git tag -a ${flavor.softwareConfigurationClass}_${flavor.bundleVersion} -m '${flavor.bundleVersion} release for ${flavor.softwareConfigurationClass}'")
                        ("git tag -a ${flavor.softwareConfigurationClass}_${flavor.bundleVersion} -m '${flavor.bundleVersion} release for ${flavor.softwareConfigurationClass}'").runCommand(workingDir = target.projectDir)
                        println("git push --tags")
                        "git push --tags".runCommand(workingDir = target.projectDir, suppressErrors = true)
                        flavor.publishMapping(target.projectDir, AWS_CORE_BUCKET, false)
                        //flavor.publishZip(target.projectDir, coreSupportedBundles, AWS_CORE_BUCKET) // zip has gotten too big to fit memory, so temporary disabled
                    }
                }

                // Create the full publish task.
                val publishNightlyFlavor = tasks.register("publishNightly${flavor.flavorCapitalized}") {
                    description = "Publish ${flavor.flavorCapitalized} to DOORE"
                    group = GradleGroupsEnum.bundle_all_nightly.gradleGroupName
                    dependsOn(allPublishNightlyTasks)
                    doLast {
                        flavor.publishMapping(target.projectDir, AWS_DOORE_BUCKET, true, AWS_NIGHTLY_REGION)
                    }
                }

                val publishManifest = tasks.register("publishManifest${flavor.flavorCapitalized}") {
                    description = "Publish ${flavor.flavorCapitalized} manifest to CORE"
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    dependsOn(publishFlavor)
                    doLast {
                        flavor.publishManifest(target.projectDir, AWS_CORE_BUCKET, false)
                    }
                }

                val publishNightlyManifest = tasks.register("publishNightlyManifest${flavor.flavorCapitalized}") {
                    description = "Publish ${flavor.flavorCapitalized} manifest to DOORE"
                    group = GradleGroupsEnum.bundle_all_nightly.gradleGroupName
                    mustRunAfter(stageNightly)
                    dependsOn(publishNightlyFlavor)
                    doLast {
                        flavor.publishManifest(target.projectDir, AWS_DOORE_BUCKET, true, AWSRegion = AWS_NIGHTLY_REGION)
                    }
                }

                tasks.register("nightlyStageAndPublish${flavor.flavorCapitalized}") {
                    description = "Stage and publish nightly bundles to DOORE"
                    group = GradleGroupsEnum.bundle_all_nightly.gradleGroupName
                    dependsOn(stageNightly, publishNightlyManifest)
                }

                tasks.register("publishCOREAssociation${flavor.flavorCapitalized}") {
                    description = "Publish ${flavor.flavorCapitalized} manifest to CORE"
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    dependsOn(publishManifest)
                    doLast {
                        flavor.publishCOREAssociation(getCOREUser(project), getCOREPassword(project), target.projectDir)
                    }
                }

                tasks.register("publishTPA${flavor.flavorCapitalized}") {
                    description = "Publish new manifest for ${flavor.flavorCapitalized} will copy current Globals versions and update only Third Party App Package version to those listed for the flavor"
                    group = GradleGroupsEnum.bundle_all.gradleGroupName
                    val clubID = project.findProperty("clubID") as String? ?: "-1"
                    val dapiEnv = project.findProperty("dapiEnv") as String? ?: "prod"
                    val tpaBundles = mutableListOf<Bundle>()
                    tpaBundles.addAll(allThirdPartyAppBundles)

                    // Need to have the TPA clean up bundle on top of the Apps to remove any app
                    // that shouldn't exist on the console
                    tpaBundles.add(TPA_CLEANUP)
                    tpaBundles.add(TPA_DUMMY_TPA_ZIP)

                    // Publish flavor to make sure all bundles exist in S3 properly before creating
                    // manifest
                    dependsOn(allPublishTPATasks)
                    doLast {
                        flavor.createAndPublishTPAManifest(getCOREUser(project), getCOREPassword(project), tpaBundles, target, clubID.toInt(), dapiEnv)
                    }
                }

                tasks.register("RunNightly${flavor.flavorCapitalized}") {
                    description = "Run nightly job for ${flavor.softwareConfigurationClass} with the current working branch."
                    group = GradleGroupsEnum.aws_nightly.gradleGroupName
                    val sourceVersion = gitBranchCommand.runCommand()
                    val awsNightlyProjectName = when (flavor.softwareConfigurationClass) {
                        "chimera" -> "Chimera_Main_Nightly"
                        "parker" -> "Parker_Nightly"
                        "lowcostgui" -> "LCGUI_Nightly"
                        "projectone" -> "Project_One_Nightly"
                        "t600xe" -> "T600xe_Nightly"
                        "vision600e" -> "Vision600e_Nightly"
                        else -> logger.error("No nightly job found for ${flavor.softwareConfigurationClass}")
                    }
                    val startBuildCommand = "aws codebuild start-build --region us-east-2 --source-version ${sourceVersion} --project-name ${awsNightlyProjectName} --query build.id --output text"
                    doLast {
                        logger.warn("Be sure to push your changes to ${sourceVersion} before running!")
                        println(startBuildCommand)
                        val awsBuildID = startBuildCommand.runCommand().trim().replace(":", "%3A")
                        println(awsBuildFoundHere)
                        println("https://console.aws.amazon.com/codesuite/codebuild/651468908752/projects/${awsNightlyProjectName}/build/${awsBuildID}/?region=us-east-2")
                    }
                }
            }
        }
    }
}