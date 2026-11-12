package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BTN_CONTENT_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_CONTENT_COLOR
import com.jht.theme.ThemeButton
import com.jht.theme.themeButtonColor
import com.jht.ui.AllDevicePreviews

/**
 * Displays an info button typically used in the Sprint8 top banner.
 * @param modifier Modifier applied to the button for layout, styling, and interaction.
 * @param onClick Callback invoked when the button is clicked.
 * @param isEnabled Boolean indicating whether the button is enabled. Defaults to true.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8TopBannerInfoButton(
    modifier: Modifier,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    val componentTag = "Sprint8TopBannerInfoButton"
    ThemeButton(
        modifier = Modifier
            .then(modifier)
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag("$componentTag:InfoButton:Button"),
        contentPadding = PaddingValues(),
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = themeButtonColor,
            contentColor = SPRINT8_BTN_CONTENT_COLOR,
            disabledContainerColor = SPRINT8_DISABLED_BTN_COLOR,
            disabledContentColor = SPRINT8_DISABLED_BTN_CONTENT_COLOR
        )
    ) {
        Icon(
            painterResource(id = R.drawable.ic_info),
            modifier = Modifier
                .fillMaxSize(0.6f)
                .testTag("$componentTag:InfoImage:Image"),
            contentDescription = null,
        )
    }
}

@AllDevicePreviews
@Composable
fun Sprint8TopBannerInfoButtonPreview(){
    Sprint8TopBannerInfoButton(
        modifier = Modifier,
        onClick = {},
        isEnabled = true
    )
}