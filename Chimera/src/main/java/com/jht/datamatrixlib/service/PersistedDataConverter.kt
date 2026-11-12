package com.jht.datamatrixlib.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.datamatrixlib.service.PersistedDataConverterUtils.PersistedKeyData
import org.json.JSONException
import org.json.JSONObject
import org.json.simple.JSONArray
import updatepackages.Version

private const val TAG = "PersistedDataConverter"

object PersistedDataConverter {

    /**
     * A set of persisted data keys whose stored values must be cleared so the console falls back to
     * the current defaults.
     *
     * A group is applied when the persisted data found on the console is older than
     * [introducedIn] (or has no version at all), which means the console has not run this reset yet.
     *
     * To add a new reset: bump [PersistedDataConverterUtils.CURRENT_VERSION], then add a group here
     * whose [introducedIn] is that new version.
     *
     * @param introducedIn the persisted data version that first performs this reset
     * @param keys the persisted data keys to clear
     */
    private class KeyResetGroup(val introducedIn: String, val keys: List<String>)

    private val KEY_RESET_GROUPS = listOf(
        // CGC-21383 hotfix: these keys needed their default values re-applied.
        KeyResetGroup(
            "1.0.0.1",
            listOf(
                SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL.name,
                SettingKeys.Application.DATA_APP_INACTIVE_LIST_STRING.name,
                SettingKeys.Application.DATA_APP_ORDER_STRING.name
            )
        ),
        // CGC-25801: Touch consoles are going back to basic theme
        KeyResetGroup(
            "1.0.0.2",
            listOf(
                SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING.name,
            )
        ),
    )

    /**
     * Converts the persisted data stored in the shared preferences.
     *
     * Call this method directly with [forceConversion] set if used for testing to force conversion
     * to run.
     *
     * Since this code is triggered every time a console is booted it will look for a version
     * key stored in the shared prefs file before executing the conversion.
     *
     * @param context the context needed to get the shared preferences out of the file system
     * @param forceConversion boolean flag to indicate the conversion should run regardless of version data in the shared preferences
     */
    @JvmStatic
    @JvmOverloads
    fun convertPersistedDataInSharedPreferences(
        context: Context,
        forceConversion: Boolean = false
    ) {
        val sharedPreferences = context.getSharedPreferences(PersistedDataConverterUtils.SHARED_PREF_FILE_NAME, 0)
        val updatedSharedPreferences: MutableMap<String, String?> = HashMap()
        val preferencesVersion: String = sharedPreferences.getString(PersistedDataConverterUtils.VERSION_KEY, "") ?: ""
        Log.d(TAG, "Checking if PersistedData should be modified")
        if (forceConversion || updatePersistedData(preferencesVersion)) {
            Log.d(TAG, "PersistedData Should be updated")
            updateOutOfDateKeys(sharedPreferences, updatedSharedPreferences)
            resetKeys(updatedSharedPreferences, collectKeysToReset(preferencesVersion))
            updateSharedPreferences(sharedPreferences, updatedSharedPreferences)
            Log.d(TAG, "Shared preferences conversion complete, new shared preference size = " + sharedPreferences.all.size)
        } else {
            Log.d(TAG, "Shared preferences are up to date")
        }
    }

    /**
     * Collects the keys from every [KeyResetGroup] that the persisted data on this console has not
     * been through yet.
     *
     * Unversioned (or unparseable) persisted data predates every group, so all of them are applied.
     *
     * @param preferencesVersion the persisted data version currently stored on the console
     * @return the keys to reset, which may be empty
     */
    private fun collectKeysToReset(preferencesVersion: String): List<String> {
        val persistedVersion = if (preferencesVersion.isEmpty()) null else Version.parseVersion(preferencesVersion)
        if (persistedVersion == null) {
            Log.d(TAG, "No usable persisted data version, applying every key reset group")
            return KEY_RESET_GROUPS.flatMap { it.keys }
        }
        return KEY_RESET_GROUPS
            .filter { group ->
                // Version.isNewer() reports on its ARGUMENT, so this reads as "the group's version is newer than what is persisted",
                // meaning this console has not run the reset yet
                val groupVersion = Version.parseVersion(group.introducedIn)
                groupVersion != null && persistedVersion.isNewer(groupVersion)
            }
            .onEach { group -> Log.d(TAG, "Applying key reset group introduced in " + group.introducedIn) }
            .flatMap { it.keys }
    }

    /**
     * Resets persisted data keys by dropping them from the preferences being written.
     *
     * A key that is absent from the shared preferences has not been overridden by the user, so the
     * console falls back to the key's DataDefault. Blanking the value instead would only match the
     * default for keys that default to an empty string.
     *
     * @param updatedSharedPreferences the preferences about to be written, modified in place
     * @param keysToReset the persisted data keys that should go back to their default
     */
    private fun resetKeys(
        updatedSharedPreferences: MutableMap<String, String?>,
        keysToReset: List<String>
    ) {
        keysToReset.forEach { key ->
            if (updatedSharedPreferences.remove(key) != null) {
                Log.d(TAG, "Reset persisted data key $key to its default")
            }
        }
    }

