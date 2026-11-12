package com.jht.chimera.ui.mainapp.createaccount.fragment;

import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.edittext.IUserTextInputProperties;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SimpleSelectorItem;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.chimera.ui.mainapp.WelcomeActivity;
import com.jht.chimera.ui.mainapp.createaccount.LoginRegisterActivity;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;

import java.util.ArrayList;
import java.util.List;


public class LoginFragment extends Fragment {
    public final String TAG = "LoginFragment";

    private UserTextInput userName;
    private UserTextInput password;
    private TextView mErrorText;
    private List<String> loginTypes;
    private SimpleSelectorItem loginTypeItems;
    private DataMatrix dataMatrix;
    private final IdleTimerManagement idleTimerManagement = new IdleTimerManagement();

    private Button mLoginButton;

    private boolean goButtonEnabled = false;

    private ImageView qrCodeImageView;

    private MessageBoxDialog mMessageBoxDialog;

    private DigitalSafetyPinDialog safetyPrompt;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataMatrix = new DataMatrix();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_fragment_layout, container, false);

        RecyclerView mRecyclerView = view.findViewById(R.id.login_type_recycler_view);
        userName = view.findViewById(R.id.login_user_name_input);
        password = view.findViewById(R.id.login_password_input);

        qrCodeImageView = view.findViewById(R.id.qr_login_view);

        userName.setOnFocusChanged(hasFocus -> {
            if (hasFocus) {
                mErrorText.setVisibility(View.GONE);
            }
        });

        password.setOnFocusChanged(hasFocus -> {
            if (hasFocus) {
                mErrorText.setVisibility(View.GONE);
            }
        });

        loginTypeItems = new SimpleSelectorItem("", new ArrayList<>());
        loginTypes = new ArrayList<>();

        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.btn_xid_login_tc)));
            loginTypes.add(DapiUtil.LOGIN_TYPE_XID);
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.barcode_login_tc)));
            loginTypes.add(DapiUtil.LOGIN_TYPE_BARCODE);
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.phone_number_tc)));
            loginTypes.add(DapiUtil.LOGIN_TYPE_PHONE);
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.email_login_tc)));
            loginTypes.add(DapiUtil.LOGIN_TYPE_EMAIL);
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.wallet_login_tc)));
            loginTypes.add(DapiUtil.LOGIN_TYPE_WALLET);
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_NAME_STRING)));
            loginTypes.add(DapiUtil.LOGIN_TYPE_CUSTOM);
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.fit_linxx_login_tc)));
            loginTypes.add("Fitlinxx"); //NON-NLS
        }
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL) && DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL)) {
            loginTypeItems.addItem(new SimpleSelectorItem(getString(R.string.register_tc)));
            loginTypes.add("Register"); //NON-NLS
        }

        Button guestButton = view.findViewById(R.id.login_guest_button);
        guestButton.setOnClickListener(v -> LaunchAppUtil.launchGoScreen());

        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) {
            guestButton.setVisibility(View.VISIBLE);
        } else {
            guestButton.setVisibility(View.INVISIBLE);
        }

        mLoginButton = view.findViewById(R.id.register_button);
        mLoginButton.setOnClickListener(v -> login());

        mErrorText = view.findViewById(R.id.user_login_error_text_view);
        view.setOnTouchListener((v, event) -> {
            if (getActivity() != null) {
                InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (getActivity().getCurrentFocus() != null && getActivity().getCurrentFocus().getWindowToken() != null) {
                        manager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
            }
            return false;
        });

        if (getContext() != null) {
            SelectorDefaultAnimations.setDefaultAnimations(getContext(), mRecyclerView);
            mRecyclerView.setAdapter(new StandardSelectorItemAdapter(getContext(), loginTypeItems, iSelectorItem -> initInputs(iSelectorItem.getIndex()), (int) getResources().getDimension(R.dimen.login_spacing)));
        }
        loginTypeItems.setSelection(0);
        initInputs(0);

        userName.setOnIMEAction(i -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                if (userName.getIsValid()) {
                    mLoginButton.setEnabled(true);
                    login();
                }
            }
            return false;
        });

        userName.setOnTextChanged(s -> {
            if (userName.getIsValid() && password.getIsValid()) {
                mLoginButton.setEnabled(true);
            }
        });

        password.setOnTextChanged(s -> {
            if (userName.getIsValid() && password.getIsValid()) {
                mLoginButton.setEnabled(true);
            }
        });

        password.setOnIMEAction(i -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                if (userName.getIsValid() && password.getIsValid()) {
                    mLoginButton.setEnabled(true);
                    login();
                }
            }
            return false;
        });

        int screenTimeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);
        if (screenTimeout > 0) {
            idleTimerManagement.addTimer(new JHTTimer(screenTimeout * 1000, () -> {
                hideKeyboard(getActivity());
                if (getActivity() instanceof WelcomeActivity) {

                    ((WelcomeActivity) getActivity()).showWelcome();
                } else if (getActivity() instanceof LoginRegisterActivity) {
                    ((LoginRegisterActivity) getActivity()).showWelcome();

                }
            }));
        }

        mMessageBoxDialog = new MessageBoxDialog(view.getContext());


        return view;
    }

    private void initInputs(int position) {
        if (loginTypes.size() == 0) return;

        IUserTextInputProperties nameProperties = userName.getInputProperties();
        IUserTextInputProperties passwordProperties = password.getInputProperties();
        nameProperties.inputValue("");
        passwordProperties.inputValue("");
        userName.setIsValid(true, "");
        password.setIsValid(true, "");
        userName.setVisibility(View.VISIBLE);
        password.setVisibility(View.VISIBLE);
        mLoginButton.setEnabled(false);
        mLoginButton.setVisibility(View.VISIBLE);
        mErrorText.setVisibility(View.GONE);
        qrCodeImageView.setVisibility(View.INVISIBLE);


        nameProperties.minChars(0);
        switch (loginTypes.get(position)) {
            case DapiUtil.LOGIN_TYPE_XID:
                nameProperties.maxChars(14);
                nameProperties.minChars(4);
                nameProperties.hint(getString(R.string.enter_xid_2_tc));
                nameProperties.label(getString(R.string.xid_tc));
                nameProperties.inputType(UserTextInput.InputType.NUMBER);
                nameProperties.imeOptions(EditorInfo.IME_ACTION_NEXT);

                passwordProperties.maxChars(4);
                passwordProperties.hint(getString(R.string.enter_passcode_tc));
                passwordProperties.label(getString(R.string.passcode_tc));
                passwordProperties.inputType(UserTextInput.InputType.NUMBER);
                passwordProperties.imeOptions(EditorInfo.IME_ACTION_DONE);
                break;
            case DapiUtil.LOGIN_TYPE_BARCODE:
                nameProperties.maxChars(65);
                nameProperties.hint(getString(R.string.enter_barcode_tc));
                nameProperties.label(getString(R.string.barcode_login_tc));
                nameProperties.inputType(UserTextInput.InputType.TEXT);
                nameProperties.imeOptions(EditorInfo.IME_ACTION_NEXT);

                passwordProperties.maxChars(65);
                passwordProperties.hint(getString(R.string.enter_last_name_tc));
                passwordProperties.label(getString(R.string.last_name_tc));
                passwordProperties.inputType(UserTextInput.InputType.TEXT);
                passwordProperties.imeOptions(EditorInfo.IME_ACTION_DONE);
                break;
            case DapiUtil.LOGIN_TYPE_CUSTOM:
                nameProperties.maxChars(65);
                nameProperties.hint("");
                nameProperties.label(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_LABEL_STRING));
                switch (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_INPUT_TYPE_INT)) {
                    case 0:
                        nameProperties.inputType(UserTextInput.InputType.NUMBER);
                        break;
                    case 2:
                        nameProperties.inputType(UserTextInput.InputType.EMAIL);
                        break;
                    default:
                        nameProperties.inputType(UserTextInput.InputType.TEXT);
                        break;
                }
                nameProperties.imeOptions(EditorInfo.IME_ACTION_NEXT);


                passwordProperties.maxChars(65);
                passwordProperties.hint("");
                passwordProperties.label(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING));
                switch (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT)) {
                    case 0:
                        passwordProperties.inputType(UserTextInput.InputType.NUMBER);
                        break;
                    case 2:
                        passwordProperties.inputType(UserTextInput.InputType.EMAIL);
                        break;
                    default:
                        passwordProperties.inputType(UserTextInput.InputType.TEXT);
                        break;

                }
                passwordProperties.imeOptions(EditorInfo.IME_ACTION_DONE);
                break;
            case DapiUtil.LOGIN_TYPE_PHONE:

                nameProperties.maxChars(14);
                nameProperties.hint(getString(R.string.enter_phone_number_tc));
                nameProperties.label(getString(R.string.phone_number_tc));
                nameProperties.inputType(UserTextInput.InputType.NUMBER);
                nameProperties.imeOptions(EditorInfo.IME_ACTION_NEXT);

                passwordProperties.maxChars(4);
                passwordProperties.hint(getString(R.string.enter_passcode_tc));
                passwordProperties.label(getString(R.string.passcode_tc));
                passwordProperties.inputType(UserTextInput.InputType.NUMBER);
                passwordProperties.imeOptions(EditorInfo.IME_ACTION_DONE);
                break;
            case DapiUtil.LOGIN_TYPE_EMAIL:

                nameProperties.maxChars(65);
                nameProperties.hint(getString(R.string.enter_email_address_tc));
                nameProperties.label(getString(R.string.email_tc));
                nameProperties.inputType(UserTextInput.InputType.EMAIL);
                nameProperties.imeOptions(EditorInfo.IME_ACTION_NEXT);

                passwordProperties.maxChars(65);
                passwordProperties.hint(getString(R.string.enter_password_tc));
                passwordProperties.label(getString(R.string.password_tc));
                passwordProperties.inputType(UserTextInput.InputType.TEXT);
                passwordProperties.imeOptions(EditorInfo.IME_ACTION_DONE);
                break;
            case "Fitlinxx": //NON-NLS
                nameProperties.maxChars(6);
                nameProperties.hint(getString(R.string.enter_fitlinxx_tc));
                nameProperties.label(getString(R.string.fit_linxx_login_tc));
                nameProperties.inputType(UserTextInput.InputType.NUMBER);
                nameProperties.imeOptions(EditorInfo.IME_ACTION_DONE);
                mLoginButton.setEnabled(true);

                password.setVisibility(View.GONE);
                break;
            case DapiUtil.LOGIN_TYPE_OAUTH:
                // this will just show the qr code
                userName.setVisibility(View.GONE);
                password.setVisibility(View.GONE);
                mLoginButton.setVisibility(View.INVISIBLE);
                break;
            case "Register": //NON-NLS
                showRegisterFragment();
                return;
            default:
                break;
        }
        passwordProperties.isPassword(true);
        userName.setInputProperties(nameProperties);
        password.setInputProperties(passwordProperties);
    }

    private void showRegisterFragment() {
        if (getActivity() instanceof LoginRegisterActivity) {
            ((LoginRegisterActivity) getActivity()).showRegisterFragment();
        } else if (getActivity() instanceof WelcomeActivity) {
            ((WelcomeActivity) getActivity()).showRegisterFragment();
        }
    }

    private void setOverlayVisibility(boolean visible) {
        if (getActivity() instanceof LoginRegisterActivity) {
            ((LoginRegisterActivity) getActivity()).setOverlayVisibility(visible);
        } else if (getActivity() instanceof WelcomeActivity) {
            ((WelcomeActivity) getActivity()).setOverlayVisibility(visible);
        }

    }

    /**
     * Performs the login. If the login succeeds saves the user information and XID/Passcode to the
     * system.
     */
    private void login() {
        hideKeyboard(getActivity());

        String loginType = loginTypes.get(loginTypeItems.getSelection());
        if (loginType.equalsIgnoreCase("Fitlinxx")) { //NON-NLS
            // TODO: Login with Fitlinxx.
            mErrorText.setVisibility(View.VISIBLE);
        } else {
            startSpinnerAnimation();
            final String xid = userName.getText();
            final String passcode = password.getText();
            final RFIDChip rfidChip = DataMatrix.getParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, RFIDChip.class);

            setOverlayVisibility(true);
            idleTimerManagement.stopMonitoring();
            goButtonEnabled = false;
            DapiLogin.login(dataMatrix, xid, passcode, loginTypes.get(loginTypeItems.getSelection()), dapiResult -> {
                Log.d(TAG, "ptp_timing: Got login result");
                if (isAdded()) {
                    if (dapiResult == 0) {
                        if (rfidChip != null) {
                            DataMatrix.putParcelable(DapiLogin.Keys.ACTION_ASSOCIATE_RFID_PARCELABLE, rfidChip);
                        }
                        LaunchAppUtil.launchGoScreen();
                    } else {
                        idleTimerManagement.monitorTimers();
                        mErrorText.setVisibility(View.VISIBLE);
                        goButtonEnabled = true;
                    }
                    stopSpinnerAnimation();
                    setOverlayVisibility(false);
                }
            });
        }
    }

    private void startSpinnerAnimation() {
        DataMatrix.sendEvent(TopBarKeys.ACTION_SHOW_BUSY_INDICATOR);
    }

    private void stopSpinnerAnimation() {
        DataMatrix.sendEvent(TopBarKeys.ACTION_HIDE_BUSY_INDICATOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.LOGIN_REGISTER.name());

        idleTimerManagement.monitorTimers();
        mLoginButton.setEnabled(false);

        goButtonEnabled = true;

        dataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {

            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) {
                if (keyCode == KeyPress.KeyCode.KEY_GO && keyState == KeyPress.KeyState.KEY_DOWN) {
                    if (goButtonEnabled) {
                        checkIfSafetyPinNeeded(()-> goButtonEnabled = !GoScreenActivity.startGoButtonWorkout());
                    }

                }
            }

        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        userName.setText("");
        password.setText("");
        DataMatrix.putParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, null);
        idleTimerManagement.stopMonitoring();

        dataMatrix.finish();
        goButtonEnabled = false;

        qrCodeImageView.setVisibility(View.INVISIBLE);

        mMessageBoxDialog.setOnCloseCallback(null); // Make sure we don't get this callback when screen is not displayed
        mMessageBoxDialog.closeMessageBox();
    }

    public void enableGoButton(boolean enabled) {
        goButtonEnabled = enabled;
    }

    // safety pin stuff!
    private void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null) safetyPrompt = new DigitalSafetyPinDialog(requireActivity().getApplicationContext());
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }
}
