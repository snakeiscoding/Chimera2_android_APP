package com.jht.switchStride.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.switchStride.R
import com.jht.switchStride.utils.cardLinearGradient
import com.jht.switchStride.utils.runScreenForwardBlue
import com.jht.switchStride.utils.runScreenReversePurple
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.PagerJumpingDotIndicator

@Composable
internal fun PagerCard(
    modifier: Modifier = Modifier,
    cardDataList: List<CardData>,
    isInReverse: Boolean = false
) {
    Card(
        modifier = modifier.size(
            width = 460.dp.computeSizeHd(), height = 640.dp.computeSizeHd()
        ),
        shape = RoundedCornerShape(size = 30.dp.computeSizeHd()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp.computeSizeHd()),
        border = BorderStroke(
            width = 1.dp.computeSizeHd(), Brush.linearGradient(
                colors = if (isInReverse) {
                    listOf(runScreenReversePurple.copy(alpha = 0.6f), runScreenReversePurple)
                } else {
                    listOf(runScreenForwardBlue.copy(alpha = 0.6f), runScreenForwardBlue)
                }
            )
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = Color.Transparent
        )
    ) {
        val pagerState = rememberPagerState { cardDataList.size.coerceAtLeast(minimumValue = 1) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = cardLinearGradient),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState
            ) { page ->
                val cardData = cardDataList[page.coerceIn(range = cardDataList.indices)]

                Column(
                    verticalArrangement = Arrangement.spacedBy(space = 16.dp.computeSizeHd()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(id = cardData.icon),
                            contentDescription = "Data Attribute Icon",
                            modifier = Modifier.size(size = 32.dp.computeSizeHd()),
                            tint = if (isInReverse) runScreenReversePurple else runScreenForwardBlue
                        )
                        Spacer(modifier = Modifier.width(width = 16.dp.computeSizeHd()))
                        Text(
                            text = cardData.attributeName,
                            fontSize = 22.sp.computeSize(designWidth = 1920f),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7F)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 30.dp.computeSizeHd()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cardData.value.firstOrNull()?.let { firstValue ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = firstValue.first,
                                    fontSize = 100.sp.computeSize(designWidth = 1920f),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                // Units
                                if (firstValue.second.isNotBlank()) {
                                    Text(
                                        text = firstValue.second,
                                        fontSize = 20.sp.computeSize(designWidth = 1920f),
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (cardData.value.size > 1) { // in case of calories --- two data points
                            VerticalDivider(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .height(height = 92.dp.computeSizeHd())
                            )
                            val lastValue = cardData.value.last()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lastValue.first,
                                    fontSize = 100.sp.computeSize(designWidth = 1920f),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = lastValue.second,
                                    fontSize = 20.sp.computeSize(designWidth = 1920f),
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7F)
                                )
                            }
                        }
                    }
                }
            }

            if (cardDataList.size > 1) {
                PagerJumpingDotIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp.computeSizeHd()),
                    state = pagerState,
                    dotSize = 16.dp.computeSizeHd(),
                    activeColor = Color.White,
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewPagerCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(painterResource(id = R.drawable.blue_background)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multi-page example: Calories / Heart Rate
            PagerCard(
                modifier = Modifier,
                cardDataList = listOf(
                    CardData(
                        R.drawable.calories_switch_stride,
                        "Calories",
                        listOf("60" to "Active", "60" to "Total")
                    ),
                    CardData(
                        R.drawable.calories_switch_stride,
                        "Heart Rate",
                        listOf("120" to "BPM")
                    )
                )
            )
            // Single-page example: Time Elapsed (no dots)
            PagerCard(
                modifier = Modifier,
                cardDataList = listOf(
                    CardData(
                        R.drawable.ic_time_total_white,
                        "Time Elapsed",
                        listOf("10:34" to "Minutes")
                    )
                )
            )
            // Multi-page example: Distance / RPM
            PagerCard(
                modifier = Modifier,
                cardDataList = listOf(
                    CardData(
                        R.drawable.ic_distance_white,
                        "Distance",
                        listOf("6.0" to "Miles")
                    ),
                    CardData(
                        R.drawable.ic_distance_white,
                        "RPM",
                        listOf("80" to "RPM")
                    )
                )
            )
        }
    }
}

data class CardData(
    @field:DrawableRes val icon: Int,
    val attributeName: String,
    val value: List<Pair<String, DataUnit>>
)

private typealias DataUnit = String