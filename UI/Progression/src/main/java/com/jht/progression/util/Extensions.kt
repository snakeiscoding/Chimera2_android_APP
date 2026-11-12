package com.jht.progression.util

/**
 * Converts this range to a zero-indexed range by subtracting 1 from both the start and end of the range.
 * The resulting range excludes the original upper bound.
 *
 * @return A new zero-indexed IntRange where the start is decremented by 1, and the upper bound is exclusive.
 *
 * Example:
 * 4..21 becomes 3 until 21 (i.e., 3..20).
 */
fun IntRange.zeroIndexed(): IntRange {
    return (this.first - 1) until this.last
}