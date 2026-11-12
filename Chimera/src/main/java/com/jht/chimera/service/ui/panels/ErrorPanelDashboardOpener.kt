@file:JvmName("ErrorPanelDashboardOpener")

package com.jht.chimera.service.ui.panels

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.jht.androidcommonalgorithms.timer.JHTTimer
import com.jht.chimera.R
import com.jht.chimera.lib.widget.dialog.ProductDashboardHost

/**
 * The custom dashboard opener used by the error panel. This contains all the logic specifically
 * related to ensuring the matrix dashboard can be opened from the error panel.
 */
class ErrorPanelDashboardOpener @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    /**
     * Synchronization lock responsible for ensuring the timer isn't being modified by multiple
     * sources.
     */
    private val timerLock = Object()

    /**
     * Whether the top section is being touched.
     */
    private var touchingTop = false

    /**
     * Whether the bottom section is being touched.
     */
    private var touchingBottom = false

    /**
     * Whether the center section is being touched.
     */
    private var touchingCenter = false

    /**
     * The touch timer if started, else `null`.
     */
    private var touchTimer: JHTTimer? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.estop_dashboard_opener_layout, this, true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Initialize using center touching.
        findViewById<View>(R.id.hidden_view_top_center).setOnTouchListener { _, event ->
            val actionMasked = event.actionMasked

            if (actionMasked == MotionEvent.ACTION_DOWN) {
                touchingCenter = true
                updateTimer()
            } else if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                touchingCenter = false
                updateTimer()
            }

            true
        }

        // Initialize using split touching.
        findViewById<View>(R.id.hidden_view_top).setOnTouchListener { _, event ->
            val actionMasked = event.actionMasked

            if (actionMasked == MotionEvent.ACTION_DOWN) {
                touchingTop = true
                updateTimer()
            } else if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                touchingTop = false
                updateTimer()
            }

            true
        }

        findViewById<View>(R.id.hidden_view_bottom).setOnTouchListener { _, event ->
            val actionMasked = event.actionMasked

            if (actionMasked == MotionEvent.ACTION_DOWN) {
                touchingBottom = true
                updateTimer()
            } else if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                touchingBottom = false
                updateTimer()
            }

            true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTimer()
    }

    /**
     * This is responsible for ensuring the [touchTimer] is started (using [startTimer]) if either
     * [touchingCenter] is `true` or both [touchingTop] and [touchingBottom] are `true`. If these
     * conditions are not met, this is responsible for ensuring the [touchTimer] is stopped (using
     * [stopTimer]). **Note,** calling this method excessively can negatively impact performance.
     * This method *should* only be called when changes occur to [touchingTop], [touchingBottom],
     * and [touchingCenter].
     *
     * @see startTimer
     * @see stopTimer
     */
    private fun updateTimer() {
        synchronized(this) {
            if (touchingCenter || (touchingTop && touchingBottom)) {
                startTimer()
            } else {
                stopTimer()
            }
        }
    }

    /**
     * Start the [touchTimer] if not already started (identified by checking whether the timer is
     * `null`). If the [touchingCenter], the timer will be started using [CENTER_TOUCH_TIME_MS];
     * otherwise, the timer will be started using [SPLIT_TOUCH_TIME_MS].
     */
    private fun startTimer() {
        synchronized(timerLock) {
            if (touchTimer == null) {
                touchTimer = JHTTimer(if (touchingCenter) CENTER_TOUCH_TIME_MS else SPLIT_TOUCH_TIME_MS) {
                    (handler ?: Handler(Looper.getMainLooper())).post {
                        ProductDashboardHost.show()
                    }
                }.also {
                    it.start()
                }
            }
        }
    }

    /**
     * Stop the [touchTimer] if it is started. **Note,** this method is also responsible for setting
     * [touchTimer] to `null` to ensure [startTimer] can properly initialize the timer the next time
     * it is called.
     */
    private fun stopTimer() {
        synchronized(timerLock) {
            touchTimer?.stop()
            touchTimer = null
        }
    }

    companion object {
        /**
         * The time required before displaying the dashboard when activating by touching the top and
         * bottom halves of the screen.
         */
        private const val SPLIT_TOUCH_TIME_MS = 5000

        /**
         * The time required before displaying the dashboard when activating by touching the top
         * center portion of the screen. This activation time is longer than split touch to ensure
         * it isn't being opened accidentally now that only one touch point is required.
         */
        private const val CENTER_TOUCH_TIME_MS = 8000
    }
}