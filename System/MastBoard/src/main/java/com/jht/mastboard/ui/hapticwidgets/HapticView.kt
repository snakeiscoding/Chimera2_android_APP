@file:JvmName("HapticView")
package com.jht.mastboard.ui.hapticwidgets

import android.content.Context
import android.util.AttributeSet
import android.view.View

class HapticView: View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    fun setOnClickListener(listener: HapticOnClickListener) {
        super.setOnClickListener(listener)
    }

    @Deprecated("This will throw an exception", ReplaceWith("setOnClickListener(listener: HapticOnClickListener)"))
    override fun setOnClickListener(l: OnClickListener?) {
        throw RuntimeException("Unsupported")
    }
}