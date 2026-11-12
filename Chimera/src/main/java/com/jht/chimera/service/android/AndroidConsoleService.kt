package com.jht.chimera.service.android

import android.content.Context
import com.jht.services.InlineConsoleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "AndroidConsoleService"

/** [AndroidConsoleService] service handles the background Android calls needed for console function,
 * such as permissions, file management, location handling, and miscellaneous Android specifics
 * */
class AndroidConsoleService(context: Context): InlineConsoleService(context)  {
    companion object {
        const val ID = TAG // UUID or unique friendly name for this service
    }
    override val id = ID

    private val androidConsoleScope = CoroutineScope(Dispatchers.IO)
    private val locationProvider = LocationProvider()

    override fun start() {
        androidConsoleScope.launch {
            //Starts Android location provider. Used with Hulu TV location
            locationProvider.start()
        }
    }

    override fun stop() {
        androidConsoleScope.cancel()
        locationProvider.stop()
    }

}