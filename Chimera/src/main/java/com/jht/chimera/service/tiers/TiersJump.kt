package com.jht.chimera.service.tiers

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys
import com.jht.chimera.lib.fleet.BearerTokenHelper
import com.jht.chimera.service.tiers.helpers.TiersFeatureParser
import com.jht.datamatrixlib.DataMatrix
import com.jht.oauthlib.api.exerciser.ExerciseApiClient
import com.jht.services.InlineConsoleService
import com.jht.utility.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "TiersJump"

/**
 * Tiers service but for new JUMP/Exerciser backend
 *
 * Get guest features if any (user specific features come with the JUMP additional user info bundle)
 * At the moment it seems that if there is no network and thus no web data, we just allow everything?
 * Was going to have a local offline copy of guest but was concerned about when to clear it.
 *
 * */
class TiersJump(context: Context): InlineConsoleService(context) {
    companion object {
        const val ID = TAG // UUID or unique friendly name for this service
    }
    override val id: String = ID // for service as a whole

    private val dataMatrix = DataMatrix()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var clubId = 0
    private var readWebJob: Job? = null

    override fun start() {
        Log.d(TAG, "Service started")
        // ConsoleServices web ready will catch environment changes, but it does not do club at this time
        dataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, {
            if (it != clubId) {
                clubId = it
                triggerWebPull()
            }
        }, true)
    }

    override fun stop() {
        dataMatrix.finish()
        serviceScope.cancel()
        Log.d(TAG, "Service stopped")
    }

    override fun onBearerReady(ready: Boolean) {
        Log.d(TAG, "Bearer ready $ready")
        if (ready) triggerWebPull()
    }

    override fun onWebPushData(data: Bundle) {
        // at some point we may want a push when guest features changed to force re-pull or include it in push data?
    }

    private fun triggerWebPull() {
        readWebJob?.cancel()
        readWebJob = serviceScope.launch {
            runCatchingCancellable {
                readFromWeb()
            }
            readWebJob = null
        }
    }

    private suspend fun readFromWeb() {
        val bearerToken = BearerTokenHelper.getAccessToken(context).getOrDefault("")
        ExerciseApiClient(
            DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT),
            bearerToken
        ).use { client ->
            // Publish even when the call fails, otherwise the previous club's restrictions stick around
            val idLists = client.getFeatureTiers(clubId)
            val featureParser = TiersFeatureParser()
            idLists?.let { featureParser.fromFeatureTierIdLists(it) }
            processFeaturesData(featureParser)
            Log.d(TAG, if (idLists != null) "read from web" else "no guest features from web, cleared")
        }
    }

    private fun processFeaturesData(featureParser: TiersFeatureParser) {
        if (featureParser.featureSet.isEmpty) {
            DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, false)
        } else {
            DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, true)
        }
        featureParser.updateGuestSet()
    }
}
