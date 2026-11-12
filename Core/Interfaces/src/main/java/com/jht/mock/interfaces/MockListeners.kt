package com.jht.mock.interfaces;

import android.os.Parcelable
import com.jht.interfaces.BoolEmitter
import com.jht.interfaces.BoolListener
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.BoolVariableListener
import com.jht.interfaces.DataEmitter
import com.jht.interfaces.DataListener
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventEmitter
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatEmitter
import com.jht.interfaces.FloatListener
import com.jht.interfaces.FloatValueListener
import com.jht.interfaces.FloatVariableListener
import com.jht.interfaces.IntEmitter
import com.jht.interfaces.IntListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.IntVariableListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.ParcelableVariableListener
import com.jht.interfaces.StringEmitter
import com.jht.interfaces.StringListener
import com.jht.interfaces.StringValueListener
import com.jht.interfaces.StringVariableListener

class MockEventListener : EventListener, EventEmitter {
    var listener: () -> Unit = {}
    override fun listen(onEvent: () -> Unit) {
        this.listener = onEvent
    }

    override fun emit() {
        listener()
    }
}

open class MockDataListener<T> : DataListener<T>, DataEmitter<T> {
    var listener: (T) -> Unit = {}

    override fun listen(onChange: (T) -> Unit) {
        this.listener = onChange
    }

    override fun emit(value: T) {
        listener(value)
    }
}

class MockBoolEvent : MockDataListener<Boolean>(), BoolListener, BoolEmitter
class MockIntEvent : MockDataListener<Int>(), IntListener, IntEmitter
class MockFloatEvent : MockDataListener<Float>(), FloatListener, FloatEmitter
class MockStringEvent : MockDataListener<String>(), StringListener, StringEmitter

open class MockDataValue<T>(private var _value: T) : DataValueListener<T>, DataEmitter<T> {
    var listener: (T) -> Unit = {}
    override val value: T
        get() {
            return _value
        }

    override fun emit(value: T) {
        this._value = value
        listener(_value)
    }

    override fun listen(onChange: (T) -> Unit) {
        this.listener = onChange
    }

    override fun listenAndTrigger(onChange: (T) -> Unit) {
        this.listener = onChange
        onChange(value)
    }
}

class MockBoolValue(value: Boolean = false) : MockDataValue<Boolean>(value), BoolValueListener, BoolEmitter
class MockIntValue(value: Int = 0) : MockDataValue<Int>(value), IntValueListener, IntEmitter
class MockFloatValue(value: Float = 0f) : MockDataValue<Float>(value), FloatValueListener, FloatEmitter
class MockStringValue(value: String = "") : MockDataValue<String>(value), StringValueListener, StringEmitter
class MockParcelableValue<T: Parcelable>(value: T) : MockDataValue<T>(value), ParcelableValueListener<T>

open class MockDataVariable<T>(private var _value: T) : DataVariableListener<T>, DataEmitter<T> {
    var listener: (T) -> Unit = {}
    override var value: T
        get() { return _value }
        set(value) { emit(value) }

    override fun emit(value: T) {
        this._value = value
        listener(_value)
    }

    override fun listen(onChange: (T) -> Unit) {
        this.listener = onChange
    }

    override fun listenAndTrigger(onChange: (T) -> Unit) {
        this.listener = onChange
        onChange(value)
    }
}

class MockBoolVariable(value: Boolean = false) : MockDataVariable<Boolean>(value), BoolVariableListener, BoolEmitter
class MockIntVariable(value: Int = 0) : MockDataVariable<Int>(value), IntVariableListener, IntEmitter
class MockFloatVariable(value: Float = 0f) : MockDataVariable<Float>(value), FloatVariableListener, FloatEmitter
class MockStringVariable(value: String = "") : MockDataVariable<String>(value), StringVariableListener, StringEmitter
class MockParcelableVariable<T : Parcelable>(value: T): MockDataVariable<T>(value), ParcelableVariableListener<T>
