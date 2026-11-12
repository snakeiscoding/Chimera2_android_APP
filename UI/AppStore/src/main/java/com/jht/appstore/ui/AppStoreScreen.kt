package com.jht.appstore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.jht.appstore.interfaces.AppStoreAppInfo
import com.jht.appstore.interfaces.MockAppStoreModel
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppStoreScreen(viewModel: AppStoreViewModel) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
        ,
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            state.items.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // No app list found
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = { it.app.uuid }) { item ->
                    AppCard(
                        item = item,
                        onInstall = { viewModel.onInstall(item.app) },
                        onOpen = { viewModel.onLaunch(item.app) },
                        onUninstall = { viewModel.onUninstall(item.app) },
                        showDeleteIcon = viewModel.showDeleteIcon()
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AppCard(
    item: AppItemUiState,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onUninstall: () -> Unit,
    showDeleteIcon: Boolean = false
) {
    val buttonWidth = 96.dp
    val colorSuccess = Color(0xFFC8E6C9)
    val colorDelete = Color(0xFFF44336)
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = item.app.iconUrl,
                contentDescription = "${item.app.name} icon",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.app.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("AppStore:AppTitle:Text"))
                Spacer(Modifier.height(6.dp))
                Text(
                    item.app.description,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("AppStore:AppDescription:Text"))
            }
            Spacer(Modifier.width(12.dp))
            when {
                item.installing -> Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("AppStore:Download:${item.app.name}:InProgress"),
                        strokeWidth = 3.dp)
                }

                item.installed -> Column {
                    FilledTonalButton(
                        onClick = onOpen,
                        modifier = Modifier
                            .width(buttonWidth)
                            .testTag("AppStore:Installed:${item.app.name}:Button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorSuccess
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_appstore_open),
                            contentDescription = "Open"
                        )
                    }
                    if (showDeleteIcon) {
                        FilledTonalButton(
                            onClick = onUninstall,
                            modifier = Modifier
                                .width(buttonWidth)
                                .testTag("AppStore:Uninstall:${item.app.name}:Button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorDelete
                            )
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_appstore_delete),
                                contentDescription = "Uninstall"
                            )
                        }
                    }
                }

                item.installFailed -> FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .width(buttonWidth)
                        .testTag("AppStore:InstalledFailed:${item.app.name}:Button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.ic_appstore_install_failed),
                        contentDescription = "Failed"
                    )
                }

                else -> FilledTonalButton(
                    onClick = onInstall,
                    modifier = Modifier
                        .width(buttonWidth)
                        .testTag("AppStore:Install:${item.app.name}:Button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_appstore_install),
                        contentDescription = "Install"
                    )
                }
            }
        }
    }
}


// ---- PREVIEW ----
private fun sampleApp(
    name: String,
    description: String = "A sample description for $name. This is only for preview.",
    installing: Boolean = false,
    installed: Boolean = false
) = AppStoreAppInfo(
    name = name,
    packageName = "com.example.$name",
    iconUrl = "https://placehold.co/64x64.png?text=App",
    description = description,
    downloadUrl = "https://example.com/$name.apk",
    installing = installing,
    installed = installed
)

private val previewItems: List<AppStoreAppInfo> = listOf(
    sampleApp("App One"),
    sampleApp("App Two", installed = true),
    sampleApp("App Three"),
    sampleApp(
        "App Four",
        "This is a long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long " +
                "This is a long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long " +
                "This is a long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long description",
        installing = true
    ),
)
private val mockAppStoreModel = MockAppStoreModel(previewItems)

@Device15inPreviews
@Device22inPreviews
@Composable
fun Preview_AppStoreScreen() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface {
            AppStoreScreen(AppStoreViewModel(mockAppStoreModel))
        }
    }
}

@Preview(showBackground = true, widthDp = 1024)
@Composable
fun Preview_AppCard() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.padding(12.dp)) {
            AppCard(
                item = AppItemUiState(previewItems.first()),
                onInstall = {},
                onOpen = {},
                onUninstall = {},
            )
        }
    }
}

