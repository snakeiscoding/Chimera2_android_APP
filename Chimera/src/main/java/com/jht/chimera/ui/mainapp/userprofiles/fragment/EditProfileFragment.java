package com.jht.chimera.ui.mainapp.userprofiles.fragment;

import static com.jht.chimera.lib.common.unit.WeightUnit.KILOGRAM;
import static com.jht.chimera.lib.common.unit.WeightUnit.POUND;
import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.edittext.validator.InputValidationResult;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.dapi.DapiCreateUser;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.DateFormat;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.Unit;
import com.jht.chimera.lib.common.unit.WeightUnit;
import com.jht.chimera.lib.common.value.Weight;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.mainapp.userprofiles.UserProfileDialog;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.DapiResult;
import com.jhtp.dapiservice.um.GenderIndentity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

/**
 * Fragment that displays and validates the information needed to register a new user
 */
@SuppressWarnings("DuplicateExpressions")
public class EditProfileFragment extends Fragment implements View.OnTouchListener {
    private String TAG = "EditProfileFragment";

    private UserProfileManager.ProfileUser mUser;
    private RadioGroup userType;
    private View editSidebar;
    private View updatePassCodeCaption;

    private UserTextInput xid;
    private UserTextInput passcode;
    private UserTextInput confirmPasscode;
    private UserTextInput email;
    private UserTextInput firstName;
    private UserTextInput lastName;
    private UserTextInput birthdate;

    private ConstraintLayout editFields;

    private RadioButton offline_user;
    private RadioButton online_user;
    private RadioButton new_user;
    private RadioButton update_user;

    private TextView user_information_text;

    private OptionSelectorItem genderSelector;
    private TextView unitsLabel;
    private RadioGroup units;
    private UserTextInput weight;
    private CheckBox termsAndConditions;
    private ImageView acceptButton;
    private RelativeLayout removeButton;
    private ImageView spinner;
    private ScrollView scroller;
    private String currentXid = "";

    private int userWeightLbs = 0;
    private int userWeightKg = 0;
    private int xIDMinLength = 0;

    private DataMatrix mDataMatrix;
    private Handler mHandler = new Handler();
    private DapiCreateUser.DapiRegisterParcelable userParcel;
    private boolean isEditingProfile = false;
    private boolean updatePasscodeOnly = false;

    private UserProfileDialog mParent;

    public EditProfileFragment(UserProfileDialog parent) {
        super();
        mParent = parent;
    }
    private void close() {
        exitBusy();
        hideKeyboard(getActivity());
        // close entire profile dialog or just go back to selection?
        mParent.showSelectFragment();
        //mParent.hide();
    }

    // seeing some exceptions with Fragment.getString
    private String getResString(@StringRes int id) { return mParent.getResources().getString(id); }

    protected Unit getUnitPreferenceFromForm() {
        return this.units.getCheckedRadioButtonId() == R.id.units_metric_radio_button ? Unit.METRIC : Unit.IMPERIAL;
    }
    protected Weight getWeightFromForm() throws NumberFormatException {
        return new Weight(Integer.valueOf(weight.getText()), this.units.getCheckedRadioButtonId() == R.id.units_metric_radio_button ? KILOGRAM : POUND);
    }
    protected Weight getWeightOrZeroFromForm() {
        try {
            return getWeightFromForm();
        } catch (NumberFormatException e) {
            return new Weight(0, POUND);
        }
    }

    private void hideInputs() {
        xid.setVisibility(View.GONE);
        passcode.setVisibility(View.GONE);
        confirmPasscode.setVisibility(View.GONE);
        email.setVisibility(View.GONE);
        firstName.setVisibility(View.GONE);
        lastName.setVisibility(View.GONE);
        birthdate.setVisibility(View.GONE);
        genderSelector.setVisibility(View.GONE);
        unitsLabel.setVisibility(View.GONE);
        units.setVisibility(View.GONE);
        weight.setVisibility(View.GONE);
        termsAndConditions.setVisibility(View.GONE);
    }

