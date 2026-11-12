package com.jht.widgets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Regression coverage for the WorkoutScreen bug where collapsing the modal inside a left-aligned
 * `Column` (shared with a wider sibling) shrank the card from the right, landing the collapsed
 * button under the card's left edge instead of its right edge like every other screen.
 */
class CadenceStatModalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val style = CadenceModalStyle(
        fontFamily = FontFamily.SansSerif,
        titleWeight = FontWeight.Bold,
        valueWeight = FontWeight.Bold,
        accentColor = Color(0xFF33DB95),
        cardWidth = 252.dp,
    )

    /**
     * Right edge stays fixed when collapsed inside a start-aligned column with a wider sibling.
     *
     * Mirrors WorkoutScreen's layout: a `Column` with `horizontalAlignment = Alignment.Start`
     * whose other child ("Up Next") is wider than the cadence modal's own card width.
     */
    @Test
    fun collapsePreservesRightEdge() {
        var isHidden by mutableStateOf(false)

        rule.setContent {
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = "Wider sibling standing in for Up Next", modifier = Modifier.width(400.dp))
                CadenceStatModal(
                    spm = 170,
                    title = "Current Cadence",
                    unitLabel = "SPM",
                    icon = painterResource(R.drawable.ic_cadence),
                    iconContentDescription = "Cadence",
                    style = style,
                    isHidden = isHidden,
                    onDismiss = {},
                    onReopen = {},
                )
            }
        }

        val expandedNode = rule.onNodeWithTag(cadenceStatModalRootTestTag).fetchSemanticsNode()
        val expandedRightEdge = expandedNode.boundsInRoot.right

        isHidden = true
        rule.waitForIdle()

        val collapsedNode = rule.onNodeWithTag(cadenceStatModalRootTestTag).fetchSemanticsNode()
        val collapsedRightEdge = collapsedNode.boundsInRoot.right

        assertEquals(expandedRightEdge, collapsedRightEdge, 0.5f)
    }

    /**
     * Left edge stays fixed when collapse corner is top start.
     *
     * Covers [CadenceModalCollapseCorner.TOP_START] - not wired into any screen yet, but the
     * parameter exists for a future comp whose card collapses toward the top-left instead of the
     * top-right.
     */
    @Test
    fun collapsePreservesLeftEdge() {
        var isHidden by mutableStateOf(false)
        val topStartStyle = style.copy(collapseCorner = CadenceModalCollapseCorner.TOP_START)

        rule.setContent {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Wider sibling on the opposite side", modifier = Modifier.width(400.dp))
                CadenceStatModal(
                    spm = 170,
                    title = "Current Cadence",
                    unitLabel = "SPM",
                    icon = painterResource(R.drawable.ic_cadence),
                    iconContentDescription = "Cadence",
                    style = topStartStyle,
                    isHidden = isHidden,
                    onDismiss = {},
                    onReopen = {},
                )
            }
        }

        val expandedNode = rule.onNodeWithTag(cadenceStatModalRootTestTag).fetchSemanticsNode()
        val expandedLeftEdge = expandedNode.boundsInRoot.left

        isHidden = true
        rule.waitForIdle()

        val collapsedNode = rule.onNodeWithTag(cadenceStatModalRootTestTag).fetchSemanticsNode()
        val collapsedLeftEdge = collapsedNode.boundsInRoot.left

        assertEquals(expandedLeftEdge, collapsedLeftEdge, 0.5f)
    }
}
