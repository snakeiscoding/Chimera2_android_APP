package com.jht.enums

enum class FrameType {
    Treadmill,
    Elliptical,
    Bike,
    Ascent,
    Stepper,
    Climbmill,
    SpinBike;

    companion object {
        // TODO we need to eventually refactor code to use same enum type
        // if the enum is not found, it will return bike
        // Note: the string is case sensitive!
        fun fromString(name: String) = entries.find { it.name == name } ?: Bike
    }

    val isTreadmill: Boolean get() = this == Treadmill
    val isBike: Boolean get() = this == Bike
    val isElliptical: Boolean get() = this == Elliptical
    val isAscent: Boolean get() = this == Ascent
    val isStepper: Boolean get() = this == Stepper
    val isClimbmill: Boolean get() = this == Climbmill
    val isSpinBike: Boolean get() = this == SpinBike

    /** Returns true if frame is active, as in the frame moves the user rather than user moves the frame */
    val isActiveFrame get() = (this == Treadmill || this == Climbmill)
}
