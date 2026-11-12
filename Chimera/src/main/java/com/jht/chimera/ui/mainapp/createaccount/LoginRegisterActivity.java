package com.jht.chimera.ui.mainapp.createaccount;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.login.RFIDLogin;
import com.jht.chimera.lib.widget.activity.TimeoutActivity;
import com.jht.chimera.lib.widget.button.HomeButton;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.ui.mainapp.createaccount.fragment.LoginFragment;
import com.jht.chimera.ui.mainapp.createaccount.fragment.RegisterFragment;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;
import com.jhtp.dapiservice.an.types.ButtonPressType;

public class LoginRegisterActivity extends TimeoutActivity {
    private static final String TAG = "LoginRegisterActivity";
    private ConstraintLayout mFragmentContainerLayout;
    private LoginFragment mLoginFragment;
    private RegisterFragment mRegisterFragment;
    private Fragment currentFragment;
    private boolean mShowRegisterUser;
    private View mOverlayView;
    private DataMatrix mDataMatrix;
    private final Handler mHandler = new Handler();
    private MessageBoxDialog mMessageBoxDialog;
    private RFIDLogin rfidLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_register_activity_layout);
        mFragmentContainerLayout = findViewById(R.id.fragment_container_layout);
        mOverlayView = findViewById(R.id.login_overlay_view);

        HomeButton mHomeButton = findViewById(R.id.buttonHome);

        mHomeButton.setOnClickListener(v -> {
            //Stores home button press events for Dapi Analytics
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.HOME_BTN_PRESS.name);

            closeActivity();

        });

        setDefaultScreenTimeout(LaunchAppUtil::launchSplashScreen);
        mMessageBoxDialog = new MessageBoxDialog(this);
    }

    public void showWelcome() {
        closeActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDataMatrix = new DataMatrix();

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            String strFragement = intent.getStringExtra("fragment");
            if (strFragement != null) {
                if (strFragement.equalsIgnoreCase("register_user")) { //NON-NLS
                    mShowRegisterUser = true;
                }
            }
        }

        mLoginFragment = new LoginFragment();
        mRegisterFragment = new RegisterFragment();

        if (mShowRegisterUser) {
            showRegisterFragment();
        }
        else{
            showLoginFragment();
        }

        initRFIDLogin();
    }

    private void initRFIDLogin() {
        rfidLogin = new RFIDLogin(mDataMatrix, new RFIDLogin.ILoginListener() {

            @Override
            public void onLoginAttemptStart() {
                showInfoDialog(null, getString(R.string.rid_login_msg_pg), false);
                setGoButtonEnabled(false);
            }

            @Override
            public void onLoginAttemptEnd(RFIDLogin.Result result, RFIDChip chip) {
                switch (result) {
                    case SUCCESS:
                        mMessageBoxDialog.closeMessageBox();
                        LaunchAppUtil.launchGoScreen();
                        break;
                    case INVALID_CREDENTIALS:
                        showInfoDialog(null, getResources().getString(R.string.login_failed_message_pg), true);
                        mMessageBoxDialog.setOnDismissListener(dialog -> rfidLogin.start());
                        break;
                    case NO_ASSOCIATED_XID:
                        mMessageBoxDialog.setMessage(getResources().getString(R.string.rfid_associate_wtih_xid_msg_pg));
                        mMessageBoxDialog.setButtonsEnabled(true);
                        mMessageBoxDialog.setButton1OnClickListener(v -> {
                            mMessageBoxDialog.closeMessageBox();
                            mMessageBoxDialog.setButtonsEnabled(false);
                            DataMatrix.putParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, null);
                            rfidLogin.start();
                        });
                        mMessageBoxDialog.setButton0OnClickListener(v -> {
                            mMessageBoxDialog.closeMessageBox();
                            mMessageBoxDialog.setButtonsEnabled(false);
                            DataMatrix.putParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, chip);
                            showLoginFragment();
                            rfidLogin.start();
                        });
                        break;
                    case CONNECTION_ERROR:
                        showInfoDialog(getResources().getString(R.string.rfid_pairing_failed_tc), getResources().getString(R.string.rfid_pairing_failed_msg_pg), true);
                        mMessageBoxDialog.setOnCloseCallback(() -> rfidLogin.start());
                        mMessageBoxDialog.setOnDismissListener((e) -> rfidLogin.start());
                        break;
                    default:
                        mMessageBoxDialog.dismiss();
                        break;
                }
                setGoButtonEnabled(true);
            }

            @Override
            public void onWalletLoginAttemptStart() {
                // Not supported in old UI
            }

            @Override
            public void onWalletLoginAttemptEnd(RFIDLogin.Result result) {
                // Not supported in old UI
            }
        });
        rfidLogin.start(); // Start RFID scanning
    }

    private void showInfoDialog(String titleText, String messageText, Boolean closeOnTouch) {
        mMessageBoxDialog.setTitle(titleText);
        mMessageBoxDialog.setMessage(messageText);
        mMessageBoxDialog.setCanceledOnTouchOutside(closeOnTouch);
        mMessageBoxDialog.show();
    }

    private void setGoButtonEnabled(boolean enabled){
        if (enabled){
            if (currentFragment == mRegisterFragment) {
                mRegisterFragment.enableGoButton(true);
            }
            else mLoginFragment.enableGoButton(true);
        } else {
            mRegisterFragment.enableGoButton(false);
            mLoginFragment.enableGoButton(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataMatrix.finish();
        if (mMessageBoxDialog != null && mMessageBoxDialog.isShowing())
            mMessageBoxDialog.dismiss();
        if (rfidLogin != null) rfidLogin.stop();
    }
    /**
     * Shows the register fragment
     */
    public void showRegisterFragment() {
        currentFragment = mRegisterFragment;
        getSupportFragmentManager().beginTransaction().replace(mFragmentContainerLayout.getId(), mRegisterFragment).commit();
    }

    /**
     * Shows the login fragment
     */
    public void showLoginFragment() {
        currentFragment = mLoginFragment;
        getSupportFragmentManager().beginTransaction().replace(mFragmentContainerLayout.getId(), mLoginFragment).commit();
    }

    /**
     * Sets whether the overlay is visible and blocking touch inputs
     *
     * @param visible whether it should be visible or not
     */
    public void setOverlayVisibility(final boolean visible) {
        mHandler.post(() -> {
            if (visible) {
                mOverlayView.setVisibility(View.VISIBLE);
                DataMatrix.sendEvent(TopBarKeys.ACTION_SHOW_BUSY_INDICATOR);
            } else {
                mOverlayView.setVisibility(View.GONE);
                DataMatrix.sendEvent(TopBarKeys.ACTION_HIDE_BUSY_INDICATOR);
            }
        });
    }

    public void closeActivity() {
        //Test if this is not needed
        //DataMatrix.sendEvent(UiServiceKeys.EVENT_HIDE_KEYBOARD);
        if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL)) {
            LaunchAppUtil.launchSplashScreen();
        }
        else if(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)){
            LaunchAppUtil.launchWelcomeScreen();
        }
        else{
            LaunchAppUtil.launchGoScreen();
        }
    }
}