    /**
     * Takes a SharedPreferences object and clears all preferences.
     * Writes all keys parings in the updatedSharedPreferences to the sharedPreferences object.
     * UPDATEDSHAREDPREFERENCES MUST HAVE ALL KEYS STILL NEEDED BY SHAREDPREFERENCES
     * @param sharedPreferences
     * @param updatedSharedPreferences
     */
    private fun updateSharedPreferences(
        sharedPreferences: SharedPreferences,
        updatedSharedPreferences: Map<String, String?>
    ) {
        sharedPreferences.edit {
            // clear() is always applied before any put in the same editor, regardless of call order — so orphaned keys are removed
            clear()
            updatedSharedPreferences.forEach { (key, value) -> putString(key, value) }
            putString(
                PersistedDataConverterUtils.VERSION_KEY,
                PersistedDataConverterUtils.CURRENT_VERSION
            )
        }
    }

    /**
     * Updates out of date shared preference datamatrix keys to the current names using the persistedSettingsKeysThesaurus.
     * Updated values are put into the updatedSharedPreferences object handed in.
     * All keys are copied over, but out of date key names are converted to their standardized names.
     * @param sharedPreferences
     * @param updatedSharedPreferences
     */
    private fun updateOutOfDateKeys(
        sharedPreferences: SharedPreferences,
        updatedSharedPreferences: MutableMap<String, String?>
    ) {
        val persistedSettingKeysThesaurus = PersistedDataConverterUtils.getInstance().persistedSettingKeyThesaurus
        for (currentSharedPreferenceKey in sharedPreferences.all.keys) {
            if (persistedSettingKeysThesaurus.containsKey(currentSharedPreferenceKey)) {
                updatedSharedPreferences.put(currentSharedPreferenceKey, sharedPreferences.getString(currentSharedPreferenceKey, ""))
            } else {
                val newPersistedKeyName = getUpdatedPersistedKeyName(currentSharedPreferenceKey, persistedSettingKeysThesaurus)
                if (!newPersistedKeyName.isEmpty()) {
                    updatedSharedPreferences.put(newPersistedKeyName, sharedPreferences.getString(currentSharedPreferenceKey, ""))
                } else {
                    Log.d(TAG, "An updated key was not found for the following obsolete key $currentSharedPreferenceKey")
                }
            }
        }
    }

    /**
     * Converts persisted data stored in a JSON file.
     *
     * Since this functionality will only be triggered when settings are imported it will always run
     * the conversion code.
     *
     * @param persistedData the JSON array of data to be imported
     */
    @JvmStatic
    fun convertPersistedDataInFile(persistedData: JSONArray) {
        Log.d(TAG, "Import settings from file conversion started.")
        val persistedKeyDataThesaurus = PersistedDataConverterUtils.getInstance().persistedSettingKeyThesaurus
        val key = "key"
        val updatedPersistedData = JSONArray()
        try {
            for (currentObject in persistedData) {
                val currentPersistedSettingData = JSONObject((currentObject.toString()))
                val currentPersistedKeyName = currentPersistedSettingData.getString(key)
                if (!persistedKeyDataThesaurus.containsKey(currentPersistedKeyName)) {
                    val newPersistedKeyName = getUpdatedPersistedKeyName(currentPersistedKeyName, persistedKeyDataThesaurus)
                    if (!newPersistedKeyName.isEmpty()) {
                        currentPersistedSettingData.put(key, newPersistedKeyName)
                    } else {
                        // If an updated key name is not found then the current data is no longer valid and should be removed
                        continue
                    }
                }
                updatedPersistedData.add(currentPersistedSettingData)
            }
            persistedData.clear()
            persistedData.addAll(updatedPersistedData)
            Log.d(TAG, "Import settings from file conversion complete.")
        } catch (e: JSONException) {
            Log.w(TAG, "Import settings from file conversion failure", e)
        }
    }

    private fun updatePersistedData(persistenceDataVersion: String): Boolean {
        if (persistenceDataVersion.isEmpty()) {
            Log.d(TAG, "PersistenceDataVersion is empty")
        } else {
            Log.d(TAG, "PersistenceDataVersion = $persistenceDataVersion")
        }
        return persistenceDataVersion.isEmpty() || persistenceDataVersion != PersistedDataConverterUtils.CURRENT_VERSION
    }

    private fun getUpdatedPersistedKeyName(
        currentPersistedKeyName: String,
        persistedSettingKeyThesaurus: Map<String, PersistedKeyData>
    ): String {
        var newKeyName = ""
        for (currentSettingKeyEntry in persistedSettingKeyThesaurus.entries) {
            if (currentSettingKeyEntry.value.obsoleteKeyNames?.contains(currentPersistedKeyName) == true) {
                newKeyName = currentSettingKeyEntry.key
                break
            }
        }
        return newKeyName
    }
}
