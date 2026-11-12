@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jht-artifacts-651468908752.d.codeartifact.us-east-2.amazonaws.com/maven/jht-release-local/")
            credentials {
                username = "aws"
                password = Runtime.getRuntime()
                    .exec("aws codeartifact get-authorization-token --domain jht-artifacts --domain-owner 651468908752 --region us-east-2 --query authorizationToken --output text")
                    .run {
                        waitFor()
                        inputStream.bufferedReader().use { it.readText() }
                    }
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        //------ All modules -----------------------------------------------------------------------
        maven {
            setUrl("https://jht-artifacts-651468908752.d.codeartifact.us-east-2.amazonaws.com/maven/jht-release-local/")
            credentials {
                username = "aws"
                password = Runtime.getRuntime().exec("aws codeartifact get-authorization-token --domain jht-artifacts --domain-owner 651468908752 --region us-east-2 --query authorizationToken --output text").run {
                    waitFor()
                    inputStream.bufferedReader().use { it.readText() }
                }
            }
        }
        // JitPack is required for compose-markdown (com.github.jeziellago).
        // Restricted to that group so nothing else resolves from JitPack.
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.jeziellago")
            }
        }
    }
}
include(":UpdateService", ":Chimera")
include(":Core:Interfaces")
include(":Core:Widgets")
include(":System:MastBoard")
include(":System:WorkoutData")
include(":Feature:Concierge")
include(":Feature:ClubCom")
include(":Feature:AppStore")
include(":UI:SimpleApp")
include(":UI:Progression")
include(":UI:Welcome")
include(":UI:WorkoutSelection")
include(":UI:WorkoutSummary")
include(":UI:DevicePairing")
include(":UI:AppLibrary")
include(":UI:StreamingApps")
include(":UI:OutOfOrder")
include(":UI:CustomHIIT")
include(":UI:TrackApp")
include(":UI:Landmarks")
include(":UI:FirstTimeSetup")
include(":UI:Sprint8")
include(":UI:Seca")
include(":UI:AppStore")
include(":UI:SpinBikeApps")
include(":UI:SwitchStride")
