package com.jht.interfaces.models

import android.content.Context
import android.content.Intent
import com.jht.chimera.BuildConfig
import com.jht.chimera.R
import com.jht.chimera.lib.common.SystemUtils
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.ConsoleToFramePrefixMap
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum
import com.jht.chimera.lib.common.dapi.DapiGetSoftware
import com.jht.chimera.lib.common.dapi.DapiGetSoftware.DapiGetLatestSoftwareResponse
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.ProjectOne
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datetime.DateTimeUtil
import com.jht.chimera.lib.common.i18n.DateFormat
import com.jht.chimera.lib.common.i18n.TimeZoneStrings
import com.jht.chimera.lib.common.usb.StorageUtils
import com.jht.chimera.lib.language.StringLookup
import com.jht.chimera.lib.network.ethernet.NetworkSettingsImportExport
import com.jht.chimera.lib.tv.tuner.IChimeraTuner
import com.jht.chimera.lib.tv.tuner.TunerFactory
import com.jht.chimera.lib.tv.tuner.US4Tuner
import com.jht.chimera.lib.tv.tuner.US6Tuner
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500Tuner
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ETuner
import com.jht.chimera.lib.update.UpdateValues
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferError
import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferEvent
import com.jht.chimera.service.systemsettings.tv_channel_manager.accessors.ChannelListAccessorFactory
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.core.CoreChannelListEndpoint
import com.jht.chimera.ui.machinesettings.pages.Calibration
import com.jht.chimera.ui.machinesettings.pages.Update
import com.jht.data.ChannelListPreview
import com.jht.data.OperationState
import com.jht.datamatrixlib.DataMatrix
import com.jht.firsttimesetup.Channel
import com.jht.firsttimesetup.FacilitySearchResponse
import com.jht.firsttimesetup.FirstTimeSetupApi
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.ImportFacilityProfile
import com.jht.firsttimesetup.ui.ClassMapper.mapImportProfileToFacilitySearchResponse
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataListener
import com.jht.interfaces.MatrixIntListener
import com.jht.mastboard.data.LEDItem
import com.jht.mastboard.data.LedZone
import com.jht.serialport.peripheral.tuner.ITuner
import com.jht.serialport.peripheral.tuner.channel.IChannel
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel
import com.jht.updatemanager.IUpdateCallback
import com.jht.updatemanager.UpdateManagerClient
import com.jht.updatemanager.UpdateManagerTypes
import com.jhtp.dapiservice.DapiResult
import dapi.IOtaUpdateInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import updatepackages.UpdatePackageList
import updatepackages.UpdateXmlGenerator
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlin.coroutines.cancellation.CancellationException


