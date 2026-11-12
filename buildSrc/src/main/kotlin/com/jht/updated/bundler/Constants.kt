package com.jht.updated.bundler

import com.jht.updated.get
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Whether to delete staging directories or not.
 *
 * fixme: In production, this should be true
 */
const val DELETE_STAGING_DIRECTORIES = false

const val CORE_PACKAGE_GROUP = "core"
const val THIRD_PARTY_PACKAGE_GROUP = "thirdpartyapps"

val chimeraAWSBuildAssets = "s3://jht-build-assets/BuildAssets/Chimera"
val parkerAWSBuildAssets = "s3://jht-build-assets/BuildAssets/Parker"
val projectOneAWSBuildAssets = "s3://jht-build-assets/BuildAssets/projectone"

val AWS_NIGHTLY_REGION = "us-east-2"
val AWS_PROD_REGION = "us-west-2"
val AWS_CORE_BUCKET = "jhtsoftware"
val AWS_DOORE_BUCKET = "jht-nightly-builds"

//fixme get Project.rootDir otherwise this may break
val buildAssetsDir = File (Properties().apply {
    load(FileInputStream(File("local.properties")))
}.getProperty("buildAssets"))
val chimeraBuildAssetsDir = File("${buildAssetsDir.absolutePath}/Chimera")
val parkerBuildAssetsDir = File("${buildAssetsDir.absolutePath}/Parker")
val projectOneBuildAssetsDir = File("${buildAssetsDir.absolutePath}/projectone")