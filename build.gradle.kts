import com.jht.updated.envVerification.EnvVerificationPlugin
import com.jht.updated.envVerification.EnvVerification

plugins {
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.allure.docker) apply false

    // Make the Compose compiler plugin available (from TOML)
    alias(libs.plugins.kotlin.compose) apply false
}

// Include both available bundling plugins.
apply<com.jht.DeployPlugin>()
apply<com.jht.updated.bundler.MergedBuildTask>()

// Include the other plugins.
apply<com.jht.updated.DeviceControlPlugin>()
apply<com.jht.updated.TestsPlugin>()
apply<com.jht.updated.LogParsersPlugin>()
apply<EnvVerificationPlugin>()

// Apply Compose compiler plugin automatically to all Kotlin modules
subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")
    }
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")
    }
}

// net.openid:appauth is pulled in transitively (via the JHT console artifacts) and its manifest
// declares an activity with android:scheme="${appAuthRedirectScheme}". Library modules never use
// that redirect activity, but the placeholder still has to resolve for the androidTest manifest
// merge to succeed, so give every library a default instead of repeating it module by module.
//
// This only hooks "com.android.library". Application modules (Chimera, UpdateService) declare
// their own real scheme in their own defaultConfig and are deliberately left alone. Even for a
// library, this callback runs when the plugin is applied - before the module's own android {}
// block - so a module-level value still wins if one is ever needed.
subprojects {
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.gradle.LibraryExtension> {
            defaultConfig.manifestPlaceholders["appAuthRedirectScheme"] = "com.jht.chimera"
        }
    }
}

allprojects {
    // Necessary exclusions while transitioning to the newer artifacts
    configurations.all {
        exclude("datamatrix", "datamatrixlib")
        exclude("android_common_algorithms", "android_common_algorithms")
        exclude("org.xwalk", "xwalk_core_library") // don't include xwalk library included in PTP artifact
    }
}

// Gradle prints nothing for a passing test task, so terminal runs show no result. One summary line per module.
// Note: afterSuite is not configuration-cache compatible and is removed in Gradle 9.
allprojects {
    tasks.withType<Test>().configureEach {
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
        }
        afterSuite(
            KotlinClosure2<TestDescriptor, TestResult, Unit>({ descriptor, result ->
                // root suite, so the counts cover the whole module
                if (descriptor.parent == null) {
                    val tag = if (result.failedTestCount > 0) "FAILED" else "ok"
                    println(
                        "[$tag] ${project.path}: ${result.testCount} tests, " +
                            "${result.successfulTestCount} passed, " +
                            "${result.failedTestCount} failed, " +
                            "${result.skippedTestCount} skipped"
                    )
                }
            })
        )
    }
}


task("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Configure environment verification tasks (environment variable checks, etc)
configure<EnvVerification> {

    // Common environment verification tasks
    common {}

    // Commercial platform environment verification tasks (Chimera, ProjectOne, LCGC)
    variant("chimera") {
        /* Example for adding required environment variables
        envVariables(
            name = "CORE Environment Variables (commercial)",
            requiredVariables = setOf("CORE_CONSOLE_CONFIG_PASSWORD"),
            warningMessage = "Commercial CORE features will function incorrectly without these environment variables (ex: CORE Console Config, TV Channel List Internet Sync).",
            documentationUrl = "https://jhtnaee.atlassian.net/wiki/x/FoC9JgE"
        )*/
    }

    /**
     * We can add other forms of environment verification as needed, such as:
     * - File existence verification (e.g., keystores, config files)
     * - Tool/version verification (e.g., Java version, NDK version)
     * - Network connectivity verification (e.g., artifact repositories)
     *
     * To add a new verification type, implement [com.jht.updated.envVerification.EnvVerificationTask] and add
     * it to the DSL using [com.jht.updated.envVerification.EnvVerificationBuilder.addTask].
     */
}