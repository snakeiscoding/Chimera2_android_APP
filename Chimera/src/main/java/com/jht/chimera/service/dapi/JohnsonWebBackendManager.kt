package com.jht.chimera.service.dapi

import android.util.Log
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.dapi.DapiUtil
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.log.ConsoleEvent
import com.jht.chimera.lib.fleet.FleetCredentialStore
import com.jht.chimera.lib.login.WalletPass
import com.jht.datamatrixlib.DataMatrix
import com.jht.oauthlib.api.BackendEnvironment
import com.jht.oauthlib.api.exerciser.ExerciseApiClient
import com.jht.oauthlib.api.exerciser.MatrixMethodProgram
import com.jht.oauthlib.api.exerciser.UserMetadata
import com.jht.oauthlib.api.jump.JumpApiClient
import com.jht.oauthlib.auth.BksCredentialProvider
import com.jht.oauthlib.auth.ConsoleOAuthConfig
import com.jht.oauthlib.auth.DeviceCodeData
import com.jht.oauthlib.auth.LoginScope
import com.jht.oauthlib.auth.QrDeviceCodeLogin
import com.jht.oauthlib.auth.QrLoginFailure
import com.jht.oauthlib.auth.QrLoginListener
import com.jht.oauthlib.auth.ResourceOwnerFlow
import com.jht.oauthlib.auth.TokenData
import com.jht.oauthlib.auth.TokenUserInfo
import com.jht.oauthlib.auth.UserInfo
import com.jht.oauthlib.microservices.auth.AuthService
import com.jht.progression.userinfo.getUserInfoFromJsonString
import com.jht.rfidlib.WalletType
import com.jht.seca.data.SecaUserData
import com.jht.seca.utils.parseSecaResponse
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.resume

class JohnsonWebBackendManager : AutoCloseable {

