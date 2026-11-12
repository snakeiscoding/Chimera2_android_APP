package com.jht.chimera.service.gattserver2.services

import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.gattserver.BuildConfig
import com.jht.gattserver2.IGattServer
import com.jht.gattserver2.SamsungWatch
import com.jht.gattserver2.services.EnhancedGymService

private const val TAG = "EnhancedGymServiceConsole"

class EnhancedGymServiceConsole(server: IGattServer, samsungWatch: SamsungWatch) : EnhancedGymService(server, samsungWatch) {
    override fun onSamsungWatchConnecting() {
        // don't think old code did anything special here
    }

    override fun onSamsungWatchDisconnecting() {
        onCustomHeartRate(0) // old code zeroed on disconnect (though the value is likely ignored based on connection status anyway?)
    }

    override fun stop() {
        onCustomHeartRate(0) // make sure HR is zeroed when service stops
        super.stop()
    }

    override fun onCustomHeartRate(hr: Int) {
        if (BuildConfig.DEBUG) Log.d(TAG, "SENDING HEART RATE: $hr")
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT, hr)
    }

    override fun onUserInfo(gender: Int, age: Int, weight: Float, height: Float) {
        /* as of 2024/4/9 PMO reports there is no expectation to get user info from the watch, not even for a guest user
            val dapiGender: GenderIndentity = when (gender) {
                Gender.Male.ordinal -> GenderIndentity.Male
                Gender.Female.ordinal -> GenderIndentity.Female
                else -> GenderIndentity.NotSpecified
            }
            DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, dapiGender.Value)
            // Use the following line to test non binary as the watch does not send this for now.
            //DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, GenderIndentity.NonBinary.Value);
            DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, age)
            DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, weight)
            DataMatrix.putFloat(DAPIKeys.DATA_USER_HEIGHT_METERS_FLOAT, height)
        }*/
    }
}
