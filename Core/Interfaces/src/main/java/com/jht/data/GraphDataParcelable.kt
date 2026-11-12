package com.jht.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Sentinel marking a graph sample with no valid reading (e.g. heart
 * rate with no strap connected). Renderers treat it as "pen up": no line or fill is drawn
 * across the invalid stretch. The View graph's GraphSeries.INVALID_PIXEL sources this value.
 *
 * -Float.MAX_VALUE so it can never collide with a real reading — including negative values a
 * future frame might report (e.g. decline incline), which the old -1f sentinel would have
 * swallowed. Deliberately not NaN: consumers compare with ==, and NaN != NaN would silently
 * break every such check. This is a `const val`, so the value is inlined at compile time —
 * externally-built consumers must be rebuilt if it ever changes again.
 */
const val INVALID_GRAPH_DATA = -Float.MAX_VALUE

// More clean-up on this class is to be done, just converted it to kotlin for now.
/**
 * Contains the information needed to draw a single history profile line. Gets the list of goal values
 * to use along with the data values. These two lists are not necessarily the same size. The
 * GraphChannelChange keeps track of the index of the goal to use and the goals are found
 * by utilizing that index.
 *
 *
 * To use this class iterate through the points calling [.get] to get the DataPoint
 * associated with the changes in the graph.
 */

@Parcelize
data class GraphInfoChannel(
    private val mTimeMs: MutableList<Float> = mutableListOf(), // CGC-18873: need time to workaround new profile graph issues for now
    private val mGoalValues: MutableList<Float> = mutableListOf(),
    val mDataValues: MutableList<GraphChannelChange> = mutableListOf(),
    private val mFutureData: Array<DataPoint>? = arrayOf()
) : Parcelable {

    private fun get(index: Int): DataPoint {
        val change: GraphChannelChange = mDataValues[index]
        return DataPoint(
            mTimeMs[change.goalIndex],
            mGoalValues[change.goalIndex],
            change.value
        )
    }

    val goalOffset: Float
    /**
    * Gets the initial value of the goal so that the beginning of the history graph can be
    * displayed starting at the beginning
    *
    * @return The goal offset
    */
    get() = if (mGoalValues.size > 0) {
        mGoalValues[0]
    } else {
        0f
    }

    val historyDataPoints: Array<DataPoint>
    /**
    * Gets the list of data points associated with this channel
    *
    * @return The list of data points
    */
    get() {
        return Array(mDataValues.size) { get(it) }
    }

    val futureDataPoints: Array<DataPoint>?
    /**
    * Gets the list of future data associated with this channel. If there is no future data
    * this may be null.
    *
    * @return The list of future data associated with this channel.
    */
    get() = mFutureData

    /**
    * Gets whether this channel has future data.
    *
    * @return whether there is future data
    */
    fun hasFutureData(): Boolean {
        return mFutureData.isNullOrEmpty()
    }
}

@Parcelize
data class GraphChannelChange(
    val goalIndex: Int,
    val value: Float
) : Parcelable

@Parcelize
data class DataPoint(
    val timeMs: Float, // CGC-18873: need time to workaround new profile graph issues for now, can remove later
    val goalValue: Float,
    val data: Float
) : Parcelable