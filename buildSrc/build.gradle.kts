plugins {
    id("groovy")
    alias(libs.plugins.dokka)
    `kotlin-dsl`
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation (gradleApi())
    implementation (localGroovy())

    /*
     * Does this do the same as dependencies { classpath('com.android.tools.build:gradle:7.2.0') }?
     * com.android.tools.build:gradle is needed for com.android.build.api.dsl.BaseFlavor import
     * otherwise we can remove this and refactor the code with changes similar to:
     * https://bitbucket.org/johnsonhealthtech/chimera2/commits/00805b2201ddf62b4b63052ebd90bcf528d73685
     * https://bitbucket.org/johnsonhealthtech/chimera2/commits/e86c48f331b851a64cdcb261a8bcd21f116bd6ba
     * This buildSrc directory appears to distribute to other build.gradle files
     */
    implementation(libs.com.android.tools.build.gradle)
    implementation(libs.org.jetbrains.kotlin.kotlinGradlePlugin)
    implementation(libs.javapoet) // HILT issue https://github.com/google/dagger/issues/3068
    implementation(libs.net.lingala.zip4j.zip4j)
    implementation(libs.org.apache.commons.commonsIo)
    implementation(libs.net.dongliu.apkParser)
    implementation(libs.org.jetbrains.annotations)
    implementation(libs.org.json.json)
    implementation(libs.javax.xml.bind.jaxbApi)
    implementation(libs.org.bouncycastle.bcprovJdk18on)

    testImplementation(libs.org.jetbrains.kotlin.kotlinTest)
}