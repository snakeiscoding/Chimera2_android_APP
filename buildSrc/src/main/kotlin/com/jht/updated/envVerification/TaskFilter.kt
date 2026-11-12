package com.jht.updated.envVerification

/**
 * Determines which Gradle tasks should trigger environment verification.
 *
 * Tasks are matched against exclusion lists and inclusion patterns to decide
 * whether they require the verifyEnvironment task as a dependency.
 */
object TaskFilter {

    /** Task names excluded from verification (exact match, case-insensitive) */
    private val excludedTaskNames = setOf(
        "clean",
        "tasks",
        "help",
        "projects",
        "dependencies",
        "dependencyInsight",
        "properties",
        "components",
        "model",
        "verifyEnvironment",
        "wrapper",
        "init",
        "prepareKotlinBuildScriptModel",
    )

    /** Task prefixes that trigger verification (case-insensitive) */
    private val targetTaskPrefixes = setOf(
        "assemble",
        "build",
        "bundle",
        "compile",
        "kapt",
        "ksp",
        "test",
        "connected",
        "install",
        "package",
        "preBuild",
        "generate",
        "merge",
        "process",
        "stage",
        "publish",
    )

    /** Task substrings that trigger verification (case-insensitive) */
    private val targetTaskContains = setOf(
        "AndroidTest",
        "Compile",
        "Kapt",
        "Release",
        "Debug",
    )

    /**
     * Determines if a task requires environment verification.
     *
     * @param taskName The name of the Gradle task
     * @return true if the task should depend on verifyEnvironment
     */
    fun requiresVerification(taskName: String): Boolean {
        if (excludedTaskNames.any { taskName.equals(it, ignoreCase = true) }) return false
        if (targetTaskPrefixes.any { taskName.startsWith(it, ignoreCase = true) }) return true
        if (targetTaskContains.any { taskName.contains(it, ignoreCase = true) }) return true
        return false
    }
}
