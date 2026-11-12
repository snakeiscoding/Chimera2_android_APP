package com.jht.interfaces;

import android.os.Parcelable
import com.jht.datamatrixlib.DataMatrix

//region Action/event listeners allow listen

class MatrixEventListener(private val manager: CallbackManager, private val key: Any) :
    EventListener {
    override fun listen(onEvent: () -> Unit) {
        manager.addEventListener(key, onEvent)
    }
}

class MatrixBoolListener(private val manager: CallbackManager, private val key: Any) :
    BoolListener {
    override fun listen(onChange: (Boolean) -> Unit) {
        manager.addBoolListener(key, onChange, false)
    }
}

class MatrixIntListener(private val manager: CallbackManager, private val key: Any) : IntListener {
    override fun listen(onChange: (Int) -> Unit) {
        manager.addIntListener(key, onChange, false)
    }
}

class MatrixFloatListener(private val manager: CallbackManager, private val key: Any) :
    FloatListener {
    override fun listen(onChange: (Float) -> Unit) {
        manager.addFloatListener(key, onChange, false)
    }
}

class MatrixStringListener(private val manager: CallbackManager, private val key: Any) :
    StringListener {
    override fun listen(onChange: (String) -> Unit) {
        manager.addStringListener(key, onChange, false)
    }
}

//endregion Action/event listeners


//region Value listeners allow get/listen

class MatrixBoolValueListener(private val manager: CallbackManager, private val key: Any) :
    BoolValueListener {
    override val value: Boolean
        get() = DataMatrix.getBoolean(key)

    override fun listen(onChange: (Boolean) -> Unit) {
        manager.addBoolListener(key, onChange, false)
    }

    override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
        manager.addBoolListener(key, onChange, true)
    }
}

/** Takes an integer key and converts it to a boolean listener to reduce need for enums in the model */
class MatrixBoolStateValueListener(
    private val manager: CallbackManager,
    private val key: Any,
    private val state: Int
) : BoolValueListener {
    override val value: Boolean
        get() = DataMatrix.getInt(key) == state

    override fun listen(onChange: (Boolean) -> Unit) {
        manager.addIntListener(key, { onChange(it == state) }, false)
    }

    override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
        manager.addIntListener(key, { onChange(it == state) }, true)
    }
}

/** Takes an integer key and sends an event when it changes to a specific value */
class MatrixEventStateValueListener(
    private val manager: CallbackManager,
    private val key: Any,
    private val state: Int,
    private val requiredPreviousState: Int = -1
) : EventListener {
    private var previousState: Int = -1
    init {
        if (requiredPreviousState != -1) {
            previousState = DataMatrix.getInt(key)
        }
    }

    override fun listen(onEvent: () -> Unit) {
        manager.addIntListener(key, { if (shouldSend(it)) onEvent() }, false)
    }

    private fun shouldSend(newState: Int): Boolean {
        val sendEvent = if (requiredPreviousState != -1) {
            previousState == requiredPreviousState && newState == state
        } else {
            newState == state
        }

        previousState = newState
        return sendEvent
    }
}

class MatrixIntValueListener(private val manager: CallbackManager, private val key: Any) :
    IntValueListener {
    override val value: Int
        get() = DataMatrix.getInt(key)

    override fun listen(onChange: (Int) -> Unit) {
        manager.addIntListener(key, onChange, false)
    }

    override fun listenAndTrigger(onChange: (Int) -> Unit) {
        manager.addIntListener(key, onChange, true)
    }
}

class MatrixFloatValueListener(private val manager: CallbackManager, private val key: Any) :
    FloatValueListener {
    override val value: Float
        get() = DataMatrix.getFloat(key)

    override fun listen(onChange: (Float) -> Unit) {
        manager.addFloatListener(key, onChange, false)
    }

    override fun listenAndTrigger(onChange: (Float) -> Unit) {
        manager.addFloatListener(key, onChange, true)
    }
}

class MatrixStringValueListener(private val manager: CallbackManager, private val key: Any) :
    StringValueListener {
    override val value: String
        get() = DataMatrix.getString(key)

    override fun listen(onChange: (String) -> Unit) {
        manager.addStringListener(key, onChange, false)
    }

    override fun listenAndTrigger(onChange: (String) -> Unit) {
        manager.addStringListener(key, onChange, true)
    }
}

class MatrixParcelableValueListener<T : Parcelable>(
    private val manager: CallbackManager,
    private val key: Any,
    private val defaultValue: T,
    private val parcelableClass: Class<T>
) : ParcelableValueListener<T> {
    override val value: T
        get() = DataMatrix.getParcelable(key, parcelableClass) ?: defaultValue

    override fun listen(onChange: (T) -> Unit) {
        manager.addParcelableListener(key, parcelableClass, onChange, false)
    }

    override fun listenAndTrigger(onChange: (T) -> Unit) {
        manager.addParcelableListener(key, parcelableClass, onChange, true)
    }
}

