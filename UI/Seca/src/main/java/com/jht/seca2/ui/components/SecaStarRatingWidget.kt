@file:OptIn(ExperimentalComposeUiApi::class)

package com.jht.seca2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jht.seca.R
import com.jht.seca2.secaUiTheme
import com.jht.theme.DynamicTheme
import com.jht.ui.AllSecaDevicePreviews
import com.jht.ui.clickableNoEffect

@Composable
fun SecaStarRatingWidget(
    modifier: Modifier,
    maxRating: Int = 5,
    rating: Int = 0,
    onRatingChanged: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .testTag("seca:StarRating:Row")
        ,
        Arrangement.spacedBy(60.dp)
    ) {
        for (i in 1..maxRating) {
            val isFull = i <= rating

            SecaStar(
                modifier = Modifier
                    .size(72.dp)
                    .clickableNoEffect { onRatingChanged(i) }
                    .testTag("seca:StarRating:$i:SecaStar"),
                isFull = isFull)
        }
    }
}

@Composable
fun SecaStar(
    modifier: Modifier,
    isFull: Boolean = false
) {
    Image(
        modifier = modifier.testTag("seca:SecaStar:Image"),
        painter = painterResource(if (isFull) R.drawable.ic_seca_star_fill else R.drawable.ic_seca_star_empty),
        contentDescription = "Rating Star")
}

//region PREVIEWS
@AllSecaDevicePreviews
@Composable
fun SecaStarRatingWidgetPreview() {
    DynamicTheme(secaUiTheme){
        SecaStarRatingWidget(
            modifier = Modifier,
            rating = 3) {}
    }
}