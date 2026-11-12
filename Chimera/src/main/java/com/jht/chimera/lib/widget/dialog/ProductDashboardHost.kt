package com.jht.chimera.lib.widget.dialog

import androidx.annotation.MainThread
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.mastboard.data.UiEventKeys
import com.jht.widgets.ProductDashboardDialog

/** A chimera specific host for ProductDashboard */
object ProductDashboardHost {
    private var dialog: ProductDashboardDialog? = null
    @JvmStatic
    val isShowing: Boolean get() = dialog?.isShowing == true

    @JvmStatic
    @MainThread
    fun show() {
        if (isShowing) return
        val context = ContextUtil.getApplicationContext() ?: return
        ProductDashboardDialog(context).also { d ->
            dialog = d
            d.setOnShowListener { onOpened() }
            d.setOnDismissListener { dialog = null; onClosed() }
            d.show()
        }
    }
    @JvmStatic
    @MainThread
    fun dismiss() { dialog?.dismiss() }

    private fun onOpened() {
        DataMatrix.putBoolean(WorkoutKeys.DATA_SETTINGS_DIALOG_OPEN_BOOL, true)
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.OPEN_MATRIX_DASHBOARD.name)
    }
    private fun onClosed() {
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.CLOSE_MATRIX_DASHBOARD.name)
        DataMatrix.putBoolean(WorkoutKeys.DATA_SETTINGS_DIALOG_OPEN_BOOL, false)
    }
}
