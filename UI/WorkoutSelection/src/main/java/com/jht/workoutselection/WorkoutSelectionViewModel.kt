package com.jht.workoutselection

import android.app.Application
import android.util.Log
import android.util.SparseArray
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.CustomWorkout
import com.jht.data.CustomWorkouts
import com.jht.data.FeaturedWorkoutAdList
import com.jht.data.intent.GoScreenIsHomeBool
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.LoginType
import com.jht.data.RfidLoginData
import com.jht.data.RfidOperatingMode
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.engage360.Engage360Handler
import com.jht.engage360.Engage360SystemModel
import com.jht.engage360.Engage360WorkoutLaunch
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.RelativeLocation
import com.jht.enums.UiEvent
import com.jht.enums.WorkoutId
import com.jht.enums.fitnessTestWorkoutIds
import com.jht.enums.goalWorkoutIds
import com.jht.enums.marathonWorkoutIds
import com.jht.enums.targetHeartRateWorkoutIds
import com.jht.enums.trainingWorkoutIds
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.LaunchWelcomeScreenOptions
import com.jht.interfaces.models.LoginModel
import com.jht.interfaces.models.MarketingModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutLaunchOptions
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutMedia
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import com.jht.widgets.RfidLoginWidgetHandler
import com.jht.widgets.helpers.AsyncUiState
import com.jht.workoutselection.vortex.parts.mainscreen.CarouselItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// Each tab has an identifier
internal const val TabFeaturedKey = "Featured"
internal const val TabCustomKey = "Custom"
internal const val TabTrainingKey = "Training"
internal const val TabAppsKey = "Apps"

/**
 * States for workout selection
 */
enum class WorkoutSelectionState {
    Featured,
    Training,
    Custom,
    Engage360,
    SingleWorkout,
}

private const val TAG = "WorkoutSelectionViewModel"
private const val TARGET_HEART_RATE_UUID = "082d7e46-fd2f-11e6-bc64-92361f002671" // should this be provided by a shared app module?

/**
 * Workout Selection View Model
 *
 * Main Functionality
 * 1. Go Button [launchQuickStartWorkout]
 * 2. Get Workout List [startSelectedItemWorkout]
 * 3. Launches a workout [startSelectedItemWorkout]
 *
 * Edge Case Functions, One off features
 * 4. Frame Go Button [onPhysicalGo] Only for frames with frame go button
 */
