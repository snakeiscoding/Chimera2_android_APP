package com.jht.interfaces.models

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.marketing.AppleFeatureAd
import com.jht.chimera.marketing.AppleWelcomeAd
import com.jht.data.FeaturedWorkoutAdList
import com.jht.data.MarketingAdList
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixBoolComputeListener
import com.jht.interfaces.MatrixIntValueListener

class MatrixMarketingModel : MarketingModel() {
    private val manager: CallbackManager = CallbackManager()
    override fun close() {
        manager.cancelAllListeners()
    }

    override val gymKitLevel = MatrixIntValueListener(manager, SettingKeys.AssetManagement.DATA_MARKETING_GYMKIT_LEVEL_INT)

    override val welcomeAds by lazy {
        MatrixBoolComputeListener(manager, SettingKeys.AssetManagement.DATA_MARKETING_WELCOME_SCREEN_ADS_BOOL) { enabled ->
            val list = if (enabled) {
                listOfNotNull(
                    AppleWelcomeAd().takeIf { MatrixDeviceModel.isAppleWatchSupported() && gymKitLevel.value > 1 },
                )
            } else {
                emptyList()
            }
            MarketingAdList(list)
        }
    }
    override val featuredWorkoutAds by lazy {
        MatrixBoolComputeListener(manager, SettingKeys.AssetManagement.DATA_MARKETING_FEATURE_CAROUSEL_ADS_BOOL) { enabled ->
            val list = if (enabled) {
                listOfNotNull(
                    AppleFeatureAd().takeIf { MatrixDeviceModel.isAppleWatchSupported() && gymKitLevel.value > 1 },
                )
            } else {
                emptyList()
            }
            FeaturedWorkoutAdList(list)
        }
    }
}

