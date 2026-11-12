package com.jht.updated

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class TestsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val switchToAndroidLauncher: Task by lazy {
            project.tasks.getByName("switchToAndroidLauncher")
        }

        project.tasks.apply {
            val chimeraMainAppTests = register("chimeraMainAppTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.MainApp com.jht.chimera.test/.runner.MainAppTestRunner")
                    }
                }
            }

            val chimeraSplashTests = register("chimeraSplashTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.SplashApp com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("chimeraSplashConsoleSwapTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.SplashAppConsoleSwap com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("chimeraSplashQuickGoTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.SplashAppQuickGo com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("chimeraTargetTrainingSetupTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.TargetTrainingSetupRunner com.jht.chimera.test/.runner.MainAppTestRunner")
                    }
                }
            }

            val chimeraMachineSettingsTests = register("chimeraMachineSettingsTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.MachineSettings com.jht.chimera.test/.runner.MachineSettingsTestRunner")
                    }
                }
            }

            val chimeraMachineSetupTests = register("chimeraMachineSetupTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.MachineSetup com.jht.chimera.test/.runner.MachineSetupTestRunner")
                    }
                }
            }

            val chimeraDefaultTests = register("chimeraDefaultTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.runner.Default com.jht.chimera.test/androidx.test.runner.AndroidJUnitRunner")
                    }
                }
            }

            register("ifitTest") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e class com.jht.chimera.ui.IFitWorkoutTest com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("allChimeraInstrumentationTests") {
                group = "tests"
                dependsOn(chimeraMainAppTests, chimeraSplashTests, chimeraMachineSettingsTests, chimeraMachineSetupTests, chimeraDefaultTests)
            }

            val chimeraEmulatorMainAppTests = register("chimeraEmulatorMainAppTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MainApp com.jht.chimera.test/.runner.MainAppTestRunner")
                    }
                }
            }

            val chimeraEmulatorSplashTests = register("chimeraEmulatorSplashTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.SplashApp com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("chimeraEmulatorSplashConsoleSwapTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.SplashAppConsoleSwap com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("chimeraEmulatorSplashQuickGoTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.SplashAppQuickGo com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }

            register("chimeraEmulatorTargetTrainingSetupTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.TargetTrainingSetupRunner com.jht.chimera.test/.runner.MainAppTestRunner")
                    }
                }
            }

            val chimeraEmulatorMachineSettingsTests = register("chimeraEmulatorMachineSettingsTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MachineSettings com.jht.chimera.test/.runner.MachineSettingsTestRunner")
                    }
                }
            }

            val chimeraEmulatorMachineSetupTests = register("chimeraEmulatorMachineSetupTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MachineSetup com.jht.chimera.test/.runner.MachineSetupTestRunner")
                    }
                }
            }

            val chimeraEmulatorDefaultTests = register("chimeraEmulatorDefaultTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", "am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.Default com.jht.chimera.test/androidx.test.runner.AndroidJUnitRunner")
                    }
                }
            }

            register("allChimeraEmulatorInstrumentationTests") {
                group = "tests"
                dependsOn(chimeraEmulatorMainAppTests, chimeraEmulatorSplashTests, chimeraEmulatorMachineSettingsTests, chimeraEmulatorMachineSetupTests, chimeraEmulatorDefaultTests)
            }

            register("backupSettings") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("root")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "mkdir -p /sdcard/test/backup")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "rm -rf /sdcard/test/backup/*")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "cp -rf /data/data/com.jht.chimera/files/settings/* /sdcard/test/backup")
                    }
                }
            }

            register("restoreSettings") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("root")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "rm -rf /data/data/com.jht.chimera/files/settings/*")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "cp -rf /sdcard/test/backup/* /data/data/com.jht.chimera/files/settings")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "find /data/data/com.jht.chimera -not -name \"lib\" -print0 | xargs -0 chown `ls -l /data/data | grep com.jht.chimera | tr -s ' ' | cut -d' ' -f3`")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "find /data/data/com.jht.chimera -not -name \"lib\" -print0 | xargs -0 chgrp `ls -l /data/data | grep com.jht.chimera | tr -s ' ' | cut -d' ' -f4`")
                    }
                    project.exec {
                        executable("adb")
                        args("shell", "find /data/data/com.jht.chimera -not -name \"lib\" -print0 | xargs -0 restorecon")
                    }
                }
            }

            register("matrixCommServiceTests") {
                group = "tests"
                dependsOn(switchToAndroidLauncher)
                doLast {
                    project.exec {
                        executable("adb")
                        args("shell", " am instrument -w -e class com.jht.chimera.service.matrixcomm.MatrixCommTest com.jht.chimera.test/.runner.SplashAppTestRunner")
                    }
                }
            }
        }
    }
}