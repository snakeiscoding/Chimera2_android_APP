package com.jht.widgets

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import com.jht.theme.DynamicTheme
import com.jht.ui.clickableNoEffect


@Composable
fun BandWidthTestPopup(
    bandwidthTestVisible: MutableState<Boolean>,
) {
    if (bandwidthTestVisible.value) {
        Popup(
            onDismissRequest = {
                bandwidthTestVisible.value = false
            },
            alignment = Alignment.Center

        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .clickableNoEffect(
                    onClick = { bandwidthTestVisible.value = false },
                ),
                contentAlignment = Alignment.Center
            ){
                BandWidthTest(false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BandWidthTest(preview:Boolean) {

    Box(modifier = Modifier
        .background(Color.White)
        .clickableNoEffect(
            onClick = {/* do nothing, so clicking inside the settings window doesn't dismiss it */ },
        )
        .fillMaxSize(.8f),
        contentAlignment = Alignment.Center
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.bandwidth_test_tc),
                            color = DynamicTheme.colors.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DynamicTheme.colors.gray1
                    )
                )
            },
            content = { padding ->
                BandWidthWebView(
                    modifier = Modifier.padding(padding)
                )
            }
        )
    }
}

// Creating a composable
// function to create WebView
// Calling this function as
// content in the above function
@SuppressLint("SetJavaScriptEnabled") // get warning about XSS vulnerability, but we need JS to run
@Composable
private fun BandWidthWebView(modifier: Modifier){

    // TODO - make this grab from the API?
    // rather than hard code it, I think it can be set from network setup?
    // SettingKeys.Experimental.DATA_BANDWIDTH_TEST_URL_STRING
    // Declare a string that contains a url
    val mUrl = "https://fast.com"

    // Adding a WebView inside AndroidView
    // with layout as full screen
    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                // javaScriptEnabled is needed for fast.com to update speed
                settings.javaScriptEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36"
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                loadUrl(mUrl)
            }
        },
        update = {
            it.loadUrl(mUrl)
        },
        modifier = modifier
    )
}