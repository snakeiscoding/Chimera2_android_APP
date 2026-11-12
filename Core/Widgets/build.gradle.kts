plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "core:widgets"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.widgets"
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
    implementation(libs.bundles.jhtConsoleCommon)
    // Dynamic string resource replacement for RestringThemedActivity
    implementation(libs.dev.b3nedikt.restring.restring)
    implementation(libs.dev.b3nedikt.viewpump.viewpump)
    implementation(libs.dev.b3nedikt.reword.reword)
    implementation(libs.bundles.imageMetaData)
    implementation(libs.translations.translations)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.compose.markdown)
    // WebViewCompat.addWebMessageListener keeps the survey bridge off other origins
    implementation(libs.androidx.webkit.webkit)
    testImplementation(libs.junit.junit)
    testImplementation(libs.org.json.json) // real org.json for unit tests (Android's is stubbed)
    testImplementation(libs.kotlinx.coroutines.test)
}