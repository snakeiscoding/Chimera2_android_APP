package com.jht.updated

import com.jht.updated.utilities.AppUpdateParser
import com.jht.updated.utilities.BootEventParser
import com.jht.updated.utilities.ParseLogs
import com.jht.updated.utilities.ProcessLaunchParser
import com.jht.updated.utilities.TunerParser
import com.jht.updated.utilities.UpdateManagerParser
import com.jht.updated.utilities.UserClicksParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Custom tasks designed for supporting interactions with logging.
 */
class LogParsersPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("parseUserEvents") {
            group = GROUP
            doLast {
                try {
                    val parser = ParseLogs()
                    parser.addParser(ProcessLaunchParser())
                    parser.addParser(UserClicksParser())
                    parser.parseLogs(File(project.rootDir, "log_input"), File(project.rootDir, "log_output"))
                } catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }
            }
        }

        target.tasks.register("parseTunerLogs") {
            group = GROUP
            doLast {
                try {
                    val parser = ParseLogs()
                    parser.addParser(TunerParser())
                    parser.parseLogs(File(project.rootDir, "log_input"), File(project.rootDir, "log_output"))
                } catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }
            }
        }

        target.tasks.register("parseTunerAndUserLogs") {
            group = GROUP
            doLast {
                try {
                    val parser = ParseLogs()
                    parser.addParser(ProcessLaunchParser())
                    parser.addParser(UserClicksParser())
                    parser.addParser(TunerParser())
                    parser.parseLogs(File(project.rootDir, "log_input"), File(project.rootDir, "log_output"))
                } catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }
            }
        }

        target.tasks.register("parseUpdateEvents") {
            group = GROUP
            doLast {
                try {
                    val parser = ParseLogs()
                    parser.addParser(AppUpdateParser())
                    parser.addParser(BootEventParser())
                    parser.addParser(UpdateManagerParser())
                    parser.addParser(UserClicksParser())
                    parser.parseLogs(File(project.rootDir, "log_input"), File(project.rootDir, "log_output"))
                } catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }
            }
        }
    }

    companion object {
        /**
         * The group tasks for this plugin need to be registered in.
         */
        private const val GROUP = "log parser"
    }
}