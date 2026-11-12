plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "feature:app_store"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.appstore"
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
    //TODO implementation libs.bundles.jhtConsoleCommon  refactor from develop
    implementation(project(":Core:Widgets"))
    implementation(libs.translations.translations)
    implementation(libs.androidCommonAlgorithms.androidCommonAlgorithms) // we could explore moving away from this?
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)

    // KTOR client is used for web API
    //TODO implementation(libs.bundles.ktorClient)  refactor from develop
    implementation(libs.io.ktor.ktorClientCore)
    implementation(libs.io.ktor.ktorClientAndroid)
    implementation(libs.io.ktor.contentNegotiation)
    implementation(libs.io.ktor.ktorSerializationKotlinxJson)
    testImplementation(libs.io.ktor.ktorClientMock)
    testImplementation(libs.io.mockk.mockk)
    testImplementation(libs.org.jetbrains.kotlinx.kotlinxCoroutinesTest)
    //TODO testImplementation(libs.bundles.ktorClientTest)  refactor from develop
}