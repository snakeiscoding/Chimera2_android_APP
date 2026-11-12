package com.jht.updated.envVerification

import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference

/**
 * Flow action that prints environment verification warnings at build completion.
 *
 * Registered via FlowScope.always() to run after all tasks complete,
 * ensuring warnings are visible at the end of build output.
 */
class PrintEnvWarnings : FlowAction<PrintEnvWarnings.Parameters> {

    interface Parameters : FlowParameters {
        @get:ServiceReference("envVerificationState")
        val service: Property<EnvVerificationService>
    }

    override fun execute(parameters: Parameters) {
        val state = parameters.service.get()
        if (state.failureMessages.isNotEmpty() && !state.warningShown) {
            state.warningShown = true
            printWarning(state.failureMessages)
        }
    }

    private fun printWarning(failureMessages: List<String>) {
        val header = "!  WARNING: ENVIRONMENT VERIFICATION FAILURES"
        val contentPrefix = "!    "

        val maxContentLength = failureMessages
            .flatMap { it.lines() }
            .maxOfOrNull { it.length } ?: 0
        val minWidth = maxOf(header.length, contentPrefix.length + maxContentLength) + 3

        val border = "!".repeat(minWidth)
        val separator = "!  " + "-".repeat(minWidth - 3)

        println(buildString {
            appendLine()
            appendLine(border)
            appendLine(header)
            appendLine(border)
            failureMessages.forEachIndexed { index, message ->
                appendLine("!")
                message.lines().forEach { line ->
                    appendLine("$contentPrefix$line")
                }
                if (index < failureMessages.lastIndex) {
                    appendLine("!")
                    appendLine(separator)
                }
            }
            appendLine("!")
            appendLine(border)
        })
    }
}
