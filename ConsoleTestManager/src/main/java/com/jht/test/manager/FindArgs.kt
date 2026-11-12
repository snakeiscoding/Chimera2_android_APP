package com.jht.test.manager

internal class FindArgs(args: Array<String>) {
    val consoleType: String = when (args.size) {
        2 -> {
            args[1]
        }
        1 -> {
            "chimera_main_matrix_chimera"
        }
        else -> {
            throw IllegalArgumentException()
        }
    }

}