package com.jht.util

import android.R
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.util.Log
import org.jetbrains.annotations.NonNls

@SuppressLint("StaticFieldLeak") // Application should next change so we can ignore the leak warning
object ContextHelper {
    @Volatile
    private var cachedApplicationContext: Context? = null

    /**
     * Retrieves the current application context in both normal runtime
     * and instrumentation test environments. The result is cached
     * after the first successful lookup.
     */
    val context: Context?
        @SuppressLint("PrivateApi")
        get() = synchronized(this) {
            cachedApplicationContext ?: run {
                val resolvedContext = try {
                    val activityThreadClass = Class.forName("android.app.ActivityThread")
                    val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
                    val application = currentApplicationMethod.invoke(null) as? Application
                    application?.applicationContext ?: resolveInstrumentationContext()
                } catch (_: Exception) {
                    resolveInstrumentationContext()
                }

                cachedApplicationContext = resolvedContext
                resolvedContext
            }
        }

    /**
     * Attempts to retrieve a context from instrumentation testing frameworks.
     */
    private fun resolveInstrumentationContext(): Context? {
        // Try ApplicationProvider (AndroidX Test Core)
        try {
            val applicationProviderClass = Class.forName("androidx.test.core.app.ApplicationProvider")
            val getApplicationContextMethod = applicationProviderClass.getMethod("getApplicationContext")
            val testContext = getApplicationContextMethod.invoke(null) as? Context
            return testContext?.applicationContext
        } catch (_: Throwable) {
            // Try InstrumentationRegistry (AndroidX Test Platform)
            try {
                val instrumentationRegistryClass = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
                val getInstrumentationMethod = instrumentationRegistryClass.getMethod("getInstrumentation")
                val instrumentationInstance = getInstrumentationMethod.invoke(null)
                val getTargetContextMethod = instrumentationInstance.javaClass.getMethod("getTargetContext")
                val targetContext = getTargetContextMethod.invoke(instrumentationInstance) as? Context
                return targetContext?.applicationContext
            } catch (_: Throwable) {
                return null
            }
        }
    }
}


@SuppressLint("PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
object UsbPermission {
    private const val TAG: @NonNls String = "USBPermission"

    @JvmStatic
    fun requestUsbPermission(context: Context, device: UsbDevice?) {
        try {
            val pkgManager = context.packageManager
            val appInfo = pkgManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            getServiceMethod.isAccessible = true
            val binder = getServiceMethod.invoke(null, Context.USB_SERVICE) as IBinder?

            val iUsbManagerClass = Class.forName("android.hardware.usb.IUsbManager")
            val stubClass = Class.forName("android.hardware.usb.IUsbManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val iUsbManager = asInterfaceMethod.invoke(null, binder)

            val grantDevicePermissionMethod = iUsbManagerClass.getDeclaredMethod(
                "grantDevicePermission",
                UsbDevice::class.java,
                Int::class.javaPrimitiveType
            )
            grantDevicePermissionMethod.isAccessible = true
            val setDevicePackageMethod = iUsbManagerClass.getDeclaredMethod(
                "setDevicePackage",
                UsbDevice::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            setDevicePackageMethod.isAccessible = true

            grantDevicePermissionMethod.invoke(iUsbManager, device, appInfo.uid)
            setDevicePackageMethod.invoke(iUsbManager, device, appInfo.packageName, 0)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error requestUSBPermission!", e)
        }
    }

    @JvmStatic
    fun requestUsbPermission() {
        try {
            val context = ContextHelper.context ?: return
            val pkgManager = context.packageManager
            val appInfo = pkgManager.getApplicationInfo(context.packageName,PackageManager.GET_META_DATA)

            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            getServiceMethod.isAccessible = true
            val binder = getServiceMethod.invoke(null, Context.USB_SERVICE) as IBinder?

            val iUsbManagerClass = Class.forName("android.hardware.usb.IUsbManager")
            val stubClass = Class.forName("android.hardware.usb.IUsbManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val iUsbManager = asInterfaceMethod.invoke(null, binder)

            val grantDevicePermissionMethod = iUsbManagerClass.getDeclaredMethod(
                "grantDevicePermission",
                UsbDevice::class.java,
                Int::class.javaPrimitiveType
            )
            grantDevicePermissionMethod.isAccessible = true
            val setDevicePackageMethod = iUsbManagerClass.getDeclaredMethod(
                "setDevicePackage",
                UsbDevice::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            setDevicePackageMethod.isAccessible = true

            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = manager.deviceList
            val deviceIterator: MutableIterator<UsbDevice> = deviceList.values.iterator()
            while (deviceIterator.hasNext()) {
                val device = deviceIterator.next()
                Log.d(TAG, "Device id: " + device.vendorId + " Device name: " + device.deviceName)

                grantDevicePermissionMethod.invoke(iUsbManager, device, appInfo.uid)
                setDevicePackageMethod.invoke(iUsbManager, device, appInfo.packageName, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requestUSBPermission!", e)
        }
    }
}


object ServiceHelper {
    @JvmStatic
    @Suppress("deprecation")
    fun startForegroundService(service: Service) {
        val mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = "id" //NON-NLS
        val name: CharSequence = "name" //NON-NLS
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        mNotificationManager.createNotificationChannel(channel)

        val notification = Notification.Builder(service, id)
            .setContentTitle("JHT Service") //NON-NLS
            .setContentText("JHT Service") //NON-NLS
            .setSmallIcon(R.drawable.sym_def_app_icon)
            .build()
        service.startForeground(1, notification)

        Log.d("ServiceStarter", "startForeground: $service")
    }
}
