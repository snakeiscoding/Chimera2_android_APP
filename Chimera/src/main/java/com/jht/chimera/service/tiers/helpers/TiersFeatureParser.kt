package com.jht.chimera.service.tiers.helpers

import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys
import com.jht.chimera.service.tiers.PremiumFeature
import com.jht.chimera.service.tiers.PremiumFeatureSet
import com.jht.chimera.ui.machinesettings.JHTTypes
import com.jht.datamatrixlib.DataMatrix
import com.jht.oauthlib.api.exerciser.FeatureTierIdLists

/** Helper to populate a [featureSet] from various sources for use with Tiers services */
class TiersFeatureParser {
    val featureSet = PremiumFeatureSet()

    private fun addId(id: String, state: JHTTypes.FEATURE_STATE) {
        // if id is a single integer assume that it is a workout ID and not an app ID
        val featureType = if (id.toIntOrNull() == null) JHTTypes.FEATURE_TYPE.FEATURE_TYPE_APP else JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT
        // name is not provided via this method, however name seems only used for debug logs, will just use id for both
        val feature = PremiumFeature(id, id, featureType, state)
        featureSet.addFeature(feature)
    }

    /** populates the feature set from oauthlib's parsed console-features response */
    fun fromFeatureTierIdLists(idLists: FeatureTierIdLists) {
        idLists.disabledFeatures?.forEach { addId(it.id, JHTTypes.FEATURE_STATE.FEATURE_DISABLED) }
        idLists.enabledFeatures?.forEach { addId(it.id, JHTTypes.FEATURE_STATE.FEATURE_ENABLED) }
        idLists.hiddenFeatures?.forEach { addId(it.id, JHTTypes.FEATURE_STATE.FEATURE_HIDDEN) }
    }

    /** Update feature set for guest user */
    fun updateGuestSet() {
        DataMatrix.putParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, featureSet)
    }

    /** Update features set for current user */
    fun updateUserSet() {
        DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, featureSet)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        featureSet.features.forEach { feature ->
            builder.append(feature.id()).append(":").append(feature.featureType).append(":").append(feature.featureState).appendLine()
        }
        return builder.toString()
    }
}
