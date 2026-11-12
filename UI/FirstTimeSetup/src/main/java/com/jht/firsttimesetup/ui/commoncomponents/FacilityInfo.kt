package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
internal fun FacilityInfo(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    testTag: String
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            label,
            modifier = Modifier.testTag("$testTag:Label"),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Start
        )

        Text(
            value,
            modifier = Modifier.testTag("$testTag:Text"),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }

}