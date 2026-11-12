package com.jht.data.sprint8

/**
 * Interface for Sprint8 workout data to avoid cyclic dependencies between modules.
 * This interface is implemented by the concrete Sprint8WorkoutData class in the Chimera module.
 */
interface ISprint8WorkoutData {
    
    /**
     * List of interval data for different levels.
     * Each level contains a list of Float values representing the workout intervals.
     * Structure: [warmup, sprint1, recovery1, sprint2, recovery2, ..., cooldown]
     */
    val intervalData: List<List<Float>>
}


