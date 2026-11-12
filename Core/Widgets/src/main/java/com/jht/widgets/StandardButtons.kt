package com.jht.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize

/*
    File for common simple buttons that we don't want to repeat everywhere.
    More complex button widgets might be found in other dedicated widget files.
 */


/** Standard themed "Start Workout" button, commonly for workout setup dialogs */
@Composable
fun StartWorkoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidthText: Boolean = true,
    buttonColor : Color = DynamicTheme.colors.play,
    buttonTitle : Int = R.string.start_workout_tc,
    btnTextColor: Color = DynamicTheme.colors.text1,
    btnShape: CornerBasedShape = MaterialTheme.shapes.medium,
    btnTextStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    Button(
        onClick,
        Modifier.then(modifier),
        enabled,
        contentPadding = PaddingValues(14.dp.computeSize()),
        shape = btnShape,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
    ) {
        Text(
            text = stringResource(buttonTitle),
            modifier = if (fullWidthText) Modifier.requiredWidth(IntrinsicSize.Max) else Modifier,
            color = btnTextColor,
            fontSize = 22.sp.computeSize(),
            style = btnTextStyle,
            textAlign = TextAlign.Center,
        )
    }
}

/** Standard themed HOME button */
@Composable
fun HomeButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    ThemeButton(
        modifier = Modifier
            .testTag("HomeButton")
            .then(modifier),
        contentPadding = PaddingValues(),
        onClick = onClick
    ) {
        Icon(
            painterResource(id = R.drawable.ic_home),
            null,
            Modifier.fillMaxSize(0.5f),
            DynamicTheme.colors.text1,
        )
    }
}

@Composable
fun PTPButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    ThemeButton(
        modifier = Modifier
            .testTag("PTPButton")
            .then(modifier),
        contentPadding = PaddingValues(),
        onClick = onClick,
    ) {
        Icon(
            painterResource(id = R.drawable.ic_ptp),
            null,
            Modifier.fillMaxSize(0.5f),
            DynamicTheme.colors.text1,
        )
    }
}

@Composable
fun PaceLogoButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor = if (isPressed) Color(0xFFb52532) else Color(0xFFf72f42)
    Box(
        modifier = modifier
            .testTag("PaceLogoButton")
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .clickableNoEffect(interactionSource = interactionSource, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(id = R.drawable.ic_pace_logo_white),
            contentDescription = "A pace logo icon",
            Modifier.fillMaxSize(0.8f),
            DynamicTheme.colors.text1,
        )
    }
}

@Preview
@Composable
fun PaceLogoButtonPreview() {
    PaceLogoButton(
        modifier = Modifier
            .size(width = 100.dp, height = 50.dp),
        onClick = {}
    )
}