package com.jht.chimera.lib.widget.button

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.view.setMargins
import com.jht.chimera.R
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.widget.dialog.ProgressBarDialog
import com.jht.concierge.service.ConciergeEvent
import com.jht.datamatrixlib.DataMatrix
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConciergeButton(contextParam: Context, attrs: AttributeSet?) : LinearLayout(contextParam, attrs) {
    private val btnImageView: ImageView
    private val btnTextView: TextView
    private val layout: LinearLayout
    private val dialog: ProgressBarDialog = ProgressBarDialog(context)
    private val progressBarAnimation: ValueAnimator = ValueAnimator.ofInt(100, 0)
    private var mDataMatrix: DataMatrix = DataMatrix()

    init {
        // inflate layout and find views
        inflate(context, R.layout.concierge_button_layout, this)
        btnImageView = findViewById(R.id.concierge_btn_icon)
        btnTextView = findViewById(R.id.concierge_btn_text)
        layout = findViewById(R.id.concierge_btn_layout)

        // setup button onClick handler
        isClickable = true
        setOnClickListener { dialog.show() }

        // Setup progress bar animation and handlers
        progressBarAnimation.duration = 3000
        progressBarAnimation.interpolator = LinearInterpolator()
        progressBarAnimation.addUpdateListener{ onProgressBarAnimationUpdate() }
        progressBarAnimation.doOnEnd {
            if (dialog.progress == 0) DataMatrix.putString(MainAppKeys.ACTION_CREATE_CONCIERGE_REQUEST_WITH_OPTIONAL_REQUEST_ID_STRING, "")
        }

        // Setup default state in case DM value is null
        if (DataMatrix.getParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent::class.java) == null){
            onRequestClosed()
        }
    }

    /**
     * Hides / Shows the text of the ConciergeButton
     * @param isHidden boolean indicating whether or not you want to hide the button's text
     */
    fun setHideText(isHidden: Boolean) {
        val iconSize: Int = dpToPx( if (isHidden) 30 else 25 )
        val marginSize: Int = dpToPx(if (isHidden) 24 else 0)
        val isTextHidden: Int = if (isHidden) GONE else VISIBLE
        val marginParams: MarginLayoutParams = layout.layoutParams as MarginLayoutParams
        btnImageView.layoutParams.height = iconSize
        btnImageView.layoutParams
        btnTextView.visibility = isTextHidden
        marginParams.setMargins(marginSize)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDataMatrix = DataMatrix()

        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL, {
            visibility = if (it) VISIBLE else GONE
        }, true)

        mDataMatrix.createParcelableAdapter(
            MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE,
            ConciergeEvent::class.java
        ) { conciergeEvent ->
            progressBarAnimation.cancel()
            when (conciergeEvent) {
                is ConciergeEvent.Loading -> onLoading(conciergeEvent)
                is ConciergeEvent.RequestClosed -> onRequestClosed()
                is ConciergeEvent.RequestOpened -> onRequestOpened()
                is ConciergeEvent.RequestAcknowledged -> onRequestAcknowledged()
                is ConciergeEvent.RequestCompleted -> onRequestComplete()
                is ConciergeEvent.RequestCancelled -> onRequestCancelled()
                is ConciergeEvent.Error -> onError(conciergeEvent)
                else -> {}
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressBarAnimation.cancel()
        dialog.dismiss()
        mDataMatrix.finish()
    }

    private fun onRequestClosed(){
        // Dismiss concierge dialog in case it is open
        dialog.dismiss()

        // Update concierge button appearance
        btnImageView.setColorFilter(Color.parseColor("#ffffff"))
        btnTextView.text = resources.getString(R.string.request_help_tc)

        // Update concierge dialog appearance and functionality
        dialog.title = context.getString(R.string.request_help_tc)
        dialog.description = context.getString(R.string.staff_will_stop_by_after_request_pg)
        dialog.progress = 100
        dialog.showAsBusy = false
        dialog.iconBottom = null
        dialog.iconTop = ProgressBarDialog.ProgressBarIcon(
            imageResource = R.drawable.ic_concierge,
            width = dpToPx(60),
            height = dpToPx(60)
        )
        dialog.button = ProgressBarDialog.ProgressBarButton(
            text = context.getString(R.string.cancel_request_tc),
            onClick = { dialog.dismiss() }
        )
        dialog.setOnShowListener { progressBarAnimation.start() }
        dialog.setOnDismissListener { progressBarAnimation.cancel() }
    }

    private fun onRequestCancelled(){
        // Not shown in the old concierge UI flow
    }


    private fun onRequestOpened() {
        // Update concierge button appearance
        btnImageView.setColorFilter(Color.parseColor("#f0b323"))
        btnTextView.text = resources.getString(R.string.help_requested_tc)

        // Update concierge dialog appearance and functionality
        dialog.title = "${context.getString(R.string.help_requested_tc)}!"
        dialog.description = context.getString(R.string.staff_will_stop_by_once_available_pg)
        dialog.progress = null
        dialog.showAsBusy = false
        dialog.iconBottom = null
        dialog.iconTop = ProgressBarDialog.ProgressBarIcon(
            imageResource = R.drawable.ic_concierge,
            width = dpToPx(60),
            height = dpToPx(60)
        )
        dialog.button = ProgressBarDialog.ProgressBarButton(
            text = context.getString(R.string.cancel_request_tc),
            onClick = {
                DataMatrix.sendEvent(MainAppKeys.ACTION_CANCEL_CONCIERGE_REQUEST)
                DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestClosed)
            }
        )
        dialog.setOnShowListener {}
        dialog.setOnDismissListener {}
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onRequestAcknowledged() {
        // Update concierge button appearance
        btnImageView.setColorFilter(Color.parseColor("#f0b323"))
        btnTextView.text = resources.getString(R.string.help_is_on_the_way_pg)

        // Update concierge dialog appearance and functionality
        dialog.iconTop = null
        dialog.title = "${context.getString(R.string.help_is_on_the_way_pg)}!"
        dialog.description = "${context.getString(R.string.staff_will_stop_by_soon_pg)}\n"
        dialog.progress = null
        dialog.showAsBusy = false
        dialog.iconBottom = null
        dialog.button = ProgressBarDialog.ProgressBarButton(
            text = context.getString(R.string.mark_request_complete_tc),
            color = Color.parseColor("#3F9C47"),
            onClick = {
                DataMatrix.sendEvent(MainAppKeys.ACTION_CANCEL_CONCIERGE_REQUEST)
                DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestCompleted)
            }
        )
        dialog.setOnShowListener {}
        dialog.setOnDismissListener {}
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onRequestComplete() {
        btnImageView.setColorFilter(Color.parseColor("#3F9C47"))
        btnTextView.text = resources.getString(R.string.request_complete)
        dialog.title = "${context.getString(R.string.the_request_is_complete_tc)}!\n"
        dialog.description = null
        dialog.button = null
        dialog.iconBottom = ProgressBarDialog.ProgressBarIcon(
            imageResource = R.drawable.ic_confirm,
            width = dpToPx(100),
            height = dpToPx(100)
        )
        GlobalScope.launch {
            delay(5000)
            DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestClosed)
        }
    }

    private fun onLoading(loadingEvent: ConciergeEvent.Loading) {
        dialog.title = loadingEvent.title
        dialog.description = null
        dialog.button = null
        dialog.progress = null
        dialog.showAsBusy = true
        dialog.iconBottom = null
        dialog.iconTop = ProgressBarDialog.ProgressBarIcon(
            imageResource = R.drawable.ic_concierge,
            width = dpToPx(60),
            height = dpToPx(60)
        )
        dialog.setOnShowListener { dialog.showAsBusy = true }
        dialog.setOnDismissListener {}
    }

    private fun onError(errorEvent: ConciergeEvent.Error) {
        dialog.title = context.getString(R.string.error_occurred_tc)
        dialog.description = context.getString(R.string.error_occurred_try_again_later_pg)
        dialog.button = null
        dialog.progress = null
        dialog.showAsBusy = false
        dialog.iconBottom = null
        dialog.iconTop = ProgressBarDialog.ProgressBarIcon(
            imageResource = R.drawable.ic_wifi_lost,
            width = dpToPx(60),
            height = dpToPx(60)
        )
        dialog.setOnShowListener {}
        dialog.setOnDismissListener { DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, errorEvent.previousState) }
        dialog.show()
    }

    private fun onProgressBarAnimationUpdate() {
        val currentProgress = progressBarAnimation.animatedValue as Int
        dialog.progress = currentProgress
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }
}