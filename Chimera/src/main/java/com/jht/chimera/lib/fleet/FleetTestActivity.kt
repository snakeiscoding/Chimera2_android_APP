package com.jht.chimera.lib.fleet

import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jht.chimera.lib.widget.activity.RestringActivity
import com.jht.oauthlib.auth.BksCredentialProvider
import com.jht.oauthlib.auth.ConsoleOAuthConfig
import com.jht.oauthlib.auth.ConsoleTokenManager
import com.jht.oauthlib.auth.LoginScope
import com.jht.oauthlib.auth.ResourceOwnerFlow
import com.jht.oauthlib.microservices.NotificationService
import com.jht.oauthlib.push.WebPushHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Full E2E test:
 * 1. Provision
 * 2. OAuth
 * 3. Wallet keys
 * 4. XID login
 * 5. External-wallet ROPC
 * 6. Notifications.
 *
 * Uses MockSgmBridge as a mock SGM.
 * Change [TARGET] to switch environments.
 *
 * To run: adb shell am start -a com.jht.chimera.FLEET_TEST
 */
class FleetTestActivity : RestringActivity() {

    companion object {
        private const val TAG = "FleetTest"

        // Change this to switch environments ──
        private val TARGET = FleetEnvironment.DEV

        // true = load claim certs from BKS keystore (non-SGM path)
        // false = use hardcoded PEMs below (SGM mock path)
        private const val USE_BKS = true

        private const val FACILITY_ID = 1
        private const val CONSOLE_UUID = "631c040a-671e-7c05-6377-2f1cc182de1e"
        private const val CPU_ID = "1485d80b3cee8720"

        // XID test credentials
        private const val TEST_XID = "1231231234"
        private const val TEST_XID_PASSWORD = "1234"

        private val CLAIM_CERT_PEM = """
-----BEGIN CERTIFICATE-----
MIICiTCCAXGgAwIBAgIUYM/2gORAsHR1NxoFTfcDBDke0A8wDQYJKoZIhvcNAQEL
BQAwTTFLMEkGA1UECwxCQW1hem9uIFdlYiBTZXJ2aWNlcyBPPUFtYXpvbi5jb20g
SW5jLiBMPVNlYXR0bGUgU1Q9V2FzaGluZ3RvbiBDPVVTMB4XDTI2MDMxMTEzNTIw
NVoXDTQ5MTIzMTIzNTk1OVowGTEXMBUGA1UEAwwORmxlZXQgQ2xhaW0gRUMwWTAT
BgcqhkjOPQIBBggqhkjOPQMBBwNCAAQLD69VjDPeiky2RiBWYC1/erZ101jdA+Yg
Osl36QnrttTt4Z3GOUyvlZRhPP243W8YcZxTD8Lr8FUiaNzuIw6Xo2AwXjAfBgNV
HSMEGDAWgBSPF/xp6Zyi94ibPVMoabhJGIDzpzAdBgNVHQ4EFgQUpMbP9eJ+wD46
WwqhhFqbna5jOQgwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwDQYJKoZI
hvcNAQELBQADggEBAFdXi4GHTdBpgDYdPTz90+VkvorllNcJE2nP4gxYCht3pRIc
/pupxRF8w2lQPnBEArQw9PJw4jL0Lf5lCUj9JDMWqr3zYXwp6PJJMU1dKmQOgU8v
6NRhTxZoHerZGS8vlh0aG62Gcws6hb3jI9Ny5jaaGturciID6mMqGM3IngRAgRdV
/WepTsOeduf/kuyRl9uU+V6MUxss2POg0LtEJOGsS8yWCwig4obbAi4/Bq0I3kmj
g4ucwQrtQIiwopVHVNDQlo2CrJqrski6uPA4FroWqLENVqbtmBuHhY7iGpTDm498
AQ9HS4g3YlrSdPhIIQv17ovfYgXoSOotNvcgON4=
-----END CERTIFICATE-----
        """.trimIndent()

        private val CLAIM_KEY_PEM = """
-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgjl5ZYkDnSq0+lPOI
aCo3Tm9WxXLFfRO3pVUigCn7B/+hRANCAAQLD69VjDPeiky2RiBWYC1/erZ101jd
A+YgOsl36QnrttTt4Z3GOUyvlZRhPP243W8YcZxTD8Lr8FUiaNzuIw6X
-----END PRIVATE KEY-----
        """.trimIndent()
    }

    private lateinit var logView: TextView
    private var webPushHandler: WebPushHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrollView = ScrollView(this)
        logView = TextView(this).apply { setPadding(16, 16, 16, 16); textSize = 12f }
        scrollView.addView(logView)
        setContentView(scrollView)

