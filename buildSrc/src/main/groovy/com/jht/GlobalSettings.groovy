package com.jht

import org.gradle.api.Project

final class GlobalSettings {
    private GlobalSettings() {}

    static boolean USE_UPDATED_DEPLOY_TASKS(Project project) {
        Properties projectProperties = new Properties()
        File propertiesFile = new File(project.projectDir, "buildSrc/gradle.properties")
//        println("Properties File: " + propertiesFile.getAbsolutePath())
        FileInputStream fs = new FileInputStream(propertiesFile)
        projectProperties.load(fs)
        return projectProperties.getProperty("USE_UPDATED_BUNDLING") == "true"
    }
}