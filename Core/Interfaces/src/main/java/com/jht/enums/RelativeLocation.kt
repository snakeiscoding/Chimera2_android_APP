package com.jht.enums


/*
        TOP_LEFT   TOP_MIDDLE   TOP_RIGHT
LEFT_TOP                                  RIGHT_TOP
LEFT_MIDDLE                               RIGHT_MIDDLE
LEFT_BOTTOM                               RIGHT_BOTTOM
        BOTTOM_LEFT  BOTTOM_MIDDLE  BOTTOM_RIGHT

 we could consolidate LEFT\RIGHT groups into a vertical MIDDLE group and reduce these by 4?
 we could also include separate axis as properties if needed? RelativeLocation(val vertical: Vertical?, val horizontal: Horizontal?)
 */

/** Provides a relative location in terms of corners and midpoints between corners */
enum class RelativeLocation {
    TOP_LEFT,
    TOP_MIDDLE,
    TOP_RIGHT,

    LEFT_TOP,
    LEFT_MIDDLE,
    LEFT_BOTTOM,

    RIGHT_TOP,
    RIGHT_MIDDLE,
    RIGHT_BOTTOM,

    BOTTOM_LEFT,
    BOTTOM_MIDDLE,
    BOTTOM_RIGHT,

    NONE
}
