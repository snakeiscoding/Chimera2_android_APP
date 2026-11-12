@file:JvmName("LCDPresentation")
package com.jht.mastboard.ui

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.jht.mastboard.lib.PanelSide
import com.jht.mastboard.lib.TouchData
import com.jht.mastboard.R
import com.jht.mastboard.interfaces.MastboardModel

/**
 * Updated implementation of the [LCDPresentation]. This is designed to better handle state (hopefully).
 *
 * fixme: I need to be able to apply the layout elsewhere...
 */
class LCDPresentation(private val context: Context, private val mastboardModel: MastboardModel, display: Display, val panelSide: PanelSide) : Presentation(context, display) {
    private val mHandler = Handler(Looper.getMainLooper())

    /**
     * The resource ID for what is currently displayed.
     */
    var currResourceId = -1
        private set

    /**
     * The resource ID populated when removing to reapply if necessary.
     *
     * fixme: This needs to be deprecated.
     */
    private var tempResourceId = -1

    private lateinit var mRootView: LCDPanelRootView

    private var mLCDDebugView: LCDDebugView? = null // fixme: Does this need to be moved elsewhere.

    /**
     * fixme: This should just be using [getLayoutInflater] instead.
     */
    private val inflater: LayoutInflater by lazy { context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }

    private var mLastX = -1

    private var mLastY = -1

    private var mLastMotionEvent: MotionEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // fixme: I don't think that this actually needs to be a custom element. This could just be a basic element.
        setContentView(R.layout.handgrip_panel_root)
        mRootView = findViewById(R.id.root_view)
    }

    override fun onStart() {
        super.onStart()
        mRootView.init(panelSide)
        mLCDDebugView = LCDDebugView(context, mRootView)
    }

    override fun onStop() {
        super.onStop()
        mLCDDebugView = null
    }

    private fun updateView(view: View) {
        if (!isShowing)
            show()

        mRootView.removeAllViews()
        mRootView.addView(view, ViewGroup.LayoutParams(WIDTH, HEIGHT))
    }

    /**
     * Call to restore the previous LCD panel state. If there isn't a cached value, this request is
     * ignored.
     *
     * fixme: This needs to be deprecated.
     */
    @Synchronized
    fun restoreCachedLCDPanel() {
        if (tempResourceId != -1) {
            currResourceId = tempResourceId
            tempResourceId = -1
            mRootView.init(panelSide)
            updateLCDPanelLayout()
        }
    }

    /**
     * Update the LCD panel using the given panel resource id. If this is -1 or matches the existing
     * value, the request is ignored.
     *
     * fixme: I'm not sure this is implemented correctly.
     */
    @Synchronized
    fun updateLCDPanel(panelResourceId: Int) {
        if (panelResourceId == -1 || panelResourceId == currResourceId)
            return

        currResourceId = panelResourceId
        updateLCDPanelLayout()
    }

    /**
     * fixme: This should ignore the request if currResourceId == -1. I just don't think it matters
     *  right now because this is called in a very restricted manner.
     */
    private fun updateLCDPanelLayout() {
        val view1 = inflater.inflate(currResourceId, null)
        updateView(view1)
    }

    fun removeView(isReset: Boolean) {
        if (!isShowing)
            return

        mRootView.removeAllViews()

        if (isReset) {
            tempResourceId = currResourceId
            mRootView.closeRootView()
        }
    }

    /**
     * fixme: I don't think this is actually needed. This seems to be a fix for an invalid
     *  implementation.
     */
    val currentView: View? @JvmName("getCurrentView") get() = mRootView.currentView

    fun showDebugView() {
        if (!isShowing)
            show()

        mLCDDebugView?.show(panelSide) ?: error("LCDPresentation: 145")
        mRootView.setDebugView(mLCDDebugView?.debugView ?: error("LCDPresentation: 146"))
    }

    fun hideDebugView() {
        if (!isShowing) return

        mRootView.setDebugView(null)
        mLCDDebugView?.hide() ?: error("LCDPresentation: 153")
    }

    /**
     * fixme: this is the only thing I need to ensure stays in this implementation.
     */
    fun updateTouch(touchData: TouchData) : Boolean {
        var touchActivity = false
        if (mLastX == -1)
            mLastX = touchData.x

        if (mLastY == -1)
            mLastY = touchData.y

        if (touchData.gestureID == TouchData.GestureID.NoGesture)
            return false

        if (!isShowing)
            return false

        var eventTime: Long = SystemClock.uptimeMillis()
        val motionEvent: MotionEvent = MotionEvent.obtain(touchData.lastTouchDownTime, eventTime, touchData.mEventAction, touchData.x.toFloat(), touchData.y.toFloat(), 0)

        // It appears that adding history data helps with the velocity tracker to support sling/swipe
        // so we interpolate one extra history data for move actions in the middle
        if (ENABLE_GESTURES && motionEvent.action == MotionEvent.ACTION_MOVE) {
            // Only allow swipe motion at bottom section of screen
            if (motionEvent.y <= 50) {
                try {
                    mLastMotionEvent?.let { mLastMotionEvent ->
                        val timeInBetween: Long = mLastMotionEvent.eventTime + (eventTime - mLastMotionEvent.eventTime) / 2
                        val xInBetween: Float = mLastMotionEvent.x + (touchData.x - mLastMotionEvent.x) / 2f
                        val yInBetween: Float = mLastMotionEvent.y + (touchData.y - mLastMotionEvent.y) / 2f
                        motionEvent.addBatch(timeInBetween, xInBetween, yInBetween, 1f, 1f, 0)
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // fixme: this should be a Log instead.
                }
            }
        }

        // only dispatch new touch event.
        if (!(mLastMotionEvent != null && motionEvent.action == mLastMotionEvent?.action && motionEvent.x == mLastMotionEvent?.x && motionEvent.y == mLastMotionEvent?.y)) {
            touchActivity = true
            mHandler.post {
                mRootView.dispatchTouchEvent(motionEvent)

                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "dispatchTouchEvent ACTION_DOWN")
                    mastboardModel.onScreenActivity()
                }
            }

            SystemClock.sleep(10)
        }

        mLastMotionEvent?.recycle()
        mLastMotionEvent = motionEvent
        return touchActivity
    }

    companion object {
        private const val TAG = "LCDPresentation"
        private const val ENABLE_GESTURES = false
        const val WIDTH = 240
        const val HEIGHT = 320

        @Suppress("unused")
        const val DENSITY = 160
    }
}