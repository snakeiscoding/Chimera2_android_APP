package com.jht.chimera.service.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.app.AndroidSetting
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.datamatrixlib.DataMatrix

private const val DEBUG: Boolean = false
private const val TAG: String = "LocationProvider"

@SuppressLint("MissingPermission")
class LocationProvider {

    private var mContext: Context? = ContextUtil.getApplicationContext()
    private var mLocationManager: LocationManager = mContext!!.getSystemService(LOCATION_SERVICE) as LocationManager
    private var mProviderName: String = LocationManager.GPS_PROVIDER
    private val mDataMatrix = DataMatrix()

    private var latitude = 0.0
    private var longitude = 0.0

    fun start() {
        setupLocationProvider()
        //Latitude and Longitude could be consolidated to a single key with a float pair since
        // they both send at the same time, but one duplicate call of mock location on change is acceptable
        mDataMatrix.createFloatAdapter(SettingKeys.Weather.DATA_WEATHER_LATITUDE_FLOAT, { value ->
            latitude = value.toDouble()
            checkLocation()
        }, true)
        mDataMatrix.createFloatAdapter(SettingKeys.Weather.DATA_WEATHER_LONGITUDE_FLOAT, { value ->
            longitude = value.toDouble()
            checkLocation()
        }, true)
    }

    fun stop(){
        mDataMatrix.finish()
    }

    private val locationListener = object : LocationListener{
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "onLocationChanged: $location")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(TAG, "onStatusChanged: $status")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled: $provider")
        }
    }

    // ProviderProperties constants require API 31
    @SuppressLint("WrongConstant")
    @Suppress("DEPRECATION")
    private fun setupLocationProvider() {
        val root = ExecuteAsRoot()
        root.addCommand("appops set com.jht.chimera android:mock_location allow")
        root.execute()
        AndroidSetting.putInt(
            mContext!!.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION,
            1
        )

        val locationProvider = mLocationManager.getProvider(mProviderName)
        if (locationProvider == null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                mLocationManager.addTestProvider(
                    mProviderName, false, false, false, false, false,
                    true, true, 1, 2
                )
            } else {
                mLocationManager.addTestProvider(
                    mProviderName, false, false, false, false, false,
                    true, true, 0, 5
                )
            }
            mLocationManager.setTestProviderEnabled(mProviderName, true)
        }

        if (DEBUG) {
            mLocationManager.requestLocationUpdates(mProviderName, 0, 0f, locationListener)
        }
    }

    private fun checkLocation(){
        if (latitude == 0.0 && longitude == 0.0) {
            Log.w(TAG, "Facility location has not been set!")
            pushLocation(43.0784,  -89.2017)
        }
        else {
            pushLocation(latitude, longitude)
        }
    }

    private fun pushLocation(latitude: Double, longitude: Double) {
        try {
            val mockLocation = Location(mProviderName)
            mockLocation.latitude = latitude
            mockLocation.longitude = longitude
            mockLocation.setAltitude(0.0)
            mockLocation.time = System.currentTimeMillis()
            mockLocation.setAccuracy(0f)
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            mLocationManager.setTestProviderLocation(mProviderName, mockLocation)
        } catch (_: Exception) {
            // Emulator generates this exception even though it lists gps as a provider?> Provider "gps" unknown
        }
    }
}