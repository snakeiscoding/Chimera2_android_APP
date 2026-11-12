package com.jht.test.util

data class SystemCommandResult (
    val result: Int = 0,
    val output: String = "",
    val error: String = "",
    val ex: Exception? = null) {

    init {
        println("System Command Result: $result")
        if(error.isNotEmpty())
            println("System Command Error: $error")
    }

}