package com.jht.chimera.ui.mainapp.userprofiles;

import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.chimera.ui.mainapp.userprofiles.fragment.EditProfileFragment;
import com.jht.chimera.ui.mainapp.userprofiles.fragment.SelectProfileFragment;
import com.jht.datamatrixlib.DataMatrix;

public class UserProfileDialog extends DialogBase {
    private FrameLayout mFragmentContainerLayout;

    private SelectProfileFragment mSelectFragment;
    private EditProfileFragment mEditFragment;
    private View mOverlayView;

    private Handler mHandler = new Handler();
    private MessageBoxDialog mMessageBoxDialog;
    private AppCompatActivity mContext;

    private IdleTimerManagement idleTimerManagement = new IdleTimerManagement();
    private boolean isBusy = false;

    public UserProfileDialog(AppCompatActivity context) {
        super(context);
        mContext = context;
        mFragmentContainerLayout = findViewById(R.id.fragment_container_layout);
        mOverlayView = findViewById(R.id.login_overlay_view);
        SelectronButton mCloseButton = findViewById(R.id.buttonClose);

        mCloseButton.setOnClickListener(v -> {
            hide();
        });

        mMessageBoxDialog = new MessageBoxDialog(context);

        mSelectFragment = new SelectProfileFragment(this);
        mEditFragment = new EditProfileFragment(this);

        int screenTimeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);
        if(screenTimeout > 0) {
            idleTimerManagement.addTimer(new JHTTimer(screenTimeout * 1000, () -> {
                mEditFragment.closePopups();
                mHandler.post(()->hide());
            }));
        }
    }

    @Override
    public void show(final View anchor) { this.show(anchor, false); } // default no animation

    @Override
    public void show(final View anchor, boolean animate) {
        super.show(anchor, animate);
        showSelectFragment(); // ensure if we come back, we are on selection fragment
        idleTimerResume();
    }

    public void showUpdatePasscode(final View anchor, UserProfileManager.ProfileUser user) {
        super.show(anchor, false);
        showEditFragment(user, true);
        idleTimerPause(); // do we want to prevent idle timeout when asking about bad passcode?
    }

    @Override
    public void hide() { this.hide(false); } // default no animation

    @Override
    public void hide(boolean animate) {
        if (isBusy) return; // don't close if busy ?
        super.hide(animate);
        idleTimerPause();
    }

    public void idleTimerResume() { idleTimerManagement.monitorTimers(); }
    public void idleTimerPause() { idleTimerManagement.stopMonitoring(); }


    private void showInfoDialog(String titleText, String messageText, Boolean closeOnTouch) {
        mMessageBoxDialog.setTitle(titleText);
        mMessageBoxDialog.setMessage(messageText);
        mMessageBoxDialog.setCanceledOnTouchOutside(closeOnTouch);
        mMessageBoxDialog.show();
    }

    /**
     * Shows the register fragment
     */
    public void showEditFragment(UserProfileManager.ProfileUser user, boolean updatePasscodeOnly) {
        if (isBusy) return; // don't close if busy ?
        mEditFragment.setUser(user, updatePasscodeOnly);
        mContext.getSupportFragmentManager().beginTransaction().replace(mFragmentContainerLayout.getId(), mEditFragment).commit();
        mContext.getSupportFragmentManager().executePendingTransactions(); // make sure all transactions are complete
    }

    /**
     * Shows the select fragment
     */
    public void showSelectFragment() {
        if (isBusy) return; // don't close if busy ?
        mContext.getSupportFragmentManager().beginTransaction().replace(mFragmentContainerLayout.getId(), mSelectFragment).commit();
        mContext.getSupportFragmentManager().executePendingTransactions(); // make sure all transactions are complete
        mSelectFragment.updateData(); // force data reload or just leave it to update events? force it in case update listener wasn't up.
        //idleTimerResume(); // resume idle timers that may have been paused?
    }

    public void profileUpdated() {
        mSelectFragment.updateData();
    }

    /**
     * Sets whether the overlay is visible and blocking touch inputs
     *
     * @param visible whether it should be visible or not
     */
    public void setOverlayVisibility(final boolean visible) {
        isBusy = visible; // if overlay is visible, we are busy
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

    @Override
    protected int getLayoutResId() {
        return R.layout.user_profile_activity_layout;
    }
}
