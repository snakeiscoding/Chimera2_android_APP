package com.jht.interfaces

import android.os.Parcelable
import com.jht.datamatrixlib.adapter.BoolAdapter
import com.jht.datamatrixlib.adapter.EventAdapter
import com.jht.datamatrixlib.adapter.FloatAdapter
import com.jht.datamatrixlib.adapter.IAdapter
import com.jht.datamatrixlib.adapter.IntAdapter
import com.jht.datamatrixlib.adapter.ParcelableAdapter
import com.jht.datamatrixlib.adapter.StringAdapter

/**
 * This manages the DataMatrix listeners like com.jht.datamatrixlib.DataMatrix class
 * but it allows different listeners to listen to the same key. The DataMatrix class
 * only allows one listener for a key. This simplify the case where you have create
 * like a distance primary and distance secondary variable with callbacks but those
 * map to the same key.
 */
class CallbackManager {
    private val listenerList = mutableListOf<IAdapter>()
    val cancelAllEventCallbacks = mutableSetOf<() -> Unit>()

    /**
     * addListener - manage our custom map of object and listener
     * since the helper classes only allow 1 listener per key
     */
    private fun addListener(listener: IAdapter): IAdapter {
        listenerList.add(listener)
        return listener // return the listener for convenience chaining
    }

    /** If you saved a reference to the listener that was added, you can remove it without altering the rest of the listeners */
    fun removeListener(listener: IAdapter) {
        listener.deregister()
        listenerList.remove(listener)
    }

    /**
     * cancelAllListeners - deregister all listeners then clear map
     */
    fun cancelAllListeners() {
        for (listener in listenerList) {
            listener.deregister()
        }
        listenerList.clear()
        cancelAllEventCallbacks.forEach { it() }
    }

    fun addEventListener(key: Any, onEvent: () -> Unit): IAdapter {
        return addListener(EventAdapter(key.toString(), { onEvent() }, false))
    }

    fun addBoolListener(key: Any, onChange: (Boolean) -> Unit, forceImmediateCallback: Boolean): IAdapter {
        return addListener(
            BoolAdapter(
                key.toString(),
                { if (it != null) onChange(it as Boolean) },
                forceImmediateCallback
            )
        )
    }

    fun addIntListener(key: Any, onChange: (Int) -> Unit, forceImmediateCallback: Boolean): IAdapter {
        return addListener(
            IntAdapter(
                key.toString(),
                { if (it != null) onChange(it as Int) },
                forceImmediateCallback
            )
        )
    }

    fun addFloatListener(key: Any, onChange: (Float) -> Unit, forceImmediateCallback: Boolean): IAdapter {
        return addListener(
            FloatAdapter(
                key.toString(),
                { if (it != null) onChange(it as Float) },
                forceImmediateCallback
            )
        )
    }

    fun addStringListener(key: Any, onChange: (String) -> Unit, forceImmediateCallback: Boolean): IAdapter {
        return addListener(
            StringAdapter(
                key.toString(),
                { if (it != null) onChange(it as String) },
                forceImmediateCallback
            )
        )
    }

    fun <T : Parcelable> addParcelableListener(key: Any, parcelableClass: Class<T>, onChange: (T) -> Unit, forceImmediateCallback: Boolean): IAdapter {
        return addListener(
            ParcelableAdapter(
                key.toString(),
                parcelableClass,
                { if (it != null) onChange(it) },
                forceImmediateCallback)
        )
    }
}