@HiltViewModel
open class WorkoutSelectionViewModel @Inject constructor(
    val systemModel: SystemModel,
    val uiBaseModel: UIBaseModel,
    idleTimerModel: IdleTimerModel,
    private val loginModel: LoginModel,
    val userModel: UserModel,
    private val workoutModel: WorkoutModel,
    private val workoutLauncherModel: WorkoutLauncherModel,
    private val navigationModel: NavigationModel,
    private val catalogModel: WorkoutCatalogModel,
    private val engage360SystemModel: Engage360SystemModel,
    private val marketingModel: MarketingModel,
    private val deviceModel: DeviceModel,
    savedStateHandle: SavedStateHandle,
    val application: Application,
) : ThemedViewModel() {

    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val uiState = mutableStateOf(WorkoutSelectionState.Featured)

    //Workout categories and lists
    var workoutCategories by mutableStateOf(listOf<CategoryWorkoutItems>())

    internal var carouselItems by mutableStateOf(listOf<CarouselItem>())
    var featuredItems by mutableStateOf(listOf<WorkoutItem>())
    private var featuredPlugins: List<BaseFeaturedWorkoutPlugin> = emptyList()

    internal var trainingCarouselItems by mutableStateOf(listOf<CarouselItem>())
    var virtualActiveItems by mutableStateOf(listOf<WorkoutItem>())
    var landmarksItems = listOf<LandmarkWorkoutItem>()
    var singleWorkoutItem : WorkoutItem? = null

    //Workout selection
    val selectedWorkoutItem = mutableStateOf<WorkoutItem?>(null)
    var currentWorkoutInputs = mutableStateListOf<WorkoutInputUi>()
    private var userChangedInputs = mutableMapOf<String, WorkoutInputUi>() //keeps track of user changes to starting pillbox values

    //Workout display variables
    val activeFeaturePlugin = mutableStateOf<BaseFeaturedWorkoutPlugin?>(null)
    val showFeaturePluginModal = mutableStateOf(false)
    val showDefaultWorkoutModal = mutableStateOf(false)
    val showVirtualActiveModal = mutableStateOf(false)
    val showLandmarksModal = mutableStateOf(false)

    //Home screen variables if the welcome screen is turned off
    val isHomeScreen = savedStateHandle.get<Boolean>(GoScreenIsHomeBool) ?: false
    private val homeScreenIdleTimer = idleTimerModel.createHomeScreenSystemTimer(isHomeScreen)

    private var startupTimeout: Job? = null
    val selectedTabKey = mutableStateOf<Any?>(TabFeaturedKey)

    private var levelInfoJob: Job? = null
    var workoutInputLevelInfoList = mutableStateOf(listOf<WorkoutInputLevelInfo>())

    val isLoadingWorkout = mutableStateOf(false)
    val isLaunchingWorkout = mutableStateOf(false)

    private val rfidLoginState = mutableStateOf(AsyncUiState.Unknown)

    val customWorkoutHandler = CustomWorkoutHandler()

    val showTiersUpgradePrompt = mutableStateOf(false)

    var engage360Handler : Engage360Handler? = null

    /**
     * Call this to register listeners when activity is resumed
     */
    override fun start() {
        systemModel.sendUiEvent(UiEvent.GO_SCREEN)
        homeScreenIdleTimer.start()

        workoutModel.isWorkoutLaunching.listen {
            isLaunchingWorkout.value = it
        }

        uiBaseModel.themeUri.listenAndTrigger {
            theme.value = loadTheme(it)
        }

        systemModel.keyGoPressed.listen { if (it) onPhysicalGo() } // TENTATIVE

        catalogModel.customWorkouts.listenAndTrigger {
            customWorkoutHandler.copyFrom(it)
        }

        loginModel.rfidSetMode(RfidOperatingMode.Normal)
        nextRfidScan()

        catalogModel.workoutListReady.listenAndTrigger { ready ->
            if(ready) {
                updateWorkoutItems() // load featured items

                //Starting PTP checks after workout items are updated
                //There is a seca tiers dependency to not auto-launch if it is disabled
                userModel.isPTP.listenAndTrigger { isPTP ->
                    if (isPTP && engage360SystemModel.showEngage360Launch && checkSecaTiersEnabled()) {
                        showPtp() // if we can show PTP, do so
                    }
                }

                if(workoutModel.singleAppMode.isNotEmpty()){
                    uiState.value = WorkoutSelectionState.SingleWorkout
                    //TODO: Make less hardcoded for switch stride specifically
                    singleWorkoutItem = featuredItems.find{it.workoutId == WorkoutId.HYBRID_TD_SWITCH_STRIDE.id }
                }
            }
        }

        // Handle the case where the workout selection starts before VA workouts are ready
        catalogModel.onVAWorkoutsChange.listen {
            Log.d(TAG, "onVAWorkoutsChange") // this should only trigger if VA workouts are loaded late
            updateWorkoutItems()
        }

        // Do we want to update if the ads change? maybe don't trigger immediate as more important listener will trigger update
        marketingModel.featuredWorkoutAds.listen {
            Log.d(TAG, "featuredWorkoutAds change")
            updateWorkoutItems(it)
        }

        // I have not seen an instance where the ready fails to toggle, but just in case
        startupTimeout = viewModelScope.launch {
            delay(15000)
            if (workoutCategories.isEmpty()) {
                Log.w(TAG, "Screen timed out waiting for catalog to be ready")
                updateWorkoutItems()
            }
        }
        // monitor for changes to workout level info
        observeLevelInput()
    }


    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        levelInfoJob?.cancel()
        startupTimeout?.cancel()
        updateCustomWorkouts()
        homeScreenIdleTimer.stop()
        loginModel.close()
        systemModel.close()
        userModel.close()
        catalogModel.close()
        marketingModel.close()
        deviceModel.close()
        engage360Handler?.close()
        engage360Handler = null
    }

    /**
     * Loads the workouts available based on the frame and enabled features
     */
    fun updateWorkoutItems(featuredWorkoutAdList: FeaturedWorkoutAdList = marketingModel.featuredWorkoutAds.value) {
        Log.d(TAG, "Catalog is ready")
        catalogModel.setFeaturedWorkouts()
        carouselItems = featuredWorkoutAdList.list.map { CarouselItem.fromAd(it) } + catalogModel.getCarouselWorkouts.map { CarouselItem.fromWorkoutItem(it) }
        featuredItems = catalogModel.getFeaturedWorkouts
        featuredPlugins = catalogModel.getFeatureWorkoutPlugins
        trainingCarouselItems = catalogModel.getTrainingCarouselWorkouts.map { CarouselItem.fromWorkoutItem(it) }
        virtualActiveItems = catalogModel.getVAWorkouts
        landmarksItems = catalogModel.getLandmarkWorkouts
        val workoutList = catalogModel.getWorkoutList

        //Take only categories that have workouts (marathon can be empty if the setting is disabled)
        workoutCategories = listOfNotNull(
            CategoryWorkoutItems(R.string.training_tc, extractWorkoutItemsForIds(trainingWorkoutIds, workoutList)).takeIf { it.items.isNotEmpty() },
            CategoryWorkoutItems(R.string.goals_tc, extractWorkoutItemsForIds(goalWorkoutIds, workoutList)).takeIf { it.items.isNotEmpty() },
            CategoryWorkoutItems(R.string.marathon_tc, extractWorkoutItemsForIds(marathonWorkoutIds, workoutList)).takeIf { it.items.isNotEmpty() },
            CategoryWorkoutItems(R.string.target_heart_rate_tc, extractWorkoutItemsForIds(targetHeartRateWorkoutIds, workoutList)).takeIf { it.items.isNotEmpty() },
            CategoryWorkoutItems(R.string.fitness_test_tc, extractWorkoutItemsForIds(fitnessTestWorkoutIds, workoutList)).takeIf { it.items.isNotEmpty() },
        )
    }

    /**
     * Loads the workout setup for the selected workout. Most workouts fall into three categories
     * 1. Launches an app
     * 2. Displays to a specific program setup
     * 3. Displays the default workout setup
     */
    fun selectWorkoutItem(workoutItem: WorkoutItem) {
        if(workoutItem.locked){
            showTiersUpgradePrompt.value = true
            selectedWorkoutItem.value = null
            return
        }
        activeFeaturePlugin.value = featuredPlugins.find { it.workoutItem.workoutId == workoutItem.workoutId }
        showFeaturePluginModal.value = (activeFeaturePlugin.value != null)
        selectedWorkoutItem.value = workoutItem

        // No loaded params for Seca, and it would get isLoadingWorkout stuck as true
        if (workoutItem.workoutId == WorkoutId.SECA.id) {
            loadPtp()
            return
        }

        loadWorkoutParameters(workoutItem)

        if (!showFeaturePluginModal.value) when(workoutItem.workoutId) {
            //Workouts that launch apps instead of the normal selection
            WorkoutId.IFIT.id,
            WorkoutId.YMCA_360.id,
            WorkoutId.PROGRESSION_WORKOUT.id -> launchFeaturedApp(workoutItem.appId)

            //Workouts that launch specific program UI
            WorkoutId.VIRTUAL_ACTIVE.id,
            WorkoutId.LANDMARKS.id -> {
                //VA and Landmarks have an additional selection screen to pick between the list of options
                //The optional id signifies an individual workout has been selected, rather than the program setup
                if (workoutItem.optionalId.isEmpty()) {
                    launchProgramSetup(workoutItem)
                } else {
                    showDefaultWorkoutModal.value = true
                }
            }

            //All other workouts show the default setup
            else -> {
                if (!launchProgramSetup(workoutItem)) showDefaultWorkoutModal.value = true
            }
        }
    }

    /**
     * Loads the starting parameters needed for the workout
     */
    fun loadWorkoutParameters(workoutItem: WorkoutItem){
        if (!isLoadingWorkout.value) {
            isLoadingWorkout.value = true

            viewModelScope.launch {
                loadWorkoutStartingInputs(workoutItem)
                activeFeaturePlugin.value?.onLoad(currentWorkoutInputs)
                isLoadingWorkout.value = false
            }
        }
    }

    /**
     * Launching the program setup for specific workouts that have custom UI displays, return true if one was triggered
     */
    fun launchProgramSetup(workoutItem: WorkoutItem): Boolean {
        when(workoutItem.workoutId){
            WorkoutId.VIRTUAL_ACTIVE.id -> showVirtualActiveModal.value = true
            WorkoutId.LANDMARKS.id-> showLandmarksModal.value = true
            else -> return false
        }
        return true
    }
    private fun hideSetupModals() {
        showFeaturePluginModal.value = false
        showDefaultWorkoutModal.value = false
        showVirtualActiveModal.value = false
        showLandmarksModal.value = false
    }

    private suspend fun loadWorkoutStartingInputs(workoutItem: WorkoutItem) {
        //Save current user changes to parameters
        currentWorkoutInputs.forEach { input ->
            userChangedInputs.put(input.identifier, input)
        }
        currentWorkoutInputs.clear()

        // suspends the coroutine until complete
        val workoutInputs = catalogModel.getWorkoutInputs(workoutItem.workoutId, workoutItem.optionalId)
        if(workoutInputs.any { it.identifier == ConsoleWorkoutVariables.LEVEL }){
            catalogModel.setWorkoutControllerModel() // suspends the coroutine until complete
        }

        workoutInputs.forEach { input ->
            //Check if there are user changed values that already exist
            userChangedInputs[input.identifier]?.let {
                //Add user changed parameter if a previous one exists
                currentWorkoutInputs.add(it)
            } ?:
            //Otherwise add a new starting input with the default value
            currentWorkoutInputs.add(
                WorkoutInputUi(
                    workoutInput = input,
                    currentValue =  mutableFloatStateOf(input.defaultValue)
                )
            )
        }

        //Ignore floors goal start from workout inputs
        if(workoutItem.workoutId == WorkoutId.LANDMARKS.id){
            currentWorkoutInputs.removeIf {it.identifier == ConsoleWorkoutVariables.FLOORS_GOAL_START }
        }
    }

    /** Start thw selected workout item, return true if it could launch the workout */
    fun startSelectedItemWorkout(): Boolean {
        selectedWorkoutItem.value?.let{
            workoutLauncherModel.checkLaunchWorkoutPin { proceed ->
                if (!proceed) return@checkLaunchWorkoutPin
                val initialConditions = mutableMapOf<String,Float>()
                for (workoutInput in currentWorkoutInputs) {
                    initialConditions[workoutInput.identifier] = workoutInput.convertToMetric()
                }
                runSpecificWorkoutLogic(it, initialConditions)
                workoutLauncherModel.launchSelectedWorkout(initialConditions)
            }
            return true
        }
        return false
    }

    private fun runSpecificWorkoutLogic(
        workoutItem: WorkoutItem,
        initialConditions: MutableMap<String, Float>
    ){
        when(workoutItem.workoutId) {
            WorkoutId.LANDMARKS.id,
            WorkoutId.VIRTUAL_ACTIVE.id -> {
                //Optional id used by VA and Landmarks
                if (workoutItem.optionalId.isNotEmpty()) {
                    setWorkoutOptionalId(workoutItem.optionalId)
                }
                //Update floors goal for Landmarks to be the selected location
                if (workoutItem.workoutId == WorkoutId.LANDMARKS.id) {
                    initialConditions[ConsoleWorkoutVariables.FLOORS_GOAL_START] =
                        (workoutItem as LandmarkWorkoutItem).floors.toFloat()
                }
            }
        }
    }

    /**
     * Here's the physical go button
     * 1. Quick start workout
     * 2. If on custom, do a custom workout
     */
    private fun onPhysicalGo() {
        if (!isLoadingWorkout.value) {
            //workoutLauncherModel.launchQuickStartWorkout()
            // start the selected workout
            when (uiState.value) {
                WorkoutSelectionState.Training -> if (!startSelectedItemWorkout()) startQuickStartWorkout()
                WorkoutSelectionState.Custom -> startSelectedCustomWorkout()
                else -> startQuickStartWorkout()
            }
        }
    }
    private fun startQuickStartWorkout() {
        workoutLauncherModel.checkLaunchWorkoutPin { proceed -> if (proceed) workoutLauncherModel.launchQuickStartWorkout() }
    }

    private fun updateCustomWorkouts() {
        if (!userModel.isGuest) {
            // is checking for changes a UI or backend job? little of both?
            val newData = customWorkoutHandler.convertToCustomWorkouts()
            val isDirty = newData != customWorkoutHandler.originalData
            if (isDirty) catalogModel.customWorkouts.value = newData
        }
    }

    fun startSelectedCustomWorkout() {
        customWorkoutHandler.selectedWorkout.value?.let {
            workoutLauncherModel.checkLaunchWorkoutPin { proceed ->
                if (proceed) catalogModel.startCustomWorkout(it.convertToCustomWorkout())
            }
        }
    }

    fun launchQuickStartWorkout() {
        if (!isLoadingWorkout.value) {
            workoutLauncherModel.checkLaunchWorkoutPin { proceed -> if (proceed) workoutLauncherModel.launchQuickStartWorkout() }
        }
    }

    fun onSelectTabKey(key: Any? = null) {
        if (selectedTabKey.value != key) {
            hideSetupModals()
            selectedTabKey.value = key
        }
    }

    fun onUserButton() {
        // CGC-18161 disable the profile button for now, unless you are a guest (However vortex UI provides logged user with a menu of other options)
        if (userModel.isGuest) {
            navigationModel.launchWelcomeScreen()
        }
    }

    fun onHomeButton() {
        //Home button hides Engage360 if it is active
        if (uiState.value == WorkoutSelectionState.Engage360){
            uiState.value = WorkoutSelectionState.Featured
            systemModel.sendUiEvent(UiEvent.CLOSE_FEATURE_WORKOUT_SETUP)
        }
        //Otherwise, launch the splash screen
        else navigationModel.launchSplashScreen()
    }

    fun showPtp() {
        catalogModel.getWorkoutList[WorkoutId.SECA.id]?.let { selectWorkoutItem(it) }
    }

    private fun loadPtp() {
        if(engage360Handler == null){
            engage360Handler = Engage360Handler(
                context = application,
                launchingWorkout = ::onPTPLaunchWorkout,
                isDebug = uiBaseModel.isDebugBuild,
                onSurveyWorkoutSelection = { onHomeButton() },
                onSurveyLogout = { navigationModel.launchSplashScreen() },
                showMessages = !userModel.isSecaUser
            )
            engage360Handler?.startServiceCommunication()
        }
        uiState.value = WorkoutSelectionState.Engage360

        // For now, they do not want the GO LCD grip when PTPN is up (cannot start a PTP workout on GO cause may be more than one on screen?)
        // We could just block the physical GO event, but they seem to prefer not showing it right now.
        systemModel.sendUiEvent(UiEvent.OPEN_FEATURE_WORKOUT_SETUP)
    }

    val rfidHandler = RfidLoginWidgetHandler(
        inProgress = { inProgress-> rfidLoginState.value = if (inProgress) AsyncUiState.Waiting else AsyncUiState.Unknown }, // popup cannot be dismissed, but maybe UI should know?
        loginComplete = { }, // note RFID tag scanning only functions if user is not logged in
        needToAssociate = ::gotoXidLoginToAssociate,
        readyForNextScan = ::nextRfidScan,
    )
    private fun nextRfidScan() {
        loginModel.rfidStartSession(rfidHandler)
    }
    private fun gotoXidLoginToAssociate(rfid: RfidLoginData) {
        // do we defer to welcome screen or provide login UI within this screen?
        // since we do not permit RFID tag scanning if someone is logged in, we can go to welcome directly without worrying about logging out a previous user
        navigationModel.launchWelcomeScreen(LaunchWelcomeScreenOptions(
            preselectLogin = LoginType.Xid,
            associateDevice = rfid,
        ))
    }


    /** monitors the current workout inputs for level changes and updates the workout level info */
    private fun observeLevelInput() {
        levelInfoJob?.cancel()
        levelInfoJob = viewModelScope.launch {
            snapshotFlow {
                currentWorkoutInputs
                    .firstOrNull { it.identifier == ConsoleWorkoutVariables.LEVEL }
                    ?.currentValue?.value
            }.collect { level ->
                workoutInputLevelInfoList.value = level
                    ?.let { catalogModel.getWorkoutInputLevelInfo(it.toInt() - 1) }
                    ?: emptyList()
            }
        }
    }



    private fun launchFeaturedApp(uuid: String) {
        workoutLauncherModel.checkLaunchWorkoutPin { proceed ->
            if (proceed) workoutLauncherModel.launchFeaturedApp(uuid)
        }
    }



    private fun onPTPLaunchWorkout(data: Engage360WorkoutLaunch) {
        workoutLauncherModel.checkLaunchWorkoutPin { proceed ->
            if (!proceed) {
                engage360Handler?.abortLaunch()
                return@checkLaunchWorkoutPin
            }
            when (data.workoutId) {
                WorkoutId.SECA.id,
                WorkoutId.MANUAL.id -> {
                        workoutLauncherModel.launchCustomWorkout(
                            WorkoutId.fromId(data.workoutId),
                            null,
                            data.workoutScript,
                            data.initialConditions
                        )
                }
                WorkoutId.VIRTUAL_ACTIVE.id -> {
                    viewModelScope.launch {
                        workoutLauncherModel.launchExistingWorkout(
                            WorkoutId.fromId(data.workoutId),
                            data.initialConditions,
                            WorkoutLaunchOptions(optionalId = data.optionId),
                        )
                    }
                }
                WorkoutId.SPRINT8.id -> {
                    viewModelScope.launch {
                        workoutLauncherModel.launchExistingWorkout(
                            WorkoutId.fromId(data.workoutId),
                            data.initialConditions,
                            WorkoutLaunchOptions(media = WorkoutMedia.None, sectionCount = 8), // old code hardcode sprints, but should it comes from actual interval count?
                        )
                    }
                }
                WorkoutId.CARDIO_SCAN.id -> {
                    workoutLauncherModel.launchCustomWorkout(
                        WorkoutId.fromId(data.workoutId),
                        TARGET_HEART_RATE_UUID, // is this for PTP Native to worry about or for the PTP consumer to decide? seems UI specific so defer to consumer
                        data.workoutScript,
                        data.initialConditions
                    )
                }
                WorkoutId.DISTANCE_GOAL.id, WorkoutId.CALORIES_GOAL.id, WorkoutId.TIME_GOAL.id -> {
                    // TODO need to set thirdPartyId? in PTP manager it does not seem to be used anywhere?
                    viewModelScope.launch {
                        workoutLauncherModel.launchExistingWorkout(
                            WorkoutId.fromId(data.workoutId),
                            data.initialConditions
                        )
                    }
                }
                else -> {
                    viewModelScope.launch {
                        workoutLauncherModel.launchExistingWorkout(
                            WorkoutId.fromId(data.workoutId),
                            data.initialConditions
                        )
                    }
                }
            }
        }
    }

    //Returns that seca is enabled, unless it is locked by tiers
    //Default case returns true
    fun checkSecaTiersEnabled() : Boolean{
        val secaWorkout = carouselItems.find{ it.tag is WorkoutItem && it.tag.workoutId == WorkoutId.SECA.id }
        if (secaWorkout != null){
            return !secaWorkout.locked
        }
        return true
    }

    fun setWorkoutOptionalId(workoutOptionalId: String) {
        workoutModel.apply {
            this.workoutOptionId.value = workoutOptionalId
        }
    }

    fun showDebugDialog() {
        if (BuildConfig.DEBUG) {
            systemModel.showDebugMenu()
        }
    }


    companion object {
        /**
         * Finds the index of the centered position in a list.
         * This returns the left-center index if list is even.
         *
         * @param listSize The size of the list.
         * @return The index of the centered position.
         */
        fun findCenteredIndex(listSize: Int): Int {
            return (listSize - 1) / 2
        }

        /**
         * Reorders a list of [WorkoutItem] to ensure the Pace workout is in the centered position for initial viewing.
         * Pace/Seca workout is determined by [WorkoutId.SECA].
         * This accomplishes 2 goals: it ensures that the first shown workout item has workouts on the
         * left and right of it for aesthetics, and ensures that, when present, the Pace workout is
         * centered on supported systems.
         *
         * @param workouts List of [WorkoutItem] to be reordered.
         * @return Reordered list of [WorkoutItem] with the Pace workout in the centered position.
         */
        fun orderPagerWorkouts(workouts: List<WorkoutItem>): List<WorkoutItem> {
            if (workouts.size <= 1) return workouts

            val paceIndex = workouts.indexOfFirst { it.workoutId == WorkoutId.SECA.id }
            if (paceIndex == -1) return workouts

            val targetIndex = findCenteredIndex(workouts.size)

            val mutable = workouts.toMutableList()
            val paceWorkout = mutable.removeAt(paceIndex)
            mutable.add(targetIndex, paceWorkout)

            return mutable
        }
    }

    internal fun needsRfidIcon(): Boolean {
        return deviceModel.rfidReaderLocation == RelativeLocation.LEFT_MIDDLE
    }
}

