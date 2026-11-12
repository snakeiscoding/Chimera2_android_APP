import com.jht.updated.bundler.bundle.UPDATER
import com.jht.updated.keystore

plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    alias (libs.plugins.allure.docker)
    id ("generate-oauth-credentials")
}

allureDocker {
    projectId = "chimera_update_service"
    allureResultsDir = "build/allure-results"
    projectVersion = "" //Dependent module so value is ignored
    isDependentModule = true
}

// Ensure the correct Android components are applied.
androidComponents(getAndroidComponents(UPDATER, project))

android {
    namespace = "com.jht.updateservice"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        versionCode = libs.versions.thisVersionCode.get().toInt()
        versionName = libs.versions.thisVersionName.get()

        minSdk = libs.versions.minSdk.get().toInt()
        //noinspection ExpiredTargetSdkVersion
        targetSdk = libs.versions.targetSdk.get().toInt()
        multiDexEnabled = libs.versions.multiDexEnabled.get().toBoolean()
        vectorDrawables.useSupportLibrary = libs.versions.useSupportLibrary.get().toBoolean()

        ndk { abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")) }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        manifestPlaceholders["appAuthRedirectScheme"] = "com.jht.updateservice"
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
            externalNativeBuild { cmake { arguments("-DFLAVOR_HW=parker") } }
        }

        register("chimera") {
            dimension = "platform"
            signingConfig = signingConfigs.getByName("signingChimera")
            externalNativeBuild { cmake { arguments("-DFLAVOR_HW=chimera") } }
        }

        // lcgui is similar to Chimera but on Parker hardware, using daughterboards for some extra functions
        register("lcgui") {
            dimension = "platform"
            signingConfig = signingConfigs.getByName("signingParker")
            externalNativeBuild { cmake { arguments("-DFLAVOR_HW=parker") } }
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
        }
    }

    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation (libs.androidx.appcompat.appcompat)
    implementation (libs.androidx.fragment.fragment)
    implementation (libs.androidx.core.core)
    implementation (libs.androidx.gridlayout.gridlayout)
    implementation (libs.androidx.legacy.legacySupportCoreUi)
    implementation (libs.androidx.legacy.legacySupportCoreUtils)
    implementation (libs.androidx.legacy.legacySupportV13)
    implementation (libs.androidx.media.media)
    implementation (libs.androidx.multidex.multidex)
    implementation (libs.androidx.recyclerview.recyclerview)
    implementation (libs.androidx.vectordrawable.vectordrawable)
    implementation (libs.androidx.constraintlayout.constraintlayout)
    implementation (libs.net.lingala.zip4j.zip4j)
    implementation (libs.org.jetbrains.annotations)
    implementation (libs.updatemanagerservicelib.updatemanagerservicelib)
    implementation (libs.translations.translations)
    implementation(libs.johnson.web.backendlib)
    implementation (libs.androidCommonAlgorithms.androidCommonAlgorithms)
    testImplementation (libs.junit.junit)
    androidTestImplementation (libs.androidx.test.rules)
    androidTestImplementation (libs.androidx.test.runner)
    androidTestImplementation (libs.androidx.test.espresso.espressoCore)
    androidTestImplementation (libs.androidx.test.uiautomator.uiautomator)
}
