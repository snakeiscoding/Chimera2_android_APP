package com.jht.chimera.ui.mainapp.createaccount.fragment;

import static com.jht.chimera.lib.common.unit.WeightUnit.KILOGRAM;
import static com.jht.chimera.lib.common.unit.WeightUnit.POUND;
import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.edittext.validator.InputValidationResult;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.dapi.DapiCreateUser;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.DateFormat;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.Unit;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.common.value.Weight;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.dialogs.TermsAndConditionsDialog;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.chimera.ui.mainapp.WelcomeActivity;
import com.jht.chimera.ui.mainapp.createaccount.LoginRegisterActivity;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.um.GenderIndentity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

/**
 * Registration page. Fragment that displays and validates the information needed to register a new user.
 */
@SuppressLint("SdCardPath")
public class RegisterFragment extends Fragment {
    private String TAG = "RegisterFragment";


    private UserTextInput xid;
    private UserTextInput passcode;
    private UserTextInput confirmPasscode;
    private UserTextInput email;
    private UserTextInput firstName;
    private UserTextInput lastName;
    private UserTextInput birthdate;
    private OptionSelectorItem genderSelector;

    private RadioGroup units;
    private UserTextInput weight;
    private CheckBox termsAndConditions;
    private AppCompatImageButton registerButton;
    private ImageView spinner;
    private String currentXid = "";

    private int userWeightLbs = 0;
    private int userWeightKg = 0;

    private DataMatrix mDataMatrix;
    private boolean goButtonEnabled = false;

    private IdleTimerManagement idleTimerManagement = new IdleTimerManagement();
    private TermsAndConditionsDialog mTermsDialog;

    private DigitalSafetyPinDialog safetyPrompt;

    protected Unit getUnitPreferenceFromForm() {
        return this.units.getCheckedRadioButtonId() == R.id.units_metric_radio_button ? Unit.METRIC : Unit.IMPERIAL;
    }

    protected Weight getWeightFromForm() throws NumberFormatException {
        return new Weight(Double.parseDouble(weight.getText()), this.units.getCheckedRadioButtonId() == R.id.units_metric_radio_button ? KILOGRAM : POUND);
    }

