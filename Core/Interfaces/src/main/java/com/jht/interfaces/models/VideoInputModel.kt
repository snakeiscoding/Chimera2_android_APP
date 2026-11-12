package com.jht.interfaces.models

import com.jht.data.AppInfo
import com.jht.interfaces.BaseModel

/*
    WARNING: THIS IS VERY ROUGH POC HACK!
    Need to find way to make things more generic?
 */

/** Provides interface to system for video inputs */
abstract class VideoInputModel : BaseModel {
    /* this is just a system level setting
    /** Select given video input. Return false if not available */
    abstract fun selectInput(inputs: VideoInputs): Boolean*/
    /** Select current video input based on system settings. Return the type of input to use. */
    abstract fun applyVideoInput(): VideoInput

    // do we need a STB model?
    /** List of apps available on video input device */
    abstract val externalAppList: List<AppInfo>
    /** Attempt to launch app on external device */
    abstract fun launchExternalApp(name: String)
    /** Send key to external device */
    abstract fun sendKey(key: RemoteKeys, count: Int = 1)
    /** Clear user data on external device */
    abstract fun clearUserData()
    /** Set external device mouse point. [x] and [y] are relative screen coordinates 0-1f */
    abstract fun moveMouseTo(x: Float, y: Float)
    abstract fun clickMouse()
    /** Set external device soft AP */
    abstract fun setSoftAp(ssid: String, pw: String)
    /** Set external device locale */
    abstract fun setLocale(localeCode: String)
    /** Callback for when input video feed changes. Will be called when set to provide initial data */
    abstract fun setInputChangeListener(onInputResolutionChange: (VideoInfo) -> Unit)
}

/** Class describing the video input format */
open class VideoInfo(
    val widthPixels: Int = 0,
    val heightPixels: Int = 0,
) {
    // consider non-zero valid or anything less than 640x480 invalid?
    // widest standard aspect ratio at this time is 2.38 or approx 21:9 (chimera1 has reported 3190x682 or 4217x1080 with nothing attached)
    open fun isValid() = (widthPixels >= 640 && heightPixels >= 480) && (widthPixels/heightPixels < 2.5)
}

/** Class describing how to get the video input */
class VideoInput(
    /** Specify the type of input */
    val type: VideoInputType = VideoInputType.CAMERA_FIRST,
    /** Provide additional input information as required by the type of input */
    val input: String = "",
)

/** Types of video inputs */
enum class VideoInputType {
    /** Using TV View (requires input string) */
    TV_VIEW,
    /** Using first camera selection */
    CAMERA_FIRST, // do "0" or first in the list?
}

/** Typical buttons on TV remote control */
enum class RemoteKeys {
    DirectionLeft, DirectionRight, DirectionUp, DirectionDown, Back,
}
