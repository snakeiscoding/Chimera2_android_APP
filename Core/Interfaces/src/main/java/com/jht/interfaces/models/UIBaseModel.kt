package com.jht.interfaces.models

import com.jht.enums.FrameType
import com.jht.interfaces.BaseModel
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.StringValueListener

abstract class UIBaseModel : BaseModel {

    /** Provides the current [FrameType] configured by the system */
    abstract val frameType: FrameType

    /** Returns true if system is a debug build. */
    abstract val isDebugBuild: Boolean

    /** Returns true if system is operating in demo mode. */
    abstract val isDemoMode: BoolValueListener

    /** Returns true if product market is full commercial (does not include lite commercial) */
    abstract val isCommercial: Boolean // stick to booleans or go enum?

    /** Returns true if product market is retail */
    abstract val isRetail: Boolean // stick to booleans or go enum?

    /** Provides the current theme being used by the system as a URI string */
    abstract val themeUri: StringValueListener

}