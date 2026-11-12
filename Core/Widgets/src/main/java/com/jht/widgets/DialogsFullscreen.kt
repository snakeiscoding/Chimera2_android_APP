package com.jht.widgets

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jht.widgets.helpers.LifeCycleOnPause
import java.util.UUID

/**
    File contains fullscreen Popup and Dialogs which needed some workaround to display properly on the consoles.
    File contains code to deal with our OS mods and some other parent-less use cases.
 */

class PhysicalScreenMetrics(context: Context) {
    val widthPixels: Int
    val heightPixels: Int
    val densityDpi: Int
    init {
        /* there is a way to get the display modes apparently?
        val display = context.display ?: return
        val modes = DisplayCompat.getSupportedModes(context, display)*/

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics().apply {
            windowManager.defaultDisplay.getRealMetrics(this)
        }
        // when screen orientation is portrait these flip, we want them always as landscape dimensions, hence the min/max
        heightPixels = minOf(metrics.heightPixels, metrics.widthPixels)
        widthPixels = maxOf(metrics.heightPixels, metrics.widthPixels)
        densityDpi = metrics.densityDpi
    }
}

// For some reason the screenHeightDp is not the full height so this function sets it to the full height
/*@Composable
private fun fixLocalConfigurationHeight() {
    val displayMetrics = DisplayMetrics()
    val context = LocalContext.current
    val windowManager = remember { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    windowManager.defaultDisplay.getRealMetrics(displayMetrics)
    LocalConfiguration.current.screenHeightDp = displayMetrics.heightPixels
    // Do we need to convert the pixels to DP for this?
    //LocalConfiguration.current.screenHeightDp = with(LocalDensity.current) { displayMetrics.heightPixels.toDp().value.toInt() }
}*/


/** When composable is positioned, will invoke [isFullscreenCallback] with indication whether it takes up whole screen within tolerance */
fun Modifier.detectFullscreen(
    verticalTolerancePx: Int = 5,
    horizontalTolerancePx: Int = 5,
    isFullscreenCallback: (isFullscreen: Boolean) -> Unit,
): Modifier = composed {
    val context = LocalContext.current
    val metrics = remember { PhysicalScreenMetrics(context) }
    this.onGloballyPositioned {
        val verticalCheck = (metrics.heightPixels - it.size.height) < verticalTolerancePx
        val horizontalCheck = (metrics.widthPixels - it.size.width) < horizontalTolerancePx
        isFullscreenCallback(verticalCheck && horizontalCheck)
    }
}


fun Modifier.fullscreen(): Modifier = composed {
    // Previous fix using LocalConfiguration would sometimes have issues when using popup inside of a dialog
    // https://stackoverflow.com/a/73089753
    val context = LocalContext.current
    val metrics = remember { PhysicalScreenMetrics(context) }
    val (width, height) = with(LocalDensity.current) {
        Pair(metrics.widthPixels.toDp(), metrics.heightPixels.toDp())
    }
    this.requiredSize(width, height)
}
fun Modifier.fillScreenHeight(/*@FloatRange(from = 0.0, to = 1.0)*/ fraction: Float = 1f): Modifier = composed {
    val context = LocalContext.current
    val metrics = remember { PhysicalScreenMetrics(context) }
    val height = with(LocalDensity.current) { metrics.heightPixels.toDp() }
    this.height(height * fraction)
}
fun Modifier.fillScreenWidth(/*@FloatRange(from = 0.0, to = 1.0)*/ fraction: Float = 1f): Modifier = composed {
    val context = LocalContext.current
    val metrics = remember { PhysicalScreenMetrics(context) }
    val width = with(LocalDensity.current) { metrics.widthPixels.toDp() }
    this.width(width * fraction)
}


/*
    While Popup can be configured as focusable, we have seen issues with dropdown menus and find Dialog to work better in that situation.
 */

@Composable
fun PopupFullscreen(
    onDismissRequest: (() -> Unit)? = null, // here to match the stock Popup but fullscreen has no outside to click dismiss?
    properties: PopupProperties = PopupProperties(),
    panelOperationMode: PanelOperationMode = PanelOperationMode.Overlay, // PopupProperties cannot be extended, so just do this separately
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val popupId = rememberSaveable { UUID.randomUUID() }
    val popupLayout = remember {
        JhtOverlayPopupLayout(
            properties = properties,
            panelOperationMode = panelOperationMode,
            composeView = view,
            density = density,
            popupId = popupId,
        ).apply {
            setContent(parentComposition) {
                currentContent()
            }
        }
    }
    LaunchedEffect(Unit) { popupLayout.show() }
    DisposableEffect(Unit) { onDispose { popupLayout.dismiss() } }
    /*  Have observed that sometimes navigating to a non-compose activity that dispose is not fired, so listen for onPause.
        Regular Popup seems fine probably cause window is attached to the activity itself?
        Should probably call the parent to close in case the UI is resumed so state will be correct?
        */
    LifeCycleOnPause { onDismissRequest?.invoke() }
}

