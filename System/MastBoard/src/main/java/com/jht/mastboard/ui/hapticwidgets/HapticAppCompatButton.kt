@file:JvmName("HapticAppCompatButton")
package com.jht.mastboard.ui.hapticwidgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class HapticAppCompatButton: AppCompatButton {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    fun setOnClickListener(listener: HapticOnClickListener) {
        super.setOnClickListener(listener)
    }

    @Deprecated("Not supported", ReplaceWith("setOnClickListener(listener: HapticOnClickListener)"))
    override fun setOnClickListener(l: OnClickListener?) {
        throw RuntimeException("Unsupported")
    }
}