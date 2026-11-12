package com.jht.spinbikeapps.targettraining.common


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jht.spinbikeapps.targettraining.model.TabBarItem
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_4
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREEN_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.ORANGE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.RED_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.YELLOW_1
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


@Composable
internal fun TabView(tabBarItems: List<TabBarItem>, navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState()
        .value?.destination?.route
    val selectedTabIndex = tabBarItems.indexOfFirst {
        it.route == currentRoute
    }.let { if (it == -1) 0 else it }
    val componentTag = "TargetTabBarComp"
    Box(
        modifier = Modifier.fillMaxSize()
            .semantics {
            testTagsAsResourceId = true
        }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp.computeSize())
                .size(
                    width = 792.dp.computeSize(1920f),
                    height = 64.dp.computeSize(1920f)
                )
                .border(
                    1.dp.computeSize(1920f),
                    color = DynamicTheme.colors.gray4,
                    shape = RoundedCornerShape(
                        topStart = 16.dp.computeSize(1920f),
                        topEnd = 16.dp.computeSize(1920f)
                    )
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp.computeSize(1920f),
                        topEnd = 16.dp.computeSize(1920f)
                    )
                )
                .background(DynamicTheme.colors.gray1)
        ) {
            tabBarItems.forEachIndexed { index, tabBarItem ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TabBarIconView(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                navController.navigate(tabBarItem.route) {
                                    launchSingleTop = true
                                }
                            }.testTag("$componentTag:SelectTargetType:TabBarIconView"),
                        index = index,
                        isSelected = selectedTabIndex == index,
                        selectedIcon = tabBarItem.selectedIcon,
                        unselectedIcon = tabBarItem.unselectedIcon,
                        title = tabBarItem.title
                    )
                    if (index != tabBarItems.lastIndex) {
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd),
                            color = GRAY_4,
                            thickness = 1.dp.computeSize(1920f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBarIconView(
    modifier: Modifier = Modifier,
    index: Int,
    isSelected: Boolean,
    selectedIcon: Int,
    unselectedIcon: Int,
    title: Int,
) {
    val indicatorColor = when (index) {
        0 -> BLUE_1
        1 -> YELLOW_1
        2 -> RED_1
        3 -> ORANGE_1
        4 -> GREEN_1
        else -> Color.White
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (isSelected) {
                    GRAY_4
                } else {
                    Color.Transparent
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp.computeSize(1920f)))
        Image(
            painter = painterResource(
                if (isSelected) selectedIcon else unselectedIcon
            ),
            contentDescription = stringResource(title),
            modifier = Modifier.size(32.dp.computeSize(1920f))
        )
        Spacer(modifier = Modifier.height(12.dp.computeSize(1920f)))
        if (isSelected) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp.computeSize(1920f),
                color = indicatorColor,
            )
        } else {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp.computeSize(1920f),
                color = GRAY_4,
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun TabViewPreview() {
    val navController = rememberNavController()
    TabView(tabBarItems = listOf(), navController = navController)
}

