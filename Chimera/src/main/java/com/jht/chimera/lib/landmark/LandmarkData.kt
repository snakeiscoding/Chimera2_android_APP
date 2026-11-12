package com.jht.chimera.lib.landmark

import android.net.Uri
import android.os.Environment
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil.getString
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager.LANDMARKS_PREFIX
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutBadge
import java.io.File
import kotlin.math.roundToInt

private fun createLandmarkWorkoutItem(
    titleId: Int,
    descriptionId: Int,
    secondaryDescriptionId: Int = 0,
    locationTitle: Int,
    stringForLocalFiles: String,
    meters: Float,
    xPivotCoordinates: List<Float>,
    yPivotCoordinates: List<Float>,
): LandmarkWorkoutItem {
    return LandmarkWorkoutItem(
        workoutId = WorkoutIds.LANDMARKS,
        appId = AppIds.LANDMARKS_UUID,
        titleId = titleId,
        descriptionId = descriptionId,
        secondaryDescriptionId = secondaryDescriptionId,
        background = listOf(
            getLandmarkBackground("$stringForLocalFiles-1.jpg"),
            getLandmarkBackground("$stringForLocalFiles-2.jpg"),
            getLandmarkBackground("$stringForLocalFiles-3.jpg"),
            getLandmarkBackground("$stringForLocalFiles-4.jpg"),
            getLandmarkBackground("$stringForLocalFiles-5.jpg"),
            ),
        thumbnail = getLandmarkBackground("$stringForLocalFiles-1.jpg"), // do landmarks have a thumbnail? yes they do, first background image
        badge = WorkoutBadge(R.drawable.landmark_icon),
        optionalId = LANDMARKS_PREFIX + getString(titleId),
        locationTitle = locationTitle,
        videoUri = getLandmarkVideo("$stringForLocalFiles.mp4"),
        meters = meters,
        floors = if(frameConfig.STEPS_PER_FLOOR > 0 ) {
            ((frameConfig.STEPS_PER_METER * meters) / frameConfig.STEPS_PER_FLOOR).roundToInt()
        } else{
            0
        },
        silhouetteUriPair = Pair(
            getLandmarkSilhouette(file = "${stringForLocalFiles}_no_fill.png", isFilledFile = false), // no fill
            getLandmarkSilhouette(file = "${stringForLocalFiles}.png", isFilledFile = true) // filled with gradient
        ),
        silhouetteUri = getLandmarkSilhouette("${stringForLocalFiles}_drawing.png"),
        xPivotCoordinates = xPivotCoordinates,
        yPivotCoordinates = yPivotCoordinates
    )
}
private val frameConfig = FrameConfigManager.getInstance().frameConfig

private val chichen_itza = createLandmarkWorkoutItem(
    titleId = R.string.chichen_itza_tc,
    descriptionId = R.string.chichen_itza_pg,
    secondaryDescriptionId = R.string.chichen_itza_vtx_pg,
    locationTitle = R.string.chichen_itza_location_tc,
    stringForLocalFiles = "chichen_itza",
    meters = 30f,
    xPivotCoordinates = listOf(54f, 63f, 78f, 41f),
    yPivotCoordinates = listOf(27f, 33f, 46f, 25f)
)

private val basils_cathedral = createLandmarkWorkoutItem(
    titleId = R.string.basils_cathedral_tc,
    descriptionId = R.string.basils_cathedral_pg,
    secondaryDescriptionId = R.string.basils_cathedral_vtx_pg,
    locationTitle = R.string.basils_cathedral_location_tc,
    stringForLocalFiles = "st_basils_cathedral",
    meters = 47.5488f,
    xPivotCoordinates = listOf(61.4f, 68.8f, 68.1f, 59.8f),
    yPivotCoordinates = listOf(57f, 40f, 94.8f, 75.2f)
)

private val tower_of_pisa = createLandmarkWorkoutItem(
    titleId = R.string.tower_of_pisa_tc,
    descriptionId = R.string.tower_of_pisa_pg,
    secondaryDescriptionId = R.string.tower_of_pisa_vtx_pg,
    locationTitle = R.string.tower_of_pisa_location_tc,
    stringForLocalFiles = "tower_of_pisa",
    meters = 55.7784f,
    xPivotCoordinates = listOf(67.2f, 83.6f, 88.8f, 67.2f),
    yPivotCoordinates = listOf(28.3f, 20.8f, 30.4f, 23.5f)
)