    private TextWatcher passcodeWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            checkConfirmPasscode(false);
        }
    };

    private final TextWatcher firstNameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            checkFirstName();
        }
    };

    private final TextWatcher lastNameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            checkLastName();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.register_fragment_layout, container, false);
        mDataMatrix = new DataMatrix();
        int screenTimeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);
        if (screenTimeout > 0) {
            idleTimerManagement.addTimer(new JHTTimer(screenTimeout * 1000, () -> {
                hideKeyboard(getActivity());
                genderSelector.closePopup();
                if (getActivity() instanceof WelcomeActivity) {
                    ((WelcomeActivity) getActivity()).showWelcome();
                } else if (getActivity() instanceof LoginRegisterActivity) {
                    ((LoginRegisterActivity) getActivity()).showWelcome();

                }
            }));
        }
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        idleTimerManagement.stopMonitoring();
        mDataMatrix.finish();
        goButtonEnabled = false;

        if (mTermsDialog != null) {
            mTermsDialog.hide();
            mTermsDialog = null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.LOGIN_REGISTER.name());

        // Allow screen to timeout if screen not tapped & no hard physical buttons pressed
        idleTimerManagement.monitorTimers();

        Activity activity = getActivity();
        Handler handler = new Handler();
        DapiCreateUser.DapiRegisterParcelable user = new DapiCreateUser.DapiRegisterParcelable();
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

        View view = getView();
        if (view == null) return;
        xid = view.findViewById(R.id.register_xid);
        passcode = view.findViewById(R.id.register_passcode);
        confirmPasscode = view.findViewById(R.id.register_confirm_passcode);
        email = view.findViewById(R.id.register_email_address);
        firstName = view.findViewById(R.id.register_first_name);
        lastName = view.findViewById(R.id.register_last_name);
        birthdate = view.findViewById(R.id.register_birthdate);
        units = view.findViewById(R.id.units_radio_group);
        weight = view.findViewById(R.id.register_weight);
        termsAndConditions = view.findViewById(R.id.register_accept_tc);
        registerButton = view.findViewById(R.id.register_user_confirm);
        spinner = view.findViewById(R.id.register_user_spinner);

        xid.setText("");
        passcode.setText("");
        confirmPasscode.setText("");
        email.setText("");
        firstName.setText("");
        lastName.setText("");
        birthdate.setText("");
        termsAndConditions.setChecked(false);

        setupXIDEntry();
        setupPasscodeEntry();
        setupConfirmPasscodeEntry();
        setupEmailEntry();
        setupNameEntry();
        setupBirthdateEntry(user);
        setupUnitsSelection(config);
        setupGenderEntry(view);

        // Weight and Units
        weightSetup(config);
        unitsSetup(config);

        setupTermsAndConditions();

        // Hide/show register button
        handler.post(this::checkInputsValid);

        registerButtonSetup(activity, user, handler);

        getView().setOnTouchListener((v, event) -> {
            if (getActivity() != null) {
                InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (getActivity().getCurrentFocus() != null && getActivity().getCurrentFocus().getWindowToken() != null && manager != null) {
                        manager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
            }
            return false;
        });

        goButtonEnabled = true;
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) {
                if (keyCode == KeyPress.KeyCode.KEY_GO && keyState == KeyPress.KeyState.KEY_DOWN) {
                    if (goButtonEnabled) {
                        checkIfSafetyPinNeeded(() -> goButtonEnabled = !GoScreenActivity.startGoButtonWorkout());
                    }
                }
            }
        });
    }

    private void setupXIDEntry() {
        currentXid = "";
        xid.setOnTextInvalid(inputValidationResult -> getString(R.string.invalid_xid_pg));
        xid.setValidator((value, previousValue) -> {
            if (value.length() < 10) {
                // xID must be 10-14 characters
                return new InputValidationResult(InputValidationResult.RESULT.GENERAL_ERROR, "", "");
            } else if (!currentXid.equals(value)) {
                currentXid = value;

                // Spinner icon next to field
                xid.setValidating(true);

                DapiLogin.checkXidExists(mDataMatrix, value, xidExists -> {
                    if (isAdded()) {
                        if (xidExists != null && xidExists) {
                            // xID already exists, force new user to select a new one
                            xid.setIsValid(false, getString(R.string.xid_unavailable_tc));
                        } else {
                            // xID doesn't exist, allow user to create new account
                            xid.setIsValid(true, "");
                        }

                        // Update text field
                        xid.initInput();

                        // Show/hide registration checkmark based on all fields
                        checkInputsValid();
                    } else {
                        Log.e(TAG, "Callback on fragment that was not added");
                    }
                });
            }
            return new InputValidationResult(InputValidationResult.RESULT.CHECKING, value, "");
        });
    }

    private void setupPasscodeEntry() {
        // Set Confirm Passcode field to disabled for now
        confirmPasscode.setEnabled(false);

        // Checks passcode length after keyboard entry button hit
        passcode.setOnIMEAction(action -> {
            moveToConfirmPasscode();
            return true;
        });

        // Checks passcode length after text has changed, without necessarily hitting enter on the keyboard
        passcode.setOnTextChanged(text -> moveToConfirmPasscode());

        // Checks passcode length after text changed
        passcode.addTextWatcher(passcodeWatcher);

        passcode.setOnTextInvalid(inputValidationResult -> getString(R.string.passcode_invalid_tc));
    }

    private void setupConfirmPasscodeEntry() {
        // Checks passcode length after keyboard entry button hit
        confirmPasscode.setOnIMEAction(action -> checkConfirmPasscode(false));

        confirmPasscode.setOnTextChanged(text -> checkConfirmPasscode(false));

        // Passcode and Confirm Passcode do NOT match
        confirmPasscode.setOnTextInvalid(inputValidationResult -> getString(R.string.passcode_invalid_tc));
    }

    private void setupEmailEntry() {
        // Verify email valid via DAPI call
        email.setOnFocusChanged(action -> verifyEmail());

        // If email detected as invalid after clicking another field, show message
        email.setOnTextInvalid(inputValidationResult -> getString(R.string.invalid_email_address_pg));
    }

    private void setupNameEntry() {
        // Verify First Name isn't empty, then verifies remaining fields
        firstName.addTextWatcher(firstNameWatcher);

        // Verify Last Name isn't empty, then verifies remaining fields
        lastName.addTextWatcher(lastNameWatcher);
    }

    /**
     *
     * @param user
     */
    private void setupBirthdateEntry(DapiCreateUser.DapiRegisterParcelable user) {
        // When field is closed, move on to Weight field
        birthdate.setOnBirthdateSet(() -> {
            birthdate.clearFocus();
            weight.requestFocus();
        });

        // Callback for new date
        birthdate.setOnDatePicked((view1, year, month, dayOfMonth) -> {
            user.birthYear = year;
            user.birthDay = dayOfMonth;
            user.birthMonth = month + 1;
        });

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -14);
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -120);
        String hint;
        int dateFormat = DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT);
        JHTCommonTypes.DateFormat df;
        if (dateFormat == DateFormat.YEAR_MONTH_DAY.ordinal()) {
            hint = getString(R.string.yyyy_mm_dd_ac);
            df = JHTCommonTypes.DateFormat.YearMonthDay;
        } else if (dateFormat == DateFormat.DAY_MONTH_YEAR.ordinal()) {
            hint = getString(R.string.dd_mm_yyyy_ac);
            df = JHTCommonTypes.DateFormat.DayMonthYear;
        } else if (dateFormat == DateFormat.MONTH_DAY_YEAR.ordinal()) {
            hint = getString(R.string.mm_dd_yyyy_ac);
            df = JHTCommonTypes.DateFormat.MonthDayYear;
        } else {
            hint = getString(R.string.yyyy_mm_dd_ac);
            df = JHTCommonTypes.DateFormat.YearMonthDay;
        }
        birthdate.setInputProperties(birthdate.getInputProperties().max(maxDate.getTimeInMillis()).min(minDate.getTimeInMillis()).hint(hint).dateFormat(df));
    }

    /**
     *
     * @param config
     */
    private void setupUnitsSelection(FrameConfig config) {
        units.check(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.id.units_metric_radio_button : R.id.units_imperial_radio_button);
        units.setOnCheckedChangeListener((group, checkedId) -> {
            weight.setInputProperties(weight.getInputProperties().hint(getString(checkedId == R.id.units_metric_radio_button ? R.string.kg_lc : R.string.lbs_lc)));
            if (checkedId == R.id.units_metric_radio_button) {
                weight.setInputProperties(weight.getInputProperties().min(config.weightKg.getMin()).max(config.weightKg.getMax()));
                if (!weight.getText().isEmpty()) {
                    weight.setText(userWeightKg + "");
                }
            } else {
                weight.setInputProperties(weight.getInputProperties().min(config.weightLbs.getMin()).max(config.weightLbs.getMax()));
                if (!weight.getText().isEmpty()) {
                    weight.setText(userWeightLbs + "");
                }
            }
        });
    }

    /**
     * Sets up dropdown.
     * @param view
     */
    private void setupGenderEntry(View view) {
        genderSelector = view.findViewById(R.id.registrationGenderSelector);
        genderSelector.setOptions(R.array.gender_list, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT));
        genderSelector.setOnOptionSelected((pos, option) -> DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, pos));
    }

    private void weightSetup(FrameConfig config) {
        weight.setOnTextInvalid(inputValidationResult -> getString(R.string.invalid_weight_pg));
        weight.setInputProperties(weight.getInputProperties().autoCorrect(true).hint(getString(units.getCheckedRadioButtonId() == R.id.units_metric_radio_button ? R.string.kg_lc : R.string.lbs_lc)));
        weight.setOnTextChanged(s -> {
            checkInputsValid();
            String weightString = weight.getText();
            if (weightString != null && !weightString.isEmpty()) {
                try {
                    int userWeight = Integer.parseInt(weightString);
                    if (units.getCheckedRadioButtonId() == R.id.units_metric_radio_button) {
                        userWeightKg = userWeight;
                        userWeightLbs = Math.round(WeightConvertUtil.convertKilogramsToPounds(userWeight));
                    } else {
                        userWeightLbs = userWeight;
                        userWeightKg = Math.round(WeightConvertUtil.convertLbsToKg(userWeight));
                    }
                } catch (Exception ignored) {}
            }
        });

        if (units.getCheckedRadioButtonId() == R.id.units_metric_radio_button) {
            weight.setInputProperties(weight.getInputProperties().min(config.weightKg.getMin()).max(config.weightKg.getMax()));
        } else {
            weight.setInputProperties(weight.getInputProperties().min(config.weightLbs.getMin()).max(config.weightLbs.getMax()));
        }

        weight.setOnIMEAction(i -> {
            checkInputsValid();
            if (registerButton.getVisibility() == View.VISIBLE) {
                registerButton.callOnClick();
            }
            return false;
        });
    }

    private void unitsSetup(FrameConfig config) {
        units.check(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.id.units_metric_radio_button : R.id.units_imperial_radio_button);
        units.setOnCheckedChangeListener((group, checkedId) -> {
            weight.setInputProperties(weight.getInputProperties().hint(getString(checkedId == R.id.units_metric_radio_button ? R.string.kg_lc : R.string.lbs_lc)));
            if (checkedId == R.id.units_metric_radio_button) {
                weight.setInputProperties(weight.getInputProperties().min(config.weightKg.getMin()).max(config.weightKg.getMax()));
                if (!weight.getText().isEmpty()) {
                    weight.setText(userWeightKg + "");
                }
            } else {
                weight.setInputProperties(weight.getInputProperties().min(config.weightLbs.getMin()).max(config.weightLbs.getMax()));
                if (!weight.getText().isEmpty()) {
                    weight.setText(userWeightLbs + "");
                }
            }
        });
    }

    private void setupTermsAndConditions() {
        //TODO: Fix this?!
        termsAndConditions.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                termsAndConditions.performClick();
                int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
                LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
                File textDir = new File("/sdcard/jht/privacy_policy/", languageProperty.locale);
                if (!textDir.exists()) {
                    textDir = new File("/sdcard/jht/privacy_policy/", languageProperty.locale.split("_")[0]);
                }
                if (!textDir.exists()) {
                    textDir = new File("/sdcard/jht/privacy_policy/", "en");
                }
                File policyFile = new File(textDir, "privacy_policy.html");
                if (policyFile.exists()) {
                    byte[] buffer = new byte[(int) policyFile.length()];
                    try (FileInputStream fin = new FileInputStream(policyFile)) {
                        //noinspection ResultOfMethodCallIgnored
                        fin.read(buffer);
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e);
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }

                if (mTermsDialog != null) {
                    mTermsDialog.hide();
                    mTermsDialog = null;
                }
                mTermsDialog = ProjectDialogFactory.termsAndConditionsDialog(termsAndConditions, policyFile.toURI().toString(), b -> {
                    termsAndConditions.setChecked(b);
                    checkInputsValid();
                });
            }

            return false;
        });
    }

    /**
     * Show/hide registration checkmark based on all fields
     */
    private void checkInputsValid() {
        if ((xid.getIsValid() && xid.getText().length() >= 10) && (passcode.getIsValid() && !passcode.getText().isEmpty())
                && (confirmPasscode.getIsValid() && !confirmPasscode.getText().isEmpty()) && (email.getIsValid() && !email.getText().isEmpty())
                && (!firstName.getText().isEmpty()) && (!lastName.getText().isEmpty()) && (birthdate.getIsValid() && !birthdate.getText().isEmpty())
                && (weight.getIsValid() && !weight.getText().isEmpty()) && termsAndConditions.isChecked()
        ) {
            registerButton.setVisibility(View.VISIBLE);
        } else {
            registerButton.setVisibility(View.GONE);
        }
    }

    private void registerButtonSetup(Activity activity, DapiCreateUser.DapiRegisterParcelable user, Handler handler) {
        registerButton.setOnClickListener(v -> {
            goButtonEnabled = false;
            if (activity instanceof WelcomeActivity) {
                ((WelcomeActivity) activity).setOverlayVisibility(true);
            } else if (activity instanceof LoginRegisterActivity) {
                ((LoginRegisterActivity) activity).setOverlayVisibility(true);
            }
            spinner.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.GONE);
            spinner.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.spinning_clockwise));
            if (xid.getIsValid()) {
                user.xid = xid.getText();
            } else {
                goButtonEnabled = false;
                idleTimerManagement.monitorTimers();
                ProjectDialogFactory.message(spinner, getString(R.string.register_fail_message), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                registerButton.setVisibility(View.VISIBLE);
            }
            user.passcode = passcode.getText();
            user.acceptedTerms = true;
            user.email = email.getText();
            user.firstName = firstName.getText();
            user.lastName = lastName.getText();
            user.setUnitPreferenceAndWeight(
                    getUnitPreferenceFromForm(),
                    getWeightFromForm());
            user.genderIndentity = GenderIndentity.fromInt(genderSelector.getSelectedPosition());

            idleTimerManagement.stopMonitoring();
            DapiCreateUser.createUser(mDataMatrix, user, dapiStatus -> handler.post(() -> {
                if (activity instanceof WelcomeActivity) {
                    ((WelcomeActivity) activity).setOverlayVisibility(false);
                } else if (activity instanceof LoginRegisterActivity) {
                    ((LoginRegisterActivity) activity).setOverlayVisibility(false);
                }
                if (dapiStatus == 0) {
                    DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, units.getCheckedRadioButtonId() == R.id.units_metric_radio_button);
                    LaunchAppUtil.launchGoScreen();
                } else {
                    goButtonEnabled = false;
                    idleTimerManagement.monitorTimers();
                    ProjectDialogFactory.message(spinner, getString(R.string.register_fail_message), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    registerButton.setVisibility(View.VISIBLE);
                }
                spinner.clearAnimation();
                spinner.setVisibility(View.GONE);
            }));
        });
    }

    private boolean verifyEmail() {
        String emailText = email.getText();

        if (!emailText.contains("@")) {
            email.setOnTextInvalid(inputValidationResult -> getString(R.string.invalid_email_address_pg));
        } else {
            /*
             * If email has "@" - this does not guarantee a legitimate email, but removes the edge case of having a singular character entered and being able to proceed. Plus, the
             * DAPI call should be what returns true/false for whether the email is available.
             */
            verifyEmailViaDAPI(email.getText());
        }
        return false;
    }

    /**
     * Verifies email{s} via DAPI call
     * @param s     email entered as a String
     */
    private void verifyEmailViaDAPI(String s) {
        // Shows the spinner image
        email.setValidating(true);

        // DAPI call to CBOR command 34, to verify whether the email already exists.
        DapiCreateUser.checkEmailExists(mDataMatrix, s, emailExists -> {
            if (isAdded()) {
                if (emailExists) {
                    // If email already exists, set email field to valid = false and hide register button
                    email.setIsValid(false, getString(R.string.email_already_in_use_message_pg));
                    registerButton.setVisibility(View.GONE);
                } else {
                    // Email can be used! Set field to valid, but do NOT show register button yet
                    email.setIsValid(true, "");
                }

                // Checks if email is empty, will show register if all other fields are also valid
                checkInputsValid();
            } else {
                Log.e(TAG, "Callback on fragment that was not added");
            }
        });
    }

    private void moveToConfirmPasscode() {
        String text = passcode.getText();
        if (text.length() == 4) {
            confirmPasscode.setEnabled(true);
            checkInputsValid();
            confirmPasscode.getFocus();
        } else {
            confirmPasscode.setEnabled(false);
            email.getFocus();
        }

        checkInputsValid();
    }

    private boolean checkConfirmPasscode(boolean updateFocus) {
        String text = passcode.getText();
        if (text.length() == 4) {
            confirmPasscode.setEnabled(true);
            if (!confirmPasscode.getText().isEmpty()) {
                if (!confirmPasscode.getText().equals(text)) {
                    confirmPasscode.setIsValid(false, getString(R.string.passcode_error_pg));
                } else {
                    confirmPasscode.setIsValid(true, "");
                }
                checkInputsValid();
            }
            if (updateFocus) {
                confirmPasscode.getFocus();
            }
        } else {
            confirmPasscode.setEnabled(false);
            if (updateFocus) {
                email.getFocus();
            }
        }

        checkInputsValid();
        return false;
    }

    private void checkFirstName() {
        if (!firstName.getText().isEmpty()) {
            firstName.setIsValid(true, "");
        } else {
            registerButton.setVisibility(View.GONE);
            firstName.setIsValid(false, getString(R.string.enter_first_name_tc));
        }
        checkInputsValid();
    }

    private void checkLastName() {
        if (!lastName.getText().isEmpty()) {
            lastName.setIsValid(true, "");
        } else {
            registerButton.setVisibility(View.GONE);
            lastName.setIsValid(false, getString(R.string.enter_last_name_tc));
        }
        checkInputsValid();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (passcode != null) {
            passcode.removeTextWatcher(passcodeWatcher);
        }

        if (firstName != null) {
            firstName.removeTextWatcher(firstNameWatcher);
        }
        if (lastName != null) {
            lastName.removeTextWatcher(lastNameWatcher);
        }
        mDataMatrix.finish();
    }



    public void enableGoButton(boolean enabled) {
        goButtonEnabled = enabled;
    }

    // safety pin stuff!
    private void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null)
                safetyPrompt = new DigitalSafetyPinDialog(requireActivity().getApplicationContext());
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }

}
