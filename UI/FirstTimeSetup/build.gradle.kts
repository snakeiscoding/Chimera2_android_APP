plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "ui:first_time_setup"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.firsttimesetup"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()
        renderscriptTargetApi = 19
        renderscriptSupportModeEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(project(":Core:Widgets"))
    implementation(project(":Core:Interfaces"))
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)
    implementation(libs.androidx.navigation.navigationCompose)
    implementation(libs.bundles.jhtConsoleCommon)
    implementation(libs.translations.translations)
    testImplementation(libs.pl.pragmatists.jUnitParams)
    testImplementation(libs.io.mockk.mockk)
    testImplementation(libs.org.jetbrains.kotlinx.kotlinxCoroutinesTest)
    testImplementation(libs.app.cash.turbine.turbine)
}