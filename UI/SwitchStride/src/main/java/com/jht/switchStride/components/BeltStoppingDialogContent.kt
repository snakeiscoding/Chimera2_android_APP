package com.jht.switchStride.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.switchStride.R
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
internal fun BeltStoppingDialogContent() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(
            vertical = 30.dp.computeSize(), horizontal = 20.dp.computeSize()
        )
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.stopping_belt_tc),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp.computeSize()))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.stay_facing_forward_pg),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
@AllDevicePreviews
@Composable
private fun BeltStoppingDialogContentPreview(){
    BeltStoppingDialogContent()
}