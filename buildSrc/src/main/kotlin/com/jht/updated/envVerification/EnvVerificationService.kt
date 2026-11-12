package com.jht.updated.envVerification

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Build service for tracking environment verification state across the build lifecycle.
 *
 * Registered as a shared service to maintain state between the verification task
 * and the build completion flow action.
 */
abstract class EnvVerificationService : BuildService<BuildServiceParameters.None> {
    /** Whether the warning has already been shown (prevents duplicate output) */
    var warningShown: Boolean = false

    /** Failure messages collected during verification */
    var failureMessages: List<String> = emptyList()

    /** Known build variant names discovered from AGP (lowercase for matching) */
    val knownVariants: MutableSet<String> = mutableSetOf()
}
