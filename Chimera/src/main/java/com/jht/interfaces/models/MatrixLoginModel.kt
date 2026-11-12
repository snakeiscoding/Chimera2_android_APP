package com.jht.interfaces.models

import android.graphics.Bitmap
import android.util.Log
import com.jht.chimera.BuildConfig
import com.jht.chimera.R
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.dapi.DapiCreateUser
import com.jht.chimera.lib.common.dapi.DapiLogin
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.unit.WeightUnit
import com.jht.chimera.lib.common.value.Weight
import com.jht.chimera.lib.login.RFIDLogin
import com.jht.chimera.lib.login.RFIDLogin.ILoginListener
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.QrLoginHandler
import com.jht.data.RegisterData
import com.jht.data.RfidLoginData
import com.jht.data.RfidLoginHandler
import com.jht.data.UserFieldDate
import com.jht.data.UserFieldDigits
import com.jht.data.UserFieldEmail
import com.jht.data.UserFieldFunction
import com.jht.data.UserFieldGender
import com.jht.data.UserFieldInteger
import com.jht.data.UserFieldText
import com.jht.data.UserFieldUnits
import com.jht.chimera.poc.WalletQR
import com.jht.data.QrAuthorizationResponse
import com.jht.chimera.service.hal.lib.hardware.RfidReaderManager
import com.jht.data.RfidOperatingMode
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.Gender
import com.jht.enums.UnitSystem
import com.jht.interfaces.CallbackManager
import com.jht.rfid.Legic.RFIDChip
import com.jht.rfid.LegicEnums.RfStandard
import com.jht.rfidlib.ReaderFeatures
import com.jhtp.dapiservice.DapiResult
import com.jhtp.dapiservice.um.GenderIndentity
import java.time.LocalDate


private const val TAG = "MatrixLoginModel"

private fun genderInterfaceToDapi(gender: Gender?) : GenderIndentity {
    return when (gender) {
        Gender.Male -> GenderIndentity.Male
        Gender.Female -> GenderIndentity.Female
        Gender.NonBinary -> GenderIndentity.NonBinary
        else -> GenderIndentity.NotSpecified
    }
}


class MatrixLoginModel : LoginModel() {
    private val manager: CallbackManager = CallbackManager()
    private var qrDataMatrix: DataMatrix? = null

    // may have multiple RPC going on with DataMatrix, so need way to track them (TODO - can we do something clever with CallbackManager or some kind of RPC manager?)
    private val dmList = ArrayList<DataMatrix>()
    private fun createDataMatrix(): DataMatrix { val dataMatrix = DataMatrix(); dmList.add(dataMatrix); return dataMatrix }
    private fun destroyDataMatrix(dataMatrix: DataMatrix?) { dataMatrix?.let { it.finish(); dmList.remove(dataMatrix) } }

    override fun logout() {
        DapiLogin.logout()
    }