/** Class containing title and items for a workout category */
class CategoryWorkoutItems(
    val titleId : Int,
    val items: List<WorkoutItem>,
)

private fun extractWorkoutItemsForIds(workoutIds: List<WorkoutId>, workoutList: SparseArray<WorkoutItem>): List<WorkoutItem> {
    val workoutItems = mutableListOf<WorkoutItem>()
    for (workoutId in workoutIds) {
        val workoutItem = workoutList.get(workoutId.id)
        if (workoutItem != null && !workoutItems.contains(workoutItem)) {
            workoutItems.add(workoutItem)
        }
    }
    return workoutItems
}


//#region CUSTOM WORKOUT
/** Handles data tracking for Custom Workouts editing UI */
class CustomWorkoutHandler {
    val customWorkouts = mutableStateListOf<CustomWorkoutEditable>() // need compose to react to items changing
    val selectedWorkout = mutableStateOf<CustomWorkoutEditable?>(null)
    var workoutInputs: List<WorkoutInputUi> = listOf()
        private set
    var originalData: CustomWorkouts? = null
        private set

    fun copyFrom(customWorkoutData: CustomWorkouts): CustomWorkoutHandler {
        originalData = customWorkoutData
        workoutInputs = customWorkoutData.workoutInputs
        customWorkouts.clear()
        customWorkoutData.workouts.forEach {
            val item = CustomWorkoutEditable(workoutInputs, it)
            customWorkouts.add(item)
        }
        if (customWorkouts.isEmpty()) addWorkout()
        else selectWorkout(if (customWorkouts.isNotEmpty()) customWorkouts[0] else null)
        return this
    }
    fun convertToCustomWorkouts(): CustomWorkouts {
        return CustomWorkouts(
            workoutInputs,
            customWorkouts.map { it.convertToCustomWorkout() }
        )
    }

