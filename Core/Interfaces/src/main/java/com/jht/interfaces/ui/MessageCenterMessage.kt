package com.jht.interfaces.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar

/**
 * The contents of a message and the formatting for the message. Priority will affect when the
 * message is displayed and whether it is displayed before other messages.
 */
@Parcelize
data class MessageCenterMessage @JvmOverloads constructor(
    var timestamp: Calendar? = null,
    var expiration: Calendar? = null,
    var isHidden: Boolean = false,
    var isHiddenOnSplash: Boolean = true, // in general messages should hide on splash
    var isCloseButtonHidden: Boolean = false,
    var showPostWorkout: Boolean = false,
    var messageHeader: String? = null,
    var messageText: String? = null,
    var backgroundColor: Int = 0,
    var textColor: Int = 0,
    var headerColor: Int = 0,
    var closeButtonColor: Int = 0,
    var priority: Int = 0,
    var hideTime: Int = 0,
    var id: Int = 0,
    var tag: String? = null,
    var workoutOffset: Int = 0
) : Parcelable {
    constructor(messageHeader: String?, messageText: String?, priority: Int) : this() {
        this.messageHeader = messageHeader
        this.messageText = messageText
        this.priority = priority
    }

    constructor(
        messageHeader: String?,
        messageText: String?,
        priority: Int,
        hideTime: Int
    ) : this() {
        this.messageHeader = messageHeader
        this.messageText = messageText
        this.priority = priority
        this.hideTime = hideTime
    }

    override fun describeContents(): Int {
        return 0
    }

    val isExpired: Boolean
        /**
         * @return true if this message expired, else false.
         */
        get() = expiration != null && Calendar.getInstance().after(expiration)
    val isRelative: Boolean
        get() = timestamp == null

    override fun equals(other: Any?): Boolean {
        return other is MessageCenterMessage && other.id == id
    }

    override fun hashCode(): Int {
        var result = timestamp?.hashCode() ?: 0
        result = 31 * result + (expiration?.hashCode() ?: 0)
        result = 31 * result + isHidden.hashCode()
        result = 31 * result + isHiddenOnSplash.hashCode()
        result = 31 * result + isCloseButtonHidden.hashCode()
        result = 31 * result + showPostWorkout.hashCode()
        result = 31 * result + (messageHeader?.hashCode() ?: 0)
        result = 31 * result + (messageText?.hashCode() ?: 0)
        result = 31 * result + backgroundColor
        result = 31 * result + textColor
        result = 31 * result + headerColor
        result = 31 * result + closeButtonColor
        result = 31 * result + priority
        result = 31 * result + hideTime
        result = 31 * result + id
        result = 31 * result + workoutOffset
        return result
    }
}