    private val TAG = "JohnsonWebBackendManager"
    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled coroutine exception", throwable)
    }
    private val scope = CoroutineScope(Dispatchers.IO + job + exceptionHandler)
    // Serializes read-plan-id -> create-or-patch -> advance-baseline, since saves race independently per call site
    private val matrixMethodSaveMutex = Mutex()

    override fun close() {
        scope.cancel()
    }

     //Interface for generic login and user info
    interface OAuthInterface {
        fun onUserInfo(userInfo: UserInfo)
        fun onAdditionalUserInfo(userMetadata: UserMetadata)
        fun onSecaData(secaUserData: SecaUserData?)
        /** Program, or null when the user has no plan on JUMP. */
        fun onMatrixMethodData(program: MatrixMethodProgram?)
        /** [idToken] is the OpenID JWT, empty when the login failed or JUMP returned none */
        fun onLoginFinished(success: Boolean, token: String, idToken: String)
    }

    //Interface for QR login status
    interface QRLoginInterface {
        /** Called when a QR code is received from the backend. */
        fun onDeviceCodeReceived(deviceCodeData: DeviceCodeData)
        /** Device code expired before the user authorized. */
        fun onDeviceCodeExpired()
        /** Device code error occurred. */
        fun onDeviceCodeError()
        /** Device code was used for login. */
        fun onDeviceCodeSuccess()
    }

    /**
     * Calls login with Resource Owner Flow Authentication
     */
    fun jumpLogin(
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        jumpUserAuthenticationInfo: UserAuthenticationInfo,
        username: String,
        passcode: String,
        loginType: String,
        oAuthInterface: OAuthInterface
    ) {
        jumpInternalLogin(
            getJumpToken = {
                //Get login scope
                val scopeType = when (loginType) {
                    DapiUtil.LOGIN_TYPE_XID -> LoginScope.XID
                    DapiUtil.LOGIN_TYPE_RFID -> LoginScope.RFID
                    DapiUtil.LOGIN_TYPE_BARCODE -> LoginScope.BARCODE
                    DapiUtil.LOGIN_TYPE_PHONE -> LoginScope.EXTERNAL_PHONE
                    DapiUtil.LOGIN_TYPE_EMAIL -> LoginScope.EXTERNAL_EMAIL
                    DapiUtil.LOGIN_TYPE_CUSTOM -> LoginScope.CUSTOM
                    else -> LoginScope.OPEN_ID
                }

                resourceOwnerAuthentication(
                    jumpAuthenticationInfo = jumpAuthenticationInfo,
                    username = username,
                    passcode = passcode,
                    scopeType = scopeType,
                )},
            jumpAuthenticationInfo = jumpAuthenticationInfo,
            jumpUserAuthenticationInfo = jumpUserAuthenticationInfo,
            oAuthInterface = oAuthInterface
        )
    }

    /**
     * Calls login with eGYM wallet information
     */
    fun egymWalletLogin(
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        jumpUserAuthenticationInfo: UserAuthenticationInfo,
        walletPass: WalletPass,
        oAuthInterface: OAuthInterface,
    ) {
        jumpInternalLogin(
            getJumpToken = {
                val username = walletPass.pass
                val password = if (walletPass.passType == WalletType.ApplePay) "APPLE" else "GOOGLE"
                val scopeType = LoginScope.EXTERNAL_WALLET

                resourceOwnerAuthentication(
                    jumpAuthenticationInfo = jumpAuthenticationInfo,
                    username = username,
                    passcode = password,
                    scopeType = scopeType,
                    issuerId = walletPass.issuerId,
                )},
            jumpAuthenticationInfo = jumpAuthenticationInfo,
            jumpUserAuthenticationInfo = jumpUserAuthenticationInfo,
            oAuthInterface = oAuthInterface
        )
    }

    /**
     * Calls login using a QR code
     */
    fun jumpQrLogin(
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        jumpUserAuthenticationInfo: UserAuthenticationInfo,
        oAuthInterface: OAuthInterface,
        qrLoginInterface: QRLoginInterface
    ): Job {
        var expiredReported = false
        return jumpInternalLogin(
            getJumpToken = {
                loginQrOAuth(jumpAuthenticationInfo, qrLoginInterface)
                {
                    //On expiry of QR
                    qrLoginInterface.onDeviceCodeExpired()
                    expiredReported = true
                }
            },
            result = { success ->
                when {
                    success -> qrLoginInterface.onDeviceCodeSuccess()
                    !expiredReported -> qrLoginInterface.onDeviceCodeError()
                    // else: expiry already reported by loginQrOAuth � don't double-fire
                }
            },
            jumpAuthenticationInfo = jumpAuthenticationInfo,
            jumpUserAuthenticationInfo = jumpUserAuthenticationInfo,
            oAuthInterface = oAuthInterface
        )
    }

    /**
     * Encodes the RFID value as tag:type "abc123:2"
     */
    private fun encodeRfidValue(tag: String, type: Byte): String = "$tag:$type"

    /**
     * Associates an RFID tag to a user id
     */
    fun rfidAssociate(environmentId: Int, jumpToken: String, userId: String, tag: String, type: Byte) {
        val value = encodeRfidValue(tag, type)
        scope.launch {
            JumpApiClient(environmentId, jumpToken).use {
                it.associateRfid(userId = userId, value = value)
            }
        }
    }

    /**
     * Disassociates an RFID tag to a user id
     */
    fun rfidDisassociate(jumpAuthenticationInfo: JumpAuthenticationInfo, tag: String, type: Byte) {
        val value = encodeRfidValue(tag, type)
        scope.launch {
            val jumpToken = resourceOwnerAuthentication(
                jumpAuthenticationInfo = jumpAuthenticationInfo,
                username = tag,
                passcode = type.toString(),
                scopeType = LoginScope.RFID,
            )
            jumpToken?.let{
                val userInfo = jumpToken.idToken?.let { TokenUserInfo.getUserInfo(it) }
                userInfo?.userId?.let {id->
                    JumpApiClient(jumpAuthenticationInfo.environmentId, jumpToken.accessToken).use {
                        it.disassociateRfid(userId = id, value = value)
                    }
                }
            }
        }
    }

    /**
     * Calls the Login and User info methods using JUMP and other web services
     * 1. Gets JUMP token from passed in method
     * 2. Gets User Info from JUMP Id Token
     * 3. Callback to the rest of the system login is finished
     */
    private fun jumpInternalLogin(
        getJumpToken: suspend () -> TokenData?,
        result: (loginSuccess: Boolean) -> Unit = {},
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        jumpUserAuthenticationInfo: UserAuthenticationInfo,
        oAuthInterface: OAuthInterface
    ) : Job {
        return scope.launch {
            try {
                ConsoleEvent.logEvent("Login: JUMP started")
                val jumpToken = getJumpToken()

                if (jumpToken != null) {
                    //The access token is used for the authentication to other endpoints
                    val accessToken = jumpToken.accessToken

                    //Get the user info from the token and Exerciser API endpoints
                    getUserInfo(
                        jumpToken = jumpToken,
                        jumpAuthenticationInfo = jumpAuthenticationInfo,
                        jumpUserAuthenticationInfo = jumpUserAuthenticationInfo,
                        oAuthInterface = oAuthInterface
                    )
                    //Finish the login. This is run after other calls to provide a distinct point of
                    //time when the login is fully finished.
                    ConsoleEvent.logEvent("Login: JUMP successful")
                    oAuthInterface.onLoginFinished(success = true, token = accessToken, idToken = jumpToken.idToken ?: "")
                    result(true)
                } else {
                    ConsoleEvent.logEvent("Login: JUMP unsuccessful")
                    oAuthInterface.onLoginFinished(success = false, token = "", idToken = "")
                    result(false)
                }

            } catch (e: Exception) {
                ConsoleEvent.logEvent("Login: JUMP exception: ${e.message}")
                oAuthInterface.onLoginFinished(success = false, token = "", idToken = "")
                result(false)
            }
        }
    }

    /**
     * Gets a JUMP token based on the resource owner authentication flow
     */
    private suspend fun resourceOwnerAuthentication(
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        username: String,
        passcode: String,
        scopeType: LoginScope = LoginScope.OPEN_ID,
        issuerId: String = ""
    ): TokenData? {
        val context = ContextUtil.getApplicationContext() ?: throw IllegalStateException("Application context not available")
        val store = FleetCredentialStore.getInstance(context)
        val identity = store.load() ?: throw IllegalStateException("FleetDeviceIdentity not available")
        val oauthConfig = ConsoleOAuthConfig(
            context = context,
            deployment = jumpAuthenticationInfo.environmentId,
            clientId = identity.oauthClientId,
            clientSecret = identity.oauthClientSecret
        )
        val resourceOwnerFlow = ResourceOwnerFlow(oauthConfig)

        val result = resourceOwnerFlow.authenticate(
            username = username,
            password = passcode,
            machineId = jumpAuthenticationInfo.machineId,
            facilityId = jumpAuthenticationInfo.facilityId,
            scopes = scopeType.authScope,
            issuerId = issuerId
        )
        result.fold(
            onSuccess = { token -> return token },
            onFailure = { error -> return null }
        )
    }

    /** JUMP login via device-authorization (QR) flow. */
    private suspend fun loginQrOAuth(
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        qrLoginInterface: QRLoginInterface,
        onExpired: () -> Unit,
    ): TokenData? = suspendCancellableCoroutine { continuation ->
        val context = ContextUtil.getApplicationContext() ?: throw IllegalStateException("Application context not available")
        val credentials = BksCredentialProvider(context).getCredentials(jumpAuthenticationInfo.environmentId)
        val oauthConfig = ConsoleOAuthConfig(
            context = context,
            deployment = jumpAuthenticationInfo.environmentId,
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret,
        )

        val jumpDeviceRedirectApp = runCatching {
            DataMatrix.getString(SettingKeys.AssetManagement.DATA_JUMP_DEVICE_REDIRECT_APP_STRING)
        }.getOrDefault("")

        val handle = QrDeviceCodeLogin(oauthConfig).start(
            deployment = BackendEnvironment.fromValue(jumpAuthenticationInfo.environmentId),
            facilityId = jumpAuthenticationInfo.facilityId.toString(),
            redirectApp = jumpDeviceRedirectApp.takeIf { it.isNotBlank() },
            listener = object : QrLoginListener {
                override fun onDeviceCodeReceived(data: DeviceCodeData) {
                    qrLoginInterface.onDeviceCodeReceived(data)
                }

                override fun onSuccess(tokens: TokenData) {
                    if (continuation.isActive) continuation.resume(tokens)
                }

                override fun onExpired() {
                    Log.w(TAG, "loginQrOAuth: device code expired before authorization")
                    onExpired()
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onFailed(error: QrLoginFailure) {
                    Log.w(TAG, "loginQrOAuth failed: ${error.message}")
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        )

        continuation.invokeOnCancellation { handle.close() }
    }

    /**
     * User info calls from the JUMP login. This directly passes out information via
     * the oAuthInterface
     *
     * 1. User Info from JUMP Id Token
     *
     * 2. User Info from user_metadata on Exercise API
     *
     * 3. If using seca, seca Info from user state on Exercise API
     */
    private suspend fun getUserInfo(
        jumpToken: TokenData,
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        jumpUserAuthenticationInfo: UserAuthenticationInfo,
        oAuthInterface: OAuthInterface
    ){
        //Parse token to get main user info like id, weight, age, gender, name
        val userInfo = jumpToken.idToken?.let { TokenUserInfo.getUserInfo(it) }
        userInfo?.let {
            ConsoleEvent.logEvent("Login: User Info Parsed")
            oAuthInterface.onUserInfo(userInfo)
        }

        ExerciseApiClient(jumpAuthenticationInfo.environmentId, jumpToken.accessToken).use { client ->
            //API call to get additional user info like isPTP user, bluetooth devices, and fitness test scores
            val additionalUserSettings = client.getUserMetadata(
                clubId = jumpAuthenticationInfo.facilityId,
                machineType = jumpUserAuthenticationInfo.machineType,
                machineId = jumpAuthenticationInfo.machineId,
                settings = jumpUserAuthenticationInfo.optionalSettings
            )
            if (additionalUserSettings != null) {
                ConsoleEvent.logEvent("Login: Additional User Info Parsed")
                oAuthInterface.onAdditionalUserInfo(additionalUserSettings)
            }

            //If using seca (determined by digital services for the club), get the seca user info
            if (jumpUserAuthenticationInfo.hasSeca) {
                userInfo?.let { info ->
                    val secaResponse = client.getSecaUserState(
                        userId = info.userId,
                        clubId = jumpAuthenticationInfo.facilityId,
                    )

                    if (secaResponse == null) {
                        ConsoleEvent.logEvent("Failed to retrieve Seca User State.")
                        return@let
                    }

                    val secaUserDataResult = parseSecaResponse(secaResponse)

                    if (secaUserDataResult.isSuccess) {
                        val secaUserData = secaUserDataResult.getOrThrow()
                        ConsoleEvent.logEvent("Login: seca data found, Scan State: " + secaUserData.scanState + "; Survey State: " + secaUserData.surveyState)
                        oAuthInterface.onSecaData(secaUserData)
                    } else {
                        Log.e(TAG, "getUserInfo: ", secaUserDataResult.exceptionOrNull())
                    }
                }
            }

            //Matrix Method program, fetched on the same client used for seca/getUserInfo above
            val program = client.getMatrixMethodProgram()
            ConsoleEvent.logEvent(
                if (program != null) "Login: Matrix Method program found" else "Login: no Matrix Method program"
            )
            oAuthInterface.onMatrixMethodData(program)
        } // END client
    } // END getUserInfo

    /** Creates the plan when [programId] is blank, otherwise patches metadata and events separately. */
    fun saveMatrixMethodProgram(
        jumpAuthenticationInfo: JumpAuthenticationInfo,
        accessToken: String,
        progressionJson: String,
        programId: String,
        lastSyncedProgressionJson: String? = null,
        onProgramCreated: (String) -> Unit = {},
        onSynced: () -> Unit = {}
    ): Job = scope.launch {
        matrixMethodSaveMutex.withLock {
            val userInfo = getUserInfoFromJsonString(progressionJson)
            ExerciseApiClient(jumpAuthenticationInfo.environmentId, accessToken).use { client ->
                if (programId.isBlank()) {
                    val newId = UUID.randomUUID().toString()
                    if (client.createMatrixMethodProgram(userInfoToJumpProgram(userInfo, newId))) {
                        onProgramCreated(newId)
                        onSynced()
                    } else {
                        Log.w(TAG, "Matrix Method create failed")
                    }
                    return@withLock
                }

                val metadataOk = client.updateMatrixMethodProgramMetadata(userInfoToJumpMetadata(userInfo, programId))
                val previousUserInfo = lastSyncedProgressionJson?.let { getUserInfoFromJsonString(it) }
                val eventsOk = userInfoToJumpEventsUpdate(userInfo, programId, previousUserInfo)?.let { update ->
                    client.updateMatrixMethodProgramEvents(update)
                } ?: true

                // Advancing on a partial failure would leave JUMP permanently missing the failed events
                if (metadataOk && eventsOk) {
                    onSynced()
                } else {
                    Log.w(TAG, "Matrix Method save incomplete (metadata=$metadataOk events=$eventsOk), will resend")
                }
            }
        }
    }

    companion object {
        /** Get the URL for web page to edit the user's profile, or empty string if not applicable */
        fun getEditUserUrl(
            environmentId: Int,
            accessToken: String,
        ): String {
            if (accessToken.isEmpty()) return ""
            val baseUrl = AuthService.getLegacyBaseUrl(environmentId)
            return "$baseUrl/user-profile?access_token=$accessToken"
        }
    }
}
