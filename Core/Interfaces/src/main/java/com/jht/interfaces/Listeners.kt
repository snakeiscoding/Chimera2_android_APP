package com.jht.interfaces;

import android.os.Parcelable

// PubSub stands for publisher/subscribers
// The following interface contains the definition for listeners (Subscribers) and Senders (publishers)

/**
 * EventListener is used to listen to event that has no data
 *
 * This can be used to listen to DataMatrix event key that has no data
 */
interface EventListener {
    fun listen(onEvent: () -> Unit)
}

/**
 * DataListener is used to listen to event that has a data type
 *
 * This can be used to listen to DataMatrix event key that has a data type
 */
interface DataListener<T> {
    fun listen(onChange: (T) -> Unit)
}

interface BoolListener : DataListener<Boolean>
interface IntListener : DataListener<Int>
interface FloatListener : DataListener<Float>
interface StringListener : DataListener<String>

/**
 * DataValueListener is used to listen to event that has a data type plus get the value
 *
 * This can be used to get/listen to DataMatrix key that has a data type
 */
interface DataValueListener<T> : DataListener<T> {
    val value: T
    fun listenAndTrigger(onChange: (T) -> Unit)
}

interface BoolValueListener : DataValueListener<Boolean>
interface IntValueListener : DataValueListener<Int>
interface FloatValueListener : DataValueListener<Float>
interface StringValueListener : DataValueListener<String>
interface ParcelableValueListener<T : Parcelable> : DataValueListener<T>

/**
 * DataVariableListener is used to listen to event that has a data type plus get and set the value
 *
 * This can be used to set/get/listen to DataMatrix key that has a data type
 */
interface DataVariableListener<T> : DataListener<T> {
    var value: T
    fun listenAndTrigger(onChange: (T) -> Unit)
}

interface BoolVariableListener : DataVariableListener<Boolean>
interface IntVariableListener : DataVariableListener<Int>
interface FloatVariableListener : DataVariableListener<Float>
interface StringVariableListener : DataVariableListener<String>
interface ParcelableVariableListener<T : Parcelable> : DataVariableListener<T>
