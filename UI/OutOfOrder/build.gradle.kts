plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "ui:out_of_order"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.outoforder"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
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
    implementation(libs.androidx.core.coreKtx)
    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.translations.translations)
    implementation(libs.bundles.jhtConsoleCommon)
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.espressoCore)
}