package com.jht.data

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/** Specify the type of login mechanism employed by backend */
enum class LoginType(val value : String) {
    // Using values from Chimera/src/main/java/com/jht/chimera/lib/common/dapi/DapiUtil.java since they are fed to DAPI login
    // NOTE: Registration is NOT a login type. However a registration can be for a specific login type.
    Guest("guest"), // should guest be actual type or does the mere absence of login imply a guest?
    Xid("xid"),
    Email("email"),
    PhoneNumber("phone_num"),
    Barcode("barcode"),
    Wallet("wallet"),
    QrCode("qr_code"),
    Custom("custom"),
    ; // END
    companion object {
        @JvmStatic fun fromValue(value: String?): LoginType? = entries.find { it.value == value }
    }
}

/** Describes a login method and what it requires */
data class LoginData(
    val label : String = "",
    val description: String = "", // new UI provides descriptive text
    val type : LoginType = LoginType.Guest,
    val imageInfo : Bitmap? = null, // some logins required extra visual data such as a QR code image
    override val fields: List<UserField> = listOf(),
) : UserFieldList() {
    var associateDeviceLogin: DeviceLoginData? = null // data necessary to pair an ID device to this login?
    override fun clearValues() {
        super.clearValues()
        associateDeviceLogin = null
    }
    fun isGuest() = type == LoginType.Guest
    fun needsInput() : Boolean { return !isGuest() && fields.isNotEmpty() } // Johnny Five
}



/** Describes a registration and what it requires */
data class RegisterData(
    val label : String = "",
    val description: String = "",
    val type : LoginType = LoginType.Guest,
    override val fields: List<UserField> = listOf(),
) : UserFieldList()



/** Data for a device that can be associated with a login */
abstract class DeviceLoginData(
    //open val deviceLabel: String, // or string ID? or have UI figure it out?
) : Parcelable { // make this Parcelable or just provide methods to encode/decode to a string value?
    // not sure what is in common because we only have one thing right now?
    /** Get user readable ID for device */
    abstract fun getIdString(): String
    // should we provide fun to decode/encode Intent if device is scanned outside of login screen and needs to navigate to it?
}

/** Class to generically describe the RFID data needed for a login */
@Parcelize
data class RfidLoginData(
    /** This the RFID type as defined for our login system */
    val dapiRfidType: Int,
    /** This the ID data for the RFID as needed by login system */
    val uid: ByteArray,
) : DeviceLoginData() {
    override fun getIdString(): String  = uid.joinToString("") { "%02X".format(it) }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RfidLoginData
        if (dapiRfidType != other.dapiRfidType) return false
        if (!uid.contentEquals(other.uid)) return false
        return true
    }
    override fun hashCode(): Int {
        var result = dapiRfidType
        result = 31 * result + uid.contentHashCode()
        return result
    }
}

/** Provides event handling for a RFID login session */
abstract class RfidLoginHandler {
    /** RFID login is being attempted */
    abstract fun onLoginAttempt(isRfid: Boolean)
    /** RFID was used to login successfully. User flow may proceed. */
    abstract fun onLoginSuccess()
    /** RFID was refused by login system */
    abstract fun onLoginFailure(isRfid: Boolean)
    /** Login system encounter some form of error */
    abstract fun onError(isRfid: Boolean)
    /** RFID is unknown. The handler can decide what to do with it. */
    abstract fun onNewRfid(rfid: RfidLoginData)
}

/** RFID modes of operation that may need toggling for certain uses */
enum class RfidOperatingMode {
    /** Normal operation: ID tags, watches, etc */
    Normal,
    /** Wallet operation: Apple, Google wallet scanning */
    Wallet, // do we need separate entries for particular wallets?
}

/** Provides event handling for a Phone login session */
abstract class PhoneLoginHandler {
    /** Phone login is being attempted */
    abstract fun onLoginAttempt()
    /** Phone login was used to login successfully. User flow may proceed. */
    abstract fun onLoginSuccess()
    /** Phone login failed */
    abstract fun onLoginFailure()
}
