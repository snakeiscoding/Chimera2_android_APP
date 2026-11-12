@file:JvmName("ErrorLogSystemInterface")
package com.jht.liberror.systeminterface

import com.jht.chimera.lib.common.SystemUtils
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.liberror.ErrorLogList
import com.jht.liberror.IErrorLogSystemInterface

/**
 * The Error Log System Interface is consumed by the Error Log library. This is included by the
 * library using reflection.
 *
 * @author Andy Rosene
 */
@Suppress("unused")
class ErrorLogSystemInterface: IErrorLogSystemInterface {
    override val inDemoMode: Boolean get() = SystemUtils.inDemoMode
    override val isMachineSetup: Boolean get() = SystemUtils.isMachineSetup
    override val ignoreAllErrors: Boolean get() = SystemUtils.ignoreAllErrors
    override val ignoreBLevelErrors: Boolean get() = SystemUtils.ignoreBLevelErrors
    override val ignoreInclineErrors: Boolean get() = SystemUtils.ignoreInclineErrors
    override val ignoreNoBatteryErrors: Boolean get() = SystemUtils.ignoreNoBatteryErrors
    override val ignoreNoResponseOfMotorSpeedSensorErrors: Boolean get() = SystemUtils.ignoreNoResponseOfMotorSpeedSensorErrors
     /**
      * Indicates whether the system is in an idle state where all errors are ignored.
      * This is mapped to SystemUtils.inIdleState for compatibility.
      */
    override val inIdleState: Boolean get() = SystemUtils.inIdleState
    override val needsErrorCodeMapping: Boolean get() = SystemUtils.needsErrorCodeMapping

    override fun updateErrorLog(errorLog: ErrorLogList) {
        DataMatrix.putParcelable(LoggerKeys.DATA_ERROR_LOG_LIST_PARCELABLE, errorLog)
    }

    override fun updateLastLoggedError(value: Int) {
        DataMatrix.putInt(HALKeys.ACTION_ERROR_CODE_INT, value)
    }

    override fun updateLastLoggedLCBError(value: Int) {
        DataMatrix.putInt(HALKeys.DATA_LCB_ERROR_CODE_INT, value)
        DataMatrix.putInt(HALKeys.ACTION_ERROR_CODE_INT, value)
    }

    override fun reportErrorCode(value: Int) {
        DataMatrix.putInt(HALKeys.ACTION_REPORT_ERROR_CODE_INT, value)
    }
}