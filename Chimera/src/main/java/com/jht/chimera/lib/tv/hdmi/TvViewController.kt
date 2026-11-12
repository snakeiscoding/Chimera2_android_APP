package com.jht.chimera.lib.tv.hdmi

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.media.tv.TvContract
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * TvViewController implements the HDMI input by using the TvView by tuning to the TV
 * input provided by Rockchip or vendor. It will listen to the activity lifecycle
 * to turn and reset the tv view.
 */
class TvViewController(activity: Activity, private val tvContainerView: ViewGroup) : View.OnAttachStateChangeListener,
    ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "TvViewController"
        private const val INPUT_ID = "com.example.partnersupportsampletvinput/.SampleTvInputService/HW0"
    }

    // The TvView has custom dispatchTouchEvent that seem to take over the touch events. Override it and pass it to the parent
    private class CustomTvView(context: Context?) : TvView(context) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            return false
        }
    }

    private val tvView = CustomTvView(activity)
    private val handler = Handler(Looper.getMainLooper())

    private val channelUri: Uri = TvContract.buildChannelUriForPassthroughInput(INPUT_ID)

    init {
        tvView.addOnAttachStateChangeListener(this)

        // Should work on Vortex which is Android 14 so API 34
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.registerActivityLifecycleCallbacks(this)
        }
    }

    override fun onViewAttachedToWindow(view: View) {
        handler.postDelayed({
            try {
                tvView.tune(INPUT_ID, channelUri)
            } catch (e: Exception) {
                Log.e(TAG, "tune error: ", e)
            }
        }, 500)
    }

    override fun onViewDetachedFromWindow(view: View) {
        tvView.reset()
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        // Do nothing
    }

    override fun onActivityStarted(activity: Activity) {
        // Do nothing
    }

    override fun onActivityResumed(activity: Activity) {
        // add view at the first index so overlays are above the tv view
        tvContainerView.addView(tvView, 0)
    }

    override fun onActivityPaused(activity: Activity) {
        // remove the view so the onViewDetachedFromWindow is triggered
        tvContainerView.removeView(tvView)
    }

    override fun onActivityStopped(activity: Activity) {
        // Do nothing
        // TODO Is this needed?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.unregisterActivityLifecycleCallbacks(this)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // Do nothing
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Do nothing
    }

}
