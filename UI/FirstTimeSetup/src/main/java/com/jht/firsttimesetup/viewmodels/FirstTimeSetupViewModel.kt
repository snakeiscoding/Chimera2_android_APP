package com.jht.firsttimesetup.viewmodels

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.jht.annotations.IODispatcher
import com.jht.annotations.MainDispatcher
import com.jht.firsttimesetup.FacilitySearchModelMapper.mapImportFacilityProfileToFacility
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.ui.Segment
import com.jht.firsttimesetup.ui.SegmentState
import com.jht.firsttimesetup.ui.SetupState
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * This viewmodel is meant to hold and control state regarding navigation and
 * user saved settings
 */
@HiltViewModel
class FirstTimeSetupViewModel @Inject constructor(
    private val systemModel: SystemModel,
    private val ftsuModel: FirstTimeSetupModel,
    private val uiModel: UIBaseModel,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ThemedViewModel() {

    private val _showMachineInfo = kotlinx.coroutines.channels.Channel<Boolean>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val showMachineInfo = _showMachineInfo.receiveAsFlow()


    private fun triggerFailure() {
        _uiState.value = _uiState.value.copy(state = FirstTimeSetupState.FAILURE)
    }

    //Exception handler for lcb frame configuration job
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> triggerFailure() }

    //According to FTSU requirements this should be the default color of LED
    private val defaultLedColor = 0xFFFF6C01.toInt()

    /**
     * This job acts like a timer on start up to help detect the frame we have. Due to communication
     * between the lcb and the console not being instant we have arbitrarily chosen 5 minutes to be
     * an appropriate amount of time to wait for the communication to be established.
     */
    private var job: Job? = null
    private var timeInSeconds = 0

    private var lastOnMoveToNextSegmentTimeStamp: Long = System.currentTimeMillis()

    @VisibleForTesting
    val MILLIS_TO_WATI_FOR_NEXT_MOVE: Int = 1000
    @VisibleForTesting
    var ignoreNextButtonTimeout = false  //flag set true during tests to speed up test and not wait for the given time set by MILLIS_TO_WATI_FOR_NEXT_MOVE

    private fun setOnLastMoveTimeStamp(){
        lastOnMoveToNextSegmentTimeStamp = System.currentTimeMillis()
    }

    fun canMoveToNextSegment(): Boolean {
        val curTime = System.currentTimeMillis()

        if (curTime - lastOnMoveToNextSegmentTimeStamp > MILLIS_TO_WATI_FOR_NEXT_MOVE || ignoreNextButtonTimeout) {
            setOnLastMoveTimeStamp()
            return true
        } else {
            return false
        }
    }

    /**
     * Test function for setting initial setup list
     */
    @VisibleForTesting
    internal fun setupGraphForTesting(
        graph: MutableList<Segment>
    ) {
        _uiState.value = _uiState.value.copy(
            setupList = graph,
            currentSegment = getFirstSegment(setupList.first())
        )
    }

    /**
     * Returns first segment when [setupList] is initialized
     */
    private fun getFirstSegment(startSegment: Segment): Segment {
        var currentSegment: Segment = startSegment
        while (currentSegment.subsegments.isNotEmpty()) {
            currentSegment = currentSegment.subsegments.first()
        }
        currentSegment.state = SegmentState.SELECTED
        return currentSegment
    }

    private val MACHINE_CALIBRATION_INDEX = 4
    @VisibleForTesting
    internal var setupList =
        mutableListOf(             //This list controls the number of pages and the order of all the pages for FTSU
            Segment(
                type = SetupState.LANGUAGE,
                state = SegmentState.SELECTED
            ), //first one has to be selected by default
            Segment(type = SetupState.NETWORK),
            Segment(type = SetupState.FACILITY_ID),
            Segment(type = SetupState.SETTINGS)
                .apply {
                    this.subsegments = listOf(
                        Segment(type = SetupState.GENERAL_SETTINGS, parent = this),
                        Segment(type = SetupState.TV_SETUP, parent = this)
                            .also { segment ->
                                segment.subsegments = listOf(
                                    Segment(type = SetupState.TV_SETUP_TUNER, parent = segment),
                                    Segment(
                                        type = SetupState.TV_SETUP_IMPORT_CHANNELS,
                                        parent = segment
                                    ),
                                    Segment(
                                        type = SetupState.TV_SETUP_EDIT_CHANNELS,
                                        parent = segment
                                    )
                                )
                            },
                    )
                },
            Segment(type = SetupState.SUMMARY)
        )

    init {
        ftsuModel.setLedColor(defaultLedColor)
        frameSetup()
        consoleSetup()

        var initClubId = ftsuModel.DEFAULT_CLUB_ID //setting club id on init so that it will be set even if FTSU is skipped through ftsu engineering mode (code 3002)
        if (isDemoMode()){
            initClubId = ftsuModel.getClubId()  //if in Demo mode the ID of the console will be saved so when you leave ftsu it wont change to -1
        }
        ftsuModel.setClubId(initClubId)
    }

    fun completeFrameSetup() {
        setupList.removeAll { it.type == SetupState.MACHINE_CALIBRATION } // remove from list if frame somehow changed
        if (ftsuModel.canCalibrate()) {
            setupList.add(MACHINE_CALIBRATION_INDEX, Segment(type = SetupState.MACHINE_CALIBRATION))
        }

        _uiState.value = _uiState.value.copy(
            setupList = setupList.toList(),
            state = FirstTimeSetupState.SUCCESS
        )
    }

    fun consoleSetup() {
        //FTSU doesn't support the older TV Tuners so it is removed from the flow to skip it and setup later in MES (How it was done before this FTSU was made)
        if (!ftsuModel.hasSupportedTuner()) {
            setupList.forEach { it.subsegments = it.subsegments.filterNot { sub -> sub.type == SetupState.TV_SETUP } }
        }
    }

    /**
     * Sets up a job to check on frame type for up to five seconds before erroring out
     */
    @VisibleForTesting
    fun frameSetup() {
        job = viewModelScope.launch(ioDispatcher + exceptionHandler) {
            delay(1000L.milliseconds) // wait another second to give console time to communicate with LCB

            while (isActive) {
                delay(1000L.milliseconds)
                timeInSeconds++
                // Note that there is logic that will sync the frame serial from LCB to UCB
                // If the LCB responded or 5 seconds has passed, use the current frame serial to attempt to configure the frame
                if (systemModel.lcbType != 0 || timeInSeconds > 5) {
                    //if we can identify the frame we set the frame and check if this frame has calibration capabilities
                    ftsuModel.setFrame(systemModel.serialNumberFrame) // error handler will trigger failure
                    completeFrameSetup()
                    job?.cancel() // once we have read the lcb then we can cancel this job and the timer
                }
            }
        }
    }

    private val supportedLanguages = systemModel.languageList

    private var currentProgressSegmentIndex = 0 //todo reset state for back navigation

    private var _uiState = MutableStateFlow(
        FirstTimeSetupUiState(
            setupList = setupList.toList(),
            supportedLanguages = supportedLanguages,
            selectedLanguage = 0,
            currentSegment = getFirstSegment(setupList.first())
        )
    )

    var facilityProfile: Facility? = null

    /** The last facility search result seen by [onSearchedFacilityChanged]. */
    private var lastSearchedFacility: Facility? = null

    /**
     * Call whenever the facility search result changes. A new result makes [facilityProfile]
     * (set by an earlier import) stale, so it is cleared and the screens show the new search.
     * Repeating the same result, e.g. on recomposition, keeps an import made after that search.
     */
    fun onSearchedFacilityChanged(facility: Facility?) {
        if (facility == null || facility == lastSearchedFacility) return
        lastSearchedFacility = facility
        facilityProfile = null
    }

    internal val uiState = _uiState.asStateFlow()


    fun onMesCodeEnter(code: Float): Boolean {
        when (code) {
            3002f -> {
                toggleEngineeringMode()
            }
            30022f -> {
                onResetFTSU()
            }
            30021f -> {
                onSkipFtsu()
            }
            30023f -> {
                viewModelScope.launch(mainDispatcher){
                    _showMachineInfo.send(true)
                }
            }
            else -> {
                return systemModel.processKeypadCode(code.toInt())
            }
        }
        return true
    }


    /**
     * Imports profile and navigates user to summary on successful import
     */
    fun importFacilityProfile() {
        viewModelScope.launch(ioDispatcher) {
            val result = ftsuModel.importProfile()
            if (result.first) { //success
                val importProfile = result.second
                importProfile?.let {
                    val facility = mapImportFacilityProfileToFacility(it)
                    facilityProfile = facility
                    _uiState.value = _uiState.value.copy(
                        importProfile = facility,
                        showImportProfileDialog = true
                    )
                } ?: run {
                    _uiState.value =
                        _uiState.value.copy(showImportProfileDialog = true, importProfile = null)
                    setShouldImportProfileSettings(false)
                }

            } else {
                _uiState.value =
                    _uiState.value.copy(showImportProfileDialog = true, importProfile = null)
                setShouldImportProfileSettings(false)
            }
        }
    }

    fun setShouldShowImportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showImportProfileDialog = show)
    }

    fun onConfirmImportProfile() {
        val facility = _uiState.value.importProfile
        setShouldImportProfileSettings(true)
        facilityProfile = facility
    }


    fun setShouldImportProfileSettings(shouldImport: Boolean) {
        _uiState.value = _uiState.value.copy(shouldImportProfileSettings = shouldImport)
    }

    override fun stop() {
        job?.cancel()
        systemModel.close()
    }

    /**
     * Returns next segment in the graph to show
     * @param [currentSegment] current segment we wish to start from
     */
    @VisibleForTesting
    internal fun navigateTree(
        currentSegment: Segment? = _uiState.value.currentSegment
    ): Segment? {
        if (currentSegment == null) return null
        if (currentSegment.parent == null && (currentSegment.subsegments.isEmpty() || currentSegment.currentSubsegmentIndex >= currentSegment.subsegments.size)) {
            //navigate to next top level segment
            val nextIndex = _uiState.value.segmentIndex + 1
            val nextSegment = setupList.getOrNull(nextIndex)
            val subIndex = nextSegment?.currentSubsegmentIndex ?: 0
            val childSegment = nextSegment?.subsegments?.getOrNull(subIndex)
            _uiState.value = _uiState.value.copy(
                //update segment index for top level
                segmentIndex = nextIndex
            )
            if (childSegment == null) return nextSegment
            nextSegment.state = SegmentState.SELECTED
            return navigateTree(childSegment)
        } else {
            //leaf node
            if (currentSegment.subsegments.isEmpty() && currentSegment.state == SegmentState.UNSELECTED) {
                currentSegment.state = SegmentState.SELECTED
                return currentSegment
            } else {
                if (currentSegment.subsegments.isNotEmpty() && currentSegment.currentSubsegmentIndex < currentSegment.subsegments.size) {
                    //we have a child node go deeper
                    val index = currentSegment.currentSubsegmentIndex
                    return navigateTree(currentSegment.subsegments[index])
                }

                val parent = currentSegment.parent
                val nextIndex = (parent?.currentSubsegmentIndex ?: 0) + 1
                if (parent != null) {
                    parent.currentSubsegmentIndex = parent.currentSubsegmentIndex + 1
                }

                val nextSegment = parent?.subsegments?.getOrNull(nextIndex)
                return if (nextSegment != null) {
                    //Move horizontal in tree
                    navigateTree(nextSegment)
                } else {
                    navigateTree(parent)
                }
            }
        }
    }

    /**
     * Navigates through flow and is intended to navigate forward
     *
     */
    fun moveToNextSegment() {
        if (canMoveToNextSegment()){
            val currentIndex = _uiState.value.segmentIndex
            val nextIndex = currentIndex + 1
            val nextSegment = setupList.getOrNull(nextIndex)
            nextSegment?.let {
                resetSegment(it)
            }
            val current = _uiState.value.currentSegment
            val next = navigateTree(current)

            next?.let {
                next.state = SegmentState.SELECTED

                _uiState.value = _uiState.value.copy(
                    currentSegment = next,
                    setupList = setupList
                )
            }

            if (currentIndex < setupList.size) {
                setupList[currentIndex].state = SegmentState.COMPLETED
            }
        }
    }


    /**
     * This function is used to get out of flow of subsegments.
     * ex. the button in the TV Settings screens that brings you to Machine Calibration/ Summary
     * based on the Frame
     */
    fun moveToNextAuntNode() {
        if (canMoveToNextSegment()){
            val currentIndex = _uiState.value.segmentIndex
            val currentSegment = uiState.value.currentSegment
            val currentParent = currentSegment.parent

            currentParent?.let {
                completeSegment(currentParent)
            } ?: run {
                completeSegment(currentSegment)
            }

            val nextSegment = navigateTree(currentSegment)

            nextSegment?.let {
                nextSegment.state = SegmentState.SELECTED
                _uiState.value.setupList

                _uiState.value = _uiState.value.copy(
                    currentSegment = nextSegment,
                    setupList = setupList
                )
            }

            setupList[currentIndex].state = SegmentState.COMPLETED

            _uiState.value = _uiState.value.copy(
                setupList = setupList
            )
        }
    }

    internal fun completeSegment(segment: Segment) {
        segment.state = SegmentState.COMPLETED

        if (segment.subsegments.isNotEmpty()) {
            segment.subsegments.forEach { subsegment ->
                completeSegment(subsegment)
            }
        }
    }

    /**
     * Clearing subsegment indices for a segment's bloodline
     * @param [currentSegment] will reset subsegment indices
     * for a segment and all of its subchildren
     */
    @VisibleForTesting
    internal fun resetSegment(
        currentSegment: Segment? = _uiState.value.currentSegment,
    ) {
        if (currentSegment != null) {
            currentSegment.state = SegmentState.UNSELECTED
            currentSegment.currentSubsegmentIndex = 0
            currentSegment.subsegments.forEach {
                resetSegment(it)
            }

        }
    }


    /**
     * will increment subsegment for current segment if it exists
     * If at the end of subsegments it circles back to beginning
     */
    fun incrementSubsegment() {
        val parentSegment = setupList[currentProgressSegmentIndex]
        val currentChildIndex = parentSegment.currentSubsegmentIndex
        val updatedSubIndex = if (currentChildIndex + 1 < parentSegment.subsegments.size) {
            currentChildIndex + 1
        } else {
            0
        }
        val updatedSubsegments =
            updateSubsegments(
                subsegments = parentSegment.subsegments,
                nextIndex = updatedSubIndex
            )
        val updatedParent = parentSegment.copy(
            subsegments = updatedSubsegments,
            currentSubsegmentIndex = updatedSubIndex
        )
        setupList[currentProgressSegmentIndex] = updatedParent
        _uiState.value = _uiState.value.copy(
            currentSubSegment = parentSegment.subsegments.getOrNull(updatedSubIndex),
            setupList = setupList.toList()
        )

    }

    /**
     * will decrement subsegment for current parent segment if it exists
     * If at the beginning it circles back to the end
     */
    fun decrementSubsegment() {
        val parentSegment = setupList[currentProgressSegmentIndex]
        val currentChildIndex = parentSegment.currentSubsegmentIndex
        val updatedSubIndex = if (currentChildIndex - 1 < 0) {
            parentSegment.subsegments.size - 1
        } else {
            currentChildIndex - 1
        }
        val updatedSubsegments = updateSubsegments(parentSegment.subsegments, updatedSubIndex)
        val updatedParent = parentSegment.copy(
            subsegments = updatedSubsegments,
            currentSubsegmentIndex = updatedSubIndex
        )
        setupList[currentProgressSegmentIndex] = updatedParent
        _uiState.value = _uiState.value.copy(
            currentSubSegment = parentSegment.subsegments.getOrNull(updatedSubIndex),
            setupList = setupList.toList()
        )
    }

    /**
     * Returns a list of updated segments based on the [nextIndex]
     * All subsegments before [nextIndex] will be marked as completed
     * and all subsegments after will be marked as unselected
     * @param [nextIndex] -> index we wish to navigate to
     * @param [subsegments] -> list of subsegments we wish to update
     */
    private fun updateSubsegments(subsegments: List<Segment>, nextIndex: Int): List<Segment> {
        val updatedList = arrayListOf<Segment>()
        subsegments.forEachIndexed { index, segment ->
            if (index < nextIndex) {
                updatedList.add(segment.copy(state = SegmentState.COMPLETED))
            } else if (index == nextIndex) {
                updatedList.add(segment.copy(state = SegmentState.SELECTED))
            } else {
                updatedList.add(segment.copy(state = SegmentState.UNSELECTED))
            }
        }
        return updatedList.toList()

    }

    /**
     * Navigates to the specified segment
     * @param [type] -> segment you wish to navigate to
     * @param [markPreviousSegmentsAsComplete] -> flag that determines whether we want to mark all segments prior
     * to current segment as completed
     */
    internal fun navigateToSegment(
        type: SetupState,
        markPreviousSegmentsAsComplete: Boolean = true
    ) {
        val segmentIndex = setupList.indexOfFirst { it.type == type }
        if (segmentIndex >= 0) {
            onSegmentClicked(segmentIndex, markPreviousSegmentsAsComplete)
        }
    }

    /**
     * Back navigation that wraps to end if at first segment
     * Note: this is an engineering and testing functionality not available to users
     */
    fun onBackClicked() {
        setupList[currentProgressSegmentIndex] =
            setupList[currentProgressSegmentIndex].copy(state = SegmentState.UNSELECTED)

        currentProgressSegmentIndex =
            if (currentProgressSegmentIndex - 1 < 0) {
                setupList.size - 1
            } else {
                currentProgressSegmentIndex - 1
            }

        setupList[currentProgressSegmentIndex] =
            setupList[currentProgressSegmentIndex].copy(state = SegmentState.SELECTED)

        _uiState.value = _uiState.value.copy(
            setupList = setupList.toList(),
            currentSegment = setupList[currentProgressSegmentIndex],
            segmentIndex = currentProgressSegmentIndex,
            currentSubSegment = setupList[currentProgressSegmentIndex].subsegments.first()
        )
    }

    fun onSegmentClicked(index: Int, markPreviousSegmentsAsComplete: Boolean = true) {
        setupList.forEachIndexed { i, segment ->
            resetSegment(segment)
            if (i < index && markPreviousSegmentsAsComplete) {
                segment.state = SegmentState.COMPLETED
            }

        }
        val segment = setupList.getOrNull(index)
        segment?.let {
            segment.state = SegmentState.SELECTED
            if (segment.subsegments.isNotEmpty()) {
                val subsegment = navigateTree(segment)
                subsegment?.let { sub ->
                    _uiState.value = _uiState.value.copy(
                        currentSegment = sub,
                        segmentIndex = index
                    )
                }

            } else {
                _uiState.value = _uiState.value.copy(
                    currentSegment = segment,
                    segmentIndex = index
                )
            }
        }
    }

    fun onResetFTSU(){
        ftsuModel.restartFirstTimeSetup()
    }

    fun toggleEngineeringMode() {
        _uiState.value =
            _uiState.value.copy(isEngineeringMode = _uiState.value.isEngineeringMode.not())
    }

    fun isEngineeringMode(): Boolean {
        return _uiState.value.isEngineeringMode
    }

    fun isFrameCalibratable(): Boolean {
        return ftsuModel.canCalibrate()
    }

    fun isTunerSupported(): Boolean {
        return ftsuModel.hasSupportedTuner()
    }

    fun onSkipFtsu() {
        ftsuModel.exitFtsu()
    }

    /**
     * Sets selected language based on @param [language] index
     * updates language via [systemModel]
     */
    fun setSelectedLanguage(language: Int) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        systemModel.setLanguage(language, true)
    }

    fun isDemoMode(): Boolean {
        return uiModel.isDemoMode.value
    }

    fun getConsoleSerialNumber(): String {
        return systemModel.serialNumberConsole
    }

    fun getFrameSerialNumber(): String {
        return systemModel.serialNumberFrame
    }

    fun getFrameTypeFromSerial() : String {
        return ftsuModel.getFrameConfig(systemModel.serialNumberFrame)
    }

    fun getFramePrefixes(frameString: String): List<String> {
        return ftsuModel.getFramePrefixes(frameString)
    }

    fun getSupportedFrames(): List<String> {
        return ftsuModel.getSupportedFrames()
    }

    fun updateConsoleSerial(serialNumber: String) {
        ftsuModel.updateConsoleSerialNumber(serialNumber)
    }

    fun getLcbType() : Int {
        return systemModel.lcbType
    }
}

