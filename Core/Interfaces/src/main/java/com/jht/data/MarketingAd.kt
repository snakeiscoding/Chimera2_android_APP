package com.jht.data

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Class describing a marketing advertisement for a UI module to use */
abstract class MarketingAd {
    /** ID for tracking */
    abstract val id: String
    /** Composable to render in the host UI */
    @Composable abstract fun Content(modifier: Modifier = Modifier)
}
/** Class containing list of [MarketingAd] and any meta data for the collection */
open class MarketingAdList(
    val list: List<MarketingAd> = listOf(),
)

/** Class describing an advertisement to display in same UI as a featured workout */
abstract class FeaturedWorkoutAd {
    /** ID for tracking */
    abstract val id: String
    /** Resource id for title string */
    abstract val titleId: Int
    /** Resource id for description string */
    abstract val descriptionId: Int
    /** Resource id for button string override */
    abstract val carouselButtonId: Int?
    /** background assets, if any */
    abstract val backgroundUri: List<Uri>

    /** Called when user interacts with ad (usually in place of a featured workout setup) */
    abstract fun onClick()
    /** Foreground content (something beyond just an image that stretch in background) */
    @Composable abstract fun Content(modifier: Modifier = Modifier, controller: AdHostController)
}
/** Class containing list of [FeaturedWorkoutAd] and any meta data for the collection */
open class FeaturedWorkoutAdList(
    val list: List<FeaturedWorkoutAd> = listOf(),
)

/** interface for the UI hosting the ad to take some control */
abstract class AdHostController {
    /** Called to indicate to the host that this ad needs to be held */
    abstract fun pause()
    /** Called to indicate to the host that this ad can be shuffled out when host desires */
    abstract fun resume()
}
/** A mock for [AdHostController] */
class MockAdHostController : AdHostController() {
    override fun pause() {}
    override fun resume() {}
}