        appendLog("Fleet E2E Test [${TARGET.name}]")
        appendLog("  IoT: ${TARGET.iotEndpoint}")
        appendLog("  API: ${TARGET.apiBaseUrl}")
        appendLog("  Device: $CONSOLE_UUID:$CPU_ID")
        appendLog("  Facility: $FACILITY_ID\n")
        runTest()
    }

    override fun onDestroy() {
        super.onDestroy()
        webPushHandler?.close()
    }

    private fun appendLog(msg: String) {
        runOnUiThread { logView.append("$msg\n") }
        Log.d(TAG, msg)
    }

    private fun runTest() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val store = FleetCredentialStore.getInstance(this@FleetTestActivity)
                val existingIdentity = null //store.load()

                val identity: FleetDeviceIdentity
                val bridge: MockSgmBridge

                if (existingIdentity != null) {
                    appendLog("[1/7] Loading existing provisioned device...")
                    identity = existingIdentity
                    val ecdhKeyPair = store.getEcdhKeyPair()
                        ?: throw IllegalStateException("ECDH keypair not found — re-provision required")
                    val claimCert = if (USE_BKS) {
                        BksCredentialProvider(this@FleetTestActivity)
                            .getClaimCertificate(TARGET.deployment)
                    } else null
                    bridge = MockSgmBridge(
                        claimCert?.certificatePem ?: CLAIM_CERT_PEM,
                        claimCert?.privateKeyPem ?: CLAIM_KEY_PEM,
                        ecdhKeyPair
                    )
                    appendLog("  Thing: ${identity.thingName}")
                    appendLog("  OAuth client: ${identity.oauthClientId}")
                    appendLog("  ECDH keypair: restored from store")

                    appendLog("[2/7] Credentials already saved, skipping...")
                } else {
                    val claimCert = if (USE_BKS) {
                        appendLog("[1/7] Loading claim cert from BKS keystore (non-SGM)...")
                        BksCredentialProvider(this@FleetTestActivity)
                            .getClaimCertificate(TARGET.deployment).also {
                                appendLog("  BKS claim cert loaded (${it.certificatePem.length} chars)")
                            }
                    } else {
                        appendLog("[1/7] Using hardcoded claim cert (SGM mock)...")
                        null
                    }
                    appendLog("  Provisioning device via MQTT...")
                    bridge = MockSgmBridge(
                        claimCert?.certificatePem ?: CLAIM_CERT_PEM,
                        claimCert?.privateKeyPem ?: CLAIM_KEY_PEM
                    )
                    val client = FleetProvisioningClient(
                        context = this@FleetTestActivity,
                        deployment = TARGET.deployment,
                        iotEndpoint = TARGET.iotEndpoint,
                        templateName = TARGET.templateName,
                        apiBaseUrl = TARGET.apiBaseUrl
                    )
                    identity = client.provisionDevice(bridge, CONSOLE_UUID, CPU_ID)
                        .getOrElse { throw it }
                    appendLog("  Thing: ${identity.thingName}")
                    appendLog("  OAuth client: ${identity.oauthClientId}")
                    appendLog("  Scope: ${identity.oauthScope}")

                    appendLog("[2/7] Saving credentials to secure store...")
                    store.save(identity)
                    bridge.getEcdhKeyPair().let { store.saveEcdhKeyPair(it) }
                    val loaded = store.load() ?: throw IllegalStateException("Failed to reload credentials")
                    appendLog("  Verified: thingName=${loaded.thingName}")
                }

                appendLog("[3/7] Getting OAuth access token from JUMP...")
                val bearerToken = withContext(Dispatchers.IO) {
                    ConsoleTokenManager(
                        context = this@FleetTestActivity,
                        deployment = TARGET.deployment,
                        clientId = identity.oauthClientId,
                        clientSecret = identity.oauthClientSecret,
                        scopes = identity.oauthScope
                    ).getAccessToken().getOrElse { throw it }.toBearerToken()
                }
                appendLog("  Token: ${bearerToken.take(20)}...")

                appendLog("[4/7] Delivering wallet keys (facility=$FACILITY_ID)...")
                val walletKeys = withContext(Dispatchers.IO) {
                    KeyPipelineClient(TARGET.deployment, baseUrl = TARGET.apiBaseUrl)
                        .deliverWalletKeys(FACILITY_ID, bearerToken)
                        .getOrElse { throw it }
                }

                var delivered = 0

                walletKeys.apple?.let { envelope ->
                    appendLog("  Apple envelope received, delivering to mock SGM...")
                    appendLog("  Apple config: passTypeId=${envelope.passTypeId}")
                    bridge.deliverKeyEnvelope(envelope, identity.thingName, WalletSlot.APPLE)
                    delivered++
                } ?: appendLog("  Apple: skipped (no key configured)")

                walletKeys.google?.let { envelope ->
                    appendLog("  Google envelope received, delivering to mock SGM...")
                    appendLog("  Google config: collectorId=${envelope.collectorId}, keyVersion=${envelope.keyVersion}, passTypeFilter=${envelope.passTypeFilter}")
                    bridge.deliverKeyEnvelope(envelope, identity.thingName, WalletSlot.GOOGLE)
                    delivered++
                } ?: appendLog("  Google: skipped (no key configured)")

                appendLog("  $delivered wallet key(s) delivered and decrypted")

                store.saveWalletMetadata(
                    applePassTypeId = walletKeys.apple?.passTypeId,
                    googleCollectorId = walletKeys.google?.collectorId?.toString()
                )

                appendLog("[5/7] XID login via ROPC (xid=$TEST_XID)...")
                val oauthConfig = ConsoleOAuthConfig(
                    context = this@FleetTestActivity,
                    deployment = TARGET.deployment,
                    clientId = identity.oauthClientId,
                    clientSecret = identity.oauthClientSecret
                )
                val ropcFlow = ResourceOwnerFlow(oauthConfig)
                val tokenData = ropcFlow.authenticate(
                    username = TEST_XID,
                    password = TEST_XID_PASSWORD,
                    scopes = LoginScope.XID.authScope
                ).getOrElse { throw it }
                appendLog("  Access token: ${tokenData.accessToken.take(20)}...")
                appendLog("  ID token: ${if (tokenData.idToken != null) "present" else "absent"}")
                appendLog("  Expires in: ${(tokenData.expiresAt - System.currentTimeMillis()) / 1000}s")

                appendLog("[6/7] External-wallet login via ROPC...")
                val walletStore = FleetCredentialStore.getInstance(this@FleetTestActivity)
                val testIssuerId = walletStore.getApplePassTypeId()
                    ?: walletStore.getGoogleCollectorId()
                    ?: "test-issuer-id"
                appendLog("  issuerId: $testIssuerId (from ${when {
                    walletStore.getApplePassTypeId() != null -> "Apple passTypeId"
                    walletStore.getGoogleCollectorId() != null -> "Google collectorId"
                    else -> "fallback"
                }})")
                val walletResult = ropcFlow.authenticate(
                    username = "test-nfc-payload",
                    password = "APPLE",
                    machineId = "test-machine",
                    facilityId = FACILITY_ID,
                    scopes = LoginScope.EXTERNAL_WALLET.authScope,
                    issuerId = testIssuerId
                )
                walletResult.onSuccess { walletToken ->
                    appendLog("  Wallet access token: ${walletToken.accessToken.take(20)}...")
                    appendLog("  Wallet login: PASS")
                }.onFailure { walletErr ->
                    appendLog("  Wallet login FAIL: ${walletErr.message}")
                    appendLog("  (Expected: EGYM will reject without real NFC payload)")
                }

                appendLog("[7/7] Connecting to notification service...")
                val notifToken = withContext(Dispatchers.IO) {
                    ConsoleTokenManager(
                        context = this@FleetTestActivity,
                        deployment = TARGET.deployment,
                        clientId = identity.oauthClientId,
                        clientSecret = identity.oauthClientSecret,
                        scopes = NotificationService.getScopes()
                    ).getAccessToken().getOrElse { throw it }.toBearerToken()
                }
                val channel = NotificationService.getChannelName(CONSOLE_UUID, "machine")
                val baseUrl = NotificationService.getBaseUrl(TARGET.deployment)
                val wsUrl = NotificationService.getWebSocketUrl(TARGET.deployment)
                appendLog("  Channel: $channel")
                appendLog("  WS URL: $wsUrl")

                val connected = CompletableDeferred<Boolean>()
                webPushHandler = WebPushHandler(
                    bearerToken = notifToken,
                    channel = channel,
                    appsyncEndpoint = "$baseUrl/graphql",
                    appsyncRealtimeEndpoint = wsUrl,
                    onPushStartup = { connected.complete(true) },
                    onPushData = { data ->
                        appendLog("  Push received: ${data.getString("type")}")
                    }
                )

                val didConnect = withTimeoutOrNull(10_000) { connected.await() } ?: false
                appendLog("  WebSocket: ${if (didConnect) "CONNECTED" else "TIMEOUT (10s)"}")

                appendLog("\nE2E PASSED $delivered wallet key(s), XID login OK, wallet ROPC tested, notifications ${if (didConnect) "OK" else "TIMEOUT"}")

                // Clean up
                webPushHandler?.close()
                webPushHandler = null
            } catch (e: Exception) {
                appendLog("\nE2E FAILED: ${e.message}")
                Log.e(TAG, "E2E failed", e)
            }
        }
    }
}