private val sydney_opera_house = createLandmarkWorkoutItem(
    titleId = R.string.sydney_opera_house_tc,
    descriptionId = R.string.sydney_opera_house_pg,
    secondaryDescriptionId = R.string.sydney_opera_house_vtx_pg,
    locationTitle = R.string.sydney_opera_house_location_tc,
    stringForLocalFiles = "sydney_opera_house",
    meters = 65f,
    xPivotCoordinates = listOf(83.6f, 80f, 70f, 67.2f),
    yPivotCoordinates = listOf(20.8f, 53f, 70f, 23.5f)
)

private val iguazu_falls = createLandmarkWorkoutItem(
    titleId = R.string.iguazu_falls_tc,
    descriptionId = R.string.iguazu_falls_pg,
    secondaryDescriptionId = R.string.iguazu_falls_vtx_pg,
    locationTitle = R.string.iguazu_falls_location_tc,
    stringForLocalFiles = "iguazu_falls",
    meters = 82f,
    xPivotCoordinates = listOf(67.2f, 83.6f, 88.8f, 67.2f),
    yPivotCoordinates = listOf(28.3f, 20.8f, 30.4f, 23.5f)
)

private val statue_of_liberty = createLandmarkWorkoutItem(
    titleId = R.string.statue_of_liberty_tc,
    descriptionId = R.string.statue_of_liberty_pg,
    secondaryDescriptionId = R.string.statue_of_liberty_vtx_pg,
    locationTitle = R.string.statue_of_liberty_location_tc,
    stringForLocalFiles = "statue_of_liberty",
    meters = 92.964f,
    xPivotCoordinates = listOf(74.8f, 60.2f, 58.6f, 49.9f),
    yPivotCoordinates = listOf(27.9f, 19.5f, 48.5f, 50.0f)
)

private val victoria_falls = createLandmarkWorkoutItem(
    titleId = R.string.victoria_falls_tc,
    descriptionId = R.string.victoria_falls_pg,
    secondaryDescriptionId = R.string.victoria_falls_vtx_pg,
    locationTitle = R.string.victoria_falls_location_tc,
    stringForLocalFiles = "victoria_falls",
    meters = 108f,
    xPivotCoordinates = listOf(74.8f, 60.2f, 58.6f, 49.9f),
    yPivotCoordinates = listOf(27.9f, 19.5f, 48.5f, 50.0f)
)

private val giza_pyramid = createLandmarkWorkoutItem(
    titleId = R.string.giza_tc,
    descriptionId = R.string.giza_pg,
    secondaryDescriptionId = R.string.giza_vtx_pg,
    locationTitle = R.string.giza_location_tc,
    stringForLocalFiles = "giza_pyramid",
    meters = 138.684f,
    xPivotCoordinates = listOf(66.2f, 67.7f, 73.4f, 70.9f),
    yPivotCoordinates = listOf(70.6f, 42.7f, 27f, 65.4f)
)

private val cologne_cathedral = createLandmarkWorkoutItem(
    titleId = R.string.cologne_cathedral_tc,
    descriptionId = R.string.cologne_cathedral_pg,
    secondaryDescriptionId = R.string.cologne_cathedral_vtx_pg,
    locationTitle = R.string.cologne_cathedral_location_tc,
    stringForLocalFiles = "cologne_cathedral",
    meters = 157f,
    xPivotCoordinates = listOf(66.2f, 67.7f, 73.4f, 70.9f),
    yPivotCoordinates = listOf(70.6f, 42.7f, 27f, 65.4f)
)

private val taj_mahal = createLandmarkWorkoutItem(
    titleId = R.string.taj_mahal_tc,
    descriptionId = R.string.taj_mahal_pg,
    secondaryDescriptionId = R.string.taj_mahal_vtx_pg,
    locationTitle = R.string.taj_mahal_location_tc,
    stringForLocalFiles = "taj_mahal",
    meters = 170.9928f,
    xPivotCoordinates = listOf(63.3f, 60.8f, 74.8f, 49.5f),
    yPivotCoordinates = listOf(26.7f, 82.9f, 22f, 29.3f)
)

private val skellig_michael = createLandmarkWorkoutItem(
    titleId = R.string.skellig_michael_tc,
    descriptionId = R.string.skellig_michael_pg,
    secondaryDescriptionId = R.string.skellig_michael_vtx_pg,
    locationTitle = R.string.skellig_michael_location_tc,
    stringForLocalFiles = "skellig_michael",
    meters = 218f,
    xPivotCoordinates = listOf(63.3f, 60.8f, 74.8f, 49.5f),
    yPivotCoordinates = listOf(26.7f, 82.9f, 22f, 29.3f)
)

