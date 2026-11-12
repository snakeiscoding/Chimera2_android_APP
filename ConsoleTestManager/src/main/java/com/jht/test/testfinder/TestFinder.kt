package com.jht.test.testfinder

import com.github.javaparser.JavaParser
import java.io.File

/**
 * Notes
 * The test finder should auto generate a list of tests to run
 * We can then spawn another Jenkins task passing it the test to run.  Curl example, but we can embed here.
 * curl -X POST http://YOUR_JENKINS_URL/job/YOUR_JOB/build?TOKEN=YOUR_API_TOKEN
 *
 * So overall steps on Jenkins build.
 * Dev Server
 *      Assemble Console Test Manager and Chimera Software.
 *      generateCIPackages
 *      call DQA Jenkins server
 * DQA Server
 *      Find and update all consoles.
 *      Create test cycle
 *      Find all tests using Java Parser
 *      Call a Jenkins job for each test passing it the test parameters.  Those jobs will report
 *          results to Zephyr
 *
 */
class TestFinder {

    fun findTests(path: File) : MutableList<InstrumentationTest> {
        val tests: MutableList<InstrumentationTest> = ArrayList()
        if(path.isDirectory) {
            findTestsInDir(path, tests)
        }
        else if(path.isFile) {
            findTestInFile(path, tests)
        }
        return tests
    }
    private fun findTestsInDir(path: File, tests: MutableList<InstrumentationTest>) {
        if(path.isDirectory) {
            for (file in path.listFiles()!!) {
                if (file.isDirectory) {
                    findTestsInDir(file, tests)
                } else if (file.extension == "java") {
                    findTestInFile(file, tests)
                }
            }
        }
    }

    private fun findTestInFile(sourceFile: File, tests: MutableList<InstrumentationTest>) {
        val parseResult = JavaParser().parse(sourceFile)

        val compilationUnit = parseResult.result?.get()

        val clazz = compilationUnit?.getClassByName(compilationUnit.primaryTypeName?.get())
        if(clazz?.isPresent == true) {
            for(method in clazz.get().methods!!) {
                val test = InstrumentationTest(
                    pkg = compilationUnit.packageDeclaration.get().nameAsString,
                    clazz = clazz.get().nameAsString,
                    function = method.nameAsString,
                    runner = "com.jht.chimera.test/.runner.MainAppTestRunner"
                )
                var isTestMethod = false
                var isAutomated = true
                for (annotation in method.annotations) {
                    when (annotation.name.toString()) {
                        "MachineSettings" -> test.runner =
                            "com.jht.chimera.test/.runner.MachineSettingsTestRunner"
                        "SplashApp" -> test.runner = "com.jht.chimera.test/.runner.SplashAppTestRunner"
                        "MainApp" -> test.runner = "com.jht.chimera.test/.runner.MainAppTestRunner"
                        "MachineSetup" -> test.runner =
                            "com.jht.chimera.test/.runner.MachineSetupTestRunner"
                        "Default" -> test.runner = "com.jht.chimera.test/.runner.MainAppTestRunner"
                        "Test" -> isTestMethod = true
                        "NotAutomated" -> isAutomated = false
                    }
                }
                if (isTestMethod && isAutomated) {
                    if (method.javadoc.isPresent) {
                        val javadoc = method.javadoc?.get()
                        if (javadoc != null) {
                            test.description = javadoc.description.toText().replace("\n", " ")
                            for (block in javadoc.blockTags) {

                                when (block.tagName.toString()) {
                                    "test_name" -> test.name = block.content.toText()
                                    "requirement" -> test.requirement = block.content.toText()
                                    "uuid" -> test.uuid = block.content.toText().trim().trimEnd()
                                }
                            }
                        }
                    }
                    tests.add(test)
                }
            }
        }
    }
}