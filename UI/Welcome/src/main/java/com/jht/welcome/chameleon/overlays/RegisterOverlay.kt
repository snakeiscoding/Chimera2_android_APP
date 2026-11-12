package com.jht.welcome.chameleon.overlays

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.concierge.ui.ConciergeButton
import com.jht.data.LoginType
import com.jht.data.RegisterData
import com.jht.data.UserField
import com.jht.data.UserFieldDigits
import com.jht.data.UserFieldEmail
import com.jht.data.UserFieldFunction
import com.jht.data.UserFieldInteger
import com.jht.data.UserFieldText
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.ThemeFlatButton
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.Device10inPreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.clickableNoEffect
import com.jht.welcome.R
import com.jht.welcome.sortFilterRegisterData
import com.jht.widgets.FieldInput
import com.jht.widgets.FieldInputCollection
import com.jht.widgets.FieldInputData
import com.jht.widgets.PrivacyPolicyPopUp
import com.jht.widgets.helpers.AsyncUiState

// register fields supported by this UI
private val registerFieldOrder = listOf(
    UserFieldFunction.LoginId,
    UserFieldFunction.Password,
    UserFieldFunction.ConfirmPassword,
    UserFieldFunction.EmailAddress,
    UserFieldFunction.FirstName,
    UserFieldFunction.LastName,
    UserFieldFunction.Birthdate,
    UserFieldFunction.Gender,
    UserFieldFunction.UnitPreference,
    UserFieldFunction.Weight,
)

@SuppressLint("MutableCollectionMutableState") // should probably look into this more with FieldInputCollection later
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterOverlay(
    rawData : RegisterData,
    modifier: Modifier = Modifier,
    theme: DynamicThemeData = defaultDynamicTheme,
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    onRegister: (RegisterData)->Unit = {},
    uiState: AsyncUiState = AsyncUiState.Unknown,
) {
    val enabled = uiState != AsyncUiState.Waiting
    val focusManager = LocalFocusManager.current
    val spaceWidth = 20.dp
    val spaceHeight = when(isFullscreen){
        true -> 20.dp
        false -> 32.dp
    }
    val data = remember(rawData) { sortFilterRegisterData(rawData, registerFieldOrder) }

    var isTermsAndConditionAccepted by remember { mutableStateOf(false) }
    val showPrivacyPolicy = remember { mutableStateOf(false) }

    // we need changes to this to trigger recompose, probably need to do something with side effect states, derived state, flows, or move this into the view model proper?
    val fieldList = remember(data) { mutableStateOf(FieldInputCollection()) }
    // UI has specific row for measurements, so we need to separate those
    val unitField = remember(data) { mutableStateOf<FieldInputData?>(null) }
    val weightField = remember(data) { mutableStateOf<FieldInputData?>(null) }
    val registerAction = {
        if (enabled) {
            val weightOk = weightField.value == null || weightField.value?.checkValidity()?.isValid() == true
            if (fieldList.value.checkValidity(false) && fieldList.value.getLastValidity() && weightOk) {
                focusManager.clearFocus()
                if (isTermsAndConditionAccepted) {
                    onRegister(data)
                } else {
                    showPrivacyPolicy.value = true
                }
            }
        }
    }
    LaunchedEffect(data) {
        val newList = FieldInputCollection()
        for (i in data.fields.indices) {
            val field = data.fields[i]
            val doneCallback = if (i == data.fields.lastIndex) registerAction else null // weight happens to be the last in the list, but this might not always be case?
            val newData = FieldInputData.build(field, "RegisterField$i", doneCallback)
            when (field.function) {
                UserFieldFunction.UnitPreference -> unitField.value = newData
                UserFieldFunction.Weight -> weightField.value = newData
                else -> newList.add(newData)
            }
        }
        fieldList.value = newList
    }


    DynamicTheme(theme) {
        Box(
            modifier = modifier
                .background(DynamicTheme.colors.background4)
                .clickableNoEffect { focusManager.clearFocus() }
        ) {
            PrivacyPolicyPopUp(showPrivacyPolicy = showPrivacyPolicy) {isTermsAndConditionAccepted = true}
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(.9f)
                    .fillMaxHeight()
                    .align(Alignment.Center)
            ) {
                // TOP BANNER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.15f)
                ) {
                    if(isFullscreen) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clickable { onBack() },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_outline_left),
                                contentDescription = null,
                                tint = DynamicTheme.colors.text1,
                                modifier = Modifier
                                    .fillMaxHeight(25 / 86f)
                                    .aspectRatio(1f)
                            )
                            Text(
                                text = stringResource(id = R.string.back_uc),
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .wrapContentWidth()
                                    .wrapContentHeight(),
                                color = DynamicTheme.colors.text1,
                                textAlign = TextAlign.Start,
                                style = DynamicTheme.typography.textStyle1
                            )
                        }
                        //Concierge Button
                        ConciergeButton(
                            modifier = Modifier.align(Alignment.CenterEnd).height(70.dp)
                        )
                    }
                    Text(
                        text = data.label,
                        modifier = Modifier
                            .wrapContentHeight()
                            .align(
                                when (isFullscreen) {
                                    true -> Alignment.Center
                                    false -> Alignment.BottomStart
                                }
                            ),

                        color = DynamicTheme.colors.text1,
                        textAlign = TextAlign.Center,
                        style = DynamicTheme.typography.textStyle3,
                    )
                }
                if(!isFullscreen) {
                    Spacer(modifier = Modifier.height(spaceHeight))
                }

                val rowModifier = Modifier
                    .fillMaxWidth()
                    .weight(.25f)

                // NON-MEASUREMENT ROWS
                val itemModifier = Modifier
                    .fillMaxHeight()
                    .weight(.3f)
                val fieldsPerRow = 3
                for (r in fieldList.value.indices step fieldsPerRow) {
                    Row(
                        modifier = rowModifier,
                        horizontalArrangement = Arrangement.spacedBy(spaceWidth),
                    ) {
                        val itemRange = r until r+fieldsPerRow
                        for (c in itemRange) {
                            if (c > fieldList.value.lastIndex) {
                                Spacer(itemModifier)
                            } else {
                                FieldInput(fieldList.value[c], itemModifier, enabled, fieldInputStyle)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spaceHeight))
                }

                // MEASUREMENT INPUT ROW
                val measurementCount = (unitField.value?.let {1} ?: 0) + (weightField.value?.let {1} ?: 0)
                Row(
                    modifier = rowModifier,
                    horizontalArrangement = Arrangement.spacedBy(spaceWidth),
                ) {
                    // Units
                    unitField.value?.let {
                        FieldInput(it, itemModifier, enabled, fieldInputStyle)
                    }
                    weightField.value?.let {
                        FieldInput(it, itemModifier, enabled, fieldInputStyle)
                    }
                    repeat(3 - measurementCount) {
                        Spacer(itemModifier)
                    }
                }

                // BOTTOM BANNER
                Box(
                    Modifier.fillMaxWidth().wrapContentHeight()
                ) {
                    // TERMS AND CONDITIONS CHECKBOX
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(spaceHeight * 6)
                            .align(Alignment.TopStart)
                            .clickable {
                                showPrivacyPolicy.value = true
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            Checkbox(
                                checked = isTermsAndConditionAccepted,
                                onCheckedChange = { if (it) showPrivacyPolicy.value = true else isTermsAndConditionAccepted = false }, // if checking, show the box, otherwise remove check
                                Modifier.testTag("TermsCheckBox"),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = DynamicTheme.colors.primary,
                                    uncheckedColor = DynamicTheme.colors.primary
                                ),
                                enabled = enabled,
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.accept_terms_and_condition_tc),
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(start = 10.dp),
                            color = DynamicTheme.colors.text1,
                            textAlign = TextAlign.Center,
                            style = DynamicTheme.typography.textStyle2,
                        )
                    }
                    // SUBMIT BUTTON
                    ThemeFlatButton(
                        stringResource(id = R.string.create_new_account_tc),
                        onClick = registerAction,
                        Modifier
                            .testTag("RegisterButton")
                            .width(250.dp)
                            .aspectRatio(250 / 70f)
                            .align(Alignment.BottomEnd),
                        enabled = enabled,
                        isPrimary = true,
                    )
                    // FAILURE MESSAGE?
                    if (uiState == AsyncUiState.Failure) Text(stringResource(id = R.string.register_fail_message), Modifier.align(Alignment.BottomStart), color = DynamicTheme.colors.error)
                }
                Spacer(modifier = Modifier.height(spaceHeight))
            } // Column
        } // BoxWithCon
    } // Theme
}