private val us_bank_tower = createLandmarkWorkoutItem(
    titleId = R.string.us_bank_tower_tc,
    descriptionId = R.string.us_bank_tower_pg,
    secondaryDescriptionId = R.string.us_bank_tower_vtx_pg,
    locationTitle = R.string.us_bank_tower_location_tc,
    stringForLocalFiles = "us_bank_tower",
    meters = 310f,
    xPivotCoordinates = listOf(43.5f, 68.0f, 82.9f, 70.6f),
    yPivotCoordinates = listOf(58.3f, 48.1f, 43.1f, 43.5f)
)

private val eiffel_tower = createLandmarkWorkoutItem(
    titleId = R.string.eiffel_tower_tc,
    descriptionId = R.string.eiffel_tower_pg,
    secondaryDescriptionId = R.string.eiffel_tower_vtx_pg,
    locationTitle = R.string.eiffel_tower_location_tc,
    stringForLocalFiles = "eiffel_tower",
    meters = 324.0024f,
    xPivotCoordinates = listOf(68.0f, 43.5f, 82.9f, 70.6f),
    yPivotCoordinates = listOf(48.1f, 58.3f, 43.1f, 43.5f)
)

private val uluru = createLandmarkWorkoutItem(
    titleId = R.string.uluru_tc,
    descriptionId = R.string.uluru_pg,
    secondaryDescriptionId = R.string.uluru_vtx_pg,
    locationTitle = R.string.uluru_location_tc,
    stringForLocalFiles = "uluru",
    meters = 348f,
    xPivotCoordinates = listOf(68.0f, 43.5f, 82.9f, 70.6f),
    yPivotCoordinates = listOf(48.1f, 58.3f, 43.1f, 43.5f)
)

private val empire_state = createLandmarkWorkoutItem(
    titleId = R.string.empire_state_tc,
    descriptionId = R.string.empire_state_pg,
    secondaryDescriptionId = R.string.empire_state_vtx_pg,
    locationTitle = R.string.empire_state_location_tc,
    stringForLocalFiles = "empire_state_building",
    meters = 381f,
    xPivotCoordinates = listOf(74.8f, 66.5f, 62.0f, 56.3f),
    yPivotCoordinates = listOf(25.2f, 30.4f, 32.9f, 17.5f)
)

private val jin_mao_tower = createLandmarkWorkoutItem(
    titleId = R.string.jin_mao_tower_tc,
    descriptionId = R.string.jin_mao_tower_pg,
    secondaryDescriptionId = R.string.jin_mao_tower_vtx_pg,
    locationTitle = R.string.jin_mao_tower_location_tc,
    stringForLocalFiles = "jin_mao_tower",
    meters = 420.5f,
    xPivotCoordinates = listOf(66.5f,66.5f,66.5f,66.5f),
    yPivotCoordinates = listOf(85.4f,85.4f,85.4f,85.4f)
)

private val rock_of_gibraltar = createLandmarkWorkoutItem(
    titleId = R.string.rock_of_gibraltar_tc,
    descriptionId = R.string.rock_of_gibraltar_pg,
    secondaryDescriptionId = R.string.rock_of_gibraltar_vtx_pg,
    locationTitle = R.string.rock_of_gibraltar_location_tc,
    stringForLocalFiles = "rock_of_gibraltar",
    meters = 426f,
    xPivotCoordinates = listOf(68.0f, 43.5f, 82.9f, 70.6f),
    yPivotCoordinates = listOf(48.1f, 58.3f, 43.1f, 43.5f)
)

private val cn_tower = createLandmarkWorkoutItem(
    titleId = R.string.cn_tower_tc,
    descriptionId = R.string.cn_tower_pg,
    secondaryDescriptionId = R.string.cn_tower_vtx_pg,
    locationTitle = R.string.cn_tower_location_tc,
    stringForLocalFiles = "cn_tower",
    meters = 457f,
    xPivotCoordinates = listOf(68.0f, 43.5f, 82.9f, 70.6f),
    yPivotCoordinates = listOf(48.1f, 58.3f, 43.1f, 43.5f)
)

