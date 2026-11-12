plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "ui:progression"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.progression"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()
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
    implementation(project(":Core:Interfaces"))
    implementation(project(":Core:Widgets"))
    implementation(libs.bundles.jhtConsoleCommon)
    implementation(libs.translations.translations)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.io.ktor.ktorSerializationKotlinxJson)
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)
    testImplementation(libs.io.mockk.mockk)
    testImplementation(libs.app.cash.turbine.turbine)
    testImplementation(libs.org.jetbrains.kotlinx.kotlinxCoroutinesTest)
    testImplementation(libs.pl.pragmatists.jUnitParams)
}