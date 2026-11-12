package com.jht.chimera.service.fleet

import android.content.Context
import android.util.Base64
import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.fleet.BearerTokenHelper
import com.jht.chimera.lib.fleet.ConsoleIdentity
import com.jht.chimera.lib.fleet.FleetCredentialStore
import com.jht.chimera.lib.fleet.FleetDeviceIdentity
import com.jht.chimera.lib.fleet.FleetEnvironment
import com.jht.chimera.lib.fleet.FleetProvisioningClient
import com.jht.chimera.lib.fleet.KeyPipelineClient
import com.jht.chimera.lib.fleet.NonSgmBridge
import com.jht.chimera.lib.fleet.SgmBridge
import com.jht.chimera.lib.fleet.SgmBridgeInterface
import com.jht.chimera.lib.fleet.WalletSlot
import com.jht.chimera.service.hal.lib.hardware.RfidReaderManager
import com.jht.datamatrixlib.DataMatrix
import com.jht.rfidlib.ReaderFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "FleetProvision"

/** [FleetProvision] service handles the fleet provision. It will
 * currently also handle the wallet key info exchange for now.
 * */
class FleetProvision(val context: Context) {

    /**
     * Provision fleet device then ask for wallet private keys for club ID.
     *
     */
    private suspend fun provisionInternal(
        dapiEnv: Int,
        bridge: SgmBridgeInterface,
        clubId: Int,
        store: FleetCredentialStore,
        allowCachedIdentity: Boolean,
    ) {
        val fleetEnvironment = FleetEnvironment.fromDeployment(dapiEnv)

        var identity: FleetDeviceIdentity? = null

        if (allowCachedIdentity) {
            Log.d(TAG, "  ----- Loading from store -----")
            identity = store.load()
        }

        if (identity == null) {
            Log.d(TAG, "  ***** Provision device | dapiEnv=$dapiEnv *****")
            val client = FleetProvisioningClient(
                context = context,
                deployment = fleetEnvironment.deployment,
                iotEndpoint = fleetEnvironment.iotEndpoint,
                templateName = fleetEnvironment.templateName,
                apiBaseUrl = fleetEnvironment.apiBaseUrl
            )
            identity = client.provisionDevice(
                bridge,
                ConsoleIdentity.fromDevice().consoleUuid,
                ConsoleIdentity.fromDevice().cpuId
            ).getOrElse { throw it }
            store.save(identity)
        }

        Log.d(TAG, "  Thing?: ${identity.thingName}")
        Log.d(TAG, "  OAuth client: ${identity.oauthClientId}")
        Log.d(TAG, "  Scope: ${identity.oauthScope}")

        Log.d(TAG, "Getting OAuth access token from JUMP...")
        val bearerToken: String = withContext(Dispatchers.IO) {
            // force refresh token during provisioning
            BearerTokenHelper.getBearerToken(context, true).getOrDefault("")
        }
        Log.d(TAG, "  Token: ${bearerToken.take(20)}...")

        if (bridge.getPublicKey().isNotEmpty()) {
            Log.d(TAG, "Delivering wallet keys (facility=$clubId)...")
            val walletKeys = withContext(Dispatchers.IO) {
                KeyPipelineClient(fleetEnvironment.deployment, baseUrl = fleetEnvironment.apiBaseUrl)
                    .deliverWalletKeys(clubId, bearerToken)
                    .getOrElse { throw it }
            }

            store.saveWalletMetadata(
                applePassTypeId = walletKeys.apple?.passTypeId,
                googleCollectorId = walletKeys.google?.collectorId?.toString()
            )
            Log.d(TAG, "   --> $walletKeys")

            if (walletKeys.apple != null) {
                val envelope = walletKeys.apple
                Log.d(TAG, "  Apple envelope received, delivering to SGM...")
                Log.v(TAG, "  Apple config: passTypeId=${envelope.passTypeId}")
                bridge.deliverKeyEnvelope(envelope, identity.thingName, WalletSlot.APPLE)
            } else {
                Log.d(TAG, "  Apple: skipped (no key configured)")
                bridge.eraseKey(WalletSlot.APPLE)
            }

            if (walletKeys.google != null) {
                val envelope = walletKeys.google
                Log.d(TAG, "  Google envelope received, delivering to SGM...")
                Log.v(TAG, "  Google config: collectorId=${envelope.collectorId}, keyVersion=${envelope.keyVersion}, passTypeFilter=${envelope.passTypeFilter}")
                bridge.deliverKeyEnvelope(envelope, identity.thingName, WalletSlot.GOOGLE)
            } else {
                Log.d(TAG, "  Google: skipped (no key configured)")
                bridge.eraseKey(WalletSlot.GOOGLE)
            }
        } else {
            Log.d(TAG, "Skipping wallet keys (facility=$clubId)...")
        }
    }


    /**
     * Provision fleet device. If the SGM is supported, deliver wallet keys.
     * Try to use cached identity if available. If the client ID/secret returns error code 2001, re-provision.
     */
    suspend fun provision() {
        Log.d(TAG, "--> Provision")
        val clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
        val dapiEnv = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
        val store = FleetCredentialStore.getInstance(context)
        val isProvisioned: Boolean

        val claimCertPem = DataMatrix.getString(HALKeys.DATA_FLEET_CLAIM_CERT_STRING)
        val claimKeyPem = DataMatrix.getString(HALKeys.DATA_FLEET_CLAIM_KEY_STRING)
        val publicKeyBase64 = DataMatrix.getString(HALKeys.DATA_FLEET_PUBLIC_KEY_BASE64_STRING)
        val signatureBase64 = DataMatrix.getString(HALKeys.DATA_FLEET_SIGNATURE_BASE64_STRING)
        val supportsWallet = RfidReaderManager.getReaderFeatures().contains(ReaderFeatures.WalletSupport)

        val bridge: SgmBridgeInterface
        if (!supportsWallet || claimCertPem.isEmpty() || claimKeyPem.isEmpty() || publicKeyBase64.isEmpty() || signatureBase64.isEmpty()) {
            Log.d(TAG, "    provision without SgmBridge")
            isProvisioned = store.isProvisionedFor(dapiEnv, "")
            bridge = NonSgmBridge(
                context = context,
                dapiEnv = dapiEnv
            )
        } else {
            Log.d(TAG, "    provision with SgmBridge")
            isProvisioned = store.isProvisionedFor(dapiEnv, publicKeyBase64)
            bridge = SgmBridge(
                claimCertPem = claimCertPem,
                claimKeyPem = claimKeyPem,
                uncompressedPublicKey = Base64.decode(publicKeyBase64, Base64.NO_WRAP),
                attestationSignature = Base64.decode(signatureBase64, Base64.NO_WRAP),
            )
        }
        if (!isProvisioned) store.clear() // make sure cache is clean

        try {
            provisionInternal(
                dapiEnv = dapiEnv,
                bridge = bridge,
                clubId = clubId,
                store = store,
                allowCachedIdentity = isProvisioned
            )
        } catch (e: Exception) {
            Log.e(TAG, "E2E failed", e)

            // skip if provision was already attempted because no cache is available
            if (!isProvisioned) return

            // this will re-attempt to re-provision if cached client ID/secret return an error
            store.clear()
            provisionInternal(
                dapiEnv = dapiEnv,
                bridge = bridge,
                clubId = clubId,
                store = store,
                allowCachedIdentity = false
            )
        }
    }

}