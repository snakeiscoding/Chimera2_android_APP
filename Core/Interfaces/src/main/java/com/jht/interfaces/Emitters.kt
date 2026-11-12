package com.jht.interfaces;

/**
 * EventEmitter is used to emit an event that has no data
 *
 * This can be used to call put to DataMatrix event key that has no data
 */
interface EventEmitter {
    fun emit()
}

/**
 * DataEmitter is used to emit an event that has a data type
 *
 * This can be used to call put to DataMatrix event key that has a data type
 */
interface DataEmitter<T> {
    fun emit(value: T)
}

interface BoolEmitter : DataEmitter<Boolean>
interface IntEmitter : DataEmitter<Int>
interface FloatEmitter : DataEmitter<Float>
interface StringEmitter : DataEmitter<String>