package com.jht.chimera.lib.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.widget.indicator.BusyIndicator
import com.jht.chimera.lib.widget.selectron.SelectronButton

class ProgressBarDialog(
    context: Context
) : Dialog(context) {
    private var iconTopImageView: ImageView? = null
    private var titleTextView: TextView? = null
    private var descriptionTextView: TextView? = null
    private var busyIndicator: BusyIndicator? = null
    private var buttonView: Button? = null
    private var iconBottomImageView: ImageView? = null
    private var closeButton: SelectronButton? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setType(ChimeraEnums.TYPE_SYSTEM_ALERT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.progress_bar_dialog_layout)

        iconTopImageView = findViewById(R.id.dialogIconTop)
        titleTextView = findViewById(R.id.dialogTitleText)
        descriptionTextView = findViewById(R.id.dialogDescriptionText)
        busyIndicator = findViewById(R.id.dialogBusyIndicator)
        buttonView = findViewById(R.id.dialogButton)
        iconBottomImageView = findViewById(R.id.dialogIconBottom)
        closeButton = findViewById(R.id.dialogCloseButton)
        closeButton?.setOnClickListener { dismiss() }
        progressBar = findViewById(R.id.dialogProgressBar)

        // Call setters for these values in-case they were set before onCreate
        // This allows us to set these value before showing the dialog for the first time
        iconTop = iconTop
        title = title
        description = description
        showAsBusy = showAsBusy
        button = button
        iconBottom = iconBottom
        progress = progress
    }

    var iconTop: ProgressBarIcon? = null
        set(icon) {
            field = icon
            if (icon != null){
                iconTopImageView?.setImageResource(icon.imageResource)
                iconTopImageView?.layoutParams?.width = icon.width
                iconTopImageView?.layoutParams?.height = icon.height
                iconTopImageView?.visibility = ImageView.VISIBLE
            }
            else iconTopImageView?.visibility = ImageView.GONE
        }

    var title: String? = null
        set(title) {
            field = title
            if (title != null) {
                titleTextView?.text = title
                titleTextView?.visibility = TextView.VISIBLE
            }
            else titleTextView?.visibility = TextView.GONE
        }

    var description: String? = null
        set(description) {
            field = description
            if (description != null) {
                descriptionTextView?.text = description
                descriptionTextView?.visibility = TextView.VISIBLE
            }
            else descriptionTextView?.visibility = TextView.GONE
        }

    var showAsBusy: Boolean = false
        set(busy) {
            field = busy
            if (busy) {
                busyIndicator?.show()
                busyIndicator?.startProgress()
            } else {
                busyIndicator?.stopProgress()
                busyIndicator?.hide()
            }
        }

    var button: ProgressBarButton? = null
        set(button) {
            field = button
            if (button != null) {
                buttonView?.text = button.text
                buttonView?.isAllCaps = false
                buttonView?.setOnClickListener(button.onClick)
                buttonView?.setBackgroundColor(button.color)
                buttonView?.visibility = Button.VISIBLE
            }
            else buttonView?.visibility = Button.GONE
        }

    var iconBottom: ProgressBarIcon? = null
        set(icon) {
            field = icon
            if (icon != null){
                iconBottomImageView?.setImageResource(icon.imageResource)
                iconBottomImageView?.layoutParams?.width = icon.width
                iconBottomImageView?.layoutParams?.height = icon.height
                iconBottomImageView?.visibility = ImageView.VISIBLE
            }
            else iconBottomImageView?.visibility = ImageView.GONE
        }

    var progress: Int? = null
        set(progress) {
            field = progress
            if (progress != null) {
                progressBar?.progress = progress
                progressBar?.visibility = ProgressBar.VISIBLE
            }
            else progressBar?.visibility = ProgressBar.GONE
        }

    data class ProgressBarButton(
        val text: String = "",
        val onClick: (View) -> Unit = {},
        val color: Int = Color.parseColor("#446278")
    )

    data class ProgressBarIcon(
        val imageResource: Int,
        val width: Int,
        val height: Int
    )
}

