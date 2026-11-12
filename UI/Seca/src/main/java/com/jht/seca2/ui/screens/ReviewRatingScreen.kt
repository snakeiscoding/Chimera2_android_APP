package com.jht.seca2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.seca.R
import com.jht.seca2.ui.SecaColors
import com.jht.seca2.ui.components.SecaButton
import com.jht.seca2.ui.components.SecaStarRatingWidget
import com.jht.seca2.ui.components.SecaTopBar
import com.jht.ui.AllSecaDevicePreviews

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ReviewRatingScreen(
    onClickExit: () -> Unit,
    onClickViewSummary: () -> Unit,
    onRatingChanged: (Int) -> Unit,
    initialRating: Int = 0)
{
    val rating = remember { mutableIntStateOf(initialRating) }

    Box(
        modifier = Modifier
            .semantics {testTagsAsResourceId = true}
            .fillMaxSize()
            .background(color = SecaColors.neutralGray0)
    ) {

        SecaTopBar(
            onExit = { onClickExit() },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .testTag("seca:ReviewRatingScreen:SecaTopBar")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            //Title Text
            Text(
                modifier = Modifier
                    .testTag("seca:ReviewRatingScreen:Title:Text"),
                style = MaterialTheme.typography.headlineLarge,
                color = SecaColors.titleText,
                textAlign = TextAlign.Center,
                text = stringResource(R.string.rate_your_experience_tc)
            )

            Spacer(modifier = Modifier.size(48.dp))

            //Star Rating Widget
            SecaStarRatingWidget(
                modifier = Modifier,
                rating = rating.intValue,
                onRatingChanged = {
                    rating.intValue = it
                    onRatingChanged(rating.intValue)
                })

            Spacer(modifier = Modifier.size(64.dp))

            //View Summary Button
            SecaButton(
                text = stringResource(R.string.view_summary_tc),
                modifier = Modifier.testTag("seca:ReviewRatingScreen:ViewSummary:SecaButton")
            ) { onClickViewSummary() }
        }
    }
}

//region PREVIEWS
@AllSecaDevicePreviews
@Composable
fun ReviewRatingScreenPreview() {
    ReviewRatingScreen(
        initialRating = 3,
        onClickExit = {},
        onClickViewSummary = {},
        onRatingChanged = {}
    )
}