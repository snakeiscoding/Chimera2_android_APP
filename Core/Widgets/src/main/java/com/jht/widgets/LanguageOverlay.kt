package com.jht.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.ui.Device22inPreviews
import com.jht.ui.clickableNoEffect

@Composable
fun LanguageOverlay(
    languageList: List<String>,
    selectedLanguageIndex: Int,
    showLanguageOverlay: MutableState<Boolean>,
    onLanguageSelected: (Int) -> Unit
) {
    if(showLanguageOverlay.value) {
        BlurBackgroundOverlay(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .clickableNoEffect { showLanguageOverlay.value = false },
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.select_language_tc),
                    modifier = Modifier
                        .padding(36.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = DynamicTheme.colors.text1,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.headlineSmall
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 36.dp)
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(languageList) { index, language ->
                        LanguageBox(language, selectedLanguageIndex, index, onLanguageSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBox(
    language: String,
    selectedLanguageIndex: Int,
    index: Int,
    onLanguageSelected: (Int) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxHeight(0.07f)
            .padding(vertical = 5.dp)
            .clickable { onLanguageSelected(index) }
            .then(
                if (index == selectedLanguageIndex)
                    Modifier.border(
                        width = 1.dp,
                        color = DynamicTheme.colors.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                        .background(DynamicTheme.colors.primary.copy(0.1f),shape = RoundedCornerShape(10.dp))

                else
                    Modifier.border(
                        width = 1.dp,
                        color = DynamicTheme.colors.gray3,
                        shape = RoundedCornerShape(10.dp)
                    )
                        .background(Color(0xFF2C2B2B).copy(0.4f),shape = RoundedCornerShape(10.dp))

            )
            .padding(horizontal = 24.dp, vertical = 21.dp),
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Text(
            text = language,
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            color = DynamicTheme.colors.text1,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Device22inPreviews
@Composable
private fun PreviewLanguageOverlay() {
    Box(modifier = Modifier.fillMaxSize()){
        LanguageOverlay(listOf("English", "Spanish", "French"), 0, remember { mutableStateOf(true) }) {}
    }
}