/**
 * UI state model for FTSU
 * [setupList] -> List of segments we want to show user for first time setup
 * [segmentIndex] -> keeps track of current segment the user is viewing
 * [currentSegment] -> Keeps track of current segment the viewing
 * [supportedLanguages] -> list of languages we support
 * [selectedLanguage] -> index of language user has selected
 * [currentSubSegment] -> Current subsegment user if viewing if applicable (Not all segments have subsegments).
 * [isEngineeringMode] -> Flag that toggles navigation and testing functionality
 */
internal data class FirstTimeSetupUiState(
    val setupList: List<Segment> = emptyList(),
    val segmentIndex: Int = 0,
    val currentSegment: Segment = setupList.first(),
    val supportedLanguages: List<String> = emptyList(),
    val selectedLanguage: Int = 0,
    val currentSubSegment: Segment? = null, // keeps track of the current subsegment to show in UI
    val isEngineeringMode: Boolean = false,
    val state: FirstTimeSetupState = FirstTimeSetupState.LOADING,
    val shouldImportProfileSettings: Boolean = false,
    val importProfile: Facility? = null,
    val showImportProfileDialog: Boolean = false,
)

/**
 * Used to determine flow for FTSU
 * Sad path being some form of hardware issue
 */
enum class FirstTimeSetupState {
    SUCCESS, FAILURE, LOADING
}

