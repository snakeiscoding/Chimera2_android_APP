package com.jht.clubcom.interfaces

import android.os.Parcelable
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.ParcelableVariableListener
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** System interfacing for Club Com which provides advertising and some access restrictions */
abstract class ClubComModel : BaseModel {
    /** Get the console model string to use in club com URLs */
    abstract val urlModelString: String

    /** True is club com feature is enabled */
    abstract val isEnabled: DataValueListener<Boolean>

    /** The current club com options */
    abstract val options: ParcelableVariableListener<ClubComOptions>

    /** Called when welcome screen is shown */
    abstract fun startSession()

    /** Callback to service [startSession] requests */
    abstract val onStartSession: EventListener

    /** Called when welcome screen is hidden */
    abstract fun stopSession()

    /** Callback to service [stopSession] requests */
    abstract val onStopSession: EventListener
}


/** Class storing info for a Club Com app */
@Parcelize
@Serializable
data class ClubComApp(
    @SerialName("uid") val id: String = "",
    @SerialName("title") val name: String = "",
    @SerialName("url") val appUrl: String = "",
    @SerialName("image") val imageUrl: String = "",
): Parcelable

/** Class representing a Club Com URL entry from their JSON */
@Parcelize
@Serializable
data class ClubComUrl(
    @SerialName("uid") val uid: String? = null,
    @SerialName("url") val url: String? = null,
): Parcelable

/** Class storing collection of info for Club Com apps */
@Parcelize
@Serializable
data class ClubComOptions(
    //mediaServerID = jo.getInt("ms");
    @SerialName("startsessionurl") val startSessionUrl: String? = null,
    @SerialName("endsessionurl") val endSessionUrl: String? = null,
    @SerialName("welcome") val welcomeUrl: ClubComUrl? = null,
    @SerialName("prerolls") val preRollUrls: List<ClubComUrl>? = null,
    @SerialName("apps") val apps: List<ClubComApp>? = null,
): Parcelable
