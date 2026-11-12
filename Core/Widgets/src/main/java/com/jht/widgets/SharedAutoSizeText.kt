package com.jht.widgets

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared state that collects reported text sizes from all subscribers
 * and broadcasts the minimum back. Hoist this above all participating
 * composables (e.g. in a parent composable or ViewModel).
 */
class SharedTextSizeState {
    private var _minSize by mutableStateOf<TextUnit?>(null)
    val minSize: TextUnit? get() = _minSize

    fun report(size: TextUnit) {
        _minSize = if (_minSize == null) size
        else minOf(_minSize!!, size, compareBy { it.value })
    }
}

/**
 * A Text composable that:
 *  1. Measures the largest font size that fits within its bounds.
 *  2. Reports that size to [sharedState].
 *  3. Renders at [sharedState].minSize once all siblings have reported,
 *     so every instance on the page uses the same (smallest) size.
 *
 * @param sharedState Hoisted state shared across all sibling instances.
 * @param minFontSize Lower bound for auto-sizing (default 10.sp).
 * @param maxFontSize Upper bound / starting candidate (default 32.sp).
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SharedAutoSizeText(
    text: String,
    sharedState: SharedTextSizeState,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    minFontSize: TextUnit = 10.sp,
    maxFontSize: TextUnit = 32.sp,
) {
    val minSize = sharedState.minSize

    if (minSize == null) {
        Text(
            text = text,
            style = style,
            autoSize = TextAutoSize.StepBased(
                minFontSize = minFontSize,
                maxFontSize = maxFontSize,
            ),
            onTextLayout = { result ->
                sharedState.report(result.layoutInput.style.fontSize)
            },
            softWrap = false,
            modifier = modifier,
        )
    } else {
        Text(
            text = text,
            style = style.copy(fontSize = minSize),
            softWrap = false,
            modifier = modifier,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Preview parameter provider — drives the "static snapshot" previews
// ─────────────────────────────────────────────────────────────

private data class SharedAutoSizePreviewParams(
    val label: String,
    /** Width of the constraining box in dp */
    val boxWidth: Dp,
    val texts: List<String>,
)

private class SharedAutoSizePreviewParamProvider :
    PreviewParameterProvider<SharedAutoSizePreviewParams> {
    override val values = sequenceOf(
        SharedAutoSizePreviewParams(
            label = "Wide — all text fits large",
            boxWidth = 300.dp,
            texts = listOf("Hello", "World", "!"),
        ),
        SharedAutoSizePreviewParams(
            label = "Medium — balanced reduction",
            boxWidth = 160.dp,
            texts = listOf("Hello", "World", "!"),
        ),
        SharedAutoSizePreviewParams(
            label = "Narrow — smallest drives all",
            boxWidth = 80.dp,
            texts = listOf("Hello", "World", "!"),
        ),
        SharedAutoSizePreviewParams(
            label = "Varied content — long text wins",
            boxWidth = 200.dp,
            texts = listOf("Hi", "A longer second label", "Medium text"),
        ),
    )
}

