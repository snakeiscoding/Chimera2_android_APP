plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "feature:concierge"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.concierge"
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
    implementation(libs.androidCommonAlgorithms.androidCommonAlgorithms) // we could explore moving away from this?
    implementation(libs.johnson.web.backendlib)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)

    // KTOR client is used for web API
    implementation(libs.bundles.ktorClient)
    testImplementation(libs.bundles.ktorClientTest)
}
