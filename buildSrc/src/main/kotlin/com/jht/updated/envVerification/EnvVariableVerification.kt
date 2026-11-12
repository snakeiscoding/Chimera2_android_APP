package com.jht.updated.envVerification

/**
 * Verifies that required environment variables are set.
 *
 * @param name Display name for this environment variable verification task (shown in pass/fail logging)
 * @param requiredVariables Set of environment variable names to verify
 * @param warningMessage Custom message describing what features are affected if variables are missing
 * @param documentationUrl Optional URL with instructions for setting up the variables
 */
class EnvVariableVerification(
    override val name: String,
    private val requiredVariables: Set<String>,
    private val warningMessage: String,
    private val documentationUrl: String? = null
) : EnvVerificationTask {

    override fun verify(): Result<Unit> {
        val missing = requiredVariables.filter { System.getenv(it) == null }

        return if (missing.isEmpty()) {
            Result.success(Unit)
        } else {
            val message = buildString {
                appendLine("[$name]")
                append("Missing: ")
                appendLine(missing.joinToString(", "))
                appendLine(warningMessage)
                if (documentationUrl != null) {
                    append("See: $documentationUrl")
                }
            }.trimEnd()
            Result.failure(IllegalStateException(message))
        }
    }
}