// ─────────────────────────────────────────────────────────────
// 1. Parametric snapshot preview — shows four static states side-by-side
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "SharedAutoSizeText — snapshots",
    showBackground = true,
    backgroundColor = 0xFF1A1A2E,
    widthDp = 380,
)
@Composable
private fun SharedAutoSizeTextSnapshotPreview(
    @PreviewParameter(SharedAutoSizePreviewParamProvider::class)
    params: SharedAutoSizePreviewParams,
) {
    PreviewScaffold {
        SectionLabel(params.label)
        Spacer(Modifier.height(8.dp))
        ConstrainedTextGroup(
            texts = params.texts,
            boxWidth = params.boxWidth,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 2. Interactive slider preview — drag to squeeze/expand the box width
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "SharedAutoSizeText — interactive slider",
    showBackground = true,
    backgroundColor = 0xFF1A1A2E,
    widthDp = 380,
    heightDp = 480,
)
@Composable
private fun SharedAutoSizeTextInteractivePreview() {
    var widthFraction by remember { mutableFloatStateOf(0.2f) }
    val minDp = 60.dp
    val maxDp = 300.dp
    val currentWidth = minDp + (maxDp - minDp) * widthFraction

    val animatedWidth by animateDpAsState(
        targetValue = currentWidth,
        animationSpec = tween(durationMillis = 200),
        label = "box_width",
    )

    PreviewScaffold {
        SectionLabel("Drag the slider to change available space")
        Spacer(Modifier.height(16.dp))

        ConstrainedTextGroup(
            texts = listOf("Free", "Pro · \$9.99/mo", "Enterprise"),
            boxWidth = animatedWidth,
        )

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DimLabel("Narrow")
                DimLabel("${animatedWidth.value.toInt()} dp")
                DimLabel("Wide")
            }
            Slider(
                value = widthFraction,
                onValueChange = { widthFraction = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 3. Side-by-side sync proof — same state, different content lengths
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "SharedAutoSizeText — sync proof",
    showBackground = true,
    backgroundColor = 0xFF1A1A2E,
    widthDp = 380,
    heightDp = 320,
)
@Composable
private fun SharedAutoSizeTextSyncProofPreview() {
    PreviewScaffold {
        SectionLabel("All boxes share one state — smallest wins")
        Spacer(Modifier.height(16.dp))

        val sharedState = remember { SharedTextSizeState() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "Hi" to 0,
                "A medium text" to 1,
                "The longest label here" to 2,
            ).forEach { (text) ->
                AnnotatedBox(
                    modifier = Modifier.weight(1f).height(64.dp),
                ) {
                    SharedAutoSizeText(
                        text = text,
                        sharedState = sharedState,
                        modifier = Modifier.fillMaxSize(),
                        style = TextStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        ),
                        maxFontSize = 28.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        DimLabel(
            "↑ Three equal-width boxes — size locked to the tightest constraint",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 4. Edge cases preview
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "SharedAutoSizeText — edge cases",
    showBackground = true,
    backgroundColor = 0xFF1A1A2E,
    widthDp = 380,
    heightDp = 560,
)
@Composable
private fun SharedAutoSizeTextEdgeCasesPreview() {
    PreviewScaffold {
        SectionLabel("Edge cases")
        Spacer(Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // Single instance — no sync needed, renders at its own best size
            EdgeCaseRow(label = "Single instance (no siblings)") {
                val state = remember { SharedTextSizeState() }
                SharedAutoSizeText(
                    text = "Solo",
                    sharedState = state,
                    modifier = Modifier.width(120.dp).height(48.dp),
                    style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White),
                    maxFontSize = 32.sp,
                )
            }

            // All identical strings — size should be equal; min == any
            EdgeCaseRow(label = "Identical strings — min == any") {
                val state = remember { SharedTextSizeState() }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { _ ->
                        SharedAutoSizeText(
                            text = "Same",
                            sharedState = state,
                            modifier = Modifier.width(80.dp).height(40.dp),
                            style = TextStyle(fontWeight = FontWeight.Medium, color = Color.White),
                            maxFontSize = 24.sp,
                        )
                    }
                }
            }

            // Very long string forces tiny size
            EdgeCaseRow(label = "One extreme outlier shrinks all") {
                val state = remember { SharedTextSizeState() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    SharedAutoSizeText(
                        text = "OK",
                        sharedState = state,
                        modifier = Modifier.width(80.dp).fillMaxHeight(),
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFF7ECFF5)),
                        maxFontSize = 28.sp,
                    )
                    SharedAutoSizeText(
                        text = "This is an extremely long label",
                        sharedState = state,
                        modifier = Modifier.width(80.dp).fillMaxHeight(),
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFFFF7E7E)),
                        maxFontSize = 28.sp,
                    )
                    SharedAutoSizeText(
                        text = "Fine",
                        sharedState = state,
                        modifier = Modifier.width(80.dp).fillMaxHeight(),
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFF7EFF9E)),
                        maxFontSize = 28.sp,
                    )
                }
            }

            // minFontSize floor — ensures text never goes below the minimum
            EdgeCaseRow(label = "minFontSize floor respected") {
                val state = remember { SharedTextSizeState() }
                SharedAutoSizeText(
                    text = "Truncated at minimum font size floor",
                    sharedState = state,
                    modifier = Modifier.width(60.dp).height(32.dp),
                    style = TextStyle(color = Color(0xFFFFD580)),
                    minFontSize = 12.sp,
                    maxFontSize = 28.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Shared preview helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun PreviewScaffold(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color(0xFF1A1A2E)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun ConstrainedTextGroup(
    texts: List<String>,
    boxWidth: Dp,
) {
    val sharedState = remember(texts) { SharedTextSizeState() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        texts.forEachIndexed { index, text ->
            AnnotatedBox(
                modifier = Modifier
                    .width(boxWidth)
                    .height(52.dp),
            ) {
                SharedAutoSizeText(
                    text = text,
                    sharedState = sharedState,
                    modifier = Modifier.fillMaxSize(),
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    ),
                    maxFontSize = 32.sp,
                )
            }
            if (index < texts.lastIndex) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun AnnotatedBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF16213E))
            .border(
                width = 1.dp,
                color = Color(0xFF0F3460),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun EdgeCaseRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Column {
        DimLabel(label)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8892B0),
            letterSpacing = 0.5.sp,
        ),
    )
}

@Composable
private fun DimLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 11.sp,
            color = Color(0xFF4A5568),
        ),
        modifier = modifier,
    )
}