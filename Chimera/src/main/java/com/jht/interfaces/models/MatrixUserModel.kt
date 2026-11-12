package com.jht.interfaces.models

import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.dapi.JumpSession
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.service.dapi.JohnsonWebBackendManager
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixFloatValueListener
import com.jht.interfaces.MatrixIntValueListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.interfaces.StringValueListener
import com.jht.seca.data.SecaUserData

class MatrixUserModel : UserModel() {

    private val manager: CallbackManager = CallbackManager()

    override val uuid = MatrixStringValueListener(manager, DAPIKeys.DATA_USER_DAPI_UUID_STRING)
    override val name = MatrixStringValueListener(manager, DAPIKeys.DATA_USER_FIRST_NAME_STRING)
    override val gender = MatrixIntValueListener(manager, DAPIKeys.DATA_USER_GENDER_ENUM_INT)
    override val weight = MatrixFloatValueListener(manager, DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT)
    override val age = MatrixIntValueListener(manager, DAPIKeys.DATA_USER_AGE_INT)
    override val editUrl by lazy { object : StringValueListener {
        // environment only changed in MES, but we could listen to it in case?
        private var environment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
        // token is the one that can change as login is async operation
        private var token = JumpSession.current().accessToken
        override val value: String
            get() {
                return JohnsonWebBackendManager.getEditUserUrl(environment, token)
            }

        override fun listenAndTrigger(onChange: (String) -> Unit) {
            listen(onChange)
            environment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
            token = JumpSession.current().accessToken
            onChange(value)
        }
        override fun listen(onChange: (String) -> Unit) {
            manager.addIntListener(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, {
                if (environment != it) {
                    environment = it
                    onChange(value)
                }
            }, false)
            manager.addParcelableListener(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, JumpSession::class.java, {
                if (token != it.accessToken) {
                    token = it.accessToken
                    onChange(value)
                }
            }, false)
        }
    }}

    override val isGuest: Boolean
        get() = DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)

    override val isPTP = object : BoolValueListener {
        private fun checkPtpSupported(): Boolean {
            //If the seca feature is enabled, we should always show the seca (PTP) experience regardless of individual user login
            //This setting is enabled at a club wide level via the facility config API.
            if(DataMatrix.getBoolean(DAPIKeys.DATA_HAS_SECA_FEATURE_BOOL)){
                return isSecaUser
            }
            else{
                val userIsPtp = (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL)) //SECA feature check
                return ProductConfig.supportsPtp() && userIsPtp
            }
        }

        private fun setCallbacks(onChange: (Boolean) -> Unit, forceCallback: Boolean) {
            manager.addBoolListener(SettingKeys.General.DATA_IS_PTP_USER_BOOL, {
                onChange(checkPtpSupported())
            }, forceCallback)

            manager.addParcelableListener(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, SecaUserData::class.java, {
                onChange(checkPtpSupported())
            }, forceCallback)
        }

        override val value: Boolean
            get() = checkPtpSupported()

        override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
            setCallbacks(onChange, true)
        }
        override fun listen(onChange: (Boolean) -> Unit) {
            setCallbacks(onChange, false)
        }
    }

    override val isSecaUser: Boolean
        get() = DataMatrix.getParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, SecaUserData::class.java) != null

    override val hasCompletedPaceSurvey: Boolean
        get() = hasCompletedPaceSurvey(
            isGuest = isGuest,
            surveyState = DataMatrix.getParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, SecaUserData::class.java)?.surveyState,
        )

    override val jumpAccessToken: String
        get() = JumpSession.current().accessToken

    override val jumpIdToken: String
        get() = JumpSession.current().idToken

    override fun close() {
        manager.cancelAllListeners()
    }

    override val walletPass = MatrixStringValueListener(manager, DAPIKeys.DATA_USER_CREATE_WALLET_PASS_STRING)
}