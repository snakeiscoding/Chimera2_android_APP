@file:JvmName("HapticAppCompatImageButton")
package com.jht.mastboard.ui.hapticwidgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * fixme: not fnished porting yet.
 */
class HapticAppCompatImageButton: AppCompatImageView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    fun setOnClickListener(listener: HapticOnClickListener) {
        super.setOnClickListener(listener)
    }

    @Deprecated("This will throw an exception", ReplaceWith("setOnClickListener(listener: HapticOnClickListener)"))
    override fun setOnClickListener(listener: OnClickListener?) {
        throw RuntimeException("This is no longer supported")
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setOnTouchListener(listener: HapticOnTouchListener) {
        super.setOnTouchListener(listener)
    }

    @Deprecated("This will throw and exception", ReplaceWith("setOnTouchListener(listener: HapticOnTouchListener)"))
    override fun setOnTouchListener(l: OnTouchListener?) {
        throw RuntimeException("This is no longer supported")
    }
}