private val fullscreenDialogProperties = DialogProperties(usePlatformDefaultWidth = false)

@Composable
fun DialogFullscreen(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = fullscreenDialogProperties,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest, properties) {
        //fixLocalConfigurationHeight()
        //content.invoke()
        Box(Modifier.fullscreen()) { content() }
    }
}

@Composable
fun PopupOverlayPanel(
    offset: IntOffset = IntOffset.Zero,
    size: IntSize = IntSize(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT),
    layoutGravity: Int = 0,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val popupId = rememberSaveable { UUID.randomUUID() }
    val popupLayout = remember {
        JhtOverlayPopupLayout(
            properties = properties,
            panelOperationMode = PanelOperationMode.Overlay,
            composeView = view,
            density = density,
            popupId = popupId,
            offset, size, layoutGravity
        ).apply {
            setContent(parentComposition) {
                currentContent()
            }
        }
    }
    LaunchedEffect(Unit) { popupLayout.show() }
    DisposableEffect(Unit) { onDispose { popupLayout.dismiss() } }
}


/** Operating modes for JHT panels (according to our OS modification) */
enum class PanelOperationMode(val flag: Int) {
    /** Panel takes up space on the screen, forcing other UI to size down */
    Inplace(if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) WindowManager.LayoutParams.TYPE_SYSTEM_ALERT else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY),
    /** Panel is overlaid over existing UI */
    Overlay(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR),
    /** Panel is a normal part of UI, a non-floating panel, but requires activity context */
    ActivityContent(WindowManager.LayoutParams.TYPE_APPLICATION),
}

/** This is a selection of code taken from compose internal PopupLayout simplified for full screen without any visual clipping (might be something they fix but need workaround now) */
@SuppressLint("ViewConstructor")
internal class JhtOverlayPopupLayout(
    properties: PopupProperties,
    panelOperationMode: PanelOperationMode,
    composeView: View,
    density: Density,
    popupId: UUID,
    offset: IntOffset = IntOffset.Zero,
    size: IntSize = IntSize(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT),
    layoutGravity: Int = 0,
) : AbstractComposeView(composeView.context), ViewRootForInspector {
    private val windowManager = composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    override val subCompositionView: AbstractComposeView get() = this

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val params = WindowManager.LayoutParams().apply {
        gravity = layoutGravity //Gravity.START or Gravity.TOP
        flags = (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH)
        if (!properties.focusable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // in order to be above our panels we need to elevate the window type
        type = getParamType(panelOperationMode)
        //token = composeView.applicationWindowToken // if token is set, this will be susceptible to panel z ordering
        x = offset.x
        y = offset.y
        width = size.width
        height = size.height
        format = PixelFormat.TRANSPARENT
        //title = composeView.context.resources.getString(R.string.default_popup_window_title)
    }

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp
    init {
        id = android.R.id.content
        setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        // Set unique id for AbstractComposeView. This allows state restoration for the state
        // defined inside the Popup via rememberSaveable()
        setTag(R.id.compose_view_saveable_id_tag, "Popup:$popupId")

        // Enable children to draw their shadow by not clipping them
        clipChildren = false
        // Allocate space for elevation
        with(density) { elevation = maxSupportedElevation.toPx() }
        // Simple outline to force window manager to allocate space for shadow.
        // Note that the outline affects clickable area for the dismiss listener. In case of shapes
        // like circle the area for dismiss might be to small (rectangular outline consuming clicks
        // outside of the circle).
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, result: Outline) {
                result.setRect(0, 0, view.width, view.height)
                // We set alpha to 0 to hide the view's shadow and let the composable to draw its
                // own shadow. This still enables us to get the extra space needed in the surface.
                result.alpha = 0f
            }
        }
    }

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    private var content: @Composable () -> Unit by mutableStateOf({})
    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }
    @Composable
    @UiComposable
    override fun Content() {
        content()
    }

    fun show() {
        windowManager.addView(this, params)
    }
    fun dismiss() {
        disposeComposition()
        setViewTreeLifecycleOwner(null)
        windowManager.removeViewImmediate(this)
    }
}


