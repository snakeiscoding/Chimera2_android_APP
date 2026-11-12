package com.jht.chimera.service.ui.panels

import android.os.Parcelable
import kotlinx.parcelize.Parcelize  //https://developer.android.com/kotlin/parcelize

/*
    This file contains purely data classes for storing information about panel behaviors for "apps"
 */


/** Key for Intent Extra passing of panel overrides [android.os.Bundle] */
const val PANEL_OVERRIDES_INTENT_KEY = "com.jht.panels.overrides"
/** key for panel overrides: boolean indicating if extra app bar needs to be temporarily hidden while activity is active */
const val PANEL_OVERRIDES_FORCE_HIDE_APP_BAR_KEY = "com.jht.forceHideAppBar"
/** key for panel overrides: boolean indicating if pause panel needs to be temporarily hidden while activity is active */
const val PANEL_OVERRIDES_FORCE_HIDE_PAUSE_PANEL_KEY = "com.jht.forceHidePausePanel"


// class to describe a panel with any options
@Parcelize
data class PanelConfig @JvmOverloads constructor(
    val panel : PanelIds,
    val mode : WorkoutPanelMode = WorkoutPanelMode.NORMAL
) : Parcelable

// a "panel set" is just Array<PanelConfig>
// we only want to change it if we have to, so need to compare sets for equality
fun isSamePanelSet(s1 : Array<PanelConfig>?, s2 : Array<PanelConfig>?) : Boolean {
    if (s1 == null && s2 == null) return true
    return s1 != null && s1.contentEquals(s2)
}

fun containsPanel(panelSet : Array<PanelConfig>?, panel : PanelIds) : Boolean {
    if (panelSet == null) return false
    for (p in panelSet) {
        if (p.panel == panel) return true
    }
    return false
}

// apparently all these extra options currently belong to bottom panel to try to keep panel transitioning simpler and/or reduce potential strain on the mods
@Parcelize
data class ExtraBarOptions(
    val showResize : Boolean = false,
    val showBack : Boolean = false,
    val showRefresh : Boolean = false,
    val showStats : Boolean = true,
    val showQuickKeys : Boolean = false,
) : Parcelable {
    fun needExtraBar() : Boolean { return showResize || showBack || showRefresh }
    companion object {
        @JvmStatic val defaultOptions = ExtraBarOptions() // one constant instance of the default
    }
}