//endregion Value listeners


//region Variable listeners allow set/get/listen, marked open in case get/set need to be overridden in special cases

open class MatrixBoolVariableListener(
    private val manager: CallbackManager,
    private val readKey: Any,
    private val writeKey: Any = readKey,
) : BoolVariableListener {
    override var value: Boolean
        get() = DataMatrix.getBoolean(readKey)
        set(v) = DataMatrix.putBoolean(writeKey, v)

    override fun listen(onChange: (Boolean) -> Unit) {
        manager.addBoolListener(readKey, onChange, false)
    }

    override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
        manager.addBoolListener(readKey, onChange, true)
    }
}

open class MatrixIntVariableListener(
    private val manager: CallbackManager,
    private val readKey: Any,
    private val writeKey: Any = readKey,
) : IntVariableListener {
    override var value: Int
        get() = DataMatrix.getInt(readKey)
        set(v) = DataMatrix.putInt(writeKey, v)

    override fun listen(onChange: (Int) -> Unit) {
        manager.addIntListener(readKey, onChange, false)
    }

    override fun listenAndTrigger(onChange: (Int) -> Unit) {
        manager.addIntListener(readKey, onChange, true)
    }
}

open class MatrixFloatVariableListener(
    private val manager: CallbackManager,
    private val readKey: Any,
    private val writeKey: Any = readKey,
) : FloatVariableListener {
    override var value: Float
        get() = DataMatrix.getFloat(readKey)
        set(v) = DataMatrix.putFloat(writeKey, v)

    override fun listen(onChange: (Float) -> Unit) {
        manager.addFloatListener(readKey, onChange, false)
    }

    override fun listenAndTrigger(onChange: (Float) -> Unit) {
        manager.addFloatListener(readKey, onChange, true)
    }
}

open class MatrixStringVariableListener(
    private val manager: CallbackManager,
    private val readKey: Any,
    private val writeKey: Any = readKey,
) : StringVariableListener {
    override var value: String
        get() = DataMatrix.getString(readKey)
        set(v) = DataMatrix.putString(writeKey, v)

    override fun listen(onChange: (String) -> Unit) {
        manager.addStringListener(readKey, onChange, false)
    }

    override fun listenAndTrigger(onChange: (String) -> Unit) {
        manager.addStringListener(readKey, onChange, true)
    }
}

open class MatrixParcelableVariableListener<T : Parcelable>(
    private val manager: CallbackManager,
    private val readKey: Any,
    private val writeKey: Any = readKey,
    private val defaultValue: T,
    private val parcelableClass: Class<T>
) : ParcelableVariableListener<T> {
    constructor(
        manager: CallbackManager,
        key: Any,
        defaultValue: T,
        parcelableClass: Class<T>
    ) : this(manager, key, key, defaultValue, parcelableClass)

    override var value: T
        get() = DataMatrix.getParcelable(readKey, parcelableClass) ?: defaultValue
        set(v) = DataMatrix.putParcelable(writeKey, v)

    override fun listen(onChange: (T) -> Unit) {
        manager.addParcelableListener(readKey, parcelableClass, onChange, false)
    }

    override fun listenAndTrigger(onChange: (T) -> Unit) {
        manager.addParcelableListener(readKey, parcelableClass, onChange, true)
    }
}

open class MatrixParcelableListener<T : Parcelable>(
    private val manager: CallbackManager,
    private val key: Any,
    private val parcelableClass: Class<T>,
) : DataListener<T> {
    override fun listen(onChange: (T) -> Unit) {
        manager.addParcelableListener(key, parcelableClass, onChange, false)
    }

}

//endregion Variable listeners


/** Provides a [DataValueListener] that must be recomputed when a given [key] changes. */
class MatrixBoolComputeListener<T>(
    private val manager: CallbackManager,
    private val key: Any,
    private val computeValue: (Boolean)->T,
) : DataValueListener<T> {
    private var initialized = false
    private val callbacks = mutableSetOf<(T) -> Unit>()
    override var value: T
        private set
    init {
        value = computeValue(DataMatrix.getBoolean(key))
        registerManagerListener()
        initialized = true
        // only need to attach this once per construction
        manager.cancelAllEventCallbacks.add {
            initialized = false
        }
    }
    private fun initializeIfNeeded() {
        if (!initialized) {
            callbacks.clear()
            value = computeValue(DataMatrix.getBoolean(key))
            registerManagerListener()
            initialized = true
        }
    }
    private fun registerManagerListener() {
        manager.addBoolListener(key, { newBool ->
            value = computeValue(newBool)
            callbacks.forEach { it(value) }
        }, false)
    }
    override fun listenAndTrigger(onChange: (T) -> Unit) {
        initializeIfNeeded()
        callbacks.add(onChange)
        onChange(value)
    }
    override fun listen(onChange: (T) -> Unit) {
        initializeIfNeeded()
        callbacks.add(onChange)
    }
}