    private TextWatcher passcodeWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            checkConfirmPasscode(false);
        }
    };

    // Should we pass user or just get the current selected from UserProfileManager ?
    public void setUser(UserProfileManager.ProfileUser user, boolean updatePasscode) {
        mUser = user; // TODO - use the actual instance or create a copy for editting then update original?
        updatePasscodeOnly = updatePasscode;
        setUserToGui(mUser);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_edit_fragment_layout, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scroller = view.findViewById(R.id.profile_edit_scroller);
        userType = view.findViewById(R.id.user_type_radio_group);
        editSidebar = view.findViewById(R.id.edit_sidebar);
        updatePassCodeCaption = view.findViewById(R.id.update_passcode);

        xid = view.findViewById(R.id.register_xid);
        passcode = view.findViewById(R.id.register_passcode);
        confirmPasscode = view.findViewById(R.id.register_confirm_passcode);
        email = view.findViewById(R.id.register_email_address);
        firstName = view.findViewById(R.id.register_first_name);
        lastName = view.findViewById(R.id.register_last_name);
        birthdate = view.findViewById(R.id.register_birthdate);
        genderSelector = view.findViewById(R.id.registrationGenderSelector);
        unitsLabel = view.findViewById(R.id.preferred_units_label_text_view);
        units = view.findViewById(R.id.units_radio_group);
        weight = view.findViewById(R.id.register_weight);
        termsAndConditions = view.findViewById(R.id.register_accept_tc);
        removeButton = view.findViewById(R.id.user_remove);
        acceptButton = view.findViewById(R.id.user_accept);
        spinner = view.findViewById(R.id.register_user_spinner);

        //  detect the touch in scroll view and parent area?
        view.setOnTouchListener(this);
        scroller.setOnTouchListener(this);

        mDataMatrix = new DataMatrix();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataMatrix.finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();

        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        userParcel = new DapiCreateUser.DapiRegisterParcelable();
        clearXidBox();
        passcode.setText("");
        confirmPasscode.setText("");
        email.setText("");
        firstName.setText("");
        lastName.setText("");
        birthdate.setText("");
        termsAndConditions.setChecked(false);
        lastName.setNextFocusForwardId(R.id.register_birthdate); // default for offline user

        userType.setOnCheckedChangeListener((group, checkedId) -> {
            hideInputs(); // just hide all first, then show what you need

            switch (checkedId) {
                case R.id.user_type_xid_new:
                case R.id.user_type_online:
                    new_user.setVisibility(View.VISIBLE);
                    user_information_text.setText(getResources().getString(R.string.online_user_description_pg));
                    break;
                case R.id.user_type_xid_update:
                    new_user.setVisibility(View.INVISIBLE);
                    user_information_text.setText("");
                    break;
                case R.id.user_type_offline:
                    new_user.setVisibility(View.INVISIBLE);
                    user_information_text.setText(getResources().getString(R.string.offline_user_description_pg));
                    break;
            }

            switch (checkedId) {
                case R.id.user_type_xid_new:
                    xIDMinLength = 10;// min for new xID is 10
                    xid.setInputProperties(xid.getInputProperties()
                            .minChars(xIDMinLength)
                            .hint(StringLookup.get(R.string.enter_xid_tc)));
                    clearXidBox();
                    xid.resetValid();
                    xid.setValidator(this::validateXid);
                    passcode.setText("");
                    passcode.resetValid();
                    xid.setVisibility(View.VISIBLE);
                    passcode.setVisibility(View.VISIBLE);
                    passcode.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    passcode.setOnIMEAction(this::imePasscode);
                    confirmPasscode.setVisibility(View.VISIBLE);
                    email.setVisibility(View.VISIBLE);
                    lastName.setNextFocusForwardId(R.id.register_xid);
                    // fall through to next case for data fields
                case R.id.user_type_xid_update:
                case R.id.user_type_offline:
                    firstName.setVisibility(View.VISIBLE);
                    lastName.setVisibility(View.VISIBLE);
                    birthdate.setVisibility(View.VISIBLE);
                    genderSelector.setVisibility(View.VISIBLE);
                    unitsLabel.setVisibility(View.VISIBLE);
                    units.setVisibility(View.VISIBLE);
                    weight.setVisibility(View.VISIBLE);
                    termsAndConditions.setVisibility(View.VISIBLE);
                    resetField(firstName);
                    resetField(lastName);
                    resetField(email);
                    resetField(weight);
                    resetField(birthdate);
                    termsAndConditions.setChecked(false);
                    genderSelector.setOptions(R.array.gender_list, getDefaultGender().Value);
                    units.check(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.id.units_metric_radio_button : R.id.units_imperial_radio_button);
                    break;
                case R.id.user_type_online:
                    xIDMinLength = 4; // min for old xID is 4
                    xid.setInputProperties(xid.getInputProperties()
                            .minChars(xIDMinLength)
                            .hint(StringLookup.get(R.string.enter_xid_2_tc)));
                    clearXidBox();
                    xid.resetValid();
                    xid.setValidator(this::validateXid);
                    resetField(passcode);
                    xid.setVisibility(View.VISIBLE);
                    passcode.setVisibility(View.VISIBLE);
                    passcode.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    passcode.setOnIMEAction(this::imeDone);
                    break;
            }
            checkInputsValid();
        });

        birthdate.setOnBirthdateSet(() -> {
            birthdate.clearFocus();
            weight.getFocus();
        });

        currentXid = "";
        xid.setOnTextInvalid(inputValidationResult -> getResString(R.string.invalid_xid_pg));
        //xid.setValidator(this::validateXid);
        xid.setOnTextChanged(s -> {
            if(s.length() > 0 && s.length() < xIDMinLength) {
                xid.setIsValid(false, getResString(R.string.invalid_xid_pg));
                xid.initInput();
            }
        });

        confirmPasscode.setEnabled(false);
        passcode.addTextWatcher(passcodeWatcher);
        //passcode.setOnIMEAction(this::imePasscode);
        passcode.setOnTextInvalid(inputValidationResult -> getResString(R.string.passcode_invalid_tc));


        email.setOnTextChanged(s -> {
            if(email.getText() != null && email.getText().length() > 0 && s.length() > 0) {
                email.setValidating(true);
                DapiCreateUser.checkEmailExists(mDataMatrix, s, emailExists -> {
                    if (isAdded()) {
                        if (emailExists != null && emailExists) {
                            email.setIsValid(false, getResString(R.string.email_already_in_use_message_pg));
                        } else {
                            email.setIsValid(true, "");
                        }
                        checkInputsValid();
                    } else {
                        Log.e(TAG, "Callback on fragment that was not added");
                    }
                });
            }
        });
        email.setOnTextInvalid(inputValidationResult -> getResString(R.string.invalid_email_address_pg));

        confirmPasscode.setOnTextChanged(s -> {
            if(passcode.getText().equals(confirmPasscode.getText())) {
                confirmPasscode.setIsValid(true, "");
            }
            else {
                confirmPasscode.setIsValid(false, getResString(R.string.passcode_error_pg));
            }

            checkInputsValid();
        });
        confirmPasscode.setOnTextInvalid(inputValidationResult -> getResString(R.string.passcode_invalid_tc));

        weight.setOnTextInvalid(inputValidationResult -> getResString(R.string.invalid_weight_pg));
        genderSelector.setOptions(R.array.gender_list, getDefaultGender().Value);
        units.check(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.id.units_metric_radio_button : R.id.units_imperial_radio_button);
        units.setOnCheckedChangeListener((group, checkedId) -> {
            boolean metric = checkedId == R.id.units_metric_radio_button;
            WeightUnit displayUnit = metric ? KILOGRAM : POUND;
            RangeInfo weightRangeInfo = metric ? config.weightKg : config.weightLbs;

            weight.setInputProperties(weight.getInputProperties().hint(getResString(checkedId == R.id.units_metric_radio_button ? R.string.kg_lc : R.string.lbs_lc)));
            weight.setInputProperties(weight.getInputProperties().min(weightRangeInfo.getMin()).max(weightRangeInfo.getMax()));

            if (!weight.getText().isEmpty() && mUser != null) { // PRKS-1475
                weight.setText(String.valueOf(Math.round(mUser.getWeight().getValue(displayUnit))));
            }
//            else if (!weight.getText().isEmpty() && mUser == null){
//                try{
//                    double num = Double.parseDouble(weight.getText());
//                    WeightUnit oldUnit = metric ? POUND : KILOGRAM;
//                    num = WeightConvertUtil.convert(num, oldUnit, displayUnit);
//                    weight.setText(String.valueOf(Math.round(num)));
//                } catch (Exception ignored){}
//            }
        });
        weight.setInputProperties(weight.getInputProperties().hint(getResString(units.getCheckedRadioButtonId() == R.id.units_metric_radio_button ? R.string.kg_lc : R.string.lbs_lc)));
        weight.setOnTextChanged(s -> {
            //Log.d(TAG, "User weight on text changed " + s); // only seems to fire when focus moves away from the input
            checkInputsValid();
            String weightString = weight.getText();
            if(weightString != null && !weightString.isEmpty()) {
                try {
                    int userWeight = Integer.parseInt(weightString);
                    if(units.getCheckedRadioButtonId() == R.id.units_metric_radio_button) {
                        userWeightKg = userWeight;
                        userWeightLbs = Math.round(WeightConvertUtil.convertKilogramsToPounds(userWeight));
                    }
                    else {
                        userWeightLbs = userWeight;
                        userWeightKg = Math.round(WeightConvertUtil.convertLbsToKg(userWeight));
                    }

                } catch(Exception ignored) {
                }
            }
        });

        if(units.getCheckedRadioButtonId() == R.id.units_metric_radio_button) {
            weight.setInputProperties(weight.getInputProperties().min(config.weightKg.getMin()).max(config.weightKg.getMax()));
        }
        else {
            weight.setInputProperties(weight.getInputProperties().min(config.weightLbs.getMin()).max(config.weightLbs.getMax()));
        }

        weight.setOnIMEAction(this::imeDone);


        termsAndConditions.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {

                int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
                LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
                File textDir = new File("/sdcard/jht/privacy_policy/", languageProperty.locale);
                String policy = "";
                if(!textDir.exists()) {
                    textDir = new File("/sdcard/jht/privacy_policy/", languageProperty.locale.split("_")[0]);
                }
                if(!textDir.exists()) {
                    textDir = new File("/sdcard/jht/privacy_policy/", "en");
                }
                File policyFile = new File(textDir, "privacy_policy.html");
                if(policyFile.exists()) {
                    byte[] buffer = new byte[(int)policyFile.length()];
                    try(FileInputStream fin = new FileInputStream(policyFile)) {
                        fin.read(buffer);
                        //policy = "<![CDATA[" + new String(buffer) + "]]>";
                        policy =  Base64.encodeToString(buffer, Base64.NO_PADDING);
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e);
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }
                ProjectDialogFactory.termsAndConditionsDialog(termsAndConditions, policyFile.toURI().toString(), b -> {
                    termsAndConditions.setChecked(b);
                    checkInputsValid();
                });
            }

            return false;
        });

        birthdate.setOnDatePicked((view1, year, month, dayOfMonth) -> {
            userParcel.birthYear = year;
            userParcel.birthDay = dayOfMonth;
            userParcel.birthMonth = month + 1;
        });

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -14);
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -120);
        String hint;
        int dateFormat = DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT);
        JHTCommonTypes.DateFormat df;
        if(dateFormat == DateFormat.YEAR_MONTH_DAY.ordinal()) {
            hint = getResString(R.string.yyyy_mm_dd_ac);
            df = JHTCommonTypes.DateFormat.YearMonthDay;
        }
        else if(dateFormat == DateFormat.DAY_MONTH_YEAR.ordinal()) {
            hint = getResString(R.string.dd_mm_yyyy_ac);
            df = JHTCommonTypes.DateFormat.DayMonthYear;
        }

        else if(dateFormat == DateFormat.MONTH_DAY_YEAR.ordinal()) {
            hint = getResString(R.string.mm_dd_yyyy_ac);
            df = JHTCommonTypes.DateFormat.MonthDayYear;
        }
        else {
            hint = getResString(R.string.yyyy_mm_dd_ac);
            df = JHTCommonTypes.DateFormat.YearMonthDay;
        }

        birthdate.setInputProperties(birthdate.getInputProperties().max(maxDate.getTimeInMillis()).min(minDate.getTimeInMillis()).hint(hint).dateFormat(df));

        acceptButton.setOnClickListener(v -> {
            hideKeyboard(getActivity());
            acceptButton.requestFocus(); // force focus away from inputs first, so they run internal validation!
            checkInputsNotEmpty();
            if (!checkInputsValid()) return;
            enterBusy();

            int slot = (mUser == null ? UserProfileManager.getInstance().getNextAvailableSlotNumber() : mUser.slotNumber);
            userParcel.xid = isUserLocalSelected() ? DapiUser.createLocalXid(slot) : xid.getText();
            userParcel.passcode = passcode.getText();
            userParcel.slotNumber = slot;
            if (isUserOnlineSelected()) {
                // Prevent using xID that is already in another slot
                UserProfileManager.ProfileUser u = UserProfileManager.getInstance().getUserByXid(userParcel.xid);
                if (u != null && !updatePasscodeOnly) {
                    exitBusy();
                    // R.string.duplicate_xid_message_pg is too generic, use more specific message?
                    ProjectDialogFactory.message(spinner, getResString(R.string.profile_xid_exists_pg) + " (" + u.slotNumber + ")", null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                } else {
                    // call login to update profile ?
                    login(userParcel.xid, userParcel.passcode, slot);
                }
            } else {
                userParcel.acceptedTerms = true;
                userParcel.email = email.getText();
                userParcel.firstName = firstName.getText();
                userParcel.lastName = lastName.getText();
                userParcel.setUnitPreferenceAndWeight(
                        getUnitPreferenceFromForm(),
                        getWeightOrZeroFromForm()
                );
                userParcel.genderIndentity = GenderIndentity.fromInt(genderSelector.getSelectedPosition());

                if (isUserNewSelected()) { // creating new xid user
                    DapiCreateUser.createUserWithInfo(mDataMatrix, userParcel, dapiStatus -> mHandler.post(() -> {
                        if (isAdded()) {
                            if (dapiStatus == 0) {
                                this.close(); //login(userParcel.xid, userParcel.passcode, slot); // since we don't need to register for a slot, just close
                            } else {
                                exitBusy();
                                ProjectDialogFactory.message(spinner, getResString(R.string.register_fail_message), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                            }
                        }
                    }));
                } else { // update existing xid user
                    userParcel.userToken = (mUser == null ? null : mUser.userToken);
                    /** Waits for the update to finish! */
                    class UpdateListener {
                        // This seems to fix the problem of hitting save on an XID user and then
                        // not seeing the changes reflected immediately when the profile selection
                        // screen is shown.  Questions for the future:
                        // - Are these really the appropriate keys to wait for?
                        // - Could this code be simplified by using promises or otherwise
                        //   generalizing the 'wait for a bunch of events' pattern?
                        int dapiStatus = -1;
                        boolean profileUpdated = false;
                        boolean resolved = false;
                        public UpdateListener() {
                            mDataMatrix.createIntAdapter(DapiCreateUser.Keys.EVENT_UPDATE_USER_RESPONSE_INT, dapiStatus -> {
                                this.dapiStatus = dapiStatus;
                                this.maybeResolve();
                            });
                            mDataMatrix.createEventAdapter(UserProfileManager.Keys.EVENT_PROFILE_UPDATED, profileUpdated -> {
                               this.profileUpdated = true;
                               this.maybeResolve();
                            });
                        }
                        protected void maybeResolve() {
                            if (resolved) return;
                            if (dapiStatus == -1) return;
                            if (!profileUpdated) return;
                            resolve();
                        }
                        protected void resolve() {
                            if (isAdded()) {
                                if (dapiStatus == 0) {
                                    close();
                                } else {
                                    exitBusy();
                                    ProjectDialogFactory.message(spinner, getResString(R.string.register_fail_message), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                }
                            }
                            mDataMatrix.finish();
                            if (isEditingProfile){
                                LaunchAppUtil.launchSplashScreen();
                            }
                        }
                    }
                    new UpdateListener();
                    DataMatrix.putParcelable(DapiCreateUser.Keys.ACTION_UPDATE_USER_PARCEL, userParcel);
                }
            }
        });

        removeButton.setOnClickListener(v -> {
            if (mUser == null) return; // no user to remove

            UserProfileManager.ProfileUser currentUser = UserProfileManager.getInstance().getCurrentProfile();
            ProjectDialogFactory.confirm(removeButton, getResString(R.string.user_delete_pg), () -> {
                if (!isAdded()) return;
                enterBusy();
                DapiCreateUser.removeProfile(mDataMatrix, mUser.slotNumber, dapiStatus -> mHandler.post(() -> {
                    mParent.setOverlayVisibility(false);
                    if (dapiStatus == DapiResult.Success.Value || dapiStatus == DapiResult.UserNotFound.Value) {
                        this.close();
                    } else {
                        // Do we error if user is not there to unlink? R.string.user_profile_was_not_found_uc ?
                        exitBusy();
                        ProjectDialogFactory.message(spinner, getResString(R.string.profile_removal_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }

                    // It will launch the splash screen when removing the current selected profile on retail machine
                    if (ProductConfig.hasUserProfiles() && currentUser.slotNumber == mUser.slotNumber) {
                        LaunchAppUtil.launchSplashScreen();
                    }
                }));
            }, null);
        });

        setUserToGui(mUser);

        mHandler.post(this::checkInputsValid);
    }

    private void resetField(UserTextInput field){
        try {
            field.setText("");
            field.resetValid();
        } catch (Exception e){
            Log.d(TAG, "resetFields: error: "+e);
        }
    }

    private void enterBusy() {
        mParent.setOverlayVisibility(true);
        showButtons(false);
        spinnerState(true);
        mParent.idleTimerPause();
    }
    private void exitBusy() {
        mParent.setOverlayVisibility(false);
        showButtons(true);
        spinnerState(false);
        mParent.idleTimerResume();
    }

    private void login(String xid, String passcode, int slot) {
        // assume overlay/buttons/spin and idle timer has been set for operation already
        DapiLogin.loginWithInfo(mDataMatrix, xid, passcode, slot, false, DapiUtil.LOGIN_TYPE_XID, dapiResult -> mHandler.post(() -> {
            Log.d(TAG, "DapiLogin.login");
            if (isAdded()) {
                if (dapiResult == 0) {
                    this.close();
                } else {
                    exitBusy();
                    ProjectDialogFactory.message(spinner, getResString(R.string.login_failed_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }
            }
        }));
    }

    private void spinnerState(boolean run) {
        if (run) {
            spinner.setVisibility(View.VISIBLE);
            spinner.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.spinning_clockwise));
        } else {
            spinner.clearAnimation();
            spinner.setVisibility(View.GONE);
        }
    }

    private void checkConfirmPasscode(boolean updateFocus) {

        String text = passcode.getText();
        if(text.length() == 4) {
            confirmPasscode.setEnabled(true);
            if(updateFocus) {
                email.getFocus();
            }
        } else {
            confirmPasscode.setEnabled(false);
            if(updateFocus) {
                email.getFocus();
            }
        }

        checkInputsValid();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (passcode != null) passcode.removeTextWatcher(passcodeWatcher);
        mDataMatrix.finish();
    }

    private void showButtons(boolean isShown) {
        int visible = (isShown ? View.VISIBLE : View.INVISIBLE);
        acceptButton.setVisibility(visible);
        if (mUser != null && !updatePasscodeOnly) removeButton.setVisibility(visible);
    }

    private boolean checkInputsValid() {
        if(
            isInputValid(xid) &&
            isInputValid(passcode) && isInputValid(confirmPasscode) &&
            isInputValid(email) &&
            isInputValid(firstName) && isInputValid(lastName) &&
            isInputValid(birthdate) && isInputValid(weight) &&
            (termsAndConditions.getVisibility() != View.VISIBLE || termsAndConditions.isChecked())
        ) {
            acceptButton.setVisibility(View.VISIBLE);
            return true;
        }
        else {
            acceptButton.setVisibility(View.INVISIBLE);
            return false;
        }
    }
    private static boolean isInputValid(UserTextInput input) {
        if (input.getVisibility() == View.VISIBLE) {
            return (input.getIsValid() && input.getText().length() > 0);
        }
        return true; // not seen, not used, not invalid
    }
    private boolean checkInputsNotEmpty() {
        UserTextInput inputs[] = { xid, passcode, confirmPasscode, email, firstName, lastName, birthdate, weight };
        boolean isOk = true;
        for (UserTextInput input : inputs) {
            if (input.getVisibility() == View.VISIBLE && input.getText().length() == 0) {
                isOk = false;
                input.setIsValid(false, "");
            }
        }
        return isOk;
    }


    private void setUserToGui(UserProfileManager.ProfileUser user) {
        if (xid == null || getView() == null) return; // no views loaded?

        editFields = getView().findViewById(R.id.edit_fields);
        offline_user = getView().findViewById(R.id.user_type_offline);
        update_user = getView().findViewById(R.id.user_type_xid_update);
        online_user = getView().findViewById(R.id.user_type_online);
        new_user = getView().findViewById(R.id.user_type_xid_new);

        user_information_text = getView().findViewById(R.id.user_information_text);

        offline_user.setVisibility(View.GONE);
        update_user.setVisibility(View.GONE);
        online_user.setVisibility(View.GONE);
        new_user.setVisibility(View.GONE);
        editSidebar.setVisibility(View.VISIBLE);
        updatePassCodeCaption.setVisibility(View.GONE);

        userType.clearCheck();
        xid.setReadOnly(false);
        if (user==null) {   // currently no user profile in this slot
            offline_user.setVisibility(View.VISIBLE);
            online_user.setVisibility(View.VISIBLE);
            userType.check(R.id.user_type_offline);
            removeButton.setVisibility(View.INVISIBLE);
            return;
        } else if (updatePasscodeOnly) {
            // basically just use the add new xid elements and let it re-login to update slot
            userType.check(R.id.user_type_online);
            xid.setReadOnly(true);
            removeButton.setVisibility(View.INVISIBLE);
            editSidebar.setVisibility(View.INVISIBLE);
            updatePassCodeCaption.setVisibility(View.VISIBLE);
            isEditingProfile = true;
        } else {    // slot has a user profile already
            isEditingProfile = true;
            removeButton.setVisibility(View.VISIBLE);
            if (user.isLocalUser()) { // slot contains a local user
                offline_user.setVisibility(View.VISIBLE);
                userType.check(R.id.user_type_offline);
            } else { // slot contains an xid user
                update_user.setVisibility(View.VISIBLE);
                userType.check(R.id.user_type_xid_update);
            }
            try {
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(editFields);
                constraintSet.connect(R.id.user_accept,ConstraintSet.TOP,R.id.user_remove,ConstraintSet.TOP,0);
                constraintSet.applyTo(editFields);
            }
            catch (Exception e){
                Log.d(TAG, "setUserToGui: Error"+e);
            }
        }
        if (user.isLocalUser()) clearXidBox();
        else xid.setText(user.xid);
        passcode.setText(user.passcode);
        confirmPasscode.setText("");
        email.setText(user.email);
        firstName.setText(user.firstName);
        lastName.setText(user.lastName);
        Calendar cal = Calendar.getInstance();
        cal.set(user.birthYear, user.birthMonth - 1, user.birthDay);
        birthdate.setText(cal); // this does not generate a change event and the control doesn't provide a date based get?
        userParcel.birthYear = user.birthYear; // so let's just set the initial parcel date here?
        userParcel.birthDay = user.birthDay;
        userParcel.birthMonth = user.birthMonth;
        genderSelector.setOptions(R.array.gender_list, user.genderIdentity.Value);
        units.check(user.unitPreferenceMetric ? R.id.units_metric_radio_button : R.id.units_imperial_radio_button);
        weight.setText(String.valueOf(Math.round(user.getWeight().getValue(user.unitPreferenceMetric ? KILOGRAM : POUND))));
        termsAndConditions.setChecked(user.acceptedTerms);
    }

    private boolean isUserLocalSelected() { return userType.getCheckedRadioButtonId() == R.id.user_type_offline; }
    private boolean isUserOnlineSelected(){return userType.getCheckedRadioButtonId() == R.id.user_type_online; }
    private boolean isUserNewSelected() { return userType.getCheckedRadioButtonId() == R.id.user_type_xid_new; }
    private boolean isUserUpdateSelected() { return userType.getCheckedRadioButtonId() == R.id.user_type_xid_update; }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideKeyboard(getActivity());
        return false;
    }

    private boolean imePasscode(int action) {
        checkConfirmPasscode(true);
        return true;
    }
    private boolean imeDone(int action) {
        checkInputsValid();
        if(acceptButton.getVisibility() == View.VISIBLE) {
            acceptButton.callOnClick();
        }
        return false;
    }

    private InputValidationResult validateXid(String value, String previousValue) {
        if(value.length() < xIDMinLength) {
            return new InputValidationResult(InputValidationResult.RESULT.GENERAL_ERROR, "", "");
        }
        else if(!currentXid.equals(value)) {
            currentXid = value;
            xid.setValidating(true);
            DapiLogin.checkXidExists(mDataMatrix, value, xidExists -> {
                if (isAdded()) {
                    if (xidExists != null) {
                        if (xidExists && isUserNewSelected()) {
                            xid.setIsValid(false, getResString(R.string.duplicate_xid_message_pg));
                        } else if (!xidExists && isUserOnlineSelected()) {
                            xid.setIsValid(false, getResString(R.string.invalid_xid_pg));
                        } else {
                            xid.setIsValid(true, "");
                        }
                    } else {
                        xid.setIsValid(true, "");
                    }
                    xid.initInput();
                    checkInputsValid();
                } else {
                    Log.e(TAG, "Callback on fragment that was not added");
                }
            });
        }
        return new InputValidationResult(InputValidationResult.RESULT.CHECKING, value, "");
    }

    private void clearXidBox() {
        // make sure currentXid doesn't retain old value (do we need to set both at same time as well?)
        xid.setText("");
        currentXid = "";
    }

    public void closePopups(){
        genderSelector.closePopup();
    }

    private GenderIndentity getDefaultGender(){
         return GenderIndentity.fromInt(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT));
    }
}