    override fun getLoginOptions(): List<LoginData> {
        val list = ArrayList<LoginData>()
        val context = ContextUtil.getApplicationContext() ?: return list // if we don't have context, just return empty list
        val rfidFeatures = RfidReaderManager.getReaderFeatures()
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL)) {
            list.add(LoginData(context.getString(R.string.btn_xid_login_tc), context.getString(R.string.xid_login_description_pg), LoginType.Xid, null, listOf(
                UserFieldDigits(UserFieldFunction.LoginId, context.getString(R.string.xid_tc), context.getString(R.string.enter_xid_2_tc), 4, 14),
                UserFieldDigits(UserFieldFunction.Password, context.getString(R.string.passcode_tc), context.getString(R.string.enter_passcode_tc), 1 ,4),
            )))
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)) {
            list.add(LoginData(context.getString(R.string.barcode_login_tc), "", LoginType.Barcode, null, listOf(
                UserFieldText(UserFieldFunction.LoginId, context.getString(R.string.barcode_login_tc), context.getString(R.string.enter_barcode_tc), 1, 65),
                UserFieldText(UserFieldFunction.Password, context.getString(R.string.last_name_tc), context.getString(R.string.enter_last_name_tc), 1 ,65),

            )))
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL)) {
            list.add(LoginData(context.getString(R.string.phone_number_tc), "", LoginType.PhoneNumber, null, listOf(
                UserFieldDigits(UserFieldFunction.LoginId, context.getString(R.string.phone_number_tc), context.getString(R.string.enter_phone_number_tc), 1, 14),
                UserFieldDigits(UserFieldFunction.Password, context.getString(R.string.passcode_tc), context.getString(R.string.enter_passcode_tc), 1 ,4),
            )))
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL)) {
            list.add(LoginData(context.getString(R.string.email_login_tc), "", LoginType.Email, null, listOf(
                UserFieldEmail(UserFieldFunction.LoginId, context.getString(R.string.email_tc), context.getString(R.string.enter_email_address_tc), 1, 65),
                UserFieldText(UserFieldFunction.Password, context.getString(R.string.password_tc), context.getString(R.string.enter_password_tc), 1 ,65),
            )))
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL) && rfidFeatures.contains(ReaderFeatures.WalletSupport)) {
            list.add(LoginData(context.getString(R.string.wallet_login_tc), context.getString(R.string.login_with_apple_or_google_wallet_description_pg), LoginType.Wallet, null))
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_QR_LOGIN_BOOL)) {
            list.add(
                LoginData(
                    label = context.getString(R.string.qr_code_tc),
                    description = "",
                    type = LoginType.QrCode
                )
            )
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL)) {
            val name = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_NAME_STRING)
            val labelId = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_LABEL_STRING)
            val labelPw = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING)
            list.add(LoginData(name, "", LoginType.Custom, null, listOf(
                when (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_INPUT_TYPE_INT)) {
                    0 -> UserFieldDigits(UserFieldFunction.LoginId, labelId, "", 1, 65)
                    2 -> UserFieldEmail(UserFieldFunction.LoginId, labelId, "", 1, 65)
                    else -> UserFieldText(UserFieldFunction.LoginId, labelId, "", 1, 65)
                },
                when (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT)) {
                    0 -> UserFieldDigits(UserFieldFunction.Password, labelPw, "", 1, 65)
                    2 -> UserFieldEmail(UserFieldFunction.Password, labelPw, "", 1, 65)
                    else -> UserFieldText(UserFieldFunction.Password, labelPw, "", 1, 65)
                }
            )))
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) {
            list.add(LoginData(context.getString(R.string.guest_tc), "", LoginType.Guest))
        }
        // make primary login method the first entry (if it is there)
        LoginType.fromValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_PRIMARY_LOGIN_METHOD_STRING))?.let { primaryLoginMethod->
            list.find { it.type == primaryLoginMethod }?.let { primaryItem->
                list.remove(primaryItem)
                list.add(0, primaryItem)
            }
        }
        return list
    }

    override fun attemptLogin(data: LoginData?, onResult: (Int) -> Unit) {
        if (data == null || data.isGuest()) {
            // Always logout here, even if already a guest: logout is what resets guest state
            // (e.g. Matrix Method progress), so skipping it when re-selecting guest let a prior
            // guest's progress leak into the new guest session.
            DapiLogin.logout() // note logout will load guest preferences over any previous selections
            onResult(0) // indicate success in case something is waiting
            return
        }
        // at the moment most logins are two fields
        val field0 = data.fields.find { it.function == UserFieldFunction.LoginId }?.getStringValue() ?: ""
        val field1 = data.fields.find { it.function == UserFieldFunction.Password }?.getStringValue() ?: ""
        val dataMatrix = createDataMatrix()
        DapiLogin.login(dataMatrix, field0, field1, data.type.value) { dapiResult: Int ->
            destroyDataMatrix(dataMatrix)
            if (dapiResult == 0 && data.type == LoginType.Xid) { // we only do RFID with XID accounts
                // if there is an RFID to associate with this account, do so
                /* Old method uses DM global, for new we try to pass ID with our login data
                val rfidChip = DataMatrix.getParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, RFIDChip::class.java)
                if (rfidChip != null) DataMatrix.putParcelable(DapiLogin.Keys.ACTION_ASSOCIATE_RFID_PARCELABLE, rfidChip)
                DataMatrix.putParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, null) // should not keep this longer than needed */
                (data.associateDeviceLogin as? RfidLoginData)?.let {
                    val rfidChip = RFIDChip(RfStandard.fromByte((it.dapiRfidType and 0xff).toByte()), it.uid)
                    DataMatrix.putParcelable(DapiLogin.Keys.ACTION_ASSOCIATE_RFID_PARCELABLE, rfidChip)
                }
            }
            onResult(dapiResult)
        }
    }

    override fun getRegisterOptions() : List<RegisterData> {
        val list = ArrayList<RegisterData>()
        val context = ContextUtil.getApplicationContext() ?: return list // if we don't have context, just return empty list
        val defaultGender = Gender.from(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT))
        val defaultUnits = UnitSystem.from(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL))
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL) && DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL)) {
            val maxBirthday = LocalDate.now().minusYears(14)
            val minBirthday = LocalDate.now().minusYears(120)
            list.add(RegisterData(context.getString(R.string.register_tc), "", LoginType.Xid,
                listOf(
                    UserFieldEmail(
                        UserFieldFunction.EmailAddress,
                        context.getString(R.string.email_tc),
                        context.getString(R.string.email_address_tc),
                        1,
                        65,
                        asyncValidation = {callback ->
                            checkExists(ExistenceCheckData(LoginType.Xid, function, value.toString())) {
                                if (it.checkData.value != value || !it.supportsCheck || it.exists == null) callback(false, "")
                                else if (it.exists == true) callback(true, context.getString(R.string.email_already_in_use_message_pg))
                                else callback(true, "")
                            }
                        }
                    ),
                    UserFieldDigits(
                        UserFieldFunction.LoginId,
                        context.getString(R.string.xid_tc),
                        context.getString(R.string.enter_xid_tc),
                        10,
                        14,
                        asyncValidation = {callback ->
                            checkExists(ExistenceCheckData(LoginType.Xid, function, value.toString())) {
                                if (it.checkData.value != value || !it.supportsCheck || it.exists == null) callback(false, "")
                                else if (it.exists == true) callback(true, context.getString(R.string.xid_unavailable_tc))
                                else callback(true, "")
                            }
                        }
                    ),
                    UserFieldDigits(
                        UserFieldFunction.Password,
                        context.getString(R.string.passcode_tc),
                        context.getString(R.string.enter_passcode_tc),
                        4,
                        4
                    ),
                    UserFieldDigits(
                        UserFieldFunction.ConfirmPassword,
                        context.getString(R.string.confirm_passcode_2_tc),
                        context.getString(R.string.confirm_passcode_tc),
                        4,
                        4
                    ),
                    UserFieldText(
                        UserFieldFunction.FirstName,
                        context.getString(R.string.first_name_tc),
                        context.getString(R.string.enter_first_name_tc),
                        1,
                        65
                    ),
                    UserFieldText(
                        UserFieldFunction.LastName,
                        context.getString(R.string.last_name_tc),
                        context.getString(R.string.enter_last_name_tc),
                        1,
                        65
                    ),
                    UserFieldDate(
                        UserFieldFunction.Birthdate,
                        context.getString(R.string.birthdate_tc),
                        "",
                        minBirthday,
                        maxBirthday,
                    ),
                    UserFieldGender(
                        UserFieldFunction.Gender,
                        context.getString(R.string.gender_tc),
                        "",
                        defaultGender,
                    ),
                    UserFieldUnits(
                        UserFieldFunction.UnitPreference,
                        context.getString(R.string.units_tc),
                        "",
                        defaultUnits,
                    ),
                    UserFieldInteger(
                        UserFieldFunction.Weight,
                        context.getString(R.string.weight_tc),
                        context.getString(R.string.enter_weight_tc),
                        1,
                        999,
                    ),
                )
            ))
        }
        return list
    }

    override fun attemptRegister(data: RegisterData, onResult: (Int) -> Unit) {
        when (data.type) {
            LoginType.Xid -> {
                val userData = DapiCreateUser.DapiRegisterParcelable()
                userData.xid = data.find(UserFieldFunction.LoginId)?.getStringValue()
                userData.passcode = data.find(UserFieldFunction.Password)?.getStringValue()
                userData.acceptedTerms = true // if we got here, they accepted
                userData.email = data.find(UserFieldFunction.EmailAddress)?.getStringValue()
                userData.firstName = data.find(UserFieldFunction.FirstName)?.getStringValue()
                userData.lastName = data.find(UserFieldFunction.LastName)?.getStringValue()
                val units = data.findCast<UserFieldUnits>(UserFieldFunction.UnitPreference)?.value
                val weight : Double = data.find(UserFieldFunction.Weight)?.getDoubleValue() ?: 0.0
                val isMetric = units?.isMetric() ?: UnitSystem.DEFAULT.isMetric()
                val oldUnit = if (isMetric) com.jht.chimera.lib.common.i18n.Unit.METRIC else com.jht.chimera.lib.common.i18n.Unit.IMPERIAL
                userData.setUnitPreferenceAndWeight(oldUnit, Weight(weight, if (isMetric) WeightUnit.KILOGRAM else WeightUnit.POUND ))
                userData.genderIndentity = genderInterfaceToDapi(data.findCast<UserFieldGender>(UserFieldFunction.Gender)?.value)
                val birthDate = data.findCast<UserFieldDate>(UserFieldFunction.Birthdate)?.value
                if (birthDate != null) {
                    userData.birthYear = birthDate.year
                    userData.birthDay = birthDate.dayOfMonth
                    userData.birthMonth = birthDate.monthValue
                }
                if (false) { // FAKE SUCCESS - TESTING ONLY !!!!!!
                    logData(userData)
                    onResult(0)
                    return
                }
                val dataMatrix = createDataMatrix()
                DapiCreateUser.createUser(dataMatrix, userData) { dapiResult: Int ->
                    //Log.d(TAG, "dapiResult $dapiResult ${DapiResult.fromInt(dapiResult)}")
                    destroyDataMatrix(dataMatrix)
                    onResult(dapiResult)
                }
            }
            else -> {
                Log.e(TAG, "Unsupported registration type " + data.type)
                onResult(DapiResult.CommandNotFound.Value)
            }
        }
    }
    private fun logData(userData: DapiCreateUser.DapiRegisterParcelable) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "xid " + userData.xid)
            Log.d(TAG, "passcode " + userData.passcode)
            Log.d(TAG, "email " + userData.email)
            Log.d(TAG, "firstName " + userData.firstName)
            Log.d(TAG, "lastName " + userData.lastName)
            Log.d(TAG, "genderIndentity " + userData.genderIndentity)
            Log.d(TAG, "birth " + userData.birthYear + " " + userData.birthDay + " " + userData.birthMonth)
            val weightUnit = if (userData.unitPreference == com.jht.chimera.lib.common.i18n.Unit.METRIC) WeightUnit.KILOGRAM else WeightUnit.POUND
            Log.d(TAG, "unit weight ${userData.unitPreference} ${userData.weight.getValue(weightUnit)}")
        }
    }


    // Rough first pass to try to generically wire our current RFID login mechanism.
    private var currentRfidSession: RFIDLogin? = null
    private var rfidDataMatrix: DataMatrix? = null
    override fun rfidStartSession(handler: RfidLoginHandler) {
        rfidStopSession() // new session replaces any existing session
        rfidDataMatrix = createDataMatrix()
        rfidDataMatrix?.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, {
            internalCreateRfidSession(handler, it)
        }, true)
    }
    private fun internalCreateRfidSession(handler: RfidLoginHandler, userDataPending: Boolean) {
        if (userDataPending || currentRfidSession != null) return
        rfidDataMatrix?.finish() // clear the checking listener before reusing for actual session
        rfidDataMatrix?.let {
            currentRfidSession = RFIDLogin(it, object : ILoginListener {
                override fun onLoginAttemptStart() {
                    handler.onLoginAttempt(true)
                }
                override fun onLoginAttemptEnd(result: RFIDLogin.Result, chip: RFIDChip) {
                    val rfidLoginData = RfidLoginData(chip.rfStandard().toByte().toInt(), chip.uid())
                    when (result) {
                        RFIDLogin.Result.SUCCESS -> handler.onLoginSuccess()
                        RFIDLogin.Result.INVALID_CREDENTIALS -> handler.onLoginFailure(true)
                        RFIDLogin.Result.NO_ASSOCIATED_XID -> handler.onNewRfid(rfidLoginData)
                        RFIDLogin.Result.CONNECTION_ERROR -> handler.onError(true)
                    }
                }

                override fun onWalletLoginAttemptStart() {
                    handler.onLoginAttempt(false)
                }

                override fun onWalletLoginAttemptEnd(result: RFIDLogin.Result?) {
                    when (result) {
                        RFIDLogin.Result.SUCCESS -> handler.onLoginSuccess()
                        RFIDLogin.Result.INVALID_CREDENTIALS -> handler.onLoginFailure(false)
                        RFIDLogin.Result.CONNECTION_ERROR -> handler.onError(false)
                        else -> handler.onError(false)
                    }
                }
            })
            currentRfidSession?.start() // Start RFID scanning
        }
    }
    override fun rfidStopSession() {
        destroyDataMatrix(rfidDataMatrix)
        rfidDataMatrix = null
        currentRfidSession?.stop()
        currentRfidSession = null
    }
    override fun rfidSetMode(rfidOperatingMode: RfidOperatingMode) {
        DataMatrix.putInt(HALKeys.ACTION_RFID_OPERATING_MODE_INT, rfidOperatingMode.ordinal)
    }


    // This was a method on LoginModel, but ultimately found implementation that did not require it. We could add it again if other need should be found?
    private fun checkExists(checkData: ExistenceCheckData, onResult: (ExistenceConfirmationData) -> Unit) {
        when (checkData.type) {
            LoginType.Xid -> checkExistsXid(checkData, onResult)
            else -> onResult(ExistenceConfirmationData(checkData))
        }
    }

    private fun checkExistsXid(checkData: ExistenceCheckData, onResult: (ExistenceConfirmationData) -> Unit) {
        when (checkData.fieldFunction) {
            UserFieldFunction.LoginId -> {
                val dataMatrix = createDataMatrix()
                DapiLogin.checkXidExists(dataMatrix, checkData.value) {
                    destroyDataMatrix(dataMatrix)
                    onResult(ExistenceConfirmationData(checkData, true, it))
                }
            }
            UserFieldFunction.EmailAddress -> {
                val dataMatrix = createDataMatrix()
                DapiCreateUser.checkEmailExists(dataMatrix, checkData.value) {
                    destroyDataMatrix(dataMatrix)
                    onResult(ExistenceConfirmationData(checkData, true, it))
                }
            }
            else -> onResult(ExistenceConfirmationData(checkData))
        }
    }



    override fun close() {
        rfidStopSession()
        qrStopSession()
        dmList.forEach { it.finish() }
        dmList.clear()
        manager.cancelAllListeners()
    }

    override fun qrStartSession(handler: QrLoginHandler) {
        qrStopSession()
        val dm = createDataMatrix()
        qrDataMatrix = dm
        Log.d(TAG, "qrStartSession: Starting QR session.")
        DapiLogin.startQrSession(dm, object : DapiLogin.OnQrLoginListener {
            override fun onCodeReady(response: QrAuthorizationResponse) {
                Log.d(TAG, "onCodeReady: QR Ready")
                handler.onCodeReady(response)
            }

            override fun onResult(qrResult: Int) {
                Log.d(TAG, "onResult: data: $qrResult")
                when (qrResult) {
                    DapiLogin.QrResult.SUCCESS -> handler.onLoginSuccess()
                    DapiLogin.QrResult.EXPIRED -> handler.onExpired()
                    else -> handler.onError()
                }
            }
        })
    }

    override fun qrStopSession() {
        qrDataMatrix?.let {
            DapiLogin.stopQrSession()
            destroyDataMatrix(it)
        }
        qrDataMatrix = null
    }

    /** Generate a QR code bitmap for adding a wallet pass */
    override suspend fun generateWalletQR(passData: String, username: String) : Bitmap? {
        try {
            val context = ContextUtil.getApplicationContext() ?: return null
            val walletQR = WalletQR()
            val dapiEnv = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
            val uuid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING)
            val passDataUrl = walletQR.generateWalletPass(context, passData, username, uuid, dapiEnv)
            if (passDataUrl.isNotEmpty()) return walletQR.generateQRCode(passDataUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating wallet QR", e)
        }
        return null
    }
}


// These two were added to interface LoginData.kt but pulled out since we don't presently need to expose "checkExists" across interface
/** Describes a login registration field to check for existence */
private data class ExistenceCheckData(
    /** The login type being checked */
    val type : LoginType,
    /** The field that is being checked */
    val fieldFunction: UserFieldFunction,
    /** The value being checked */
    val value: String,
)
private data class ExistenceConfirmationData(
    /** The data that was checked */
    val checkData : ExistenceCheckData,
    /** Whether the system supports checking this field for existence */
    val supportsCheck: Boolean = false,
    /** Whether the system can confirm the value exists(true) or does not(false) or cannot determine (null) */
    val exists: Boolean? = null,
)
