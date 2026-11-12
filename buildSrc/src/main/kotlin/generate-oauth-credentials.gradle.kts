import com.jht.aws.AwsSecrets.getSecret
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.Security
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Generates a BKS keystore containing JUMP OAuth credentials
 * and fleet claim certificates. Password derived from
 * APK signing certificate to match BksCredentialProvider.derivePassword().
 *
 * Each platform flavor gets its own BKS in src/{flavor}/assets/
 * because different signing certs produce different passwords.
 */

tasks.register("generateOAuthCredentials") {
    group = "build"
    description = "Generate BouncyCastle keystore with fleet credentials from AWS"

    // Track outputs so Gradle skips re-generation when BKS files exist
    val appExt = project.extensions.getByType(com.android.build.gradle.AppExtension::class.java)
    appExt.productFlavors
        .filter { it.signingConfig != null }
        .forEach { flavor ->
            outputs.file(file("src/${flavor.name}/assets/fleet-credentials.bks"))
        }

    doLast {
        Security.addProvider(BouncyCastleProvider())
        val useBuildServer = USE_RC_VERSIONS(project)
        val base = if (useBuildServer) "/CodeBuild/" else "/local-dev/"

        // JUMP OAuth credentials from Secrets Manager in jht-engineering AWS account
        val oauthKeys = listOf(
            "JUMP_CLIENT_ID",
            "JUMP_CLIENT_SECRET",
            "JUMP_DEV_CLIENT_ID",
            "JUMP_DEV_CLIENT_SECRET"
        )
        val secrets = oauthKeys.associateWith { key -> getSecret(base, key) }.toMutableMap()

        // CORE console config password from Secrets Manager
        secrets["CORE_CONSOLE_CONFIG_PASSWORD"] = getSecret(base, "CORE_CONSOLE_CONFIG_PASSWORD")

        // API keys for various services and integrations
        secrets["CALIBRATION_API_KEY"] = getSecret(base, "CALIBRATION_API_KEY")
        secrets["EGYM_API_KEY"] = getSecret(base, "EGYM_API_KEY")
        secrets["MYZONE_API_KEY"] = getSecret(base, "MYZONE_API_KEY") // This is for MATUS001
        secrets["HOTP_SALT_TOKEN"] = getSecret(base, "HOTP_SALT_TOKEN")
        secrets["SSO_TOTP_SECRET"] = getSecret(base, "SSO_TOTP_SECRET")
        secrets["WORKOUT_HISTORY_JWT_SECRET"] = getSecret(base, "WORKOUT_HISTORY_JWT_SECRET")
        secrets["USER_PROFILE_MANAGEMENT_PASSWORD"] = getSecret(base, "USER_PROFILE_MANAGEMENT_PASSWORD")
        secrets["SSO_ZIP_PASSWORD"] = getSecret(base, "SSO_ZIP_PASSWORD")
        secrets["UPDATE_ZIP_PASSWORD"] = getSecret(base, "UPDATE_ZIP_PASSWORD")

        // API keys per-environment for the Concierge service
        secrets["CONCIERGE_PRODUCTION_SECRET"] = getSecret(base, "CONCIERGE_PRODUCTION_SECRET")
        secrets["CONCIERGE_STAGING_SECRET"] = getSecret(base, "CONCIERGE_STAGING_SECRET")
        secrets["CONCIERGE_QA_SECRET"] = getSecret(base, "CONCIERGE_QA_SECRET")
        secrets["CONCIERGE_DEV_SECRET"] = getSecret(base, "CONCIERGE_DEV_SECRET")

        // Fleet claim certificates from SSM in jht-ops
        // Both prod and dev certs bundled and then runtime selects based on deployment
        // Claim keys and certs are only used when no SGM is available
        secrets["FLEET_CLAIM_CERT_PEM"] = getOpsSsmParameter("/fleet/prod/claim/cert-pem")
        secrets["FLEET_CLAIM_PRIVATE_KEY"] = getOpsSsmParameter("/fleet/prod/claim/private-key")
        secrets["FLEET_NONPROD_CLAIM_CERT_PEM"] = getOpsSsmParameter("/fleet/dev/claim/cert-pem")
        secrets["FLEET_NONPROD_CLAIM_PRIVATE_KEY"] = getOpsSsmParameter("/fleet/dev/claim/private-key")

        // Generate a BKS per platform flavor, keyed to its signing cert
        val appExtension = project.extensions
            .getByType(com.android.build.gradle.AppExtension::class.java)

        appExtension.productFlavors
            .filter { it.signingConfig != null }
            .forEach { flavor ->
                val sc = appExtension.signingConfigs.getByName(flavor.signingConfig!!.name)
                val password = derivePasswordFromSigningConfig(sc)

                val keyStore = KeyStore.getInstance("BKS", "BC")
                keyStore.load(null, password)
                val protection = KeyStore.PasswordProtection(password)
                secrets.forEach { (alias, value) ->
                    val secretKey = SecretKeySpec(value.toByteArray(Charsets.UTF_8), "RAW")
                    keyStore.setEntry(alias, KeyStore.SecretKeyEntry(secretKey), protection)
                }

                val assetsDir = file("src/${flavor.name}/assets")
                assetsDir.mkdirs()
                val outputFile = File(assetsDir, "fleet-credentials.bks")
                FileOutputStream(outputFile).use { fos ->
                    keyStore.store(fos, password)
                }
                logger.lifecycle("Fleet credentials BKS written to ${outputFile.absolutePath} (${sc.name})")
            }
    }
}

fun derivePasswordFromSigningConfig(
    signingConfig: com.android.build.gradle.internal.dsl.SigningConfig
): CharArray {
    val keystoreFile = signingConfig.storeFile
        ?: throw GradleException("Signing config '${signingConfig.name}' has no storeFile")
    val keystorePassword = signingConfig.storePassword
        ?: throw GradleException("Signing config '${signingConfig.name}' has no storePassword")
    val keyAlias = signingConfig.keyAlias
        ?: throw GradleException("Signing config '${signingConfig.name}' has no keyAlias")

    val ks = KeyStore.getInstance("JKS")
    keystoreFile.inputStream().use { ks.load(it, keystorePassword.toCharArray()) }
    val cert = ks.getCertificate(keyAlias)
        ?: throw GradleException("Certificate not found for alias: $keyAlias in ${signingConfig.name}")
    val certBytes = cert.encoded

    /*
     * Using HMAC-SHA256(SHA-256(cert), salt) for password derivation
     * This matches BksCredentialProvider.derivePassword()
     */
    val hash = java.security.MessageDigest.getInstance("SHA-256").digest(certBytes)
    val mac = javax.crypto.Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(hash, "HmacSHA256"))
    val derived = mac.doFinal("fleet-credentials-salt".toByteArray(Charsets.UTF_8))
    return java.util.Base64.getEncoder().encodeToString(derived).toCharArray()
}

// Add to preBuild tasks
tasks.named("preBuild").configure {
    dependsOn("generateOAuthCredentials")
}
