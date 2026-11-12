package com.jht.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Lays out items in equal sized boxes. [itemComposable] composable gets item from [items] based on [startingIndex] as grid is filled. */
@Composable
fun <T> BoxGrid(
    rows: Int,
    columns: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    startingIndex: Int = 0,
    spacing: Dp = 0.dp,
    itemComposable: @Composable BoxScope.(BoxGridCell<T>) -> Unit,
) {
    // LazyVerticalGrid is scrollable and does not enforce children sizes, this will
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(rows) { row ->
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(columns) { column ->
                    Box(
                        Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val index = startingIndex + row * columns + column
                        if (index < items.size) {
                            itemComposable(
                                BoxGridCell(items[index], index, row, column)
                            )
                        }
                    }
                }
            }
        }
    }
}
/** useful context for composable in a [BoxGrid] */
class BoxGridCell<T>(
    val item: T,
    val index: Int,
    val row: Int,
    val column: Int,
) {
    override fun toString(): String = "($row, $column) [$index] item=$item"
}


/** A [Row] that uses [IntrinsicSize.Min] and [Alignment.CenterVertically] */
@Composable
fun CenterMinRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .height(IntrinsicSize.Min),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
