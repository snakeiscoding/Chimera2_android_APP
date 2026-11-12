package com.jht.widgets.helpers

/** Common states that UI might need to track if it has async operation (using a common one so we don't have declare new one all the time) */
enum class AsyncUiState {
    Unknown,    // Unknown or Idle, basically operation has not run yet
    Waiting,    // async operation is in progress
    Success,    // async operation reports success or completion
    Failure,    // async operation reports failure
}