    fun selectWorkout(customWorkout: CustomWorkoutEditable?) {
        selectedWorkout.value = customWorkout
    }

    fun addWorkout() {
        // should the model provide a createDefaultWorkout instead of this doing it?
        val newWorkout = CustomWorkout(
            intervalData = workoutInputs.associate {
                Pair(it.identifier, listOf(it.defaultValue))
            }
        )
        val newItem = CustomWorkoutEditable(workoutInputs, newWorkout)
        customWorkouts.add(newItem)
        selectWorkout(newItem)
    }

    fun deleteWorkout() {
        selectedWorkout.value?.let {
            customWorkouts.remove(it)
            selectWorkout(if (customWorkouts.isNotEmpty()) customWorkouts[0] else null)
        }
    }
}

/** Holds the temporary data for custom workout while being edited. Rearranges interval data by interval instead of by input. */
class CustomWorkoutEditable(
    val workoutInputs: List<WorkoutInput>,
    val originalData: CustomWorkout,
) {
    val title = mutableStateOf(originalData.title)
    private val hasCoolDown = mutableStateOf(originalData.hasCoolDown)
    val intervals = mutableStateListOf<CustomWorkoutIntervalEditable>()
    val selectedInterval = mutableIntStateOf(0)
    val uiStartingColumn = mutableIntStateOf(0) // we want to preserve the starting column when switching between workouts in the UI
    init {
        // flip from map of string/list-value to list of map of string/value-state
        val intervalCount = originalData.intervalData.minOf { it.value.size }
        repeat(intervalCount) { index->
            val segmentMap = mutableMapOf<String, MutableState<Float>>()
            workoutInputs.forEach {
                segmentMap[it.identifier] = mutableFloatStateOf(originalData.intervalData[it.identifier]?.get(index) ?: 0f)
            }
            intervals.add(CustomWorkoutIntervalEditable(segmentMap))
        }
        selectInterval(0)
    }
    fun convertToCustomWorkout(): CustomWorkout {
        // repack intervals into per-input lists
        val intervalData = mutableMapOf<String, List<Float>>()
        workoutInputs.forEach {input->
            intervalData[input.identifier] = getIntervalsFor(input)
        }
        return CustomWorkout(
            originalData.uuid,
            title.value,
            intervalData,
            hasCoolDown.value,
        )
    }
    fun selectInterval(index: Int) {
        selectedInterval.intValue = index
    }
    fun previousInterval(): Int {
        val newIndex = maxOf(0, selectedInterval.intValue - 1)
        selectInterval(newIndex)
        return newIndex
    }
    fun nextInterval(): Int {
        val newIndex = minOf(intervals.lastIndex, selectedInterval.intValue + 1)
        selectInterval(newIndex)
        return newIndex
    }
    fun addInterval() {
        val newSegment = getInterval(intervals.lastIndex)?.deepCopy() ?: CustomWorkoutIntervalEditable.fromInputs(workoutInputs)
        intervals.add(newSegment)
        selectInterval(intervals.lastIndex)
    }
    fun canDeleteInterval() = intervals.size > 1 // do we block last interval from being deleted?
    fun deleteInterval() {
        if (canDeleteInterval()) {
            val currentIndex = selectedInterval.intValue
            val previousIndex = maxOf(0, currentIndex - 1)
            intervals.removeAt(currentIndex)
            selectInterval(previousIndex)
        }
    }
    fun refreshInterval() {
        getInterval()?.setToDefaultValues(workoutInputs)
    }
    fun getInterval(index: Int = selectedInterval.intValue) = if (index >= 0 && index < intervals.size) intervals[index] else null
    fun getIntervalsFor(input: WorkoutInput): List<Float> {
        return intervals.map {
            it.data[input.identifier]?.value ?: input.defaultValue
        }
    }
}

/** Tracks data for an editable interval of a Custom Workout */
class CustomWorkoutIntervalEditable (
    val data: Map<String, MutableState<Float>>,
) {
    fun deepCopy(): CustomWorkoutIntervalEditable {
        return CustomWorkoutIntervalEditable(
            data.keys.associateWith { mutableFloatStateOf(data[it]!!.value) }
        )
    }
    fun setToDefaultValues(workoutInputs: List<WorkoutInput>) {
        workoutInputs.forEach {
            data[it.identifier]?.value = it.defaultValue
        }
    }
    companion object {
        fun fromInputs(workoutInputs: List<WorkoutInput>) = CustomWorkoutIntervalEditable(workoutInputs.associate {
            Pair(it.identifier, mutableFloatStateOf(it.defaultValue))
        })
    }
}
//#endregion CUSTOM WORKOUT

