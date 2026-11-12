package com.jht.mock.interfaces.models

import com.jht.data.FeaturedWorkoutAdList
import com.jht.data.MarketingAdList
import com.jht.interfaces.models.MarketingModel
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.MockIntValue

open class MockMarketingModel : MarketingModel() {
    override fun close() {}
    override val gymKitLevel = MockIntValue(0)
    override val welcomeAds = MockDataValue(MarketingAdList())
    override val featuredWorkoutAds = MockDataValue(FeaturedWorkoutAdList())
}