//region PREVIEWS
@Composable
private fun previewData() : RegisterData {
    val fieldList: List<UserField> = listOf(
        UserFieldDigits(
            UserFieldFunction.LoginId,
            stringResource(id = R.string.xid_tc),
            stringResource(id = R.string.enter_xid_2_tc),
            4,
            14
        ),
        UserFieldDigits(
            UserFieldFunction.Password,
            stringResource(id = R.string.passcode_tc),
            stringResource(id = R.string.enter_passcode_tc),
            0,
            4
        ),
        UserFieldDigits(
            UserFieldFunction.Password,
            stringResource(id = R.string.confirm_passcode_2_tc),
            stringResource(id = R.string.confirm_passcode_tc),
            0,
            4
        ),
        UserFieldEmail(
            UserFieldFunction.EmailAddress,
            stringResource(id = R.string.email_tc),
            stringResource(id = R.string.email_address_tc),
            1,
            65
        ),
        UserFieldText(
            UserFieldFunction.FirstName,
            stringResource(id = R.string.first_name_tc),
            stringResource(id = R.string.enter_first_name_tc),
            1,
            65
        ),
        UserFieldText(
            UserFieldFunction.LastName,
            stringResource(id = R.string.last_name_tc),
            stringResource(id = R.string.enter_last_name_tc),
            1,
            65
        ),
        UserFieldInteger(
            UserFieldFunction.Weight,
            stringResource(id = R.string.weight_tc),
            stringResource(id = R.string.enter_weight_tc),
            1,
            3
        ),
        //TODO: Waiting on height integration
        /*LoginField(
            LoginFieldType.Number,
            "Feet",
            "",
            1,
            1
        ),
        LoginField(
            LoginFieldType.Number,
            stringResource(id = R.string.inches_lc),
            "",
            1,
            2
        )*/
    )
    return RegisterData(stringResource(id = R.string.register_tc), "", LoginType.Xid, fieldList)
}

@Device10inPreviews
@Device15inPreviews
@Composable
private fun PreviewRegisterScreenDark() {
    RegisterOverlay(previewData(), Modifier.fillMaxSize(), isFullscreen = true)
}

@Device22inPreviews
@Composable
private fun PreviewRegisterScreenDarkLarge() {
    RegisterOverlay(previewData(), Modifier.fillMaxSize(), isFullscreen = false)
}
//endregion PREVIEWS
