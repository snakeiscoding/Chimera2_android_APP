package com.jht.widgets

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.jht.interfaces.models.SystemModel
import com.jht.ui.combinedClickableNoEffect
import com.jht.widgets.helpers.ViewConfigurationOverride
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlin.math.hypot

/**
 * How fast someone needs to "fling" to get the debug menu to show up
 */
private const val debugFlingSpeed = 800f

private const val debugTotalMovementRequired = 80f
internal const val brandLogoTestTag = "BrandLogo"

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,

    /**
     * May want click area to be larger than image area
     */
    clickModifier: Modifier = Modifier,

    logoUri: Uri = Uri.EMPTY,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onDebugFling: () -> Unit = {},
) {
    // make it a bit easier for testers and developers using debug builds
    val longPressTimeMillis = if (BuildConfig.DEBUG) 1000L else 5000L
    var totalDrag by remember { mutableStateOf(Offset.Zero) }

    ViewConfigurationOverride(
        longPressTimeoutMillis = longPressTimeMillis,
        touchSlop = 20f, // default is 8
    ) {
        Box(
            clickModifier
                .testTag(brandLogoTestTag)
                .combinedClickableNoEffect(
                    onClick = onClick, onLongClick = onLongPress
                )
                // Track flings on brand for debug menu
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()

                    detectDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            totalDrag = Offset.Zero
                        },
                        onDrag = { change, drag ->
                            totalDrag += drag
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            val speed = hypot(velocity.x, velocity.y)
                            if (speed >= debugFlingSpeed && totalDrag.getDistance() >= debugTotalMovementRequired) {
                                onDebugFling()
                            }
                        }
                    )
                }
        ) {
            // GlideImage will crash at run time if there is not some kind of size constraint. Need either a max fill, fixed size, or aspect ratio
            val imageModifier = modifier.align(Alignment.Center)

            // we could maybe look into using a resource URI with GlideImage, but might be best to keep it simple when we can
            if (logoUri == Uri.EMPTY) {
                Image(
                    modifier = imageModifier,
                    painter = painterResource(id = R.drawable.ic_logo_matrix),
                    contentDescription = null,
                )
            } else {
                GlideImage(
                    modifier = imageModifier,
                    model = logoUri,
                    contentDescription = null,
                    failure = placeholder(R.drawable.ic_logo_matrix),
                )
            }
        }
    }
}

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    clickModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onDebugFling: () -> Unit = {},
) {
    val context = LocalContext.current
    val state = remember { BrandLogoState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    BrandLogo(modifier, clickModifier, state.logoUri.value, onClick, onLongPress, onDebugFling)
}

private class BrandLogoState(context: Context) {
    // defaulting to visible states for previews, on device the model will update them with actual states
    val logoUri = mutableStateOf<Uri>(Uri.EMPTY)
    val systemModel: SystemModel

    init {
        val injection = getInjectionEntryPoint(context)
        systemModel = injection.system()
    }

    fun start() {
        systemModel.logoUri.listenAndTrigger {
            logoUri.value = Uri.parse(it)
        }
    }

    fun stop() {
        systemModel.close()
    }
}