/** Provide a basis for a standalone [Dialog] hosting Composable content */
abstract class ComposeDialog(context: Context) : Dialog(context) {
    /** call in [onCreate] to set type to system which only requires an application context since it is not attached to a particular view or activity */
    protected fun setWindowType(panelOperationMode: PanelOperationMode = PanelOperationMode.Overlay) {
        window!!.setType(panelOperationMode.flag)
    }
    /** call in [onCreate] to set dialog to take the full screen */
    protected fun makeFullScreen() {
        // make dialog 100% of screen
        val metrics = PhysicalScreenMetrics(context)
        window?.let { w->
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(w.attributes)
            lp.width = metrics.widthPixels
            lp.height = metrics.heightPixels
            w.attributes = lp
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // requestFeature must be done before content is added
        window!!.requestFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawableResource(android.R.color.transparent)

        val rootView = StandAloneComposeView(context)
        rootView.setContent { Content() }
        setContentView(rootView)
    }

    @Composable
    abstract fun Content()
}

/** A version of ComposeView that does not require an Activity parent (copied from ComposeView but based AbstractStandAloneComposeView) */
class StandAloneComposeView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractStandAloneComposeView(context, attrs, defStyleAttr) {
    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }
    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    /**
     * Set the Jetpack Compose UI content for this view.
     * Initial composition will occur when the view becomes attached to a window or when
     * [createComposition] is called, whichever comes first.
     */
    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow) {
            createComposition()
        }
    }
}

/** Version of [AbstractComposeView] that does not require an Activity */
abstract class AbstractStandAloneComposeView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
/*
    Compose usually relies on Activity for life cycle management, so we need to provide that when there is no Activity hosting it.
    Some hints online
    https://medium.com/mobile-app-development-publication/building-a-jetpack-compose-window-with-abstractcomposeview-a7efb1768e0b
    https://stackoverflow.com/a/70460554
 */
    //@Suppress("RedundantVisibilityModifier")
    //protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false

    private val lifecycleOwner = MyLifecycleOwner()
    private val myViewModelStore = ViewModelStore()
    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = myViewModelStore
    }

    init {
        /*val composeView = LocalView.current
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())*/
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(viewModelStoreOwner)

        // compose 1.5 broke re-used compose view then fixed in 1.7, so commenting this block until they break it again
        // for now just adding ON_START gets recompose to work again
        /*val coroutineContext = AndroidUiDispatcher.CurrentThread
        val runRecomposeScope = CoroutineScope(coroutineContext)
        val recomposer = Recomposer(coroutineContext)
        this.compositionContext = recomposer
        runRecomposeScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }*/
    }
}
// Need to either provide a lifecycle handler or attach one from a parent
internal class MyLifecycleOwner : SavedStateRegistryOwner {
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var mSavedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)

    val isInitialized: Boolean
        get() = true
    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = mLifecycleRegistry

    fun setCurrentState(state: Lifecycle.State) {
        mLifecycleRegistry.currentState = state
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        mLifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        mSavedStateRegistryController.performRestore(savedState)
    }

    fun performSave(outBundle: Bundle) {
        mSavedStateRegistryController.performSave(outBundle)
    }
}


/**
 * hacky way to get instrumentation tests using the custom pop-ups to work.
 * This is needed since the bottom workout bar needs TYPE_SYSTEM_ERROR to function properly with
 * our custom OS mods, however this type fails during some instrumentation tests.
 */
private fun getParamType(
    panelOperationMode: PanelOperationMode,
): Int {
    // TODO add other test classes that need to use the dialog that is failing in some instrumentation tests
    val classAndType : List<Pair<String, Int>> = listOf(
        Pair("com.jht.widgets.ConciergeDialogInstrumentationTests", WindowManager.LayoutParams.TYPE_APPLICATION_PANEL) // this one works for concierge instrumentation tests
    )
    classAndType.forEach {
        try {
            Class.forName(it.first)
            Log.d("DialogsFullscreen", "test class ${it.first} found, using WindowManager.LayoutParams.${it.second}")
            return it.second
        } catch (e: ClassNotFoundException) {
            Log.d("DialogsFullscreen", "test class ${it.first} not found")
        }
    }
    Log.d("DialogsFullscreen", "no test classes found, using WindowManager.LayoutParams.TYPE_SYSTEM_ERROR")
    return panelOperationMode.flag
}