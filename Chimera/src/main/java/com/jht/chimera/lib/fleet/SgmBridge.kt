package com.jht.chimera.lib.fleet

import android.util.Base64
import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Fleet bridge backed by the SGM via the FLEET interface.
 *
 * Usage:
 *   val fleet = rfidReaderManager.fleet ?: error("RFID reader has no fleet support")
 *   val bridge = SgmBridge(fleet)
 *   client.provisionDevice(bridge, consoleUuid, cpuId)
 */
class SgmBridge(
    private val claimCertPem: String,
    private val claimKeyPem: String,
    private val uncompressedPublicKey: ByteArray,
    private val attestationSignature: ByteArray,
) : SgmBridgeInterface {

    override fun getCertPem(): String {
        return claimCertPem
    }

    override fun getKeyPem(): String {
        return claimKeyPem
    }

    override fun getPublicKey(): ByteArray {
        return uncompressedPublicKey
    }

    override fun getKeySignature(): ByteArray {
        return attestationSignature
    }

    override fun deliverKeyEnvelope(envelope: EciesEnvelope, thingName: String, slotId: WalletSlot) {
        Log.d(TAG, "Delivering key envelope to SGM (slot=$slotId)")
        val ephPk = Base64.decode(envelope.ephemeralPublicKey, Base64.NO_WRAP)
        val ct = Base64.decode(envelope.ciphertext, Base64.NO_WRAP)
        val tag = Base64.decode(envelope.authTag, Base64.NO_WRAP)
        val aad = thingName.toByteArray(Charsets.UTF_8)

        require(ephPk.size == 65) { "Ephemeral public key must be 65 bytes, got ${ephPk.size}" }
        require(tag.size == 16) { "Auth tag must be 16 bytes, got ${tag.size}" }
        require(ct.size in 1..256) { "Ciphertext must be 1-256 bytes, got ${ct.size}" }
        require(aad.size in 1..128) { "AAD must be 1-128 bytes, got ${aad.size}" }

        // Build wallet config bytes based on slot type
        val walletConfig = buildWalletConfig(envelope, slotId)

        // Build flat binary matching sgm ecies envelope format:
        // [ephemeral_pk:65][ct_len:2 BE][ct:M][tag:16][aad_len:2 BE][aad:L][wallet_config:...]
        val buf = ByteBuffer.allocate(65 + 2 + ct.size + 16 + 2 + aad.size + walletConfig.size)
            .order(ByteOrder.BIG_ENDIAN)
        buf.put(ephPk)                      // 65 bytes (0x04 || x || y)
        buf.putShort(ct.size.toShort())     // ct_len BE
        buf.put(ct)                         // ciphertext
        buf.put(tag)                        // 16-byte auth tag
        buf.putShort(aad.size.toShort())    // aad_len BE
        buf.put(aad)                        // thing_name as AAD
        buf.put(walletConfig)               // slot-specific wallet config
        val data = buf.array()
        val msgBase64 = Base64.encodeToString(data, Base64.NO_WRAP)

        Log.d(TAG, "deliverKeyEnvelope $slotId size: ${data.size}")
        //Log.i(TAG, "msgBase64: ${data.toHex()}")

        when (slotId) {
            WalletSlot.APPLE -> DataMatrix.putString(HALKeys.ACTION_FLEET_PUSH_APPLE_MSG_BASE64_STRING, msgBase64)
            WalletSlot.GOOGLE -> DataMatrix.putString(HALKeys.ACTION_FLEET_PUSH_GOOGLE_MSG_BASE64_STRING, msgBase64)
        }

    }

    override fun eraseKey(slotId: WalletSlot) {
        // Sending an empty string will trigger an erase of the key
        when (slotId) {
            WalletSlot.APPLE -> DataMatrix.putString(HALKeys.ACTION_FLEET_PUSH_APPLE_MSG_BASE64_STRING, "")
            WalletSlot.GOOGLE -> DataMatrix.putString(HALKeys.ACTION_FLEET_PUSH_GOOGLE_MSG_BASE64_STRING, "")
        }
    }


    companion object {
        private const val TAG = "SgmBridge"

        /**
         * Serialize wallet config to binary based on slot type.
         *
         * Apple (0x02): [passTypeId_len:2 BE][passTypeId:N UTF-8]
         * Google (0x03): [collectorId:4 BE][keyVersion:4 BE][passTypeFilter:4 BE]
         */
        internal fun buildWalletConfig(envelope: EciesEnvelope, slotId: WalletSlot): ByteArray {
            return when (slotId) {
                WalletSlot.APPLE -> {
                    val passTypeId = envelope.passTypeId
                        ?: error("Apple envelope missing passTypeId")
                    val sha256 = MessageDigest.getInstance("SHA-256")
                    val sha256passID = sha256.digest(passTypeId.toByteArray())
                    ByteBuffer.allocate(sha256passID.size)
                        .order(ByteOrder.BIG_ENDIAN)
                        .put(sha256passID)
                        .array()
                }
                WalletSlot.GOOGLE -> {
                    val collectorId = envelope.collectorId
                        ?: error("Google envelope missing collectorId")
                    val keyVersion = envelope.keyVersion
                        ?: error("Google envelope missing keyVersion")
                    val passTypeFilter = envelope.passTypeFilter ?: 0
                    ByteBuffer.allocate(12)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(collectorId.toInt())
                        .putInt(keyVersion)
                        .putInt(passTypeFilter)
                        .array()
                }
            }
        }
    }
}
