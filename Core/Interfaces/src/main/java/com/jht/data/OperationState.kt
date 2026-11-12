package com.jht.data

/**
 * Represents the state of an asynchronous operation.
 *
 * @param T The type of data returned on success.
 */
sealed class OperationState<out T> {
    /**
     * Indicates the operation is in progress.
     */
    data object Loading : OperationState<Nothing>()

    /**
     * Indicates the operation completed successfully.
     *
     * @property data The result data from the operation.
     */
    data class Success<T>(val data: T) : OperationState<T>()

    /**
     * Indicates the operation failed.
     *
     * @property error The exception that occurred.
     */
    data class Error(val error: Exception) : OperationState<Nothing>()
}
