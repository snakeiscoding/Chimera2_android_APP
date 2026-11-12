package com.jht.welcome.chameleon.overlays

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

// quick and dirty recreation from Chimera/src/main/java/com/jht/chimera/ui/mainapp/WelcomeActivity.java
// Might want to consider that Android docs seems to be pushing people to exoplayer

private const val TAG = "VideoOverlay"

/** Plays the given video URIs in a loop */
@Composable
fun VideoOverlay(
    uriList: List<Uri>,
    modifier: Modifier = Modifier,
    // should we have an error callback so parent can hide overlay if something goes wrong?
    onError: (Exception)-> Unit = {},
) {
    // if the URI list changes, this will recompose anyway
    val loopPlayer = remember { MediaLoopPlayer(uriList, onError) }
    DisposableEffect(Unit) {
        onDispose {
            loopPlayer.stop()
        }
    }
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) { loopPlayer.mediaPlayer.setSurface(holder.surface) }
                    override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) { loopPlayer.mediaPlayer.setSurface(holder.surface) }
                    override fun surfaceDestroyed(holder: SurfaceHolder) { loopPlayer.mediaPlayer.setSurface(null) }
                })
            }
        },
        modifier = modifier.testTag(TAG),
        update = { loopPlayer.start() }, // start loopPlayer here to make sure UI is ready?
    )
}

private class MediaLoopPlayer(
    val uris: List<Uri>,
    val onError: (Exception)-> Unit,
) {
    val mediaPlayer = MediaPlayer()
    var uriIndex = 0

    fun start() {
        uriIndex = 0
        mediaPlayer.setOnCompletionListener { mp: MediaPlayer? ->
            uriIndex = (uriIndex + 1) % uris.size
            playVideo()
        }
        playVideo()
    }
    fun stop() {
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.release()
    }
    private fun playVideo() {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(uris[uriIndex].path)
            mediaPlayer.setOnPreparedListener { mp: MediaPlayer? ->
                // old UI toggles from "Default" to "Foreground" chimera audio here? (and then back to "Default" when player is stopped)
                mp?.start()
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, e)
            onError(e)
        }
    }
}
