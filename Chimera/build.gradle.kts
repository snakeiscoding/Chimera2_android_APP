import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.bundler.bundle.CONSOLE_APPS
import com.jht.updated.keystore

plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.allure.docker)
    id("generate-oauth-credentials")
}

/**
 * Learn more about Allure here: https://jhtnaee.atlassian.net/wiki/spaces/CSR/pages/4652400668/Setting+up+Allure+Reporting
 */
allureDocker {
    val projectId: String = System.getenv("ALLURE_PROJECT_ID") ?: "dev-chimera-manual"
    val flavorStr: String = System.getenv("ALLURE_FLAVOR") ?: JHTFlavor.ChimeraMainMatrixChimera.flavor
    val extraEnvironmentVals: String = "release_version = ${JHTFlavor.getJHTFlavorFromFlavorCapitalized(flavorStr).bundleVersion}\n"
    this.projectId = projectId
    allureResultsDir = "build/allure-results"
    projectVersion = getLocalVersion(getGitInfo(project.rootDir))
    this.extraEnvironmentVals = extraEnvironmentVals
}


// Ensure the correct Android components are applied.
androidComponents(getAndroidComponents(CONSOLE_APPS, project))

android {
    namespace = "com.jht.chimera"
    testNamespace = "com.jht.chimera.test"
    compileSdk = libs.versions.compileSdk.get().toInt()

    sourceSets {
        named("main") {
        }
    }

    defaultConfig {
        versionCode = libs.versions.thisVersionCode.get().toInt()
        versionName = libs.versions.thisVersionName.get()

        minSdk = libs.versions.minSdk.get().toInt()
        //noinspection ExpiredTargetSdkVersion
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["grantAllPermissions"] = "true"
        multiDexEnabled = libs.versions.multiDexEnabled.get().toBoolean()
        vectorDrawables.useSupportLibrary = libs.versions.useSupportLibrary.get().toBoolean()

        ndk { abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")) }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        manifestPlaceholders["appAuthRedirectScheme"] = "com.jht.chimera"
    }

    configurations.all {
        resolutionStrategy {
            force(
                libs.androidx.annotation.annotation.get(),
                libs.androidx.test.monitor.get()
            )
        }
    }

    signingConfigs {
        register("signingChimera") {
            storeFile = File(project.keystore, "platform.keystore.chimera")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        register("signingParker") {
            storeFile = File(project.keystore, "platform.keystore.parker")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // https://stackoverflow.com/questions/17040494/signing-product-flavors-with-gradle
    // NOTE: app_name is defined in strings_custom.xml per flavor
    flavorDimensions += listOf("product", "ui", "platform") // FIXME: These need to be defined in common.
    productFlavors {
        //------------------------------------------------------------------------------------------
        // PLATFORM denotes the hardware platform the flavor runs on
        //------------------------------------------------------------------------------------------
        register("parker") {
            dimension = "platform"
            signingConfig = signingConfigs.getByName("signingParker")
        }

        register("chimera") {
            dimension = "platform"
            signingConfig = signingConfigs.getByName("signingChimera")
        }

        // lcgui is similar to Chimera but on Parker hardware, using daughterboards for some extra functions
        register("lcgui") {
            dimension = "platform"
            signingConfig = signingConfigs.getByName("signingParker")
        }

        //------------------------------------------------------------------------------------------
        // UI denotes a base ui framework to use
        //------------------------------------------------------------------------------------------
        register("matrix") {
            dimension = "ui"
        }

        //------------------------------------------------------------------------------------------
        // PRODUCT denotes differences specific to a product (essentially that which does not fall
        // into the previous dimensions)
        //------------------------------------------------------------------------------------------

        register("parker_main") {
            dimension = "product"
        }

        // vision600e is parker with tv tuner and some ui variation
        register("vision600e") {
            dimension = "product"
        }

        // t600xe is vision600e with some different brand graphics
        register("t600xe") {
            dimension = "product"
        }

        register("chimera_main") {
            dimension = "product"
        }

        register("projectone") {
            dimension = "product"
        }
    }

    buildTypes {
        getByName("release") {
            // need to null the default signingConfig in order to defer to flavors
            signingConfig = null
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            // need to null the default signingConfig in order to defer to flavors
            signingConfig = null
            isMinifyEnabled = false
            isShrinkResources = false

            // potentially helpful on chimera1/chimera2 which behave oddly with apk in debug, but note if this is false the debugger will not attach to our debug app
            //isDebuggable = false
        }
    }

    ndkVersion = libs.versions.ndkVersion.get()

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        // IHRSA Temp Code Sprint8/Landmark Fragments
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }

    packaging {
        resources {
            excludes.addAll(arrayOf("META-INF/DEPENDENCIES", "META-INF/LICENSE",
                "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md", "META-INF/NOTICE",
                "META-INF/NOTICE.txt", "META-INF/notice.txt", "META-INF/ASL2.0", "META-INF/AL2.0",
                "META-INF/*.kotlin_module", "META-INF/LGPL2.1"))
        }
    }

    lint {
        abortOnError = false
    }

    testOptions {
        // Robolectric needs the merged resources to inflate layouts and apply AppCompat themes.
        unitTests.isIncludeAndroidResources = true
    }
}

// WARNING - The include order is very important.
dependencies {
    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.androidx.constraintlayout.constraintlayout)
    implementation(libs.androidx.fragment.fragment)
    implementation(libs.androidx.core.core)
    implementation(libs.androidx.gridlayout.gridlayout)
    implementation(libs.androidx.legacy.legacySupportCoreUi)
    implementation(libs.androidx.legacy.legacySupportCoreUtils)
    implementation(libs.androidx.legacy.legacySupportV13)
    implementation(libs.androidx.media.media)
    implementation(libs.androidx.multidex.multidex)
    implementation(libs.androidx.recyclerview.recyclerview)
    implementation(libs.androidx.vectordrawable.vectordrawable)
    implementation(libs.androidx.lifecycle.lifecycleService) // for ConsoleService

    implementation(libs.com.google.zxing.core) // for QR code generation POC

    testImplementation(libs.junit.junit)
    testImplementation(libs.io.mockk.mockk)
    androidTestImplementation("io.mockk:mockk-android:1.13.11")
    testImplementation(libs.pl.pragmatists.jUnitParams)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.espressoCore)
    androidTestImplementation(libs.androidx.test.uiautomator.uiautomator)

    //
    // Platform Flavor includes
    //


    //
    // UI Flavor includes
    //


    //
    // Product Flavor includes
    //

    "vision600eImplementation"(libs.vision600e.vlcAndroid.libvlc)
    "chimeraImplementation"(libs.chimera.vlcAndroid.libvlc)
//    "parkerImplementation"(libs.parker.vlcAndroid.libvlc)
    "lcguiImplementation"(libs.lcgui.vlcAndroid.libvlc)

    "parkerImplementation"(libs.parker.com.jht.virtualactive.virtualActive) {
        exclude(group = "translations", module = "translations")
    }
    "chimeraImplementation"(libs.chimera.com.jht.virtualactive.virtualActive) {
        exclude(group = "translations", module = "translations")
    }
    "lcguiImplementation"(libs.lcgui.com.jht.virtualactive.virtualActive) {
        exclude(group = "translations", module = "translations")
    }

    /*
     * NOTE:
     *
     * The bump sensor is available in multiple flavors. If a flavor doesn't use the bump sensor,
     * the files will be removed from the classpath during packaging.
     *
     * FIXME: Need to verify this assumption. If not true, simply add to chimera and projectone.
     */
    implementation(libs.com.jht.libbumpsensor.libbumpsensor){
        exclude(group = "translations", module = "translations")
    }
    //
    // Module dependencies
    //
    implementation(project(path= ":Core:Interfaces"))
    implementation(libs.bundles.jhtConsoleCommon)
    implementation(project(path= ":Core:Widgets"))
    implementation(project(path= ":System:MastBoard")) // could do "projectoneImplementation"(project(path= ":System:MastBoard")) unless we phase out P1 flavor
    implementation(project(path= ":Feature:Concierge"))
    implementation(project(path= ":Feature:ClubCom"))
    implementation(project(path= ":UI:SimpleApp"))
    implementation(project(path= ":UI:Progression"))
    implementation(project(path= ":UI:Welcome"))
    implementation(project(path= ":UI:WorkoutSelection"))
    implementation(project(path= ":UI:WorkoutSummary"))
    implementation(project(path= ":UI:DevicePairing"))
    implementation(project(path= ":UI:AppLibrary"))
    implementation(project(path= ":UI:StreamingApps"))
    implementation(project(path= ":UI:OutOfOrder"))
    implementation(project(path= ":UI:CustomHIIT"))
    implementation(project(path= ":UI:TrackApp"))
    implementation(project(path= ":UI:Landmarks"))
    implementation(project(path= ":UI:Sprint8"))
    implementation(project(path= ":UI:Seca"))
    implementation(project(path= ":UI:FirstTimeSetup"))
    implementation(project(path= ":Feature:AppStore"))
    implementation(project(path= ":UI:AppStore"))
    implementation(project(path= ":UI:SpinBikeApps"))
    implementation(project(path= ":UI:SwitchStride"))
    implementation(project(path= ":System:WorkoutData"))


    implementation(libs.androidx.annotation.annotation)

    implementation(libs.androidx.constraintlayout.constraintlayout)
    implementation(libs.androidx.percentlayout.percentlayout)

    implementation(libs.com.amazonaws.awsAndroidSdkCore)
    implementation(libs.com.amazonaws.awsAndroidSdkS3)
    implementation(libs.com.amazonaws.awsAndroidSdkCognitoidentityprovider)
    implementation(libs.com.amazonaws.awsAndroidSdkCognito)
    implementation(libs.com.amazonaws.awsAndroidSdkDdb)
    implementation(libs.com.amazonaws.awsAndroidSdkDdbMapper)

    implementation(libs.com.google.code.gson.gson)

    // Dynamic string resource replacement, translations loaded from /sdcard/jht/translation
    implementation(libs.dev.b3nedikt.restring.restring)
    implementation(libs.dev.b3nedikt.viewpump.viewpump)
    implementation(libs.dev.b3nedikt.reword.reword)

    implementation(libs.com.googlecode.jsonSimple.jsonSimple) {
        exclude("org.hamcrest", "hamcrest-core")
    }

    implementation(libs.net.lingala.zip4j.zip4j)

    implementation(libs.no.nordicsemi.android.ble)
    implementation(libs.no.nordicsemi.android.bleCommon)

    implementation(libs.androidCommonAlgorithms.androidCommonAlgorithms)

    implementation(libs.androidUiWidgets.androidUiWidgets){
        exclude(group = "translations", module = "translations")
    }
    implementation(libs.chimeraDrivers.chimeraDrivers)
    implementation(libs.gattServer.gattServer)
    implementation(libs.crashreportlib.crashreportlib)
    implementation(libs.csafelib.app)
    implementation(libs.dapi.dapiservicelib)
    implementation(libs.datamatrix.datamatrixlib)
    implementation(libs.gymkitmodule.gymkitmodule)
    implementation(libs.iptvPlayer.iptvPlayer){
        exclude(group = "translations", module = "translations")
    }
    implementation(libs.com.jht.serialport.serialport){
        exclude(group = "translations", module = "translations")
    }
    implementation(libs.com.jht.engage360.ptpNative)
    implementation(libs.translations.translations)
    implementation(libs.updatemanagerservicelib.updatemanagerservicelib)
    implementation(libs.workoutcontroller.workoutcontroller)
    implementation(libs.com.journeyapps.zxingAndroidEmbedded)
    implementation(libs.org.javaWebsocket.javaWebSocket)
    implementation(libs.com.jht.rfidreaders.rfidreaders)
    implementation(libs.johnson.web.backendlib)
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)

    // AWS IoT Device SDK v2 for Fleet Provisioning by Claim
    implementation(libs.aws.iot.device.sdk) {
        exclude(group = "software.amazon.awssdk.crt", module = "aws-crt")
        exclude(group = "software.amazon.awssdk", module = "iot")
        exclude(group = "software.amazon.awssdk", module = "sts")
    }
    implementation(libs.net.openid.appauth)
    implementation(libs.aws.crt.android)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)

    // KTOR client is used for CORE Documents API (multipart file uploading)
    implementation(libs.bundles.ktorClient)
    testImplementation(libs.bundles.ktorClientTest)

    testImplementation(libs.junit.junit)
    testImplementation(libs.org.json.json)
    testImplementation(libs.org.jetbrains.kotlinx.kotlinxCoroutinesTest)

    androidTestImplementation(libs.androidx.test.espresso.espressoContrib)
    androidTestImplementation(libs.androidx.test.espresso.espressoRemote)
    androidTestImplementation(libs.androidx.test.espresso.espressoWeb)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator.uiautomator)
    androidTestImplementation(libs.com.jht.irlibrary.irLibrary) {
        exclude(group = "translations", module = "translations")
    }
    androidTestImplementation (libs.androidx.annotation.annotation)
    androidTestImplementation(libs.androidx.test.runner)

    implementation(libs.org.jetbrains.annotations)
    androidTestImplementation(libs.com.squareup.okhttp3.mockwebserver)

    // JWT token for workout history auto login poc for web team
    implementation(libs.com.auth0.javaJwt)

    implementation(libs.androidx.navigation.navigationFragment)
    implementation(libs.androidx.navigation.navigation.ui)

    //  Compose and HILT dependencies
    implementation(libs.bundles.jhtCompose)
    debugImplementation(libs.bundles.jhtComposeDebug)
    testImplementation(libs.bundles.jhtComposeTest)
    androidTestImplementation(libs.bundles.jhtComposeAndroidTest)
    ksp(libs.bundles.jhtComposeKsp)
    kspTest(libs.bundles.jhtComposeKspTest)
    kspAndroidTest(libs.bundles.jhtComposeKspAndroidTest)
    // IHRSA Temp Code Sprint8/Landmark Fragments
    implementation("androidx.compose.ui:ui-viewbinding:1.3.2")

    implementation(libs.com.jht.liberror.liberror)
    implementation(files("libs/vortex_display.jar")) // wraps IRkDisplayDeviceManagementService

}
