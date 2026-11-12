import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.jht.updated.DefaultConfigs
import com.jht.updated.bundler.JHTFlavor
import com.jht.updated.putStringBuildConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Custom doLast implementation that wraps the given action in a try-catch block that automatically
 * outputs the stacktrace of any caught exception before rethrowing the exception. This is a
 * convenience method that removes the need to rerun tasks on failure with `--stacktrace`.
 *
 * @param action The action to perform.
 * @return This [Task] for chaining.
 * @throws Exception If anything goes wrong during execution of [action].
 */
@Throws(Exception::class)
fun Task.doLastWrapped(action: Task.() -> Unit): Task = doLast {
    try {
        action()
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

/**
 * Runs a string as a command in the shell. Throws [IOException] if command produces error stream.
 */
fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
    suppressErrors: Boolean = false
): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(timeoutAmount, timeoutUnit) }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (!suppressErrors and error.isNotEmpty()) {
            throw IOException(error)
        }
        inputStream.bufferedReader().readText().trim()
    }

private const val OPS_CLAIM_CERT_ROLE_ARN = "arn:aws:iam::401257549720:role/fleet-claim-cert-build-access"

fun findAwsExecutable(): String {
    return try {
        val process = ProcessBuilder("which", "aws")
            .redirectErrorStream(true)
            .start()
        val path = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        path.ifEmpty { "aws" }
    } catch (e: Exception) {
        "aws"
    }
}

/**
 * Reads an SSM parameter from jht-ops by auto-assuming the cross-account role.
 * Build environment just needs jht-engineering AWS account credentials
 */
fun getOpsSsmParameter(name: String, region: String = "us-west-2"): String {
    val creds = assumeRole(OPS_CLAIM_CERT_ROLE_ARN)

    val os = System.getProperty("os.name").lowercase()
    val awsExecutable = if (os.contains("mac")) {
        // Note: can't figure why this doesn't run on mac even when aws is in PATH
        // It seems like using pb.environment() causes it to break
        findAwsExecutable()
    } else {
        "aws"
    }

    val pb = ProcessBuilder(
        awsExecutable, "ssm", "get-parameter",
        "--region", region,
        "--name", name,
        "--with-decryption",
        "--query", "Parameter.Value",
        "--output", "text"
    )
    pb.environment()["AWS_ACCESS_KEY_ID"] = creds.accessKeyId
    pb.environment()["AWS_SECRET_ACCESS_KEY"] = creds.secretAccessKey
    pb.environment()["AWS_SESSION_TOKEN"] = creds.sessionToken
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
    pb.redirectError(ProcessBuilder.Redirect.PIPE)
    val process = pb.start()
    process.waitFor(60, TimeUnit.SECONDS)
    val error = process.errorStream.bufferedReader().readText().trim()
    if (error.isNotEmpty()) throw IOException(error)
    return process.inputStream.bufferedReader().readText().trim()
}

private data class StsCreds(val accessKeyId: String, val secretAccessKey: String, val sessionToken: String)


private fun assumeRole(roleArn: String): StsCreds {
    val json = "aws sts assume-role --role-arn $roleArn --role-session-name gradle-build --output json".runCommand()
    val credentials = groovy.json.JsonSlurper().parseText(json) as Map<*, *>
    val creds = credentials["Credentials"] as Map<*, *>
    return StsCreds(
        accessKeyId = creds["AccessKeyId"] as String,
        secretAccessKey = creds["SecretAccessKey"] as String,
        sessionToken = creds["SessionToken"] as String,
    )
}

data class GitInfo(val branch: String, val commitId: String)

private var gitInfo: GitInfo? = null

/**
 * abstracted to separate function as Allure Reporting on Apply Plugins needs to know what the local version is before this is set later on.
 * Feel like it maybe best practice to cache this as to not needlessly run more commands
 */
fun getGitInfo(rootDir: File) : GitInfo {
    if(gitInfo != null)
        return gitInfo!!
    else {
        // found gradle plugins for git info, but had problems getting them to work, so just brute forcing this now based on online examples
        // technically does not need to be set to rootDir explicitly, just being specific cause I can
        val branch = "git rev-parse --abbrev-ref HEAD".runCommand(workingDir = rootDir).replace("/", "_")
        val commitId = "git rev-parse HEAD".runCommand(workingDir = rootDir)
        println("Git info $branch $commitId")
        return GitInfo(branch,commitId)
    }
}

/**
 * Abstracted to function for Allure Reporting to get this value at the apply plugins stage
 */
fun getLocalVersion(gitInfo: GitInfo) : String {
    //Date and branch for the local version. Changes to this value cause the build config to
    //regenerate and fully rebuild the app. We're limiting this timestamp to the day for
    //iterative build performance.
    val date = LocalDateTime.now()
    return "${date.year}.${date.monthValue}.${date.dayOfMonth}.0.${gitInfo.branch}.${gitInfo.commitId}"
}

/**
 * Get the custom application android component extensions that needs to be applied to build
 * bundles.
 */
fun getAndroidComponents(buildBundle: com.jht.updated.bundler.BuildBundle, project: Project): Action<ApplicationAndroidComponentsExtension> = Action<ApplicationAndroidComponentsExtension> {
    val gitInfo = getGitInfo(project.rootDir)
    val epochMs = 1767247200000L //Jan 1, 2026 12AM Central Time
    val localVersion = if (USE_RC_VERSIONS(project)) null
        else {
            getLocalVersion(gitInfo)
        }

    /*
     * Automatically enable/disable unsupported flavor combinations. This is the replacement for
     * the legacy variantFilter implementation.
     */
    beforeVariants { avb ->
        val product = avb.productFlavors.find { it.first == "product" }?.second ?: error("Missing product flavor")
        val ui = avb.productFlavors.find { it.first == "ui" }?.second ?: error("Missing ui flavor")
        val platform = avb.productFlavors.find { it.first == "platform" }?.second ?: error("Missing platform flavor")
        avb.enable = JHTFlavor.flavor(product, ui, platform) != null
    }

    /*
     * Processing functionality that happens on each valid variant. This is responsible for doing
     * the following:
     *
     * 1. Overriding the versionName value for the VariantOutput. This is done to ensure the correct
     * version of software is displayed.
     * 2. Set the build config fields.
     *
     * This is the replacement for previous android.applicationVariants implementation.
     */
    onVariants { avb ->
        val product = avb.productFlavors.find { it.first == "product" }?.second ?: error("Missing product flavor")
        val ui = avb.productFlavors.find { it.first == "ui" }?.second ?: error("Missing ui flavor")
        val platform = avb.productFlavors.find { it.first == "platform" }?.second ?: error("Missing platform flavor")

        val flavor = JHTFlavor.flavor(product, ui, platform) ?: error("Unsupported flavor encountered: ($product, $ui, $platform)")
        val version = localVersion ?: buildBundle.version(flavor)
        avb.outputs.forEach { it.versionName.set(version) }

        // Apply the custom build config fields for this flavor.
        DefaultConfigs().applyConfigs(avb, flavor, version)
        // Build info common to all variants
        avb.putStringBuildConfig("GIT_BRANCH", gitInfo.branch, "Git branch at build time")
        avb.putStringBuildConfig("GIT_HASH", gitInfo.commitId, "Last git commit hash at build time")
        avb.buildConfigFields?.put("BUILD_TIME", BuildConfigField("java.util.Date", "new java.util.Date(${epochMs}L)", "The current system time at the time of this build."))
    }
}