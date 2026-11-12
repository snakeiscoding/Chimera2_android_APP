package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.ui.computeSize

/**
 * Reusable dropdown menu
 * @param [title] title of drop down
 * @param [menu] list of items user can select from drop down
 * @param [expandMenu] controls whether the drop down is expanded
 * @param [onExpandClicked] -> passes the inverse of current [expandMenu]
 * @param [onExpandedMenuItemSelected] handles selection of menu item
 * @param [selectedItemIndex] currently selected item index 0 by default
 * @param [dropDownExpandedColor] color for arrow icon when expanded
 * @param [dropDownCollapsedColor] color for arrow icon when collapsed
 * @param [labelColor] color for title
 * @param [testTag] test tags for this component should follow the following structure
 * Activity:ObjectName this component handles the last part as the menu requires an index for
 * menu items. Please omit the :WidgetType as specified in
 * https://jhtnaee.atlassian.net/wiki/spaces/DT/pages/4281991208/Compose+Test+Tags
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DropDownMenu(
    modifier: Modifier = Modifier,
    title: String,
    menu: List<String>,
    expandMenu: Boolean = false,
    onExpandClicked: (expandMenu: Boolean) -> Unit = {},
    onExpandedMenuItemSelected: (itemIndex: Int) -> Unit = {},
    selectedItemIndex: Int = 0,
    dropDownExpandedColor: Color = Color.White.copy(.33f),
    dropDownCollapsedColor: Color = Color.White.copy(.33f),
    labelColor: Color = Color.White.copy(.69f),
    testTag: String,
    menuWidth: androidx.compose.ui.geometry.Size,
) {

    Column(
        modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = Modifier
                .testTag(testTag)
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(4.dp.computeSize()))

        Card(
            modifier = Modifier
                .testTag("$testTag:Button")
                .clickable {
                    val shouldExpand = !expandMenu
                    onExpandClicked(shouldExpand)
                }
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
            border = BorderStroke(1.dp, colorResource(R.color.selected_color)),
            shape = RoundedCornerShape(
                topStart = 5.dp,
                topEnd = 5.dp,
                bottomStart = if (!expandMenu) 5.dp else 0.dp,
                bottomEnd = if (!expandMenu) 5.dp else 0.dp,
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    menu.getOrNull(selectedItemIndex) ?: "Index Error",
                    style = MaterialTheme.typography.labelLarge, color = Color.White.copy(.98f)
                )

                Icon(
                    imageVector = if (expandMenu) {
                        Icons.Filled.KeyboardArrowUp
                    } else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "",
                    tint = if (expandMenu) {
                        dropDownExpandedColor
                    } else dropDownCollapsedColor
                )
            }
        }


        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp


        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = colorResource(R.color.grey_7),
                surfaceTint = colorResource(R.color.grey_7)
            ),
            shapes = MaterialTheme.shapes.copy(RoundedCornerShape(bottomStart = 5.dp , bottomEnd = 5.dp,))
        ){
            DropdownMenu(
                modifier = Modifier
                    .heightIn(max = screenHeightDp / 2f - 16.dp)
                    .width(with(LocalDensity.current) { menuWidth.width.toDp() - 32.dp })
                    .border(
                        1.dp,
                        color = colorResource(R.color.selected_color),
                        shape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp),
                    ).background(color = colorResource(R.color.grey_7 ))
                    .clipToBounds(),
                expanded = expandMenu,
                onDismissRequest = { onExpandedMenuItemSelected(selectedItemIndex) },
            ) {
                menu.forEachIndexed { index, item ->
                    val isSelected = index == selectedItemIndex

                    DropdownMenuItem(
                        text = {
                            Text(
                                item,
                                modifier = Modifier
                                    .testTag("$testTag$index:Button"),
                                color = if (isSelected) colorResource(R.color.dark_text) else colorResource(
                                    R.color.light_text
                                ),
                                overflow = TextOverflow.Clip
                            )
                        },
                        onClick = { onExpandedMenuItemSelected(index) },
                        modifier = Modifier
                            .background(
                                color =
                                    if (isSelected) colorResource(R.color.selected_color)
                                    else colorResource(R.color.grey_7),
                                shape =
                                    if (isSelected) RectangleShape
                                    else RoundedCornerShape(10.dp)
                            ),
                    )
                }
            }
        }
    }

}

@Preview
@Composable
private fun DropDownMenuPreview() {
    val menu = listOf("Power Puff Girls", "Spider-Man", "Goblins", "Basket of Oranges", "Cookies")
    DropDownMenu(menu = menu, title = "Random thoughts", modifier = Modifier.height(100.dp), testTag = "", menuWidth = androidx.compose.ui.geometry.Size.Zero)
}

@Preview
@Composable
private fun DropDownExpandedMenuPreview() {
    val menu = listOf("Power Puff Girls", "Spider-Man", "Goblins", "Basket of Oranges", "Cookies")
    DropDownMenu(menu = menu, title = "Random thoughts", expandMenu = true, testTag = "", menuWidth = androidx.compose.ui.geometry.Size.Zero)
}