plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "system:workoutdata"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.workoutdata"
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
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)

    // Room (currently an in-memory DB; swap to a file-backed builder to persist).
    // KSP is applied imperatively via compose.gradle.kts, so the typed `ksp(...)`
    // accessor isn't generated — use the string-based configuration like that script does.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    add("ksp", libs.androidx.room.compiler)

    testImplementation(libs.org.jetbrains.kotlinx.kotlinxCoroutinesTest)
    testImplementation(libs.junit.junit)
    testImplementation(libs.robolectric)            // runs real SQLite for Room tests on the JVM
    testImplementation(libs.androidx.test.core)      // ApplicationProvider
}