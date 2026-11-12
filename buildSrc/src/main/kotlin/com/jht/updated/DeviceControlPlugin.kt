package com.jht.updated

import doLastWrapped
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Custom tasks designed to perform device control functions.
 *
 * **PLEASE DON'T REMOVE, THESE GREATLY SIMPLIFY COMMON OPERATIONS.**
 *
 * FIXME: Consider only applying these to specific projects rather than the whole workspace.
 */
class DeviceControlPlugin : Plugin<Project> {
    companion object {
        private const val DEVICE_CONTROL_GROUP = "device control"
    }

    override fun apply(project: Project) {
        project.tasks.apply {
            register("simulateClickBackButton") {
                description = "Simulate clicking the back button in Android. Convenient because we removed it via OS mods."
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "input keyevent KEYCODE_BACK")
                    }
                }
            }

            register("simulateClickHomeButton") {
                description = "Simulate clicking the home button in Android. Convenient because we removed it via OS mods."
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "input keyevent KEYCODE_HOME")
                    }
                }
            }

            register("switchToAndroidLauncher") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am start -n com.android.launcher3/com.android.launcher3.Launcher")
                    }
                }
            }

            register("switchToAndroidLauncherChimera2") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am start -n com.android.launcher3/com.android.launcher3.uioverrides.QuickstepLauncher")
                    }
                }
            }

            register("simulateGoButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_GO\"")
                    }
                }
            }

            register("simulateStopButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_STOP\"")
                    }
                }
            }

            register("simulatePauseButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_PAUSE\"")
                    }
                }
            }

            register("simulateSpeedUpButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_SPEED_UP\"")
                    }
                }
            }

            register("simulateSpeedDownButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_SPEED_DOWN\"")
                    }
                }
            }

            register("simulateInclineUpButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_INCLINE_UP\"")
                    }
                }
            }

            register("simulateInclineDownButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_INCLINE_DOWN\"")
                    }
                }
            }

            register("simulateResistanceUpButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_RESISTANCE_UP\"")
                    }
                }
            }

            /**
             * Enable the automation mode.
             */
            register("enableAutomationMode") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.loggerservice.ACTION_UPDATE_TEST_AUTOMATION_MODE\" --es value true")
                    }
                }
            }

            /**
             * Disable automation mode.
             */
            register("disableAutomationMode") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.loggerservice.ACTION_UPDATE_TEST_AUTOMATION_MODE\" --es value false")
                    }
                }
            }

            register("simulateResistanceDownButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_RESISTANCE_DOWN\"")
                    }
                }
            }

            register("simulateCooldownButton") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.keypress\" --es key_code \"KEY_COOLDOWN\"")
                    }
                }
            }

            register("disablePersistentServices") {
                description = "Disable persistent services. Needs to be followed by a reboot which could very easy be added"
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "settings put secure jht_disable_persistent 1")
                    }
                }
            }

            register("enablePersistentServices") {
                description = "Enable persistent services. Needs to be followed by a reboot which could very easy be added"
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "settings put secure jht_disable_persistent 0")
                    }
                }
            }

            register("reboot") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("reboot")
                    }
                }
            }

            register("p1ShowDebugViews") {
                group = DEVICE_CONTROL_GROUP
                doLastWrapped {
                    project.exec {
                        executable("adb")
                        args("shell", "am broadcast -a \"com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY\" --es key DATA_P1_ENABLE_DEBUG_VIEW_BOOL --es type boolean --es value true")
                    }
                }
            }
        }
    }
}