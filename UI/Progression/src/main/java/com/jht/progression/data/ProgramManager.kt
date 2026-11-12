package com.jht.progression.data

import androidx.compose.ui.graphics.Color
import com.jht.enums.ProgramType
import com.jht.progression.R
import com.jht.progression.data.program.program10k.Program10k
import com.jht.progression.data.program.program5k.Program5k
import com.jht.progression.ui.MmColors

/**
 * Stores and manages retrieving
 */
class ProgramManager {

    val programs: List<Program> by lazy {
        getProgressivePrograms()
    }

    /**
     * Gets a list of the progressive programs that are available on the console
     */
    private fun getProgressivePrograms(): List<Program> {
        val programList = mutableListOf<Program>()
        programList.add(get5kProgram())

        //TODO: Add the 10k program back in whenever we get to it
//        programList.add(get10kProgram())
        return programList
    }
}

fun getProgramOfType(type: ProgramType): Program {
    return when (type) {
        ProgramType.Program5k -> get5kProgram()
        ProgramType.Program10k -> get10kProgram()
    }
}

/**
 * Gets the info for the 5k program.
 */
private fun get5kProgram(): Program {
    return Program(
        programType = ProgramType.Program5k,
        nameId = R.string.five_k_tc,
        workoutInterface = Program5k(),
        baselineInterface = Baseline15Min(),
        color = MmColors.futuristicBlue,
    )
}

private fun get10kProgram(): Program {
    return Program(
        programType = ProgramType.Program10k,
        nameId = R.string.ten_k_tc,
        workoutInterface = Program10k(),
        baselineInterface = Baseline15Min(),
        color = Color(0xFFEA8115),
    )
}