class MatrixFirstTimeSetupModel(
    private val firstTimeSetupApi: FirstTimeSetupApi,
    private val context: Context,
    private val applicationScope: CoroutineScope
) : FirstTimeSetupModel() {


    /**
     * Thoughts regarding channels related functionality are mentioned here for the future me or you.
     * Since channels will eventually migrate to using a UUID to identify them migrating them in FTSU
     * will address current issues and future migrations.
     * Plan to update channels
     * use a map to map uuid ->  channel
     * this will allow us to avoid duplicates and pitfalls of
     * using the channels index as its identifier in UI
     *
     * pros:
     * Consistent Ui (all channels will be uniquely identified)
     * Operations to channels will be reflected as intended
     *
     * cons:
     * management of channels can become burdensome
     * updating channels can become troublesome
     */
    /**
     * Hashmap mapping [java.util.UUID] as string to a [IChannel] in string format
     */
    private var channelMap: LinkedHashMap<String, IChannel> = LinkedHashMap()
    /**
     * Caching the facility search as we search for it will allow us to
     * easily export this profile
     */
    private var cachedFacilitySearch: FacilitySearchResponse? = null

    //used to parse object from and to json
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true   // tolerate extra fields
        isLenient = true           // tolerate relaxed JSON
        encodeDefaults = false      // use defaults if missing
        allowTrailingComma = true
    }
    private val MAX_VOLUME = 30
    private val MIN_VOLUME = 0

    /** TODO: Dumbledore TV Source Auto-Detection Integration
        We need to adjust TV Auto-Detection for Dumbledore (FTSU). Details are as follows:
            - [com.jht.chimera.service.hal.TvTunerManager] contains logic that listens for DM events
              using key [HALKeys.EVENT_MACHINE_SETUP_COMPLETE] and starts TV auto-detection upon receiving it.
            - Dumbledore needs to send this as soon as possible (machine boot up)
            - Dumbledore needs to handle edge cases where we might get to FTSU TV functionalities before
              the TV source auto-detection process has time to complete (and thus before TV is ready for use)
    **/
    private val typeString = DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)
    private val type = SettingKeys.TVSource.fromName(typeString)
    private var tvTuner = TunerFactory.getTuner(context, type)

    //list that holds update packages
    private var updatePackageList: UpdatePackageList? = null
    private val UPDATE_RSCU_ID: String = "update_rscu_id" //NON-NLS

    private val AUTO_UPDATE_ID: String = "auto_update_manager_id" //NON-NLS

    private val APPS_UPDATE_ID = "63a74e6c-8380-4416-963a-2ee9d2357cc6"

    private val SYSTEM_RSCU_PATH = "/sdcard/Download"

    private val tvSources: List<SettingKeys.TVSource> = SettingKeys.TVSource.getList().toList()

    private val downloadChannelState = kotlinx.coroutines.channels.Channel<Boolean?>(kotlinx.coroutines.channels.Channel.BUFFERED)

    private val dataMatrix = DataMatrix()

    /**
     * Flow that returns whether we can update via over the air
     */
    private val canUpdateOta: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /**
     * Flow that returns the name of update version if any at all
     */
    private val updateVersionName: MutableStateFlow<String?> = MutableStateFlow(null)


    /**
     * Interface for updating over the air
     */
    private val otaUpdateInterface: IOtaUpdateInterface = object : IOtaUpdateInterface {
        override fun getSystemSoftwareType(): String? {
            return UpdateValues.getOtaSwType()
        }

        override fun getLatestSoftware(softwareType: String) {
            update(softwareType)
        }

        override fun getOtaUpdateXmlFileDir(): File {
            return File(Update.SYSTEM_OTA_PATH)
        }
    }

    /**
     * Updates [updatePackageList] and [updateVersionName]
     * update file
     */
    private fun loadManifest(manifest: File) {
        updatePackageList = null
        val packages = UpdateXmlGenerator.readUpdateFile(manifest)
        if (packages != null && packages.equipmentType == BuildConfig.UPDATE_EQUIPMENT) {
            updatePackageList = packages
        } else {
            return
        }

        updatePackageList?.let {
            updateVersionName.value = it.getUpdatePackageWithId(APPS_UPDATE_ID).versionString
        }
    }


    private fun update(softwareType: String) {
        DapiGetSoftware.getLatestSoftware(
            softwareType
        ) { response: DapiGetLatestSoftwareResponse ->
            val artifactResult: Int
            val result = DapiResult.fromInt(response.dapiResponse)
            artifactResult = when (result) {
                DapiResult.Success -> UpdateManagerClient.DAPI_RESPONSE_SUCCESS
                DapiResult.NoSoftwareFound -> UpdateManagerClient.DAPI_RESPONSE_NO_SOFTWARE_FOUND

                DapiResult.GeneralError -> UpdateManagerClient.DAPI_RESPONSE_FAILURE

                else -> UpdateManagerClient.DAPI_RESPONSE_FAILURE
            }
            otaUpdateManager.getLatestSoftwareVersionResponse(
                artifactResult, response.softwareType,
                response.version, response.url, response.md5
            )
        }
    }


    /**
     * Manager for detecting ota updates
     */
    private val otaUpdateManager = UpdateManagerClient(context, object : IUpdateCallback {
        override fun applyUpdatesCallback(
            returnCode: Int,
            id: String?,
            intent: Intent?
        ) {
        }

        override fun checkUpdateCallback(
            returnCode: Int,
            id: String?,
            intent: Intent?
        ) {

            if (id == UPDATE_RSCU_ID) {
                when (returnCode) {
                    UpdateManagerTypes.CHECK_UPDATE_NEW_UPDATE,
                    UpdateManagerTypes.CHECK_UPDATE_DOWNLOADING_UPDATE,
                    UpdateManagerTypes.CHECK_UPDATE_READY_TO_INSTALL -> {
                        canUpdateOta.value = true
                        val manifest = File("$SYSTEM_RSCU_PATH/update.xml")
                        if (manifest.exists()) {
                            loadManifest(manifest)
                        }
                    }

                    else -> {
                        canUpdateOta.value = false
                    }
                }
            }
        }

        override fun downloadedUpdateFilesCallback(
            returnCode: Int,
            id: String?,
            intent: Intent?
        ) {
        }

        override fun checkStatusCallback(returnCode: Int, id: String?) {}

    })

    init {
        otaUpdateManager.initOtaUpdates(otaUpdateInterface)
        createChannelsMap()
        dataMatrix.createParcelableAdapter<ChannelListTransferEvent?>(
            ImportExportKeys.EVENT_FINISHED_TV_CHANNEL_TRANSFER_PARCELABLE,
            ChannelListTransferEvent::class.java,
            { event: ChannelListTransferEvent? ->
                applicationScope.launch {
                    onChannelListTransferFinished(
                        event
                    )
                }
            },
            false
        )
    }

    private suspend fun onChannelListTransferFinished(event: ChannelListTransferEvent?) {
        val isSuccess = event is ChannelListTransferEvent.TransferSuccessful
        downloadChannelState.send(isSuccess)
    }

    //todo will use to check frame type
    private val frameConfig = FrameConfigManager.getInstance()
    private val manager: CallbackManager = CallbackManager()

    override val isCalibrating: DataListener<Int> by lazy {
        MatrixIntListener(manager, HALKeys.DATA_CALIBRATION_STATUS_INT)
    }

    override fun isTreadmill(): Boolean {
        return SystemUtils.isTreadmill
    }

    override fun canUpdateOtaFlow(): StateFlow<Boolean?> {
        return canUpdateOta.asStateFlow()
    }

    override fun startBackgroundDownload() {
        otaUpdateManager.startBackgroundDownload(AUTO_UPDATE_ID)
    }

    override fun installUpdate() {
        setFirstTimeSetupAsComplete()
        otaUpdateManager.applyUpdate(AUTO_UPDATE_ID)
    }

    override fun softwareUpdateVersion(): StateFlow<String?> {
        return updateVersionName.asStateFlow()
    }

    override fun checkForOtaUpdates() {
        val hasInternet = DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL)
        if (hasInternet.not()) {
            canUpdateOta.value = false
        }
        val forceDowngrade =
            DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL)
        otaUpdateManager.checkUpdateOta(UPDATE_RSCU_ID, forceDowngrade)
    }

    override fun exitFTSU() {
        setFirstTimeSetupAsComplete()
        LaunchAppUtil.launchSplashScreen()
    }

    override fun tvChannelUp() {
        tvTuner?.channelUp()
    }

    override fun tvChannelDown() {
        tvTuner?.channelDown()
    }

    override fun tvVolumeUp() {
        val volume = DataMatrix.getInt(HALKeys.DATA_SYSTEM_VOLUME_INT)
        if (volume < MAX_VOLUME) { //
            DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, volume + 1)
        }
    }

    override fun tvVolumeDown() {
        val volume = DataMatrix.getInt(HALKeys.DATA_SYSTEM_VOLUME_INT)
        if (volume > MIN_VOLUME) { //
            DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, volume - 1)
        }
    }


    override fun tvOkClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.ENTER)
    }

    override fun getTvChannelList(includeHiddenChannels: Boolean): List<Channel> {
        runCatching {
            val defaultChannelUuid = tvTuner?.getDefaultChannel() ?: ""

            val mappedChannels = channelMap.map { item ->
                TvTunerMapper.mapToChannel(item.value).copy(
                    isDefault = item.key == defaultChannelUuid,
                    uuid = item.key,
                )
            }

            return mappedChannels
        }

        return emptyList()
    }

    override fun getDefaultChannel(): String {
        runCatching {
            val defaultChannelUuid = tvTuner?.getDefaultChannel() ?: ""
            return channelMap[defaultChannelUuid]?.name() ?: ""
        }
        return ""
    }

    override fun tvMenu() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.MENU)
    }

    override fun tvBackButton() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.EXIT)
    }

    override fun tvClosedCaption() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.TOGGLE_CC)

    }

    override fun tvUpArrow() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.UP)
    }

    override fun tvDownArrow() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.DOWN)
    }

    override fun tvRightArrow() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.RIGHT)
    }

    override fun tvLeftArrow() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.LEFT)
    }

    override fun tvZeroClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.ZERO)
    }

    override fun tvOneClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.ONE)
    }

    override fun tvTwoClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.TWO)
    }

    override fun tvThreeClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.THREE)
    }

    override fun tvFourClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.FOUR)
    }

    override fun tvFiveClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.FIVE)
    }

    override fun tvSixClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.SIX)
    }

    override fun tvSevenClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.SEVEN)
    }

    override fun tvEightClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.EIGHT)
    }

    override fun tvNineClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.NINE)
    }

    override fun tvCheckClick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.SELECT)

    }

    override fun tvDashCLick() {
        tvTuner?.tuner()?.clickRemoteButton(ITuner.REMOTE_BUTTON.DASH)
    }

    override fun onChannelClicked(channelUuid: String) {
        when (tvTuner) {
            is LG6500Tuner -> {
                val channel = channelMap[channelUuid] as? EnseoChannel
                channel?.let {
                    (tvTuner as LG6500Tuner).setChannel(channel)
                }
            }

            is US4Tuner -> {
                val channel = channelMap[channelUuid] as? EnseoChannel
                channel?.let {
                    (tvTuner as US4Tuner).setChannel(channel)
                }

            }

            is US6Tuner -> {
                val channel = channelMap[channelUuid] as? SwiftChannel
                channel?.let {
                    (tvTuner as US6Tuner).setChannel(channel)
                }
            }

            is Lg750ETuner -> {
                val channel = channelMap[channelUuid] as? LgChannel
                channel?.let {
                    (tvTuner as Lg750ETuner).setChannel(channel)
                }

            }
        }
    }

    override fun setChannelCustomName(name: String, channelUuid: String) {
        val channel = channelMap[channelUuid]
        channel?.name(name)
        tvTuner?.tuner()?.saveSettings()
    }

    override fun toggleHideChannel(channelUuid: String, hide: Boolean) {
        when (tvTuner) {
            is LG6500Tuner -> {
                val channel = channelMap[channelUuid] as? EnseoChannel
                channel?.let {
                    (tvTuner as LG6500Tuner).updateChannelEnabledStatus(channel, !hide)
                }
            }

            is US4Tuner -> {
                val channel = channelMap[channelUuid] as? EnseoChannel
                channel?.let {
                    (tvTuner as US4Tuner).updateChannelEnabledStatus(channel, !hide)
                }
            }

            is US6Tuner -> {
                val channel = channelMap[channelUuid] as? SwiftChannel
                channel?.let {
                    (tvTuner as US6Tuner).updateChannelEnabledStatus(channel, !hide)
                }
            }

            is Lg750ETuner -> {
                val channel = channelMap[channelUuid] as? LgChannel
                channel?.let {
                    (tvTuner as Lg750ETuner).updateChannelEnabledStatus(channel, !hide)
                }
            }
        }
    }

    override fun setDefaultChannel(channelUuid: String) {
        // TODO: CGC-22432 Remove once UUID migration is complete — old index-based path below
         val channels = tvTuner?.tuner()?.getChannelList(true)
         val channel = channelMap[channelUuid]
         channel?.let {
             val name = it.number()
             val selectedChannel = channels?.firstOrNull { channel -> channel.number() == name }
             val channelIndex = channels?.indexOf(selectedChannel)
             channelIndex?.let { index ->
                 tvTuner.runCatching {
                     tvTuner?.setDefaultChannel(channelUuid)
                     tvTuner?.setDefaultChannel(index)
                     tvTuner?.tuner()?.saveSettings()
                 }
             }
         }
    }

    override fun closeTuner() {
        tvTuner?.destroy()
    }

    override fun hasSupportedTuner(): Boolean {
        val supportedSources = mutableListOf<SettingKeys.TVSource>()
        supportedSources.add(SettingKeys.TVSource.STB)
        supportedSources.add(SettingKeys.TVSource.IPTV)
        supportedSources.add(SettingKeys.TVSource.US9)
        supportedSources.add(SettingKeys.TVSource.KM7)
        supportedSources.add(SettingKeys.TVSource.BR3)
        supportedSources.add(SettingKeys.TVSource.JP1)
        supportedSources.add(SettingKeys.TVSource.CAB)

        supportedSources.forEach { source ->
            if (source == type) {
                return true
            }
        }

        return false
    }

    override fun setClubId(id: Int) {
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, id)
    }

    override fun getClubId (): Int {
        return DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
    }

    override fun previewChannelsFromCore(
        facilityId: Int?
    ): Flow<OperationState<ChannelListPreview>> = flow {
        emit(OperationState.Loading)

        val clubId = facilityId ?: DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
        val environment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)

        try {
            val currentSource = SettingKeys.TVSource.fromName(
                DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)
            ) ?: throw ChannelListAccessorFactory.UnsupportedTvSourceException()
            val channelListAccessor = ChannelListAccessorFactory.create(currentSource, context)
            CoreChannelListEndpoint(environment, clubId, context).use { endpoint ->
                val preview = endpoint.previewChannelList(channelListAccessor)
                emit(OperationState.Success(preview))
            }
        } catch (e: Exception) {
            // Rethrow cancellation exceptions to maintain proper flow cancellation
            if (e is CancellationException) throw e

            when (e) {
                is ChannelListAccessorFactory.UnsupportedTvSourceException,
                is CoreChannelListEndpoint.ChannelListNotFoundException -> {
                    // Fail silently for unsupported TV sources or missing channel lists
                    emit(OperationState.Success(ChannelListPreview.NONE))
                }
                // We can add better exception handling by inspecting the cause of error or improving error handling upstream. Leaving as is for demo due to time constraints.
                else -> emit(OperationState.Error(RuntimeException("An unknown exception caused the channel list preview to fail")))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun importChannelsFromCore(
        facilityId: Int?
    ): Flow<OperationState<Unit>> = flow {
        emit(OperationState.Loading)

        val clubId = facilityId ?: DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
        val environment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)

        try {
            val currentSource = SettingKeys.TVSource.fromName(
                DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)
            ) ?: throw ChannelListAccessorFactory.UnsupportedTvSourceException()
            val channelListAccessor = ChannelListAccessorFactory.create(currentSource, context)
            CoreChannelListEndpoint(environment, clubId, context).use { endpoint ->
                val result = endpoint.importChannelList(channelListAccessor)
                when {
                    result is ChannelListTransferEvent.TransferSuccessful -> {
                        tvTuner?.tuner()?.loadSettings()
                        // Important to persist UUIDs to disk so MES loads the same UUIDs across sessions
                        tvTuner?.tuner()?.saveSettings()
                        createChannelsMap()
                        emit(OperationState.Success(Unit))
                    }
                    result is ChannelListTransferEvent.TransferFailed &&
                    result.error is ChannelListTransferError.ChannelListAlreadyUpToDate -> {
                        emit(OperationState.Success(Unit))
                    }
                    else -> emit(OperationState.Error(RuntimeException("Channel list import failed")))
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            when (e) {
                is ChannelListAccessorFactory.UnsupportedTvSourceException,
                is CoreChannelListEndpoint.ChannelListNotFoundException -> {
                    // Fail silently for unsupported TV sources or missing channel lists
                    emit(OperationState.Success(Unit))
                }
                else -> emit(OperationState.Error(RuntimeException("Channel list import failed", e)))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun importChannelsFromUsb(onFinished: () -> Unit, onError: () -> Unit, tunerMode: Int) {
        val operations = tvTuner?.getSupportedOperations(tunerMode)
        val canImport =
            operations?.contains(IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB)
        if (canImport == true) {
            tvTuner?.executeOperation(
                null,
                IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB,
                StorageUtils.getUSBPath(),
                { _, isDone, error ->
                    if (isDone && error == null) {
                        onFinished()
                    } else {
                        onError()
                    }
                },
                {}
            )
        }
    }

    override suspend fun exportProfile(profile: ImportFacilityProfile): Boolean {
        val result = runCatching {
            val file = File(StorageUtils.getUSBPath(),"facilityProfile.json")
            val outputStream = FileOutputStream(file)
            outputStream.use {
                val writer = OutputStreamWriter(outputStream)
                writer.use {
                    //adding additional properties to populate UI
                    val exportProfile = profile.copy(
                        facilityId = cachedFacilitySearch?.facilityId ?: DEFAULT_CLUB_ID,
                        facilityName = cachedFacilitySearch?.name,
                        facilityAddress = cachedFacilitySearch?.address,
                        digitalProducts = cachedFacilitySearch?.digitalProducts ?: emptyList(),
                        selectedLanguageIndex = profile.selectedLanguageIndex,
                        country = cachedFacilitySearch?.country,
                        state = cachedFacilitySearch?.state
                    )
                    exportWirelessSettings() //save wireless settings if they are available
                    val json = json.encodeToString(value = exportProfile)
                    writer.write(json)
                }
            }
        }

        return result.isSuccess
    }

    override suspend fun  importProfile(): Pair<Boolean, ImportFacilityProfile?> {
        var importedProfile : ImportFacilityProfile? = null
        val result = runCatching {
            val file = File (StorageUtils.getUSBPath(),"facilityProfile.json")
            val profileString = file.readText()
            val profile = json.decodeFromString<ImportFacilityProfile>(profileString)
            importedProfile = profile
            cachedFacilitySearch = mapImportProfileToFacilitySearchResponse(profile)
        }
        return Pair( result.isSuccess, importedProfile)
    }

    override fun getUiExperiences(): Map<String, String> {
        AppIds.HEROS_JOURNEY_UUID
        return mutableMapOf(
            "Matrix" to AppIds.DEFAULT_APP_UUID,
            "Hero's Journey" to AppIds.HEROS_JOURNEY_UUID
        )
    }

    override fun restartFirstTimeSetup() {
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, false)
        LaunchAppUtil.launchSplashScreen()
    }

    private var timeZoneStrings = TimeZoneStrings(context)

    override fun setTvSourceType(sourceIndex: Int) {
        val source = tvSources.getOrNull(sourceIndex) ?: return
        DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, source.name)
        tvTuner = TunerFactory.getTuner(context, source)
        createChannelsMap()
    }

    override fun getTvSourceList(): List<String> {
        return tvSources.map { it.displayName }
    }

    override fun getFrameType(frameSerialNumber: String): String {
        val frame = FrameConfigEnum.getFrameFromSerial(frameSerialNumber, 0)
        return frame?.createFrameConfig()?.machineType?.name ?: ""
    }

    /**
     * Get the frame based only on the serial number and not LCB type
     */
    override fun getFrameConfig(frameSerialNumber: String): String {
        val frame = FrameConfigEnum.getFrameFromSerial(frameSerialNumber, 0)
        return frame?.toString() ?: ""
    }

    override fun getFrameMarketingName(frameSerialNumber: String): String {
        val frame = FrameConfigEnum.getFrameFromSerial(frameSerialNumber, 0)
        return frame?.createFrameConfig()?.let { config ->
            // is MODEL close enough to marketing name, or do we need to add a specific marketing name field to frame configs?
            context.getString(config.MODEL)
        } ?: ""
    }

    override fun getConsoleMarketingName(): String {
        val consoleType = ProductConfig.getConsoleType()
        return consoleType.marketingName
    }

    override fun getSupportedFrames(): List<String> {
        val supportedFramesList = mutableListOf<String>()
        val supportedFramesEnum = FrameConfigEnum.getSupportedFrames()
        supportedFramesEnum.forEach { supportedFramesList.add(it.toString()) }
        return supportedFramesList
    }

    override fun getFramePrefixes(frameString: String): List<String> {
        val frame = FrameConfigEnum.getFrameTypeFromString(frameString)
        return frame?.createFrameConfig()?.framePrefixes ?: emptyList()
    }

    override fun isSerialNumberTupleValid(console: String, frame: String): Boolean {
        // NOTE: CBOR Command 1011 could be used with ACTION_VALIDATE_SETTINGS_STRING but this requires internet connectivity
        // and the current logic seems wrong since it returns valid if there's no Internet and invalid if there's any error.
        // The valid tuple map is stored locally. Any update to add new console and frame support requires a new software build
        // so make sure to also update this valid tuple map when new console and frame are added.
        if (console.isBlank() || frame.isBlank()) return false
        return ConsoleToFramePrefixMap.any { (consolePrefix, framePrefixes) ->
            // Create a raw version of the Map Prefix by stripping any "C"
            val cleanConsoleMap = if (consolePrefix.startsWith("C", ignoreCase = true)) consolePrefix.substring(1) else consolePrefix
            // Check if the User Input matches either the Map string OR the raw Map string.
            val matchConsole = console.contains(consolePrefix, ignoreCase = true) || console.contains(cleanConsoleMap, ignoreCase = true)
            
            matchConsole && framePrefixes.any { framePrefix ->
                // same for the Frame
                val cleanFrameMap = if (framePrefix.startsWith("F", ignoreCase = true)) framePrefix.substring(1) else framePrefix
                frame.contains(framePrefix, ignoreCase = true) || frame.contains(cleanFrameMap, ignoreCase = true)
            }
        }
    }

    override fun updateConsoleSerialNumber(serialNumber: String) {
        DataMatrix.updateString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, serialNumber)
    }

    override fun updateFrameSerialNumber(serialNumber: String) {
        DataMatrix.updateString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, serialNumber)
    }

    override fun updateBridgeSerialNumber(serialNumber: String) {
        DataMatrix.updateString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING, serialNumber)
    }

    override fun setFrame(frameSerialNumber: String) {
        // call getFrameFromSerial with LCB type 0 to ignore the LCB type check so
        // always assume the frame serial number is the source of truth
        val frame = FrameConfigEnum.getFrameFromSerial(frameSerialNumber, 0)
        frameConfig.setFrame(frame)
    }

    /**
     * Searches for Facility @param[facilityId]
     * Return a [Pair] 1) [FacilitySearchResponse] and 2) a [String] indicating any error otherwise null
     */
    override suspend fun searchForFacility(facilityId: String): Pair<FacilitySearchResponse?, String?> {
        return try {
            val response = firstTimeSetupApi.searchFacilityId(facilityId)
            cachedFacilitySearch = response
            Pair(response, null)
        } catch (e: Exception) {
            Pair(null, e.message)
        }
    }

    //exports wireless settings from usb and returns whether it is successful
    override fun exportWirelessSettings(): Boolean {
        return NetworkSettingsImportExport.usbExportWifiConfig()
    }

    //imports wireless settings from usb and returns whether it is successful
    override fun importWirelessSettings(): Boolean {
        return NetworkSettingsImportExport.usbImportWifiConfig()
    }

    //sets bandwidth test url
    override fun setSpeedTestUrl(url: String) {
        DataMatrix.putString(SettingKeys.Experimental.DATA_BANDWIDTH_TEST_URL_STRING, url)
    }

    //sends event to start web view
    override fun sendBandWidthTestEvent() {
        DataMatrix.sendEvent(LoggerKeys.ACTION_SHOW_BANDWIDTH_TEST)
    }

    override fun launchEngineeringScreen() {
        LaunchAppUtil.launchSystemSetup()
    }

    override fun exitFtsu() {
        setFirstTimeSetupAsComplete()
        LaunchAppUtil.launchSplashScreen()
    }

    override fun setTimeZone(timeZone: String, position: Int) {
        val iana = timeZoneStrings.getIANAString(position).ifEmpty { timeZone }
        DataMatrix.putString(SettingKeys.General.DATA_IANA_TIME_ZONE_STRING, iana)
        DataMatrix.putInt(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT, position)
        DateTimeUtil.setTimezone(timeZoneStrings.getIANAString(position))
    }

    override fun getTimeZoneList(): List<String> {
        return context.resources.getStringArray(R.array.timezoneDisplay).toList()
    }

    override fun getIanaTimeZoneList(): List<String> {
        return context.resources.getStringArray(R.array.ianaTimezone).toList()
    }

    override fun getCurrentTimezone(): String {
        return DataMatrix.getString(SettingKeys.General.DATA_IANA_TIME_ZONE_STRING)
    }

    override fun isTimeZoneSet(): Boolean {
        // The index defaults to -1 until something stores a time zone.
        return DataMatrix.getInt(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT) >= 0
    }

    override fun getCurrentTimeZoneIndex(): Int {
        val index = DataMatrix.getInt(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT, 0)
        return if (index >= 0) index else 0
    }

    override fun getDateFormats(): List<String> {
        return context.resources.getStringArray(R.array.dateFormatList).toList()
    }

    override fun getCurrentDateFormatIndex(): Int {
        return DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT, 0)
    }

    override fun setDateFormat(index: Int) {
        DataMatrix.putInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT, index)
        DateTimeUtil.setDateFormat(DateFormat.entries[index])
    }

    override fun getUiExperience(): String {
        return DataMatrix.getString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING)
    }

    override fun setUiExperience(uiExperience: String) {
        DataMatrix.putString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING, uiExperience)
    }

    override fun setIsMetricFormat(isMetric: Boolean) {
        DataMatrix.putBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL, isMetric)
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, isMetric)
    }

    override fun getIsMetricFormat(): Boolean {
        return DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL)
    }

    override fun getUnitFormatTypes(): List<String> {
        return listOf(StringLookup.get(R.string.metric_tc), StringLookup.get(R.string.imperial_tc))
    }

    override fun getIs24HourTimeFormatType(): Boolean {
        return DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL)
    }

    override fun setTimeFormatType(is24Hour: Boolean) {
        DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, is24Hour)
        DateTimeUtil.setTimeFormat(!is24Hour)
    }

    override fun getSupportedTimeFormats(): List<String> {
        return context.resources.getStringArray(R.array.timeFormatList).toList()
    }

    override fun isOnyx(): Boolean {
        return SystemUtils.isOnyx
    }

    override fun setLedColor(color: Int) {
        DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT, color)
        val ledItem = LEDItem(color, LedZone.ALL)
        DataMatrix.putParcelable(ProjectOne.ACTION_LED_COLOR_HEX_PARCELABLE, ledItem)
    }

    override fun getLedColor(): Int {
        return DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT)
    }

    override fun canCalibrate(): Boolean {
        return SystemUtils.isAscent || SystemUtils.isTreadmill
    }

    override fun startCalibration() {
        val start =
            DataMatrix.getInt(HALKeys.DATA_CALIBRATION_STATUS_INT) != Calibration.CalState.CALIBRATING.value
        DataMatrix.putBoolean(HALKeys.ACTION_CALIBRATE_BOOL, start)
    }

    override fun close() {
        manager.cancelAllListeners()
        dataMatrix.finish()
        downloadChannelState.close()
    }

    /**
     * Sets keys to stop loading FTSU
     */
    private fun setFirstTimeSetupAsComplete() {
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true)
        DataMatrix.sendEvent(HALKeys.EVENT_MACHINE_SETUP_COMPLETE)
    }

    /**
     * Sets local channels list and channel map
     */
    private fun createChannelsMap() {
        channelMap.clear()
        val channels = tvTuner?.tuner()?.getChannelList(true)

        channels?.forEach { channel ->
            val mappedChannel = TvTunerMapper.mapToChannel(channel)
            channelMap[mappedChannel.uuid] = channel
        }
    }
}