private val oriental_pearl_radio_and_tv_tower = createLandmarkWorkoutItem(
    titleId = R.string.oriental_pearl_radio_and_tv_tower_tc,
    descriptionId = R.string.oriental_pearl_radio_and_tv_tower_pg,
    secondaryDescriptionId = R.string.oriental_pearl_radio_and_tv_tower_vtx_pg,
    locationTitle = R.string.oriental_pearl_radio_and_tv_tower_location_tc,
    stringForLocalFiles = "oriental_pearl_radio_and_tv_tower",
    meters = 468f,
    xPivotCoordinates = listOf(66.5f,66.5f,66.5f,66.5f),
    yPivotCoordinates = listOf(85.4f,85.4f,85.4f,85.4f)
)

private val shanghai_world_financial_center = createLandmarkWorkoutItem(
    titleId = R.string.shanghai_world_financial_center_tc,
    descriptionId = R.string.shanghai_world_financial_center_pg,
    secondaryDescriptionId = R.string.shanghai_tower_vtx_pg,
    locationTitle = R.string.shanghai_world_financial_center_location_tc,
    stringForLocalFiles = "shanghai_world_financial_center",
    meters = 492f,
    xPivotCoordinates = listOf(66.5f,66.5f,66.5f,66.5f),
    yPivotCoordinates = listOf(85.4f,85.4f,85.4f,85.4f)
)

private val taipei_101 = createLandmarkWorkoutItem(
    titleId = R.string.taipei_101_tc,
    descriptionId = R.string.taipei_101_pg,
    secondaryDescriptionId = R.string.taipei_101_vtx_pg,
    locationTitle = R.string.taipei_101_location_tc,
    stringForLocalFiles = "taipei_101",
    meters = 509.016f,
    xPivotCoordinates = listOf(76.4f,70.3f,68.1f,53.6f),
    yPivotCoordinates = listOf(27.5f, 95.4f, 21.2f, 8.3f)
)

private val one_world_trade_center = createLandmarkWorkoutItem(
    titleId = R.string.one_world_trade_center_tc,
    descriptionId = R.string.one_world_trade_center_pg,
    secondaryDescriptionId = R.string.one_world_trade_center_vtx_pg,
    locationTitle = R.string.one_world_trade_center_location_tc,
    stringForLocalFiles = "one_world_trade_center",
    meters = 541.3248f,
    xPivotCoordinates = listOf(72.4f, 75.5f, 57.3f, 55.4f),
    yPivotCoordinates = listOf(56.7f, 50.2f, 8.9f, 17.2f)
)

private val shanghai_tower = createLandmarkWorkoutItem(
    titleId = R.string.shanghai_tower_tc,
    descriptionId = R.string.shanghai_tower_pg,
    secondaryDescriptionId = R.string.shanghai_tower_vtx_pg,
    locationTitle = R.string.shanghai_tower_location_tc,
    stringForLocalFiles = "shanghai_tower",
    meters = 632f,
    xPivotCoordinates = listOf(66.5f, 66.5f,66.5f,66.5f),
    yPivotCoordinates = listOf(85.4f,85.4f,85.4f,85.4f)
)

private val christ_the_redeemer = createLandmarkWorkoutItem(
    titleId = R.string.christ_the_redeemer_tc,
    descriptionId = R.string.christ_the_redeemer_pg,
    secondaryDescriptionId = R.string.christ_the_redeemer_vtx_pg,
    locationTitle = R.string.christ_the_redeemer_location_tc,
    stringForLocalFiles = "christ_the_redeemer",
    meters = 733.9584f,
    xPivotCoordinates = listOf(69.0f,64.4f,68.6f,54.2f),
    yPivotCoordinates = listOf(29.5f,48.5f,25.6f,32.5f)
)

private val burj_khalifa = createLandmarkWorkoutItem(
    titleId = R.string.burj_khalifa_tc,
    descriptionId = R.string.burj_khalifa_pg,
    secondaryDescriptionId = R.string.burj_khalifa_vtx_pg,
    locationTitle = R.string.burj_khalifa_location_tc,
    stringForLocalFiles = "burj_khalifa",
    meters = 828.1416f,
    xPivotCoordinates = listOf(66.5f,68.9f,47.9f,72.3f),
    yPivotCoordinates = listOf(85.4f,80.0f,62.9f,76.7f)
)

private val the_gherkin = createLandmarkWorkoutItem(
    titleId = R.string.the_gherkin_tc,
    descriptionId = R.string.the_gherkin_pg,
    secondaryDescriptionId = R.string.gherkin_vtx_pg,
    locationTitle = R.string.the_gherkin_location_tc,
    stringForLocalFiles = "the_gherkin",
    meters = 180.137f,
    xPivotCoordinates = listOf(66.5f,68.9f,47.9f,72.3f),
    yPivotCoordinates = listOf(85.4f,80.0f,62.9f,76.7f)
)

