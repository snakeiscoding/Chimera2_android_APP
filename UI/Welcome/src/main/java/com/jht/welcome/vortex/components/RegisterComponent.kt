package com.jht.welcome.vortex.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.jht.theme.ThemeButton
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.largerThan16
import com.jht.welcome.R
import com.jht.welcome.sortFilterRegisterData
import com.jht.widgets.BlurBackgroundOverlay
import com.jht.widgets.FieldInput
import com.jht.widgets.FieldInputCollection
import com.jht.widgets.FieldInputData
import com.jht.widgets.FieldInputDefaults.defaultStyle
import com.jht.widgets.ScrollHintOverlay
import com.jht.widgets.helpers.AsyncUiState

// register fields supported by this UI
private val registerFieldOrder = listOf(
    UserFieldFunction.EmailAddress,
    UserFieldFunction.LoginId,
    UserFieldFunction.Password,
    UserFieldFunction.ConfirmPassword,
    UserFieldFunction.FirstName,
    UserFieldFunction.LastName,
    UserFieldFunction.Birthdate,
    UserFieldFunction.Gender,
    UserFieldFunction.UnitPreference,
    UserFieldFunction.Weight,
)

@SuppressLint("SuspiciousIndentation", "UnusedBoxWithConstraintsScope")
@Composable
internal fun RegisterComponent(
    modifier: Modifier = Modifier,
    rawData: RegisterData = RegisterData(),
    theme: DynamicThemeData = defaultDynamicTheme,
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    onRegister: (RegisterData) -> Unit = {},
    uiState: AsyncUiState = AsyncUiState.Unknown,
) {
    val enabled = uiState != AsyncUiState.Waiting
    val focusManager = LocalFocusManager.current

    var isTermsAndConditionAccepted by remember { mutableStateOf(false) }
    val showPrivacyPolicy = remember { mutableStateOf(false) }
    val showTermsAndConditions = remember { mutableStateOf(false) }

    val data = remember(rawData) { sortFilterRegisterData(rawData, registerFieldOrder) }
    val fieldList = remember(data) { FieldInputCollection() }

    var tosCheckboxIsChecked by remember { mutableStateOf(false) }


    val registerAction = {
        if (enabled) {
            if (fieldList.checkValidity(false) && fieldList.getLastValidity()) {
                focusManager.clearFocus()
                if (isTermsAndConditionAccepted) {
                    // If checkbox has been checked, go directly to register attempt
                    onRegister(data)
                } else {
                    // If checkbox hasn't been checked, ensure user is shown T&C
                    showTermsAndConditions.value = false

                    // Only T&C is assumed as a necessity
                    showPrivacyPolicy.value = false
                }
            }
        }
    }
    fieldList.clear()
    fieldList.addAll(data.fields.mapIndexed { i, field ->
        val doneCallback = if (i == data.fields.size - 1) registerAction else null
        FieldInputData.build(field, "RegisterField$i", doneCallback)
    })

    DynamicTheme(theme) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .clickableNoEffect { focusManager.clearFocus() },
            contentAlignment = Alignment.Center
        ) {
            val screenHeight = this.maxHeight
            val screenWidth = this.maxWidth
            Box(
                modifier = Modifier
                    .padding(top = screenHeight * 0.09f)
                    .width(if (largerThan16()) screenWidth * 0.52f else screenWidth * 0.6f)
                    .height(screenHeight * 0.85f),
                contentAlignment = Alignment.Center
            ) {
                if (showPrivacyPolicy.value || showTermsAndConditions.value)
                    TermsConditionPrivacyPolicyPopUp(
                        showPrivacyPolicy = showPrivacyPolicy,
                        onBack = {
                            showPrivacyPolicy.value = false
                            showTermsAndConditions.value = false
                        },
                        onClose = onClose,
                        showTermsAndConditions = showTermsAndConditions
                    )
                BlurBackgroundOverlay(
                    modifier = modifier.wrapContentSize(),
                    showCloseButton = false,
                    showBackButton = true,
                    onCloseButtonClick = onClose,
                    onBackButtonClick = onBack
                ) {
                    val fieldsPerRow = 2
                    val listState = rememberLazyListState()

                    Column(
                        Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 16.dp.computeSize(),
                            horizontal = 32.dp.computeSize()
                        ),
                    ) {
                        ScrollHintOverlay(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            scrimColor = Color.Black,          // match the blur overlay bg
                            tint = DynamicTheme.colors.text1
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    Text(
                                        text = stringResource(R.string.register_for_xid_tc),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = DynamicTheme.colors.text1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp.computeSize()),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )

                                    Spacer(Modifier.padding(6.dp.computeSize()))

                                    Text(
                                        text = stringResource(R.string.register_for_xid_description_pg),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DynamicTheme.colors.text1.copy(alpha = 0.8f),
                                        modifier = Modifier.width(screenWidth * 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.padding(10.dp.computeSize()))
                                }

                                for (r in fieldList.indices step fieldsPerRow) {
                                    val itemRange = r until r + fieldsPerRow
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            itemRange.forEach { c ->
                                                Spacer(Modifier.padding(end = 20.dp.computeSize()))
                                                if (c < fieldList.size)
                                                    FieldInput(
                                                        data = fieldList[c],
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(
                                                                screenHeight * 0.1f,
                                                                screenHeight * 0.13f
                                                            )
                                                            .align(Alignment.CenterVertically),
                                                        enabled = enabled,
                                                        fieldInputOptions = defaultStyle().copy(minHeight = screenHeight * 0.07f)
                                                    )
                                                else Spacer(modifier = Modifier.weight(1f))
                                                Spacer(Modifier.padding(end = 20.dp.computeSize()))
                                            }
                                        }
                                        Spacer(Modifier.padding(top = 12.dp.computeSize()))
                                    }
                                }
                            } // LazyColumn
                        } // ScrollHintOverlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 20.dp.computeSize(),
                                    top = 8.dp.computeSize()
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {

                            OutlinedCheckbox(
                                modifier = Modifier.testTag("TermsCheckBox"),
                                    checked = tosCheckboxIsChecked,
                                    onCheckedChange = { tosCheckboxChanged: Boolean ->
                                        if (!tosCheckboxChanged) { // Checkbox has been UNCHECKED
                                            // Set to true so user isn't shown again unless the
                                            // label is clicked
                                            isTermsAndConditionAccepted = false
                                            showTermsAndConditions.value = false
                                            showPrivacyPolicy.value = false
                                        } else {
                                            isTermsAndConditionAccepted = true
                                            showTermsAndConditions.value = true
                                            showPrivacyPolicy.value = false
                                        }
                                        tosCheckboxIsChecked = tosCheckboxChanged
                                },
                                size = 24.dp.computeSize(),
                                borderColor = DynamicTheme.colors.buttonPrimaryDefault,
                                checkColor = DynamicTheme.colors.buttonPrimaryDefault,
                                cornerRadius = 4.dp,
                                borderWidth = 2.dp
                            )

                            Text(
                                text = stringResource(R.string.i_accept_the_tc),
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy_tc),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable {
                                        showPrivacyPolicy.value = true
                                    },
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                textDecoration = TextDecoration.Underline
                            )
                            Text(
                                text = stringResource(R.string.ampersand),
                                modifier = Modifier.padding(start = 4.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = stringResource(R.string.terms_of_use_tc),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable { showTermsAndConditions.value = true },
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                        Spacer(Modifier.padding(top = 8.dp.computeSize()))

                        Row(
                            modifier = Modifier
                                .padding(
                                    start = 20.dp.computeSize(),
                                    top = 8.dp.computeSize()
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // REGISTER VIA PHONE
                            /* Not needed for now
                            ThemeButton(
                                onClick = { },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(screenHeight * 0.055f)
                                    .testTag("RegisterViaPhone"),
                                shape = MaterialTheme.shapes.large,
                                border = BorderStroke(0.dp, Color.Transparent),
                                colors = ButtonDefaults.buttonColors().copy(
                                    containerColor = DynamicTheme.colors.buttonGhostDefault
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = 24.dp.computeSize(),
                                    vertical = 6.dp.computeSize()
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.register_via_phone_tc),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DynamicTheme.colors.text1,
                                )
                            }
                            */

                            Spacer(modifier = Modifier.weight(2f))

                            // SUBMIT BUTTON
                            ThemeButton(
                                    // User will only be able to click the register button and
                                    // have the console attempt registration if
                                    // isTermsAndConditionAccepted is set true.
                                    // isTermsAndConditionAccepted can only be true if the
                                    // checkbox has been clicked.
                                onClick = registerAction,
                                modifier = Modifier
                                    .weight(2f)
                                    .height(screenHeight * 0.055f)
                                    .padding(horizontal = 12.dp.computeSize())
                                    .testTag("LoginButton"),
                                shape = MaterialTheme.shapes.large,
                                border = BorderStroke(0.dp, Color.Transparent),
                                colors = ButtonDefaults.buttonColors().copy(
                                    containerColor = DynamicTheme.colors.buttonPrimaryDefault
                                ),
                                enabled = enabled
                            ) {
                                Text(
                                    text = "${stringResource(R.string.register_tc)} ${
                                        stringResource(
                                            R.string.xid_tc
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DynamicTheme.colors.text2,
                                )
                            }
                        } // END BUTTON ROW

                        // FAILURE MESSAGE ROW
                        if (uiState == AsyncUiState.Failure) Text(
                            stringResource(id = R.string.register_fail_message),
                            Modifier.align(Alignment.CenterHorizontally),
                            color = DynamicTheme.colors.error
                        )

                    } // END COLUMN
                }
            }
        }
    }


}

@Composable
internal fun OutlinedCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    size: Dp = 24.dp.computeSize(),
    borderColor: Color = DynamicTheme.colors.buttonPrimaryDefault,
    checkColor: Color = DynamicTheme.colors.buttonPrimaryDefault,
    cornerRadius: Dp = 4.dp,
    borderWidth: Dp = 2.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_checkmark),
                contentDescription = "Checked",
                tint = checkColor,
                modifier = Modifier.fillMaxSize(0.6f) // tick scales to 60% of box
            )
        }
    }
}


//region PREVIEWS
@AllDevicePreviews
@Composable
private fun PreviewRegisterScreenDarkLarge() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
    { RegisterComponent(Modifier.fillMaxSize(), previewData()) }
}
@AllDevicePreviews
@Composable
private fun PreviewRegisterFailure() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
    {
        RegisterComponent(
            modifier = Modifier.fillMaxSize(),
            uiState = AsyncUiState.Failure,
            rawData = previewData(),
        )
    }
}

@Composable
private fun previewData(): RegisterData {
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
    return RegisterData(
        stringResource(id = R.string.register_tc),
        "",
        LoginType.Xid,
        fieldList
    )
}
//endregion PREVIEWS
