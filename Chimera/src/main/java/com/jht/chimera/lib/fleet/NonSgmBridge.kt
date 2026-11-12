package com.jht.chimera.lib.fleet

import android.content.Context
import android.util.Base64
import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.oauthlib.auth.BksCredentialProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Fleet bridge backed by the non-SGM
 *
 */
class NonSgmBridge(
    context: Context,
    dapiEnv : Int,
) : SgmBridgeInterface {

    val fleetEnvironment = FleetEnvironment.fromDeployment(dapiEnv)
    val claimCert = BksCredentialProvider(context).getClaimCertificate(fleetEnvironment.deployment)

    override fun getCertPem(): String {
        return claimCert.certificatePem
    }

    override fun getKeyPem(): String {
        return claimCert.privateKeyPem
    }

    override fun getPublicKey(): ByteArray {
        return byteArrayOf()
    }

    override fun getKeySignature(): ByteArray {
        return byteArrayOf()
    }

    override fun deliverKeyEnvelope(envelope: EciesEnvelope, thingName: String, slotId: WalletSlot) {
        // not supported
    }

    override fun eraseKey(slotId: WalletSlot) {
        // not supported
    }

}
