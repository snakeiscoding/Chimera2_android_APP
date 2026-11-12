package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BTN_DEFAULT_BORDER
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_4
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
internal fun ZoneButton(
    modifier: Modifier = Modifier,
    zone: String,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val componentTag = "ZoneButtonComp"
    Button(
        modifier = modifier
            .height(64.dp.computeSize(1920f))
            .semantics {
                testTagsAsResourceId = true
            }.testTag("$componentTag:Zone:Button")
        ,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp.computeSize(1920f)),
        colors = ButtonDefaults.buttonColors(containerColor = GRAY_4),
        border = BorderStroke(
            2.dp.computeSize(1920f),
            if (isSelected) textColor else BTN_DEFAULT_BORDER
        )
    ) {
        Text(
            text = zone,
            color = textColor,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 22.sp.computeSize(1920f)
            ),
            maxLines = 1
        )
    }
}

@AllDevicePreviews
@Composable
private fun ZoneButtonPreview(){
    ZoneButton(
        zone = "1",
        textColor = Color.Red,
        isSelected = false
    ) { }
}