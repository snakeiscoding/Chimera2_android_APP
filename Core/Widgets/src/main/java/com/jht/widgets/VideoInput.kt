package com.jht.widgets

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.tv.TvContract
import android.media.tv.TvView
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jht.interfaces.models.VideoInput
import com.jht.interfaces.models.VideoInputType
import com.jht.theme.DynamicTheme
import com.jht.widgets.helpers.ComposableLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "VideoInputWidget"

/** Displays a video input using android Camerax preview (or tv view if needed) */
@Composable
fun VideoInputBox(
    modifier: Modifier = Modifier,
    videoInput: VideoInput = VideoInput(),
) {
    when (videoInput.type) {
        VideoInputType.TV_VIEW -> VideoInputTv(modifier, videoInput.input)
        else -> VideoInputCameraX(modifier, videoInput.type)
    }
}


private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

/** Uses camera preview API */
@OptIn(ExperimentalCamera2Interop::class)
@Composable
private fun VideoInputCameraX(
    modifier: Modifier = Modifier,
    videoInputType: VideoInputType = VideoInputType.CAMERA_FIRST,
) {
    val isError = remember { mutableStateOf(false) }
    //val isStreaming = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            //implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            //controller = cameraController
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // match parent or use BoxWithConstraints and apply actual size?
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            /*previewStreamState.observe(lifecycleOwner) {
                // unfortunately once it enters streaming state, it never returns to idle when things break, only returns to idle when view hides
                isStreaming.value = (it == PreviewView.StreamState.STREAMING)
            }*/
        }
    }
    // preview setup
    LaunchedEffect(videoInputType) {
        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                // we are capturing video now (we get a series of onCaptureStarted / onCaptureCompleted
                // did not seem to get any of the failure callbacks, only sign of issues was camera trying to reopen
                // this is called a lot during streaming so keep it brief, mutable state should only update if different
                isError.value = false
            }
            override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                super.onCaptureFailed(session, request, failure)
                isError.value = true
            }
            override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long) {
                super.onCaptureBufferLost(session, request, target, frameNumber)
                isError.value = true
            }
        }
        val maxVideoSize = Size(3840, 2160) // default max to UHD and fallback to lower from there
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(maxVideoSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        val previewBuilder = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setResolutionSelector(resolutionSelector)
        val previewExtender = Camera2Interop.Extender(previewBuilder)
        previewExtender.setSessionCaptureCallback(captureCallback)
        val preview = previewBuilder.build()
        /*preview.setSurfaceProvider{ surfaceRequest: SurfaceRequest? ->
            surfaceRequest?.let {
                Log.d("VideoInputX", "surfaceRequest chosen resolution ${it.resolution}")
                previewView.surfaceProvider.onSurfaceRequested(it)
            }
        }*/
        val cameraSelector = CameraSelector.Builder().addCameraFilter { list ->
            if (list.isEmpty()) list
            else when (videoInputType) {
                else -> listOf(list.first()) // default to first
            }
        }.build()
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

            camera.cameraInfo.cameraState.observe(lifecycleOwner) {
                // if state reports error, something is wrong
                if (it.error != null) isError.value = true
            }
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
    }
    // the actual UI
    Box(
        modifier,
    ) {
        AndroidView(
            factory = { previewView },
        )
        if (isError.value) Box(
            Modifier
                .testTag("videoInputErrorIndicator")
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Icon(
                Icons.Filled.Warning,
                null,
                Modifier
                    .fillMaxSize(0.2f)
                    .align(Alignment.Center),
                tint = DynamicTheme.colors.warning
            )
        }
    }
}

private class CustomTvView(context: Context?) : TvView(context) {
    // The TvView has custom dispatchTouchEvent that seem to take over the touch events. Override it and pass it to the parent
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return false
    }
}

/** Uses TvView API */
@Composable
private fun VideoInputTv(
    modifier: Modifier = Modifier,
    input: String,
) {
    val isError = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tvView = remember {
        CustomTvView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // match parent or use BoxWithConstraints and apply actual size?
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setCallback(object: TvView.TvInputCallback() {
                override fun onTuned(inputId: String, channelUri: Uri) {
                    super.onTuned(inputId, channelUri)
                    Log.d(TAG, "onTuned $inputId $channelUri")
                }
                override fun onVideoAvailable(inputId: String?) {
                    super.onVideoAvailable(inputId)
                    Log.d(TAG, "onVideoAvailable $inputId")
                }
                override fun onVideoUnavailable(inputId: String?, reason: Int) {
                    super.onVideoUnavailable(inputId, reason)
                    Log.d(TAG, "onVideoUnavailable $inputId $reason")
                }
                override fun onConnectionFailed(inputId: String?) {
                    super.onConnectionFailed(inputId)
                    Log.d(TAG, "onConnectionFailed $inputId")
                }
                override fun onDisconnected(inputId: String?) {
                    super.onDisconnected(inputId)
                    Log.d(TAG, "onDisconnected $inputId")
                }
                override fun onVideoSizeChanged(inputId: String?, width: Int, height: Int) {
                    super.onVideoSizeChanged(inputId, width, height)
                    Log.d(TAG, "onVideoSizeChanged $inputId")
                }
            })
        }
    }

    ComposableLifecycle { _, event ->
        // Using ON_START/ON_STOP to happen on visibility change as resume/pause can trigger when already visible
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                tvView.reset() // not sure this is really needed
            }
            Lifecycle.Event.ON_START -> {
                scope.launch {
                    delay(500) // hate using a delay, but sometimes will not come back without some pause
                    isError.value = try {
                        val channelUri: Uri = TvContract.buildChannelUriForPassthroughInput(input)
                        tvView.tune(input, channelUri)
                        false
                    } catch (e: Exception) {
                        Log.e(TAG, "TvView tune error", e)
                        true
                    }
                }
            }
            else -> {}
        }
    }
    Box(
        modifier,
    ) {
        AndroidView(
            factory = { tvView },
        )
        if (isError.value) Box(
            Modifier
                .testTag("videoInputErrorIndicator")
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Icon(
                Icons.Filled.Warning,
                null,
                Modifier
                    .fillMaxSize(0.2f)
                    .align(Alignment.Center),
                tint = DynamicTheme.colors.warning
            )
        }
    }
}

