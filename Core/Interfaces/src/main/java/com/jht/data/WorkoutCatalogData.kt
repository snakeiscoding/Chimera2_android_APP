package com.jht.data

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jht.enums.UnitMeasurement
import java.util.UUID

/**
 * Describes a workout item.
 *
 * @property workoutId ID for the workout.
 * @property appId Application ID for launching a different screen on workout start.
 * @property titleId String ID for UI title.
 * @property descriptionId String ID for UI description.
 * @property background List of URIs for UI backgrounds; multiple backgrounds are optional.
 * @property thumbnail URI for UI thumbnail background.
 * @property badge Shows a small badge on the thumbnail for the workout and a few other spots.
 * @property optionalId Used for VA and Landmarks (multiple options share the same workout ID). Format: VA = "VA:unique_id", Landmarks = "Landmarks:title_string".
 * @property locked Indicates whether the item is locked; used for tier-based access control.
 * @property carouselLogoId If not null, used as the carousel header instead of the title.
 * @property badgeLabelId String ID for the label shown next to the badge icon; falls back to [titleId] when unset.
 * @property previewImageId Drawable ID for the setup modal preview image; falls back to the first [background] when unset.
 */
open class WorkoutItem @JvmOverloads constructor(
    var workoutId: Int,
    var appId: String,
    @param:StringRes
    var titleId: Int = 0,
    @param:StringRes
    var descriptionId: Int = 0,
    @param:StringRes
    var carouselButtonId: Int? = null,
    var background: List<Uri> = listOf(),
    var thumbnail: Uri = Uri.EMPTY,
    var badge: WorkoutBadge = WorkoutBadge(),

    //Optional Field - could be stripped out into a VAWorkoutItem and added to LandmarkWorkoutItem?
    var optionalId: String = "",

    //for tiers we need to lock out items (or should this be part of a UiWorkoutItem extending WorkoutItem instead of adding it here?)
    var locked: Boolean = false,

    @param:DrawableRes
    val carouselLogoId: Int? = null,

    @param:StringRes
    var badgeLabelId: Int = 0,
)

/** Label for the badge row, falling back to the title when the workout sets no override. */
@get:StringRes
val WorkoutItem.badgeLabelRes: Int
    get() = if (badgeLabelId != 0) badgeLabelId else titleId

/**
 * Image for the setup modal: a bundled drawable when the workout overrides it, otherwise
 * the first SD card background. Null when neither is set.
 */
val WorkoutItem.previewImageModel: Any?
    get() = background.firstOrNull()

// Class for holding info needed to draw badge for workouts. Some badges need a tinted icon according to the theme.
data class WorkoutBadge(
    val backgroundIcon: Int = 0, // drawable used for icon background
    val tintedInnerIcon: Int = 0, // drawable used for icon. Will be tinted to match primary color
) {
    fun hasBackground() : Boolean {
        return backgroundIcon > 0
    }

    fun hasTintedIcon() : Boolean {
        return tintedInnerIcon > 0
    }
}

/** A WorkoutItem with additional fields for a landmark workout */
class LandmarkWorkoutItem(
    // Base Class Fields
    workoutId: Int,
    appId: String,
    titleId: Int = 0,
    descriptionId: Int = 0,
    carouselButtonId: Int? = null,
    background: List<Uri> = listOf(),
    thumbnail: Uri = Uri.EMPTY,
    badge: WorkoutBadge = WorkoutBadge(),
    optionalId: String = "",

    // Landmark Fields
    var locationTitle: Int = 0, //Location string
    var secondaryDescriptionId: Int = 0,
    var videoUri: Uri = Uri.EMPTY, //URI of the landmarks video
    var meters: Float = 0f, //Meters height of the location. Used to calculate the floor goal
    var floors: Int = 0, //Floors goal
    var silhouetteUri: Uri  = Uri.EMPTY, // needed for LandmarkWorkoutActivity
    var silhouetteUriPair: Pair<Uri,Uri> = Pair(Uri.EMPTY, Uri.EMPTY), // needed for new LandmarksActivity.
    var xPivotCoordinates: List<Float> = listOf(), // x coordinates for shifting all 4 background images
    var yPivotCoordinates: List<Float> = listOf(), // y coordinates for shifting all 4 background images
) : WorkoutItem(workoutId, appId, titleId, descriptionId, carouselButtonId, background, thumbnail, badge, optionalId) {
    private fun getOrLastOrZero(list: List<Float>, index: Int) = list.getOrNull(index) ?: list.lastOrNull() ?: 0f
    fun getXPivotCoordinate(index: Int) = getOrLastOrZero(xPivotCoordinates, index)
    fun getYPivotCoordinate(index: Int) = getOrLastOrZero(yPivotCoordinates, index)
}

