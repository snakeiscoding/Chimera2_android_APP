package com.jht.updated.envVerification

/**
 * Contract for environment verification tasks.
 * Implement this interface to add new types of pre-build verification.
 */
interface EnvVerificationTask {
    /** Unique name for this task (used in logging) */
    val name: String

    /**
     * Run the verification.
     * @return Result.success(Unit) if task passed,
     *         Result.failure(exception) with warning message if task failed
     */
    fun verify(): Result<Unit>
}
