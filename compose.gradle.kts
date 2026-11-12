/*
    Dependencies for JetPack Compose modules
    Will probably be phased out as we convert modules from old gradle to kts
 */

//https://stackoverflow.com/questions/61559352/add-jetpack-compose-to-existing-project
/*
    https://dagger.dev/hilt/gradle-setup
    Seems to need a classpath up at top level gradle dependencies>
        classpath("com.google.dagger:hilt-android-gradle-plugin:???")
    Just apply this gradle in the UI module
        apply from: "../../compose.gradle"

    There seems to be an issue of javapoet needing to be a newer version
    https://github.com/google/dagger/issues/3068
 */

// kapt is being phased out for ksp, but HILT did not support ksp yet
apply (plugin = "com.google.devtools.ksp")
apply (plugin = "com.google.dagger.hilt.android")

val libs = extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")

dependencies {
    //  Compose and HILT dependencies
    add("implementation", libs.findBundle("jhtCompose").get())
    add("debugImplementation", libs.findBundle("jhtComposeDebug").get())
    add("testImplementation", libs.findBundle("jhtComposeTest").get())
    add("androidTestImplementation", libs.findBundle("jhtComposeAndroidTest").get())
    add("ksp", libs.findBundle("jhtComposeKsp").get())
    add("kspTest", libs.findBundle("jhtComposeKspTest").get())
    add("kspAndroidTest", libs.findBundle("jhtComposeKspAndroidTest").get())
}