/** Describes a helper variable used to setup pillboxes and get the initial inputs needed to start a workout */
open class WorkoutInput(
    var min: Float,
    var max: Float,
    var defaultValue: Float,
    var stepSize: Float,
    var precision: Int,
    var label: String,
    var unitString: String,
    var unitMeasurement: UnitMeasurement? = null,
    var identifier : String
)

/** Uses the workout input and a mutable state to track the current user value */
class WorkoutInputUi(
    workoutInput: WorkoutInput,
    var currentValue: MutableState<Float>,
) : WorkoutInput(
    workoutInput.min,
    workoutInput.max,
    workoutInput.defaultValue,
    workoutInput.stepSize,
    workoutInput.precision,
    workoutInput.label,
    workoutInput.unitString,
    workoutInput.unitMeasurement,
    workoutInput.identifier
){
    fun convertToMetric() : Float{
        when (unitMeasurement) {
            UnitMeasurement.Mile,
            UnitMeasurement.MilePerHour -> {
                return (currentValue.value * UnitMeasurement.Mile.ratio/ UnitMeasurement.Kilometer.ratio).toFloat() // mi -> km
            }
            UnitMeasurement.Pound -> {
                return  (currentValue.value * UnitMeasurement.Pound.ratio/ UnitMeasurement.Kilogram.ratio).toFloat() // lbs -> kg
            }
            UnitMeasurement.Inch -> {
                // would this ever be used as a starting condition?  I think maybe not ...
                return (currentValue.value * UnitMeasurement.Inch.ratio/ UnitMeasurement.Centimeter.ratio).toFloat() // inches -> cm
            }
            else -> {
                return currentValue.value
            }
        }
    }
}



/** Describes a helper variable used to setup a level WorkoutInput with this data present as a small box
 * displaying the info for each level somewhere nearby on screen */
class WorkoutInputLevelInfo(
    var name: String,
    var average: String,
    var max: String
)


/** Contains data for a custom workout */
class CustomWorkout (
    val uuid: UUID = UUID.randomUUID(),
    val title: String = "",
    val intervalData: Map<String, List<Float>> = mapOf(),
    val hasCoolDown: Boolean = true,
) { // See Chimera/src/main/java/com/jht/chimera/ui/mainapp/goscreen/WorkoutCustom/CustomWorkoutItem.java
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CustomWorkout
        if (uuid != other.uuid) return false
        if (title != other.title) return false
        if (hasCoolDown != other.hasCoolDown) return false
        if (intervalData.size != other.intervalData.size) return false
        intervalData.forEach {map->
            val otherList = other.intervalData[map.key]
            if (map.value.size != otherList?.size) return false
            for (i in 0 until map.value.size) {
                if (map.value[i] != otherList[i]) return false
            }
        }
        return true
    }
}


/** Collection of [CustomWorkout] and data needed for using custom workout data */
class CustomWorkouts (
    val workoutInputs: List<WorkoutInputUi> = listOf(),
    val workouts: List<CustomWorkout> = listOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CustomWorkouts
        // input list
        if (workoutInputs.size != other.workoutInputs.size) return false
        for (i in workoutInputs.indices) {
            if (workoutInputs[i] != other.workoutInputs[i]) return false
        }
        // workout list
        if (workouts.size != other.workouts.size) return false
        for (i in workouts.indices) {
            if (workouts[i] != other.workouts[i]) return false
        }
        return true
    }
}



/**
 * A common plugin interface for featured workouts
 * It is expected that the system constructs them and provides the UI module with a list, then UI module can call its methods.
 * Hosting UI loads and holds workout inputs as the source of truth.
 * (This is currently all-in-one object, but we could breakout a widget/handler pair to allow customization outside of modules?)
*/
abstract class BaseFeaturedWorkoutPlugin(
    private val appContext: Context, // might need this for DI interfaces? or do we just pass InjectionEntryPoint instance directly? might other need for context?
    val workoutItem: WorkoutItem, // have system insert the workoutItem in case we need something like the current images?
) {
    /** Bundle of workout setup data specific to the workout app activity */
    open val appSetupBundle = Bundle()

    /** Setup UI to display in workout selection. It should leverage DynamicTheme if it is meant to follow look of the parent. */
    @Composable
    abstract fun SetupScreen( // should we do whole screen or just the minimum contents to fit? some workouts might need highly custom dialog though? options? styles? templates?
        workoutInputs: List<WorkoutInputUi>,
        onDismissRequest: () -> Unit = {}, // some setups may have embedded methods of dismissal
    )

    /** Begin the featured workout */
    abstract fun startWorkout(workoutInputs: List<WorkoutInputUi>)

    /** Called by hosting view model to setup plugin */
    abstract fun onLoad(workoutInputs: List<WorkoutInputUi>)

    /** Called by hosting view model to tear down plugin */
    abstract fun onUnload()
}

