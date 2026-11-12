package com.jht.interfaces.models

import com.jht.interfaces.BaseModel
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.FloatValueListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.StringValueListener

/**
 * Model that contains information about the current user
 */
abstract class UserModel : BaseModel {
    /** Provides the current user's UUID */
    abstract val uuid: StringValueListener
    /** Provides the current user's name */
    abstract val name: StringValueListener
    /** Provides the web page URL to edit a user if one is available. Empty if not available. */
    abstract val editUrl: StringValueListener

    /** Provides the current user's gender (0=male,1=female,2=nonBinary,3=unspecified) */
    abstract val gender: IntValueListener
    /** Provides the current user's weight in kilograms */
    abstract val weight: FloatValueListener
    /** Provides the current user's age in years */
    abstract val age: IntValueListener

    /** Returns true if the current user is a guest */
    abstract val isGuest : Boolean
    /** Returns true if the current logged in account is a PTP user. If system does not support PTP, always returns false. */
    abstract val isPTP : BoolValueListener

    abstract val isSecaUser: Boolean

    /** True when the logged in user has completed the PACE onboarding survey. False for guests and users without PACE data. */
    abstract val hasCompletedPaceSurvey: Boolean

    /** JUMP access token for the current login, empty when not logged in through JUMP */
    abstract val jumpAccessToken: String

    /** JUMP OpenID id token (a JWT) for the current login, empty when not logged in through JUMP */
    abstract val jumpIdToken: String

    /** Returns the wallet pass used to create an actual wall pass */
    abstract val walletPass: StringValueListener
}
