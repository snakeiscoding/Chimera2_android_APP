package com.jht.updated.envVerification

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.flow.FlowScope
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

/**
 * Gradle plugin that enforces environment verification before build tasks.
 *
 * Registers a verifyEnvironment task that checks configured environment requirements
 * and displays warnings at build completion for any failures.
 */
abstract class EnvVerificationPlugin @Inject constructor(
    private val flowScope: FlowScope
) : Plugin<Project> {

    override fun apply(target: Project) {
        if (target != target.rootProject) {
            target.logger.warn("EnvVerificationPlugin should only be applied to the root project")
            return
        }

        val extension = target.extensions.create("envVerification", EnvVerification::class.java)

        val serviceProvider = target.gradle.sharedServices.registerIfAbsent(
            "envVerificationState",
            EnvVerificationService::class.java
        ) {}
        val service = serviceProvider.get()

        flowScope.always(PrintEnvWarnings::class.java) {}

        val verifyTask = registerVerifyTask(target, extension, service)
        configureTaskDependencies(target, verifyTask, service)
    }

    private fun registerVerifyTask(
        project: Project,
        extension: EnvVerification,
        service: EnvVerificationService
    ): TaskProvider<Task> {
        return project.tasks.register("verifyEnvironment") {
            group = "verification"
            description = "Runs all environment verification tasks"

            doLast {
                val buildVariants = detectBuildVariantsFromTaskGraph(project, service)
                val failures = mutableListOf<String>()

                if (buildVariants.isNotEmpty()) {
                    val commonTasks = extension.commonTasks
                    val variantSpecificTasks = extension.getVariantSpecificTasks(buildVariants)
                    val totalTasks = commonTasks.size + variantSpecificTasks.values.sumOf { it.size }

                    if (totalTasks == 0) {
                        project.logger.lifecycle("No environment verification tasks configured.")
                        return@doLast
                    }

                    project.logger.lifecycle("Running $totalTasks environment verification task(s) for variants: ${buildVariants.joinToString(", ")}")

                    if (commonTasks.isNotEmpty()) {
                        runVerificationTasks(commonTasks, "common", project.logger, failures)
                    }

                    variantSpecificTasks.forEach { (variant, tasks) ->
                        runVerificationTasks(tasks, "variant($variant)", project.logger, failures)
                    }
                } else {
                    val allTasks = extension.getAllTasks()
                    if (allTasks.isEmpty()) {
                        project.logger.lifecycle("No environment verification tasks configured.")
                        return@doLast
                    }

                    val totalTasks = allTasks.values.sumOf { it.size }
                    project.logger.lifecycle("Running $totalTasks environment verification task(s) across all variants...")

                    allTasks.forEach { (variantPattern, tasks) ->
                        val groupName = variantPattern?.let { "variant($it)" } ?: "common"
                        runVerificationTasks(tasks, groupName, project.logger, failures)
                    }
                }

                service.failureMessages = failures

                if (failures.isEmpty()) {
                    project.logger.lifecycle("All environment verification tasks passed.")
                } else {
                    project.logger.warn("${failures.size} verification failure(s) - details will be shown at end of build.")
                }
            }
        }
    }

    private fun runVerificationTasks(
        tasks: List<EnvVerificationTask>,
        groupName: String,
        logger: Logger,
        failures: MutableList<String>
    ) {
        logger.lifecycle("")
        logger.lifecycle("[$groupName]")
        tasks.forEach { task ->
            val result = task.verify()
            if (result.isSuccess) {
                logger.lifecycle("  [PASS] ${task.name}")
            } else {
                logger.warn("  [FAIL] ${task.name}")
                result.exceptionOrNull()?.message?.let { failures.add(it) }
            }
        }
    }

    private fun detectBuildVariantsFromTaskGraph(
        project: Project,
        service: EnvVerificationService
    ): Set<String> {
        val taskNames = project.gradle.startParameter.taskNames
        if (taskNames.isEmpty()) return emptySet()

        val detectedVariants = mutableSetOf<String>()

        for (taskName in taskNames) {
            val simpleName = taskName.substringAfterLast(":").lowercase()

            // Sort by length descending to prefer more specific matches
            // e.g., "chimera_mainmatrixchimeradebug" should match before "chimeradebug"
            service.knownVariants
                .sortedByDescending { it.length }
                .firstOrNull { simpleName.contains(it) }
                ?.let { detectedVariants.add(it) }
        }

        return detectedVariants
    }

    private fun configureTaskDependencies(
        project: Project,
        verifyTask: TaskProvider<Task>,
        service: EnvVerificationService
    ) {
        project.allprojects(Action {
            pluginManager.withPlugin("com.android.application") {
                val androidComponents = extensions
                    .findByType(ApplicationAndroidComponentsExtension::class.java)

                androidComponents?.onVariants { variant ->
                    service.knownVariants.add(variant.name.lowercase())
                }
            }

            tasks.configureEach(Action {
                if (TaskFilter.requiresVerification(name)) {
                    dependsOn(verifyTask)
                }
            })
        })
    }
}
