package com.jht.chimera.lib.fleet

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * To be able to use DataStore across different processes, you need to construct the DataStore object
 * using the MultiProcessDataStoreFactory. If there are multiple DataStores active for a given file in
 * the same process, DataStore will throw IllegalStateException when reading or updating data.
 */
class FleetCredentialStore private constructor(
    private val context: Context
) {
    private val TAG = "FleetCredentialStore"

    private val dataStore: DataStore<Preferences> =
        MultiProcessDataStoreFactory.create(
            serializer = PreferencesFileSerializer,
            produceFile = {
                File(context.filesDir, "datastore/fleet_device.preferences_pb")
            }
        )

    private val secretKey: SecretKey = getOrCreateKey()

    suspend fun save(identity: FleetDeviceIdentity) {
        dataStore.edit { prefs ->
            prefs[KEY_DEPLOYMENT] = identity.deployment
            prefs[KEY_THING_NAME] = identity.thingName
            prefs[KEY_CERT_PEM] = encrypt(identity.certificatePem)
            prefs[KEY_PRIVATE_KEY] = encrypt(identity.privateKeyPem)
            prefs[KEY_OAUTH_CLIENT_ID] = encrypt(identity.oauthClientId)
            prefs[KEY_OAUTH_CLIENT_SECRET] = encrypt(identity.oauthClientSecret)
            prefs[KEY_OAUTH_SCOPE] = identity.oauthScope
            prefs[KEY_SGM_PUBLIC_KEY] = encrypt(identity.sgmPublicKey)
        }
    }

    suspend fun load(): FleetDeviceIdentity? {
        val prefs = readPrefs()
        val thingName = prefs[KEY_THING_NAME] ?: return null

        return try {
            FleetDeviceIdentity(
                deployment = prefs[KEY_DEPLOYMENT] ?: -1,
                thingName = thingName,
                certificatePem = decrypt(prefs[KEY_CERT_PEM] ?: return null),
                privateKeyPem = decrypt(prefs[KEY_PRIVATE_KEY] ?: return null),
                oauthClientId = decrypt(prefs[KEY_OAUTH_CLIENT_ID] ?: return null),
                oauthClientSecret = decrypt(prefs[KEY_OAUTH_CLIENT_SECRET] ?: return null),
                oauthScope = prefs[KEY_OAUTH_SCOPE] ?: "",
                sgmPublicKey = decrypt(prefs[KEY_SGM_PUBLIC_KEY] ?: return null),
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /**
     * Check if the cache is valid by making sure thing name exists, deployment matches, and SGM public key matches
     * in case the dapi environment was changed or SGM is removed/replaced.
     */
    suspend fun isProvisionedFor(deployment: Int, sgmPublicKey: String): Boolean {
        val prefs = readPrefs()
        val encryptedSgmPublicKey = prefs[KEY_SGM_PUBLIC_KEY] ?: return false
        val cachedSgmPublicKey = decrypt(encryptedSgmPublicKey)

        return prefs[KEY_THING_NAME] != null &&
                (prefs[KEY_DEPLOYMENT] ?: -1) == deployment &&
                cachedSgmPublicKey == sgmPublicKey
    }

    suspend fun saveWalletMetadata(applePassTypeId: String?, googleCollectorId: String?) {
        dataStore.edit { prefs ->
            applePassTypeId?.let { prefs[KEY_APPLE_PASS_TYPE_ID] = it }
            googleCollectorId?.let { prefs[KEY_GOOGLE_COLLECTOR_ID] = it }
        }
    }

    suspend fun getApplePassTypeId(): String? {
        return readPrefs()[KEY_APPLE_PASS_TYPE_ID]
    }

    suspend fun getGoogleCollectorId(): String? {
        return readPrefs()[KEY_GOOGLE_COLLECTOR_ID]
    }

    fun getApplePassTypeIdBlocking(): String? = runBlocking {
        getApplePassTypeId()
    }

    fun getGoogleCollectorIdBlocking(): String? = runBlocking {
        getGoogleCollectorId()
    }

    suspend fun saveBearerJson(token: String) =
        dataStore.edit {
            it[KEY_BEARER_TOKEN] = encrypt(token)
        }

    suspend fun getBearerJson(): String? =
        readPrefs()[KEY_BEARER_TOKEN]?.let { token ->
            decrypt(token)
        }

    suspend fun saveEcdhKeyPair(keyPair: KeyPair) {
        val privBase64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
        val pubBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

        dataStore.edit { prefs ->
            prefs[KEY_ECDH_PRIVATE_KEY] = encrypt(privBase64)
            prefs[KEY_ECDH_PUBLIC_KEY] = encrypt(pubBase64)
        }
    }

    suspend fun getEcdhKeyPair(): KeyPair? {
        val prefs = readPrefs()
        val privEncoded = prefs[KEY_ECDH_PRIVATE_KEY] ?: return null
        val pubEncoded = prefs[KEY_ECDH_PUBLIC_KEY] ?: return null

        return try {
            val privBytes = Base64.decode(decrypt(privEncoded), Base64.NO_WRAP)
            val pubBytes = Base64.decode(decrypt(pubEncoded), Base64.NO_WRAP)
            val kf = KeyFactory.getInstance("EC")
            KeyPair(
                kf.generatePublic(X509EncodedKeySpec(pubBytes)),
                kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun readPrefs(): Preferences {
        return dataStore.data
            .catch { _ -> emit(emptyPreferences()) }
            .first()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        if (encoded.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.d(TAG, "Decrypt exception: $e")
            ""
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keystore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keystore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "fleet_credential_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val GCM_TAG_BITS = 128

        private val KEY_DEPLOYMENT = intPreferencesKey("deployment")
        private val KEY_THING_NAME = stringPreferencesKey("thing_name")
        private val KEY_CERT_PEM = stringPreferencesKey("cert_pem")
        private val KEY_PRIVATE_KEY = stringPreferencesKey("private_key")
        private val KEY_OAUTH_CLIENT_ID = stringPreferencesKey("oauth_client_id")
        private val KEY_OAUTH_CLIENT_SECRET = stringPreferencesKey("oauth_client_secret")
        private val KEY_OAUTH_SCOPE = stringPreferencesKey("oauth_scope")
        private val KEY_ECDH_PRIVATE_KEY = stringPreferencesKey("ecdh_private_key")
        private val KEY_ECDH_PUBLIC_KEY = stringPreferencesKey("ecdh_public_key")
        private val KEY_APPLE_PASS_TYPE_ID = stringPreferencesKey("apple_pass_type_id")
        private val KEY_GOOGLE_COLLECTOR_ID = stringPreferencesKey("google_collector_id")
        private val KEY_SGM_PUBLIC_KEY = stringPreferencesKey("public_key_base64")
        private val KEY_BEARER_TOKEN = stringPreferencesKey("bearer_token")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FleetCredentialStore? = null

        @JvmStatic
        fun getInstance(context: Context): FleetCredentialStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FleetCredentialStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}