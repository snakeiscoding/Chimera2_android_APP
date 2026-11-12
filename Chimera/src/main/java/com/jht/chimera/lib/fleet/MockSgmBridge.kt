package com.jht.chimera.lib.fleet

import android.util.Base64
import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MockSgmBridge(
    private val claimCertPem: String,
    private val claimKeyPem: String,
    existingEcdhKeyPair: KeyPair? = null,
) : SgmBridgeInterface {

    private val ecdhKeyPair: KeyPair = existingEcdhKeyPair ?: KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    fun getEcdhKeyPair(): KeyPair = ecdhKeyPair

    private val uncompressed: ByteArray = exportUncompressedPoint(ecdhKeyPair.public as ECPublicKey)
    private val signature: ByteArray = signWithClaimKey(uncompressed)


    private fun signWithClaimKey(ecdhPublicKey: ByteArray): ByteArray {
        val keyPem = claimKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val derBytes = Base64.decode(keyPem, Base64.NO_WRAP)
        val claimPrivateKey = KeyFactory.getInstance("EC")
            .generatePrivate(PKCS8EncodedKeySpec(derBytes))
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(claimPrivateKey)
        sig.update(ecdhPublicKey)
        return sig.sign()
    }

    override fun getCertPem(): String {
        return claimCertPem
    }

    override fun getKeyPem(): String {
        return claimKeyPem
    }

    override fun getPublicKey(): ByteArray {
        return uncompressed
    }

    override fun getKeySignature(): ByteArray {
        return signature
    }

    override fun deliverKeyEnvelope(envelope: EciesEnvelope, thingName: String, slotId: WalletSlot) {
        val kp = ecdhKeyPair

        try {
            val ephemeralBytes = Base64.decode(envelope.ephemeralPublicKey, Base64.NO_WRAP)
            val ciphertext = Base64.decode(envelope.ciphertext, Base64.NO_WRAP)
            val authTag = Base64.decode(envelope.authTag, Base64.NO_WRAP)
            val aad = thingName.toByteArray(Charsets.UTF_8)

            val plaintext = eciesDecrypt(kp, ephemeralBytes, ciphertext, authTag, aad)
            Log.d(TAG, "ciphertext ${ciphertext.size}-byte wallet key locally (slot=$slotId)")

            when (slotId) {
                WalletSlot.APPLE -> Log.d(TAG, "Apple config: passTypeId=${envelope.passTypeId}")
                WalletSlot.GOOGLE -> Log.d(TAG, "Google config: collectorId=${envelope.collectorId}, keyVersion=${envelope.keyVersion}, passTypeFilter=${envelope.passTypeFilter}")
            }
            plaintext.fill(0)
        } catch (e: Exception) {
            Log.e(TAG, "ECIES decrypt failed", e)
        }
    }

    override fun eraseKey(slotId: WalletSlot) {
        // Do nothing
    }

    companion object {
        private const val TAG = "MockSgmBridge"
        private const val HKDF_INFO = "wallet-key-wrap-v1"
        private const val HKDF_DERIVED_LENGTH = 28
        private const val AES_KEY_LENGTH = 16

        private fun eciesDecrypt(
            kp: KeyPair,
            ephemeralPubBytes: ByteArray,
            ciphertext: ByteArray,
            authTag: ByteArray,
            aad: ByteArray
        ): ByteArray {
            val kf = KeyFactory.getInstance("EC")
            val ecParams = (kp.public as ECPublicKey).params
            val x = BigInteger(1, ephemeralPubBytes.copyOfRange(1, 33))
            val y = BigInteger(1, ephemeralPubBytes.copyOfRange(33, 65))
            val point = java.security.spec.ECPoint(x, y)
            val ephemeralPub = kf.generatePublic(java.security.spec.ECPublicKeySpec(point, ecParams))

            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(kp.private)
            ka.doPhase(ephemeralPub, true)
            val sharedSecret = ka.generateSecret()

            // HKDF-SHA-256 to 28 bytes (16 AES key + 12 GCM IV)
            val derived = hkdfSha256(sharedSecret, byteArrayOf(), HKDF_INFO.toByteArray(), HKDF_DERIVED_LENGTH)
            sharedSecret.fill(0)

            val aesKey = derived.copyOfRange(0, AES_KEY_LENGTH)
            val iv = derived.copyOfRange(AES_KEY_LENGTH, HKDF_DERIVED_LENGTH)
            derived.fill(0)

            val gcmInput = ciphertext + authTag
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
            cipher.updateAAD(aad)
            aesKey.fill(0)
            return cipher.doFinal(gcmInput)
        }

        private fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
            // Extract
            val mac = Mac.getInstance("HmacSHA256")
            val effectiveSalt = if (salt.isEmpty()) ByteArray(32) else salt
            mac.init(SecretKeySpec(effectiveSalt, "HmacSHA256"))
            val prk = mac.doFinal(ikm)

            // Expand
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(info)
            mac.update(byteArrayOf(1))
            return mac.doFinal().copyOfRange(0, length)
        }

        private fun exportUncompressedPoint(pubKey: ECPublicKey): ByteArray {
            val point = pubKey.w
            val x = point.affineX.toByteArray().padOrTrimTo(32)
            val y = point.affineY.toByteArray().padOrTrimTo(32)
            return ByteArray(65).apply {
                this[0] = 0x04
                System.arraycopy(x, 0, this, 1, 32)
                System.arraycopy(y, 0, this, 33, 32)
            }
        }

        private fun ByteArray.padOrTrimTo(len: Int): ByteArray = when {
            size == len -> this
            size > len -> copyOfRange(size - len, size)
            else -> ByteArray(len - size) + this
        }
    }
}
