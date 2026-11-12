package com.jht.progression.data

import androidx.compose.ui.graphics.Color
import com.jht.enums.ProgramType

data class Program(
    val programType: ProgramType,
    val nameId: Int,
    val workoutInterface: IWorkoutInterface,
    val baselineInterface: IBaseline,
    val color: Color
)