private val merdeka_118 = createLandmarkWorkoutItem(
    titleId = R.string.merdeka_118_tc,
    descriptionId = R.string.merdeka_118_pg,
    secondaryDescriptionId = R.string.merdeka_118_vtx_pg,
    locationTitle = R.string.mmerdeka_118_location_tc,
    stringForLocalFiles = "merdeka_118",
    meters = 678.7896f,
    xPivotCoordinates = listOf(66.5f, 68.9f, 47.9f, 72.3f),
    yPivotCoordinates = listOf(85.4f, 80.0f, 62.9f, 76.7f)
)

private val tokyo_skytree = createLandmarkWorkoutItem(
    titleId = R.string.tokyo_skytree_tc,
    descriptionId = R.string.tokyo_skytree_pg,
    secondaryDescriptionId = R.string.tokyo_skytree_vtx_pg,
    locationTitle = R.string.tokyo_skytree_location_tc,
    stringForLocalFiles = "tokyo_skytree",
    meters = 633.984f,
    xPivotCoordinates = listOf(66.5f, 68.9f, 47.9f, 72.3f),
    yPivotCoordinates = listOf(85.4f, 80.0f, 62.9f, 76.7f)
)

private val willis_tower = createLandmarkWorkoutItem(
    titleId = R.string.willis_tower_tc,
    descriptionId = R.string.willis_tower_pg,
    secondaryDescriptionId = R.string.willis_tower_vtx_pg,
    locationTitle = R.string.willis_tower_location_tc,
    stringForLocalFiles = "willis_tower",
    meters = 442.2648f,
    xPivotCoordinates = listOf(66.5f, 68.9f, 47.9f, 72.3f),
    yPivotCoordinates = listOf(85.4f, 80.0f, 62.9f, 76.7f)
)

private val the_clock_towers = createLandmarkWorkoutItem(
    titleId = R.string.the_clock_towers_tc,
    descriptionId = R.string.the_clock_towers_pg,
    secondaryDescriptionId = R.string.clock_towers_vtx_pg,
    locationTitle = R.string.the_clock_towers_location_tc,
    stringForLocalFiles = "the_clock_towers",
    meters = 601.0656f,
    xPivotCoordinates = listOf(66.5f, 68.9f, 47.9f, 72.3f),
    yPivotCoordinates = listOf(85.4f, 80.0f, 62.9f, 76.7f)
)

private val lotte_world_tower = createLandmarkWorkoutItem(
    titleId = R.string.lotte_world_tower_tc,
    descriptionId = R.string.lotte_world_tower_pg,
    secondaryDescriptionId = R.string.lotte_world_tower_vtx_pg,
    locationTitle = R.string.lotte_world_tower_location_tc,
    stringForLocalFiles = "lotte_world_tower",
    meters = 555.0408f,
    xPivotCoordinates = listOf(66.5f, 68.9f, 47.9f, 72.3f),
    yPivotCoordinates = listOf(85.4f, 80.0f, 62.9f, 76.7f)
)


private val EMPTY = LandmarkWorkoutItem(
    workoutId = 0,
    appId = "EMPTY"
)

// CGC-22992 Temporarily block newer landmarks on pre-vortex era consoles (if longer term, might consider something more specific for feature flag?)
private val includeNewLandmarks = ProductConfig.hardware.isVortexBase

val landmarkWorkoutItemList = listOfNotNull(
    chichen_itza,
    basils_cathedral,
    tower_of_pisa,
    sydney_opera_house,
    iguazu_falls,
    statue_of_liberty,
    victoria_falls,
    giza_pyramid,
    cologne_cathedral,
    taj_mahal,
    the_gherkin.takeIf { includeNewLandmarks },
    skellig_michael,
    us_bank_tower,
    eiffel_tower,
    uluru,
    empire_state,
    jin_mao_tower,
    rock_of_gibraltar,
    willis_tower.takeIf { includeNewLandmarks },
    cn_tower,
    oriental_pearl_radio_and_tv_tower,
    shanghai_world_financial_center,
    taipei_101,
    one_world_trade_center,
    lotte_world_tower.takeIf { includeNewLandmarks },
    the_clock_towers.takeIf { includeNewLandmarks },
    shanghai_tower,
    tokyo_skytree.takeIf { includeNewLandmarks },
    merdeka_118.takeIf { includeNewLandmarks },
    christ_the_redeemer,
    burj_khalifa,
)

