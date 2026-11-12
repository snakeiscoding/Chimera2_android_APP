package com.jht.widgets.helpers

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private typealias TabComposable = @Composable (selected: Boolean, onSelect: ()-> Unit) -> Unit // provide optional Any data for passing data down?
private typealias BodyComposable = @Composable () -> Unit
private typealias KeyType = Any // should we template the class? provide a strongly typed wrapper?
private typealias SelectFunction = (key: KeyType) -> Unit

// way to store tab components? should this be a widget helper? could this evolve into cleaner widget?
class TabItem(
    val tab: TabComposable,
    val body: BodyComposable,
    val key: KeyType, // should we have a tracking object of some kind in case we need to match separate instances?
) {
    //fun isSameKey(other: TabItem?) : Boolean = (this.key != null && this.key == other?.key) // do not consider null as a match
}

//fun List<TabItem>.findKey(key: Any) : TabItem? = this.find { it.key == key } // since Key parameter cannot be null, don't need to check for null keys

// do we need a way to handle keys in a strong typed manner without being dependent on underlying UI system?
//@JvmInline value class TabKey(val key: Any?)



class TabList(
    val selectedKey: KeyType? = null,
    val onSelect: SelectFunction = {},
) : ArrayList<TabItem>() {

    constructor(selectedKey: KeyType?, onSelect: SelectFunction, items: Iterable<TabItem>) : this(selectedKey, onSelect) {
        this.addAll(items)
    }

    fun add(tab: TabComposable, body: BodyComposable, key: KeyType) = this.add(TabItem(tab, body, key))
    fun add(vararg tabItems: TabItem): TabList {
        this.addAll(tabItems)
        return this
    }

    @Composable
    fun CurrentTabBody(
        noneComposable: @Composable () -> Unit = {},
    ) {
        val item = this.find { it.key == selectedKey }
        if (item != null) item.body() else noneComposable()
    }

    @Composable
    fun TabColumn(
        modifier: Modifier = Modifier,
        state: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        verticalArrangement: Arrangement.Vertical =
            if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
    ) {
        LazyColumn(modifier, state, contentPadding, reverseLayout, verticalArrangement, horizontalAlignment, flingBehavior, userScrollEnabled) {
            items(this@TabList, key = {it.key}) { item ->
                item.tab(selectedKey == item.key) { onSelect(item.key) }
            }
        }
    }

    @Composable
    fun TabRow(
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        this@TabList.forEach{ item->
            Row(modifier.padding(contentPadding)) {
                item.tab(selectedKey == item.key) { onSelect(item.key) }
            }
        }
    }
}
