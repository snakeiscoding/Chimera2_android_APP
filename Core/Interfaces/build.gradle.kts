plugins {
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.allure.docker)
}

allureDocker {
    projectId = "core:interfaces"
    allureResultsDir = "build/allure-results"
    projectVersion = ""
    isDependentModule = true
}

apply(from = "../../compose.gradle.kts")

android {
    namespace = "com.jht.interfaces"
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
        aidl = true
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
    implementation(libs.androidx.core.coreKtx)
    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.androidx.lifecycle.lifecycleService) // for ConsoleService
    implementation(libs.org.jetbrains.kotlinx.kotlinxSerializationJson)
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.espressoCore)
    dokkaPlugin(libs.dokka.android.plugin)
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    // Replace Kotlin logo with our logo (store logo in module root folder for now)
    val dokkaBaseConfiguration = """
    {
      "customAssets": ["${file("logo-icon.svg").path.replace('\\','/')}"]
    }
    """
    pluginsMapConfiguration.set(
            // fully qualified plugin name to json configuration
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to dokkaBaseConfiguration
        )
    )
    // we can add module level documentation by including a markdown file template (HTML also seems to pick it up)
    dokkaSourceSets {
        configureEach {
            // module name defaults to project name, but we could override it in dokka
            //moduleName.set("Interfaces") // can mess with links if there is whitespace, capitals end up preceded by dash
            includes.from("module.md")
        }
    }
}

