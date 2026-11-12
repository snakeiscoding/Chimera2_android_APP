package com.jht.interfaces.models

import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.enums.FrameType
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixBoolValueListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.interfaces.StringValueListener

class MatrixUIBaseModel(
) : UIBaseModel() {
    private val manager: CallbackManager = CallbackManager()

    override val isDebugBuild: Boolean
        get() = com.jht.chimera.BuildConfig.DEBUG

    override val isDemoMode: BoolValueListener = MatrixBoolValueListener(manager, SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)

    override val isRetail: Boolean = com.jht.chimera.BuildConfig.MARKET_RETAIL

    override val isCommercial: Boolean = com.jht.chimera.BuildConfig.MARKET_COMMERCIAL

    override val frameType: FrameType
        get() {
            val name: String = FrameConfigManager.getInstance().frameConfig.machineClassType.name
            return FrameType.fromString(name)
        }

    override val themeUri: StringValueListener = MatrixStringValueListener(manager, SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING)

    override fun close() {
        manager.cancelAllListeners()
    }
}