// class to describe what "panel set" should be used during the common workout phases
@Parcelize
data class PanelBehavior @JvmOverloads constructor(
    val idle : Array<PanelConfig>?,
    val regular : Array<PanelConfig>?,
    val fullscreen : Array<PanelConfig>?,
    val summary : Array<PanelConfig>?,
    val options: ExtraBarOptions? = null
) : Parcelable {

    fun bottomHasExtraBar() : Boolean { return options != null && options.needExtraBar()}
    fun bottomHasResize() : Boolean { return options != null && options.showResize } // default is false
    fun bottomHasRefresh() : Boolean { return options != null && options.showRefresh } // default is false
    fun bottomHasBack() : Boolean { return options != null && options.showBack } // default is false
    fun bottomHasStat() : Boolean { return options == null || options.showStats } // default is true

    fun containsPanel(panel : PanelIds) : Boolean {
        if (containsPanel(idle, panel)) return true
        if (containsPanel(regular, panel)) return true
        if (containsPanel(fullscreen, panel)) return true
        if (containsPanel(summary, panel)) return true
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PanelBehavior

        if (!isSamePanelSet(idle, other.idle)) return false
        if (!isSamePanelSet(regular, other.regular)) return false
        if (!isSamePanelSet(fullscreen, other.fullscreen)) return false
        if (!isSamePanelSet(summary, other.summary)) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idle?.contentHashCode() ?: 0
        result = 31 * result + (regular?.contentHashCode() ?: 0)
        result = 31 * result + (fullscreen?.contentHashCode() ?: 0)
        result = 31 * result + (summary?.contentHashCode() ?: 0)
        result = 31 * result + (options?.hashCode() ?: 0)
        return result
    }

    fun copy(): PanelBehavior = PanelBehavior(idle, regular, fullscreen, summary, options)
    fun copy(extraBarOptions: ExtraBarOptions): PanelBehavior = PanelBehavior(idle, regular, fullscreen, summary, extraBarOptions)
    fun copy(showResize : Boolean = false, showBack : Boolean = false, showRefresh : Boolean = false) = PanelBehavior(idle, regular, fullscreen, summary, ExtraBarOptions(showResize, showBack, showRefresh))


    // the default constants
    companion object {
        // the resize function seems to be inherent to the extra bar and not changeable currently beyond not using the extra bar
        @JvmStatic val NoExtraBar = ExtraBarOptions.defaultOptions
        @JvmStatic val WebExtraBar = ExtraBarOptions(showResize = true, showBack = true, showRefresh = true)
        @JvmStatic val BackExtraBar = ExtraBarOptions(showResize = true, showBack = true, showRefresh = false)
        @JvmStatic val RefreshExtraBar = ExtraBarOptions(showResize = true, showBack = false, showRefresh = true)
        @JvmStatic val ResizeExtraBar = ExtraBarOptions(showResize = true, showBack = false, showRefresh = false)

        @JvmStatic val NoPanels = arrayOf<PanelConfig>()
        @JvmStatic val NormalRegularPanels = arrayOf(
            PanelConfig(PanelIds.TOP_PANEL),
            PanelConfig(PanelIds.LEFT_PANEL), PanelConfig(
                PanelIds.RIGHT_PANEL),
            PanelConfig(PanelIds.BOTTOM_PANEL)
        )
        @JvmStatic val NormalFullExtraPanels = arrayOf(PanelConfig(PanelIds.BOTTOM_PANEL)) // resize bar is part of the bottom panel

        @JvmStatic val NoPanelBehavior = PanelBehavior(null, null, null, null)
        @JvmStatic val NormalPanelBehavior = PanelBehavior(null, NormalRegularPanels, null, null)
        @JvmStatic val NormalPanelExtraBehavior = PanelBehavior(null, NormalRegularPanels, NormalFullExtraPanels, null)
        @JvmStatic val NormalPanelWebBehavior = NormalPanelExtraBehavior.copy(WebExtraBar)
        @JvmStatic val NormalPanelBackBehavior = NormalPanelExtraBehavior.copy(BackExtraBar)
        @JvmStatic val NormalPanelRefreshBehavior = NormalPanelExtraBehavior.copy(RefreshExtraBar)
        @JvmStatic val NormalPanelResizeBehavior = NormalPanelExtraBehavior.copy(ResizeExtraBar)
        //@JvmStatic fun normalExtra(showResize : Boolean = false, showBack : Boolean = false, showRefresh : Boolean = false) = NormalPanelExtraBehavior.copy(showResize, showBack, showRefresh)
    }
}

/** Calculates minimum panels to add or remove while keeping the stack order */
class PanelSetDifference(oldSet : Array<PanelConfig>?, newSet : Array<PanelConfig>?) {
    val removeList: MutableList<PanelConfig> = mutableListOf()
    val addList: MutableList<PanelConfig> = mutableListOf()
    init {
        if (oldSet == null) {
            // no old to remove, just add all new if there are any
            newSet?.let { addList.addAll(it) }
        } else if (newSet == null) {
            // if no new, remove all the old
            removeList.addAll(oldSet)
        } else {
            var i = 0
            var j = 0
            while (i < oldSet.size || j < newSet.size) {
                if (i < oldSet.size) {
                    // remove old panels unless we find a match in same order in the new panels
                    if (j < newSet.size && oldSet[i] == newSet[j]) {
                        ++i; ++j
                    } else {
                        removeList.add(oldSet[i++])
                    }
                } else {
                    // no more old, just add it
                    addList.add(newSet[j++])
                }
            }
        }
    }
    fun isNotEmpty() = removeList.isNotEmpty() || addList.isNotEmpty()
}
