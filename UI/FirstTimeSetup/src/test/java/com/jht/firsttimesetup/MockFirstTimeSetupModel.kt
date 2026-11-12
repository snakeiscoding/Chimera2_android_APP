package com.jht.firsttimesetup

import com.jht.data.ChannelListPreview
import com.jht.data.OperationState
import com.jht.interfaces.DataListener
import com.jht.mock.interfaces.MockDataVariable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf


class MockFirstTimeSetupModel : FirstTimeSetupModel() {

    var loadSuccessResponse: Boolean = true

    var searchFacilityResponse : FacilitySearchResponse? = null

    var exportSuccess = false
    var importSuccess = false

    var canCalibrate = false

    var _canUpdate: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val canUpdate: StateFlow<Boolean?> = _canUpdate
    fun setCanUpdate(newVal: Boolean?){
        _canUpdate.value = newVal
    }

    var _versionName: MutableStateFlow<String?> = MutableStateFlow(null)
    val versionName: StateFlow<String?> = _versionName
    fun setVersionName(versionName: String?){
        _versionName.value = versionName
    }

    override fun setTvSourceType(sourceIndex: Int) {}

    override fun getTvSourceList(): List<String> {
        return emptyList()
    }

    override fun getFrameType(frameSerialNumber: String): String {
        return ""
    }

    override fun getFrameConfig(frameSerialNumber: String): String {
        return ""
    }

    override fun getFrameMarketingName(frameSerialNumber: String): String {
        return ""
    }

    override fun getConsoleMarketingName(): String {
        return ""
    }

    override fun getSupportedFrames(): List<String> {
        return emptyList()
    }

    override fun getFramePrefixes(frameString: String): List<String> {
        return emptyList()
    }

    override fun isSerialNumberTupleValid(console: String, frame: String): Boolean {
        return true
    }

    override fun updateConsoleSerialNumber(serialNumber: String) {
    }

    override fun updateFrameSerialNumber(serialNumber: String) {
    }

    override fun updateBridgeSerialNumber(serialNumber: String) {
    }

    override fun setFrame(frameSerialNumber: String) {
    }

    override suspend fun searchForFacility(facilityId: String): Pair<FacilitySearchResponse?, String?> {
        if (loadSuccessResponse) {
            return Pair(
                searchFacilityResponse,
                null
            )
        } else {
            return Pair(
                null,
                "Error"
            )
        }
    }

    override fun exportWirelessSettings(): Boolean {
        return exportSuccess
    }

    override fun importWirelessSettings(): Boolean {
        return importSuccess
    }

    override fun setSpeedTestUrl(url: String) {}

    override fun sendBandWidthTestEvent() {}

    override fun launchEngineeringScreen() {}

    override fun exitFtsu() {}

    override fun setTimeZone(timeZone: String, position: Int) {}

    override fun getTimeZoneList(): List<String> {
        return emptyList()
    }

    override fun getIanaTimeZoneList(): List<String> {
        return emptyList()
    }

    override fun getCurrentTimezone(): String {
        return ""
    }

    var timeZoneSet = true

    override fun isTimeZoneSet(): Boolean {
        return timeZoneSet
    }

    override fun getCurrentTimeZoneIndex(): Int {
        return 1
    }

    override fun getDateFormats(): List<String> {
        return emptyList()
    }

    override fun getCurrentDateFormatIndex(): Int {
        return 0
    }

    override fun setDateFormat(index: Int) {
    }

    override fun getUiExperience(): String {
        return ""
    }

    override fun setUiExperience(uiExperience: String) {
    }

    override fun setIsMetricFormat(isMetric: Boolean) {
    }

    override fun getIsMetricFormat(): Boolean {
        return false
    }

    override fun getUnitFormatTypes(): List<String> {
        return emptyList()
    }

    override fun getIs24HourTimeFormatType(): Boolean {
        return true
    }

    override fun setTimeFormatType(is24Hour: Boolean) {
    }

    override fun getSupportedTimeFormats(): List<String> {
       return emptyList()
    }

    override fun isOnyx(): Boolean {
        return true
    }

    override fun setLedColor(color: Int) {
    }

    override fun getLedColor(): Int {
        return 0
    }

    override fun canCalibrate(): Boolean {
        return canCalibrate
    }

    override fun startCalibration() {

    }

    override val isCalibrating: DataListener<Int>
        get() = MockDataVariable(0)

    override fun isTreadmill(): Boolean {
        return true
    }

    override fun canUpdateOtaFlow(): StateFlow<Boolean?> {
        return canUpdate
    }

    override fun startBackgroundDownload() {

    }

    override fun installUpdate() {

    }

    override fun softwareUpdateVersion(): StateFlow<String?> {
        return versionName
    }

    override fun checkForOtaUpdates() {

    }

    override fun exitFTSU() {

    }

    override fun tvChannelUp() {}

    override fun tvChannelDown() {}

    override fun tvVolumeUp() {}

    override fun tvVolumeDown() {}

    override fun tvOkClick() {}

    override fun getTvChannelList(includeHiddenChannels: Boolean): List<Channel> {
        return emptyList()
    }

    override fun getDefaultChannel(): String {
        return "ABC"
    }

    override fun tvMenu() {}

    override fun tvBackButton() {}

    override fun tvClosedCaption() {}

    override fun tvUpArrow() {
        
    }

    override fun tvDownArrow() {
        
    }

    override fun tvRightArrow() {
        
    }

    override fun tvLeftArrow() {
        
    }

    override fun tvZeroClick() {
        
    }

    override fun tvOneClick() {
        
    }

    override fun tvTwoClick() {
        
    }

    override fun tvThreeClick() {
        
    }

    override fun tvFourClick() {
        
    }

    override fun tvFiveClick() {
        
    }

    override fun tvSixClick() {
        
    }

    override fun tvSevenClick() {
        
    }

    override fun tvEightClick() {
        
    }

    override fun tvNineClick() {
        
    }

    override fun tvCheckClick() {
        
    }

    override fun tvDashCLick() {
        
    }

    override fun onChannelClicked(channelUuid: String) {
    }

    override fun setChannelCustomName(name: String, channelUuid: String) {
    }

    override fun toggleHideChannel(channelUuid: String, hide: Boolean) {
    }

    override fun setDefaultChannel(channelUuid: String) {
    }


    override fun closeTuner() {
        
    }

    override fun hasSupportedTuner(): Boolean {
        return true
    }

    override fun setClubId(id: Int) {

    }

    override fun getClubId(): Int {
        return 1963
    }

    override fun previewChannelsFromCore(facilityId: Int?): Flow<OperationState<ChannelListPreview>> {
        return flowOf(OperationState.Success(ChannelListPreview(emptyList(), null)))
    }

    override fun importChannelsFromCore(facilityId: Int?): Flow<OperationState<Unit>> {
        return flowOf(OperationState.Success(Unit))
    }

    override fun importChannelsFromUsb(
        onFinished: () -> Unit,
        onError: () -> Unit,
        tunerMode: Int
    ) {

    }


    override suspend fun exportProfile(profile: ImportFacilityProfile): Boolean {
        return false
    }

    override suspend fun importProfile(): Pair<Boolean, ImportFacilityProfile?> {
        TODO("Not yet implemented")
    }

    override fun getUiExperiences(): Map<String, String> {
        return mapOf()
    }

    override fun restartFirstTimeSetup() {  }

    override fun close() { }
}