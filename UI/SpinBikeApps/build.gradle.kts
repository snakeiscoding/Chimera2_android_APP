plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "ui:spin_bike_apps"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.spinbikeapps"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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

    // TargetTrainingViewModel resolves strings at construction, so Robolectric needs the merged resources
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
    implementation(libs.androidx.graphics.shapes.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)

    testImplementation(libs.io.mockk.mockk)
    testImplementation(libs.androidx.compose.ui.uiTestJunit4)
}