fun getLandmarkDataFromTitle(title: String): LandmarkWorkoutItem {
    return when(title){
        getString(chichen_itza.titleId) -> chichen_itza
        getString(basils_cathedral.titleId) -> basils_cathedral
        getString(tower_of_pisa.titleId) -> tower_of_pisa
        getString(sydney_opera_house.titleId) -> sydney_opera_house
        getString(iguazu_falls.titleId) -> iguazu_falls
        getString(statue_of_liberty.titleId) -> statue_of_liberty
        getString(victoria_falls.titleId) -> victoria_falls
        getString(giza_pyramid.titleId) -> giza_pyramid
        getString(cologne_cathedral.titleId) -> cologne_cathedral
        getString(taj_mahal.titleId) -> taj_mahal
        getString(skellig_michael.titleId) -> skellig_michael
        getString(us_bank_tower.titleId) -> us_bank_tower
        getString(eiffel_tower.titleId) -> eiffel_tower
        getString(uluru.titleId) -> uluru
        getString(empire_state.titleId) -> empire_state
        getString(jin_mao_tower.titleId) -> jin_mao_tower
        getString(rock_of_gibraltar.titleId) -> rock_of_gibraltar
        getString(cn_tower.titleId) -> cn_tower
        getString(oriental_pearl_radio_and_tv_tower.titleId) -> oriental_pearl_radio_and_tv_tower
        getString(shanghai_world_financial_center.titleId) -> shanghai_world_financial_center
        getString(taipei_101.titleId) -> taipei_101
        getString(one_world_trade_center.titleId) -> one_world_trade_center
        getString(shanghai_tower.titleId) -> shanghai_tower
        getString(christ_the_redeemer.titleId) -> christ_the_redeemer
        getString(burj_khalifa.titleId) -> burj_khalifa
        getString(the_gherkin.titleId) -> the_gherkin
        getString(merdeka_118.titleId) -> merdeka_118
        getString(tokyo_skytree.titleId) -> tokyo_skytree
        getString(willis_tower.titleId) -> willis_tower
        getString(the_clock_towers.titleId) -> the_clock_towers
        getString(lotte_world_tower.titleId) -> lotte_world_tower
        else -> EMPTY
    }
}

fun getFloorsFromLandmarkItem(landmarkItem:LandmarkWorkoutItem) : Int {
    return ((frameConfig.STEPS_PER_METER * landmarkItem.meters) / frameConfig.STEPS_PER_FLOOR).roundToInt()
}

// "landmarks/eiffel_tower/silhouette_eiffel_tower.png" for filled/graded silhouette"
// "landmarks/eiffel_tower/silhouette_eiffel_tower_no_fill.png" for empty silhouette"
fun getLandmarkSilhouette(file: String, isFilledFile: Boolean): Uri {
    return Uri.fromFile(
        File(
            Environment.getExternalStorageDirectory().path,
            "jht" + File.separator + "landmarks" +
                    File.separator + file.dropLast(if (isFilledFile) 4 else 12) +
                    File.separator + "silhouette_" + file
        )
    )
}

// "landmarks/chichen_itza/chichen_itza-1.jpg"
private fun getLandmarkBackground(file: String): Uri {
    return Uri.fromFile(
        File(
            Environment.getExternalStorageDirectory().path,
            "jht" + File.separator + "landmarks" +
                    File.separator + file.dropLast(6) + // drops the -1.jpg, which will match the parent folder
                    File.separator + file
        )
    )
}

// "landmarks/chichen_itza/chichen_itza.mp4"
private fun getLandmarkVideo(file: String): Uri {
    return Uri.fromFile(
        File(
            Environment.getExternalStorageDirectory().path,
            "jht" + File.separator + "landmarks" +
                    File.separator + file.dropLast(4) + // drops the .mp4, which will match the parent folder
                    File.separator + file
        )
    )
}

// "landmarks/chichen_itza/full_screen/chichen_itza_drawing.png"
private fun getLandmarkSilhouette(file: String): Uri {
    return Uri.fromFile(
        File(
            Environment.getExternalStorageDirectory().path,
            "jht" + File.separator + "landmarks" +
                    File.separator + file.dropLast(12) + // gets rid of _drawing.png, for location folder name
                    File.separator + "full_screen" + File.separator + file
        )
    )
}
