package com.jht.interfaces.models

import com.jht.chimera.lib.common.config.ConsoleType
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.clubcom.interfaces.ClubComModel
import com.jht.clubcom.interfaces.ClubComOptions
import com.jht.clubcom.service.MODEL_CXV
import com.jht.clubcom.service.MODEL_SEVENX
import com.jht.clubcom.service.MODEL_TOUCH
import com.jht.clubcom.service.MODEL_TOUCHXL
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixBoolValueListener
import com.jht.interfaces.MatrixEventListener
import com.jht.interfaces.MatrixParcelableVariableListener
import com.jht.interfaces.ParcelableVariableListener

class MatrixClubComModel : ClubComModel() {
    private val manager: CallbackManager = CallbackManager()

    override val urlModelString: String
        get() = when (ProductConfig.getConsoleType()) {
            ConsoleType.chimera_16, ConsoleType.chimera2_16 -> MODEL_TOUCH
            ConsoleType.chimera_22, ConsoleType.chimera2_22 -> MODEL_TOUCHXL
            ConsoleType.athena -> MODEL_CXV
            else -> MODEL_SEVENX
        }

    override val isEnabled = MatrixBoolValueListener(manager, SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL)

    override fun startSession() {
        DataMatrix.sendEvent(ClubComKeys.ACTION_CLUB_COM_START_SESSION)
    }
    override val onStartSession = MatrixEventListener(manager, ClubComKeys.ACTION_CLUB_COM_START_SESSION)

    override fun stopSession() {
        DataMatrix.sendEvent(ClubComKeys.ACTION_CLUB_COM_END_SESSION)
    }
    override val onStopSession = MatrixEventListener(manager, ClubComKeys.ACTION_CLUB_COM_END_SESSION)

    override val options: ParcelableVariableListener<ClubComOptions> by lazy {
        MatrixParcelableVariableListener(
            manager,
            ClubComKeys.DATA_CLUB_COM_OPTIONS_PARCELABLE,
            ClubComOptions(),
            ClubComOptions::class.java
        )
    }

    override fun close() {
        manager.cancelAllListeners()
    }
}
