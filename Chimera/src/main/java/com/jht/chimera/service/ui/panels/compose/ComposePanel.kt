package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.service.ui.panels.ExtraBarOptions
import com.jht.chimera.service.ui.panels.IPanel
import com.jht.chimera.service.ui.panels.WorkoutPanelMode
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.defaultDynamicTheme
import com.jht.widgets.AbstractStandAloneComposeView


/**
    This is a base class for compose panels.
    Need to override Content() with the composable and getPanelParams() for window parameters.
 */
abstract class ComposePanel(context: Context) : AbstractStandAloneComposeView(context), IPanel {
    protected open var mPanelMode = WorkoutPanelMode.NORMAL
    protected val panelOffsetPixels = mutableStateOf(Offset.Zero) // in pixels (not DP)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener) // we need panel add and update, this seems to cover both
    }
    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        super.onDetachedFromWindow()
    }

    override fun panelAdded() {
        // think shouldCreateCompositionOnAttachedToWindow is true by default, should we do this anyway just in case?
        /*if (isAttachedToWindow) {
            createComposition()
        }*/
        //lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun panelUpdated() {
        // nothing at this level
    }

    override fun panelRemoved() {
        //lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        disposeComposition()
    }

    override fun dispose() {
        // any clean up not handled by panelRemoved
    }

    /*protected fun refresh() {
        disposeComposition()
        if (isAttachedToWindow) {
            createComposition()
        }
    }*/

    override fun getView(): View {
        return this
    }

    override fun getDebugName(): String {
        return javaClass.name
    }

    override fun setPanelMode(mode: WorkoutPanelMode): Boolean {
        if (mPanelMode == mode) return false // return false if no change
        mPanelMode = mode
        return true
    }

    protected val theme = mutableStateOf(defaultDynamicTheme)
    fun updateTheme(themeData: DynamicThemeData) {theme.value = themeData}
    @Composable
    final override fun Content() {
        DynamicTheme(theme.value) {
            ThemedContent()
        }
    }

    @Composable
    abstract fun ThemedContent() // panel fills this in and it automatically gets the theme scoped

    // some common panel functions to call from manager (would it be better on IPanel or some other interface/base?)
    open fun setFullscreen(isFull: Boolean) {} // if panel needs to respond to fullscreen change
    open fun setAppBar(options: ExtraBarOptions?) {} // if panel needs to deal with app extra options
    open fun setPanelOverrides(panelOverrides: Bundle) {}
    open fun clearPanelOverrides(panelOverrides: Bundle) {}
    open fun onWorkoutReset() {} // signal to panel that another workout is launching so it can reset anything it might need
    open fun onPendingBehaviorChange(isPending: Boolean) {} // Whether there's a pending behavior change for the panels

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        // cannot seem to find a way to get this from OS directly, so basically recreating the OS mod layout math to figure out screen offset
        val params = panelParams
        val h: Int = view.height
        val w: Int = view.width
        var x: Int = if (w >= RealScreen.widthPixels) 0 else params.x
        var y: Int = if (h >= RealScreen.heightPixels) 0 else params.y
        val leftoverH = if (RealScreen.heightPixels > h) RealScreen.heightPixels - h else 0
        val leftoverW = if (RealScreen.widthPixels > w) RealScreen.widthPixels - w else 0
        val layoutDirection = view.layoutDirection
        val absoluteGravity = Gravity.getAbsoluteGravity(params.gravity, layoutDirection)
        val verticalGravity = params.gravity and Gravity.VERTICAL_GRAVITY_MASK
        when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> x += leftoverW / 2
            Gravity.RIGHT -> x = RealScreen.widthPixels - w - x // offset actual moves to the left
            Gravity.LEFT -> {}
        }
        when (verticalGravity) {
            Gravity.TOP -> {}
            Gravity.CENTER_VERTICAL -> y += leftoverH / 2
            Gravity.BOTTOM -> y = RealScreen.heightPixels - h - y // mod actually moves the other way
        }
        panelOffsetPixels.value = Offset(x.toFloat(), y.toFloat())
    }

}


