package com.jht.chimera.ui.mainapp.userprofiles.fragment;

import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.chimera.ui.mainapp.userprofiles.ProfileRecyclerAdapter;
import com.jht.chimera.ui.mainapp.userprofiles.UserProfileDialog;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;


public class SelectProfileFragment extends Fragment implements ProfileRecyclerAdapter.ProfileEvents {
    public static final String TAG = "SelectProfileFragment";

    private TextView mErrorText;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ProfileRecyclerAdapter mRecycler;

    private RFIDChip rfidChip;
    private DataMatrix dataMatrix;

    protected UserProfileDialog mParent;

    public SelectProfileFragment(UserProfileDialog parent) {
        super();
        mParent = parent;
    }

    private void close() { mParent.hide(); }

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
        return inflater.inflate(R.layout.profile_select_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(R.id.profile_recycler_view);

        mErrorText = view.findViewById(R.id.user_login_error_text_view);
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard(getActivity());
            }
            return false;
        });

        if(getContext() != null) {
            //GridLayoutManager mLayoutManager = new  GridLayoutManager(mRecyclerView.getContext(), 1, GridLayoutManager.HORIZONTAL, false);
            mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            mRecycler = new ProfileRecyclerAdapter(UserProfileManager.getInstance().getProfileData(), this);
            //mRecyclerView.setHasFixedSize(true);
            //mRecyclerView.setNestedScrollingEnabled(false);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mRecycler);
            //SelectorDefaultAnimations.setDefaultAnimations(getContext(), mRecyclerView); // this doesn't work for horz recycler
        }
    }

    public void updateData() {
        //Log.d(TAG, "updateData()");
        //UserProfileManager.getInstance().reloadData(); // Dapi service and main app may each have a profile singleton, make sure we grab latest data from backing store
        // https://stackoverflow.com/questions/36495009/force-recyclerview-to-redraw-its-items
        if (mRecycler != null) {
            //Log.d(TAG, "updateData() force recycler");
            mRecyclerView.getLayoutManager().removeAllViews();
            mRecyclerView.setAdapter(null);
            mRecyclerView.setLayoutManager(null);
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerView.swapAdapter(mRecycler, false);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecycler.notifyDataSetChanged();
        }
    }

    private void showEditFragment(UserProfileManager.ProfileUser user) {
        mParent.showEditFragment(user, false);
    }

    private void setOverlayVisibility(boolean visible) {
        mParent.setOverlayVisibility(visible);
    }

    public void setRFIDTag(RFIDChip rfidTag) {
        rfidChip = rfidTag;
    }


    /**
     * Performs the login. If the login succeeds saves the user information and XID/Passcode to the
     * system.
     */
    private void login(UserProfileManager.ProfileUser user) {

        hideKeyboard(getActivity());

        setOverlayVisibility(true);
        mParent.idleTimerPause(); //idleTimerManagement.stopMonitoring();
        //goButtonEnabled = false;
        DapiLogin.login(dataMatrix, user.xid, user.passcode, user.slotNumber, DapiUtil.LOGIN_TYPE_PROFILE, dapiResult -> {
            Log.d(TAG, "DapiLogin.login");
            Log.d(TAG, "ptp_timing: Got login result");
            dataMatrix.finish(); // avoid any other login transactions from tripping this?
            if (isAdded()) {
                setOverlayVisibility(false);
                if (dapiResult == 0) {
                    if (rfidChip != null) {
                        DataMatrix.putParcelable(DapiLogin.Keys.ACTION_ASSOCIATE_RFID_PARCELABLE, rfidChip);
                    }
                    LaunchAppUtil.launchSplashScreen(); // call this to refresh screen
                } else {
                    // TODO - just work off local copy ? does that need to be done by DapiUserManagement in service thread?
                    mParent.idleTimerResume(); //idleTimerManagement.monitorTimers();
                    mErrorText.setVisibility(View.VISIBLE);
                    //goButtonEnabled = true;
                }
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        //Test if this is not needed
        //DataMatrix.sendEvent(UiServiceKeys.EVENT_HIDE_KEYBOARD);
        rfidChip = null;
        //idleTimerManagement.stopMonitoring();

        dataMatrix.finish();
        //goButtonEnabled = false;
    }

    public void enableGoButton(boolean enabled) {
        //goButtonEnabled = enabled;
    }


    @Override
    public void onProfileSelected(UserProfileManager.ProfileUser user) {
        Log.d(TAG, "onProfileSelected " + (user == null ? "null" : user.getFullName()));
        if (DataMatrix.getInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT) != SSOKeys.States.IDLE) {
            // do something to stall
            DialogFactory.message(mRecyclerView, getString(R.string.change_program_please_wait_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            return;
        }
        // either need to select guest or another user profile
        if (user == null || user.isGuest()) {
            DapiLogin.logout();
            LaunchAppUtil.launchSplashScreen(); // call this to refresh screen
        } else {
            login(user);
        }

        //this is only for retail machines
        if (ProductConfig.hasUserProfiles()) {
            switchUser(user);
        }
    }

    @Override
    public void onEditProfile(UserProfileManager.ProfileUser user) {
        Log.d(TAG, "onEditProfile " + (user==null?"null":user.getFullName()));
        // Need to go to profile edit screen with current user info
        showEditFragment(user);
    }

    @Override
    public void OnNewUser() {
        Log.d(TAG, "OnNewUser");
        // Need to go to profile edit screen with default user info
        showEditFragment(null);
    }

    /**
     * This function will send the CBOR login commands of an xID user when switching to the user
     */
    public void switchUser(UserProfileManager.ProfileUser user) {
        if (user == null || user.isLocalUser() || user.isGuest()) {
            Log.d(TAG, "Not found a valid xID user.");
            return;
        }

        DapiLogin.loginWithInfo(
                dataMatrix,
                user.xid,
                user.passcode,
                user.slotNumber,
                true,
                DapiUtil.LOGIN_TYPE_XID,
                dapiStatus -> {
                    dataMatrix.finish();
                }
        );
    }
}
