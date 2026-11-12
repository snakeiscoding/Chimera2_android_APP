package com.jht.interfaces.ui

import android.os.Parcelable
import java.util.UUID

/**
 * Base class that is sent through UI Service model to reach specific panels. Specific panel data
 * classes need to derive from this class.
 */
abstract class BasePanelData(private val panelId: UUID) : Parcelable
{
    fun getPanelId() : UUID {
        return panelId;
    }
}