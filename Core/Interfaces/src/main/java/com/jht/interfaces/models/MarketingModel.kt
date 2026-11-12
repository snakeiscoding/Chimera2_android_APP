package com.jht.interfaces.models

import com.jht.data.FeaturedWorkoutAdList
import com.jht.data.MarketingAdList
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.IntValueListener

/** Provides interfacing related to marketing system settings */
abstract class MarketingModel : BaseModel {
    /** The version of Gymkit marketing resources to use. Zero indicates unknown. */
    abstract val gymKitLevel: IntValueListener

    /** Provide welcome ad list if any */
    abstract val welcomeAds: DataValueListener<MarketingAdList>
    /** Provide workout selection ad list any */
    abstract val featuredWorkoutAds: DataValueListener<FeaturedWorkoutAdList>
}
