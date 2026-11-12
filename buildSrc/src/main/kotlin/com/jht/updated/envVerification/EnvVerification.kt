package com.jht.updated.envVerification

/**
 * DSL extension for configuring environment verification tasks.
 *
 * Usage in build.gradle.kts:
 * ```
 * envVerification {
 *     common {
 *         envVariables(
 *             name = "Common Environment Variables",
 *             requiredVariables = setOf("COMMON_TOKEN"),
 *             warningMessage = "Common features won't work."
 *         )
 *     }
 *     variant("parker") {
 *         envVariables(
 *             name = "Parker Environment Variables",
 *             requiredVariables = setOf("PARKER_API_KEY"),
 *             warningMessage = "Parker features won't work."
 *         )
 *     }
 * }
 * ```
 */
open class EnvVerification {
    internal val commonTasks = mutableListOf<EnvVerificationTask>()
    internal val variantSpecificTasks = mutableMapOf<String, MutableList<EnvVerificationTask>>()

    /** Configure verification tasks that run for all build variants */
    fun common(configure: EnvVerificationBuilder.() -> Unit) {
        val builder = EnvVerificationBuilder()
        builder.configure()
        commonTasks.addAll(builder.tasks)
    }

    /** Configure verification tasks that run only for a specific build variant */
    fun variant(variantName: String, configure: EnvVerificationBuilder.() -> Unit) {
        val builder = EnvVerificationBuilder()
        builder.configure()
        variantSpecificTasks.getOrPut(variantName.lowercase()) { mutableListOf() }.addAll(builder.tasks)
    }

    /** Get variant-specific tasks (excluding common) for a set of build variants */
    internal fun getVariantSpecificTasks(variants: Set<String>): Map<String, List<EnvVerificationTask>> {
        val result = mutableMapOf<String, List<EnvVerificationTask>>()

        variantSpecificTasks.forEach { (pattern, tasks) ->
            // Check if any build variants in the provided set match our defined variant-specific env verification tasks
            if (variants.any { it.lowercase().contains(pattern) }) {
                result[pattern] = tasks.toList()
            }
        }

        return result
    }

    /** Get all configured verification tasks (common + all variant-specific) */
    internal fun getAllTasks(): Map<String?, List<EnvVerificationTask>> {
        val result = mutableMapOf<String?, List<EnvVerificationTask>>()

        if (commonTasks.isNotEmpty()) {
            result[null] = commonTasks.toList()
        }

        variantSpecificTasks.forEach { (variant, tasks) ->
            result[variant] = tasks.toList()
        }

        return result
    }
}

/** Builder for adding environment verification */
open class EnvVerificationBuilder {
    internal val tasks = mutableListOf<EnvVerificationTask>()

    /** Add an environment variable verification task */
    fun envVariables(
        name: String,
        requiredVariables: Set<String>,
        warningMessage: String,
        documentationUrl: String? = null
    ) {
        addTask(
            EnvVariableVerification(
                name = name,
                requiredVariables = requiredVariables,
                warningMessage = warningMessage,
                documentationUrl = documentationUrl
            )
        )
    }

    /** Add a custom verification task */
    fun addTask(task: EnvVerificationTask) {
        tasks.add(task)
    }
}
