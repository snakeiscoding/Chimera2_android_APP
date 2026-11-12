package com.jht.chimera.service.dapi;

import static com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL;
import static com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.ProjectOne.DATA_LED_STYLE_SYSTEM_INT;
import static com.jht.chimera.lib.workoutapps.AppInfo.IGNORE_APP_ANALYTICS_ID;
import static com.jht.chimera.service.dapi.DAPIService.FIELD_NA_PLACEHOLDER;
import static com.jhtp.dapiservice.DapiResult.InvalidRequest;
import static com.jhtp.dapiservice.DapiResult.InvalidUsernameOrPassword;
import static com.jhtp.dapiservice.DapiResult.LoginFailed;
import static com.jhtp.dapiservice.DapiResult.Success;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.DefaultEquations;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.dapi.DapiCreateUser;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUserExtendedInfo;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.dapi.JumpSession;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.common.unit.WeightUnit;
import com.jht.chimera.lib.common.value.Weight;
import com.jht.chimera.service.tiers.helpers.TiersFeatureParser;
import com.jht.chimera.lib.login.WalletPass;
import com.jht.chimera.lib.login.WalletPassCreator;
import com.jht.chimera.ui.bluetooth.BluetoothBackgroundPairing;
import com.jht.mastboard.data.LEDStyle;
import com.jht.data.ProgressionUpdate;
import com.jht.data.QrAuthorizationResponse;
import com.jht.datamatrixlib.DMCallback;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.interfaces.models.MatrixProgressionModelKt;
import com.jht.oauthlib.api.exerciser.MatrixMethodProgram;
import com.jht.oauthlib.api.exerciser.ConnectedDeviceSettings;
import com.jht.oauthlib.api.exerciser.FeatureTierIdLists;
import com.jht.oauthlib.api.exerciser.HandGripSettings;
import com.jht.oauthlib.api.exerciser.PhoenixUserSettings;
import com.jht.oauthlib.api.exerciser.UserMetadata;
import com.jht.oauthlib.auth.DeviceCodeData;
import com.jht.oauthlib.auth.UserInfo;
import com.jht.progression.userinfo.UserInfoUtilKt;
import kotlinx.coroutines.Job;
import com.jht.rfid.Legic.RFIDChip;
import com.jht.seca.data.SecaUserData;
import com.jhtp.dapiservice.DapiResult;
import com.jhtp.dapiservice.an.events.WorkoutEvent;
import com.jhtp.dapiservice.comm.CborCommands;
import com.jhtp.dapiservice.comm.DapiComm;
import com.jhtp.dapiservice.um.DapiUserInfo;
import com.jhtp.dapiservice.um.GenderIndentity;
import com.jhtp.dapiservice.um.IDapiUserManagementResponse;
import com.jhtp.dapiservice.um.LoginType;
import com.jhtp.dapiservice.um.UserSetting;
import com.jhtp.dapiservice.um.UserSettingsKeys;
import com.jhtp.dapiservice.wt.DapiDate;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DAPIUserManagement implements IDapiUserManagementResponse {
    @NonNull
    private static final String TAG = "DAPIUserManagement";

    private UserProfileManager profileManager;
    private final Object profileManagerLock = new Object();
    private SyncUserThread syncTimer;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private String mXid = "";
    private String mLoginXid = "";
    private String mLoginPass = "";
    private int mLoginSlot = -1;
    private String mLoginType = "";
    private boolean mLoginSwitchUser = true; // basically should the current login transaction impact current user
    //private String mResponseKey = "";
    private DapiUser mUser = new DapiUser();
    protected int mMaxHr = -1;
    protected int mUserFtp = -1;
    protected String bleHR_macAddress="";
    protected String bleAudio_macAddress="";
    protected String antPlusHR_id="";
    protected boolean hapticsOn = true;
    protected LEDStyle ledStyle = LEDStyle.DEFAULT;
    private boolean saveUserSettings = false;
    private DapiCreateUser.DapiRegisterParcelable pendingUserUpdate = null; // need to carry user update data over to DAPI callback

    // Used to scan for bluetooth when we find auto-connect BT devices
    private final BluetoothBackgroundPairing backgroundPairing = new BluetoothBackgroundPairing(ContextUtil.getApplicationContext());

    private final JohnsonWebBackendManager johnsonWebBackendManager;
    @Nullable
    private String jumpToken = null;

    private Job qrJob = null;

    boolean fallbackUseDAPI = false;

    //Temporary JUMP and DAPI synchronization variables
    private boolean jumpFinished = false;
    private boolean dapiFinished = false;

    // Last progression json synced to JUMP, used to send only changed events.
    // Written from the IO thread login callback and read/written from main thread adapters.
    private volatile String matrixMethodLastSyncedJson = null;

    private DAPIUserManagement() {

        if (ProductConfig.hasUserProfiles()) {
            profileManager = UserProfileManager.getInstance();
            selectLastProfileUser(); // no threads/timers started, so no need for sync lock
            syncTimer = new SyncUserThread();
        } else {
            mUser.sync();
            mUser.syncDisplayUnits();
        }
        //JUMP authentication setup
        johnsonWebBackendManager = new JohnsonWebBackendManager();
        DapiComm.Instance.initAuthProvider(() -> jumpToken);

        setupDataMatrix();
    }

    private void setupDataMatrix() {
        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_ENABLE_DAPI_FALLBACK_BOOL, value -> fallbackUseDAPI = value);

        mDataMatrix.createEventAdapter(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS, keyReturnedBack -> removeAllProfiles(true));
        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, pos -> removeAllProfiles(false));

        // handle action logout user
        mDataMatrix.createEventAdapter(DapiLogin.Keys.ACTION_LOGOUT, keyReturnedBack -> logout());

        mDataMatrix.createEventAdapter(UserProfileManager.Keys.EVENT_REFRESH_CURRENT_SLOT, keyReturnedBack -> {
            synchronized (profileManagerLock) {
                selectLastProfileUser();
            }
        });

        mDataMatrix.createIntAdapter(DapiCreateUser.Keys.ACTION_REMOVE_PROFILE_SLOT_INT, this::removeProfile, false);

        // Handles login when we already have user token (Phone login)
        mDataMatrix.createStringAdapter(DapiLogin.Keys.ACTION_LOGIN_USER_TOKEN, new DMCallback<String>() {
            @Override
            public void dataMatrixCallback(String value) {
                mXid = "phoneLoginXid";
                mLoginXid = "phoneLoginXid";
                mLoginType = LoginType.XID.Value;
                mLoginPass = "phoneLoginPass";
                mLoginSlot = 0;
                mLoginSwitchUser = true;
                handleLogin(value);
            }
        });

        // handle action login
        mDataMatrix.createParcelableAdapter(DapiLogin.Keys.ACTION_LOGIN_PARCEL, DapiLogin.DapiLoginParcelable.class, loginParcelable -> {
            mXid = loginParcelable.Xid;
            mLoginXid = loginParcelable.Xid;
            mLoginType = loginParcelable.loginType;
            mLoginPass = loginParcelable.passcode;
            mLoginSlot = loginParcelable.slotNumber;
            mLoginSwitchUser = loginParcelable.switchToUser;
            if (mLoginSlot < 0) mLoginSlot = 0;
            // 2021-02-09 need to keep mLoginSlot for profile manager, however we are no longer sending it due security issue with duplicate machine ids

            // Create xid wallet pass info for wallet pass generation
            // Note: Moving this above the line that re-maps LOGIN_TYPE_EMAIL to LOGIN_TYPE_CUSTOM so that email login passes are created correctly
            String type = mLoginType.toLowerCase();
            String walletPass = "";
            switch (type) {
                case DapiUtil.LOGIN_TYPE_XID:
                    walletPass = WalletPassCreator.INSTANCE.generateXidPass(mXid, mLoginPass);
                    break;
                case DapiUtil.LOGIN_TYPE_RFID:
                    walletPass = WalletPassCreator.INSTANCE.generateRfidPass(mXid, mLoginPass);
                    break;
                case DapiUtil.LOGIN_TYPE_CUSTOM:
                    walletPass = WalletPassCreator.INSTANCE.generateCustomPass(mXid, mLoginPass);
                    break;
                case DapiUtil.LOGIN_TYPE_BARCODE:
                    walletPass = WalletPassCreator.INSTANCE.generateBarcodePass(mXid);
                    break;
                case DapiUtil.LOGIN_TYPE_PHONE:
                    walletPass = WalletPassCreator.INSTANCE.generatePhonePass(mXid, mLoginPass);
                    break;
                case DapiUtil.LOGIN_TYPE_EMAIL:
                    walletPass = WalletPassCreator.INSTANCE.generateEmailPass(mXid, mLoginPass);
                    break;
            }
            DataMatrix.updateString(DAPIKeys.DATA_USER_CREATE_WALLET_PASS_STRING, walletPass);

            // email login type was removed so it now uses custom
            if (mLoginType.equalsIgnoreCase(DapiUtil.LOGIN_TYPE_EMAIL)) mLoginType = DapiUtil.LOGIN_TYPE_CUSTOM;

            //logout(); // logout previous user, if it has xid
            if (mLoginType.equalsIgnoreCase(DapiUtil.LOGIN_TYPE_PROFILE) || DapiUser.isLocalUser(loginParcelable.Xid)) {
                // Do we need to attempt to unlink any xid user that might have been in this slot?
                //DapiComm.Instance.UserManagement.unlinkUser(mLoginSlot, DapiUtil.getMachineId()); // would we need to wait for this to finish?
                DataMatrix.putInt(DapiLogin.Keys.EVENT_LOGIN_RESPONSE_INT, (setUserToProfile(mLoginSlot) ? Success.Value : InvalidUsernameOrPassword.Value));
            } else {
                JUMPLogin(loginParcelable);

                if (fallbackUseDAPI){
                    dapiFinished = false;
                    DapiComm.Instance.UserManagement.logIn(loginParcelable.Xid, loginParcelable.passcode,
                            DapiUtil.getMachineId(), DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING),
                            0, DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT), mLoginType, false, new ArrayList<>());
                }
            }
        });

        // TODO this is a quick copy and paste of JUMPLogin function above to get wallet demo working (work with Alex on integration later)
        mDataMatrix.createParcelableAdapter(DapiLogin.Keys.ACTION_LOGIN_WALLET_PASS_PARCELABLE, WalletPass.class, this::JUMPWalletLogin);

        mDataMatrix.createIntAdapter(DapiLogin.Keys.ACTION_LOGIN_QR_INT, action -> {
            if (action == DapiLogin.QrAction.START) JUMPQrLogin();
            else if (action == DapiLogin.QrAction.STOP) JUMPQrLoginStop();
        });

        // handle action check email exists
        mDataMatrix.createStringAdapter(DapiCreateUser.Keys.ACTION_CHECK_EMAIL_EXISTS_STRING, email -> {
            //if(fallbackUseDAPI){
                DapiComm.Instance.UserManagement.checkEmailExists(email);
            /*}
            else{
                johnsonWebBackendManager.checkEmailRegistration(email);
            }*/
        }, false);

        // handle action check xid exists
        mDataMatrix.createStringAdapter(DapiLogin.Keys.ACTION_CHECK_XID_EXISTS_STRING, xid -> {
            //if(fallbackUseDAPI){
                DapiComm.Instance.UserManagement.checkXidExists(xid);
            /*}
            else{
                johnsonWebBackendManager.checkXidRegistration(xid);
            }*/
        }, false);

        // handle action register user
        mDataMatrix.createParcelableAdapter(DapiCreateUser.Keys.ACTION_REGISTER_USER_PARCEL, DapiCreateUser.DapiRegisterParcelable.class, registerParcelable -> {
            DapiUserInfo userInfo = new DapiUserInfo();
            userInfo.AcceptedTerms = registerParcelable.acceptedTerms;
            userInfo.Birthday = new DapiDate(registerParcelable.birthYear, registerParcelable.birthMonth, registerParcelable.birthDay);
            userInfo.Gender = registerParcelable.genderIndentity;
            userInfo.FirstName = registerParcelable.firstName;
            userInfo.LastName = registerParcelable.lastName;
            DapiUser.setInfoUnitPreferenceAndWeight(userInfo,
                    registerParcelable.getUnitPreference(),
                    registerParcelable.getWeight());
            if (!userInfo.UnitsPreferenceMetric) { // PRKS-2434 short term fix for register v3 command using Kg (TODO - waiting for better solution)
                userInfo.Weight = WeightConvertUtil.convertLbsToKg(userInfo.Weight);
            }
            userInfo.AgeYears = DapiUser.getAge(registerParcelable.birthYear, registerParcelable.birthMonth, registerParcelable.birthDay);
            userInfo.LanguageId = FIELD_NA_PLACEHOLDER;
            userInfo.ShareWorkoutData = true;

            // try to create new user
            if (DapiUser.isLocalUser(registerParcelable.xid)) {
                // local user so just create user
                synchronized (profileManagerLock) {
                    profileManager.updateUser(registerParcelable);
                }
                UserProfileManager.sendChangeEvent();
                DataMatrix.putInt(DapiCreateUser.Keys.EVENT_REGISTER_RESPONSE_INT, Success.Value);
            } else {
                mXid = registerParcelable.xid;
                mLoginXid = registerParcelable.xid;
                mLoginPass = registerParcelable.passcode;
                mLoginSwitchUser = true; // this was the original behavior
                mLoginSlot = registerParcelable.slotNumber;
                if (mLoginSlot < 0) mLoginSlot = 0;
                // if using profileManager, should we store user as "xid x" to hold slot until data is retrieved in case dapi does not respond?

                DapiComm.Instance.UserManagement.registerUser(
                        registerParcelable.email,
                        registerParcelable.xid,
                        registerParcelable.passcode,
                        userInfo,
                        DapiUtil.getMachineId(),
                        null
                );

                // Update the wallet pass info in case register user succeeds so that it is available
                String walletPass = WalletPassCreator.INSTANCE.generateXidPass(mXid, mLoginPass);
                DataMatrix.updateString(DAPIKeys.DATA_USER_CREATE_WALLET_PASS_STRING, walletPass);
            }
        });

        // handle action update user
        mDataMatrix.createParcelableAdapter(DapiCreateUser.Keys.ACTION_UPDATE_USER_PARCEL, DapiCreateUser.DapiRegisterParcelable.class, registerParcelable -> {
            DapiUserInfo userInfo = new DapiUserInfo();
            userInfo.AcceptedTerms = registerParcelable.acceptedTerms;
            userInfo.Birthday = new DapiDate(registerParcelable.birthYear, registerParcelable.birthMonth, registerParcelable.birthDay);
            userInfo.Gender = registerParcelable.genderIndentity;
            userInfo.FirstName = registerParcelable.firstName;
            userInfo.LastName = registerParcelable.lastName;
            DapiUser.setInfoUnitPreferenceAndWeight(userInfo, registerParcelable.getUnitPreference(), registerParcelable.getWeight());
            userInfo.AgeYears = DapiUser.getAge(registerParcelable.birthYear, registerParcelable.birthMonth, registerParcelable.birthDay);
            userInfo.LanguageId = FIELD_NA_PLACEHOLDER;
            userInfo.ShareWorkoutData = true;
            // See if userToken was passed, if not use the current user token
            String userToken = (registerParcelable.userToken == null || registerParcelable.userToken.isEmpty() ? mUser.userToken : registerParcelable.userToken);
            // Update user info command only receives back an acknowledgement, so we don't need to worry about catching user data in a response.
            //if (BuildConfig.DEBUG) Log.d(TAG, "update " + userInfo);

            pendingUserUpdate = registerParcelable; // save this for the callback
            if (DapiUser.isLocalUser(registerParcelable.xid)) {
                updatePendingUserProfile();
                DataMatrix.putInt(DapiCreateUser.Keys.EVENT_UPDATE_USER_RESPONSE_INT, Success.Value);
            } else {
                //DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, userInfo.UnitsPreferenceMetric);

                DapiComm.Instance.UserManagement.updateUserInfo(userToken, userInfo);
            }
        });

        mDataMatrix.createParcelableAdapter(DapiLogin.Keys.ACTION_ASSOCIATE_RFID_PARCELABLE, RFIDChip.class, chip -> {
            if(fallbackUseDAPI) {
                DapiComm.Instance.UserManagement.rfidAssociate(mUser.userToken, StringUtil.byteArray2Hex(chip.uid()), chip.rfStandard().toByte(), DapiUtil.getMachineId());
            }
            else{
                johnsonWebBackendManager.rfidAssociate(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT), jumpToken != null ? jumpToken : "",
                        JumpSession.current().getUserId(), StringUtil.byteArray2Hex(chip.uid()), chip.rfStandard().toByte());
            }
        });

        mDataMatrix.createParcelableAdapter(DapiLogin.Keys.ACTION_DISASSOCIATE_RFID_PARCELABLE, RFIDChip.class, chip -> {
            if (fallbackUseDAPI) {
                DapiComm.Instance.UserManagement.rfidDisassociate(StringUtil.byteArray2Hex(chip.uid()), chip.rfStandard().toByte(), DapiUtil.getMachineId());
            }
            else{
                johnsonWebBackendManager.rfidDisassociate(getJumpAuthenticationData(), StringUtil.byteArray2Hex(chip.uid()), chip.rfStandard().toByte());
            }
        });

        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_DAPI_UUID_STRING, uuid -> mUser.uuid = uuid);

        mDataMatrix.createFloatAdapter(DAPIKeys.ACTION_UPDATE_USER_WEIGHT_KG_FLOAT, weightKg -> {
            mUser.setWeight(new Weight(weightKg, WeightUnit.KILOGRAM));
            mUser.sync();
            if (profileManager != null) { synchronized (profileManagerLock) {profileManager.updateUser(mUser);} }
        }, false);

        mDataMatrix.createStringAdapter(DAPIKeys.ACTION_UPDATE_CUSTOM_WORKOUT_JSON_STRING, text -> {
            mUser.dapiUserExtendedInfo.customWorkouts.put(FrameConfigManager.getInstance().getFrameConfig().machineClassType.name(), text);
            mUser.sync();
            if (profileManager != null) { synchronized (profileManagerLock) {profileManager.updateUser(mUser);} }
            DataMatrix.sendEvent(SSOKeys.ACTION_SSO_SAVE_USER_CUSTOM_WORKOUT);
        }, false);

        mDataMatrix.createParcelableAdapter(DAPIKeys.ACTION_UPDATE_PROGRESSION_DATA_PARCELABLE, ProgressionUpdate.class, update -> {
            int currentGeneration = DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT);
            if (update.getGeneration() != currentGeneration) {
                // Stale - a user switch happened after this was sent; don't resurrect it.
                Log.w(TAG, "Dropping stale progression update (generation " + update.getGeneration() + ", current " + currentGeneration + ")");
                return;
            }
            String progressionJson = update.getProgressionJson();

            if (fallbackUseDAPI) {
                // DAPI fallback still stores Matrix Method progress on SSO
                mUser.dapiUserExtendedInfo.progressionData = progressionJson;
                mUser.syncExtendedInfo();
                if (profileManager != null) { synchronized (profileManagerLock) {profileManager.updateUser(mUser);} }
                DataMatrix.sendEvent(SSOKeys.ACTION_SSO_SAVE_USER_CUSTOM_WORKOUT);
                return;
            }

            if (jumpToken == null || jumpToken.isEmpty()) return;

            // Only changed events are sent, so completing one event does not resend the program
            johnsonWebBackendManager.saveMatrixMethodProgram(getJumpAuthenticationData(), jumpToken,
                    progressionJson, DataMatrix.getString(DAPIKeys.DATA_MATRIX_METHOD_PROGRAM_ID_STRING),
                    matrixMethodLastSyncedJson,
                    newProgramId -> {
                        if (DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) != currentGeneration) {
                            Log.w(TAG, "Dropping Matrix Method plan id from stale session");
                            return null;
                        }
                        DataMatrix.putString(DAPIKeys.DATA_MATRIX_METHOD_PROGRAM_ID_STRING, newProgramId);
                        return null;
                    },
                    () -> {
                        if (DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) != currentGeneration) {
                            Log.w(TAG, "Dropping Matrix Method sync baseline from stale session");
                            return null;
                        }
                        // Advance the diff baseline only once JUMP has really accepted it
                        matrixMethodLastSyncedJson = progressionJson;
                        return null;
                    });
        });

        mDataMatrix.createStringAdapter(DAPIKeys.ACTION_UPDATE_EXTENDED_USER_INFO_JSON_STRING, text -> {
            mUser.dapiUserExtendedInfo = DapiUserExtendedInfo.parseJson(text);
            mUser.extendedInfoReceived = true;
            if (!fallbackUseDAPI) {
                // JUMP owns Matrix Method, so SSO progression is never applied; dropping it here
                // also keeps syncExtendedInfo from overwriting the plan fetched from JUMP.
                mUser.dapiUserExtendedInfo.progressionData = "";
            }
            mUser.syncExtendedInfo();
            if (profileManager != null) { synchronized (profileManagerLock) {profileManager.updateUser(mUser);} }
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_CUSTOM_WORKOUT_BOOL, false);
        }, false);

        // Adds the program complete event to the 1500 analytics message
        mDataMatrix.createParcelableAdapter(DAPIKeys.EVENT_DAPI_WORKOUT_COMPLETE_ANALYTICS_PARCELABLE, WorkoutEvent.class, workoutEvent -> {
            Log.e(TAG, "adding workout event info with program ID " + workoutEvent.getProgramId() + " time " + workoutEvent.getTotalTimeSeconds());
            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            if (config.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ||
                    config.machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
                DapiComm.Instance.Analytics.programCompleteSteps(workoutEvent.getProgramId(), workoutEvent.getSubProgramId(), workoutEvent.getLevel(), (int) workoutEvent.getTotalTimeSeconds(), (int) workoutEvent.getTotalDistanceSteps());
            } else {
                DapiComm.Instance.Analytics.programCompleteDistance(workoutEvent.getProgramId(), workoutEvent.getSubProgramId(), workoutEvent.getLevel(), (int) workoutEvent.getTotalTimeSeconds(), workoutEvent.getTotalDistanceKm());
            }
        });

        mDataMatrix.createStringAdapter(DAPIKeys.ACTION_DAPI_ANALYTICS_STRING, machineID -> {
            DapiComm.Instance.Analytics.AN_ReportEventSet(machineID);
            DapiComm.Instance.Analytics.clearCurrEventSet();
        }, false);

        mDataMatrix.createIntAdapter(DAPIKeys.EVENT_DAPI_ANALYTICS_APP_INT, app -> {
            if (app != IGNORE_APP_ANALYTICS_ID) DapiComm.Instance.Analytics.appEvent(app);
        }, false);
        mDataMatrix.createStringAdapter(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, button -> DapiComm.Instance.Analytics.clickEvent(button), false);

        // Only save the FTP based on a test score
        mDataMatrix.createIntAdapter(DAPIKeys.ACTION_UPDATE_USER_FTP_INT, this::updateUserFtp, false);

        mDataMatrix.createIntAdapter(DAPIKeys.ACTION_UPDATE_USER_MAXHR_INT, this::updateUserMaxHeartRate, false);

        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING, this::updateUserAudioBLEMacAddr, false);
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, this::updateUserHRBLEMacAddr, false);
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING, this::updateUserAntHRDeviceID, false);
        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_USER_HAPTICS_ON_BOOL, this::updateUserHapticsOn,false);
        // fixme: this needs updating.
        mDataMatrix.createIntAdapter(DAPIKeys.DATA_USER_LED_STYLE_INT, this::updateUserLedStyle, false);
    }

    JohnsonWebBackendManager.OAuthInterface oAuthInterface = new JohnsonWebBackendManager.OAuthInterface() {
        @Override
        public void onAdditionalUserInfo(@NotNull UserMetadata userMetadata) {
            if(!fallbackUseDAPI) {
                applyUserMetadata(userMetadata);

                TiersFeatureParser tiersFeatureParser = new TiersFeatureParser();
                FeatureTierIdLists tierIdLists = userMetadata.getFeatureTierIdLists();
                if (tierIdLists != null) {
                    tiersFeatureParser.fromFeatureTierIdLists(tierIdLists);
                }
                tiersFeatureParser.updateUserSet();
            }
        }

        @Override
        public void onUserInfo(@NotNull UserInfo userInfo) {
            if(!fallbackUseDAPI) {
                parseUserInfo(userInfo);
            }
            else{
                JumpSession session = JumpSession.current();
                DataMatrix.putParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, new JumpSession(userInfo.getUserId(), session.getAccessToken(), session.getIdToken()));
            }
        }

        @Override
        public void onLoginFinished(boolean success, @NotNull String token, @NotNull String idToken) {
            // both tokens in one put so a reader never sees a new access token with an old id token
            DataMatrix.putParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, new JumpSession(JumpSession.current().getUserId(), token, idToken));
            jumpFinished = true;
            checkJumpDapiFinished();
            if (!fallbackUseDAPI) {
                jumpToken = token;
                DataMatrix.putInt(DapiLogin.Keys.EVENT_LOGIN_RESPONSE_INT, success? Success.Value : InvalidUsernameOrPassword.Value);
            }
        }

        @Override
        public void onSecaData(@Nullable SecaUserData secaUserData) {
            DataMatrix.putParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, secaUserData);
        }

        @Override
        public void onMatrixMethodData(@Nullable MatrixMethodProgram program) {
            if (fallbackUseDAPI) return;
            if (program == null) return;

            // Kept even for an empty shell so the next save patches that plan instead of duplicating it
            String planId = program.getId();
            if (!planId.isEmpty()) {
                DataMatrix.putString(DAPIKeys.DATA_MATRIX_METHOD_PROGRAM_ID_STRING, planId);
            }

            // An empty shell holds nothing, so applying it would wipe locally held progress
            if (!MatrixMethodProgramMapperKt.isActiveJumpPlan(program)) return;

            // JUMP's schema differs from the progression json UI/Progression consumes
            com.jht.progression.userinfo.UserInfo userInfo = MatrixMethodProgramMapperKt.jumpProgramToUserInfo(program, System.currentTimeMillis());
            if (userInfo == null) {
                Log.w(TAG, "Could not map Matrix Method program, leaving progression data untouched");
                return;
            }
            String progressionJson = UserInfoUtilKt.getUserProgramString(userInfo);
            matrixMethodLastSyncedJson = progressionJson;
            DataMatrix.updateString(DAPIKeys.DATA_PROGRESSION_PROGRAM_STRING, progressionJson);
        }
    };

    /**
     * Calls the login through JUMP and retrieves the user info
     */
    private void JUMPLogin(DapiLogin.DapiLoginParcelable loginParcelable){
        jumpFinished = false;
        DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, true);
        johnsonWebBackendManager.jumpLogin(getJumpAuthenticationData(), getUserAuthenticationInfo(),loginParcelable.Xid, loginParcelable.passcode,
                loginParcelable.loginType, oAuthInterface);
    }
    
    private void JUMPWalletLogin(WalletPass walletPass) {
        jumpFinished = false;
        johnsonWebBackendManager.egymWalletLogin(getJumpAuthenticationData(), getUserAuthenticationInfo(), walletPass, oAuthInterface);
    }

    private void JUMPQrLogin() {
        jumpFinished = false;
        // Cancel any prior session so a new launch can't race an old one.
        JUMPQrLoginStop();
        qrJob = johnsonWebBackendManager.jumpQrLogin(getJumpAuthenticationData(), getUserAuthenticationInfo(), oAuthInterface, new JohnsonWebBackendManager.QRLoginInterface() {
            @Override
            public void onDeviceCodeReceived(@NotNull DeviceCodeData deviceCodeData) {
                DataMatrix.putParcelable(DapiLogin.Keys.EVENT_QR_CODE_PARCELABLE, toQrAuthorizationResponse(deviceCodeData));
            }

            @Override
            public void onDeviceCodeExpired() {
                DataMatrix.putInt(DapiLogin.Keys.EVENT_QR_LOGIN_RESPONSE_INT, DapiLogin.QrResult.EXPIRED);
            }

            @Override
            public void onDeviceCodeError() {
                DataMatrix.putInt(DapiLogin.Keys.EVENT_QR_LOGIN_RESPONSE_INT, DapiLogin.QrResult.ERROR);
            }

            @Override
            public void onDeviceCodeSuccess() {
                DataMatrix.putInt(DapiLogin.Keys.EVENT_QR_LOGIN_RESPONSE_INT, DapiLogin.QrResult.SUCCESS);
            }
        });
    }

    /** Clears per user Matrix Method state so nothing carries across an identity switch. */
    private void resetMatrixMethodState() {
        matrixMethodLastSyncedJson = null;
    }

    /**
     * Returns the basic info needed for jump authentication
     */
    private JumpAuthenticationInfo getJumpAuthenticationData(){
        String machineId = DapiUtil.getMachineId();
        int curDapiEnv = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT);
        int clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);
        return new JumpAuthenticationInfo(curDapiEnv, machineId, clubId);
    }

    /**
     * Returns the user specific information needed for retrieval of data
     */
    private UserAuthenticationInfo getUserAuthenticationInfo(){
        String machineType = FrameConfigManager.getInstance().getFrameConfig().machineType.name();
        List<String> settings = new ArrayList<>(Arrays.asList(DAPIKeys.Constants.TYPE_USER_SETTING, DAPIKeys.Constants.TYPE_USER_CONNECTED_DEVICE_SETTING, UserSettingsKeys.HandGripSettings.Key));
        boolean hasSeca = DataMatrix.getBoolean(DAPIKeys.DATA_HAS_SECA_FEATURE_BOOL);
        return new UserAuthenticationInfo(machineType, hasSeca, settings);
    }

    private void JUMPQrLoginStop() {
        if (qrJob != null) {
            qrJob.cancel(null);
            qrJob = null;
        }
    }

    private static QrAuthorizationResponse toQrAuthorizationResponse(@NonNull DeviceCodeData data) {
        String complete = data.getVerificationUriComplete();
        if (complete == null || complete.isEmpty()) complete = data.getVerificationUri();
        return new QrAuthorizationResponse(
                data.getDeviceCode(),
                data.getUserCode(),
                data.getVerificationUri(),
                complete,
                data.getExpiresIn(),
                data.getInterval());
    }

    private void parseUserInfo(@NonNull UserInfo userInfo) {
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
        // the user id and the tokens arrive in separate callbacks, so each keeps the other's fields
        JumpSession session = JumpSession.current();
        DataMatrix.putParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, new JumpSession(userInfo.getUserId(), session.getAccessToken(), session.getIdToken()));
        mUser.uuid = userInfo.getUserId();
        mUser.userToken = ""; // JUMP user should leave this empty
        mUser.xid = mLoginXid;
        //mUser.passcode = mLoginPass; // what are ramifications of this? we only need it for profiles?
        mUser.genderIdentity = GenderIndentity.fromInt(userInfo.getGender());
        float oauthWeightKg = userInfo.getWeightKg();
        boolean hasOauthWeight = oauthWeightKg > 0;
        float weightKg = hasOauthWeight ? oauthWeightKg : DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT);
        mUser.setWeight(new Weight(weightKg, WeightUnit.KILOGRAM));
        DataMatrix.putBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL, !hasOauthWeight);
        mUser.age = userInfo.getAge();
        mUser.unitPreferenceMetric = userInfo.getUnitPreferenceMetric();
        mUser.firstName = userInfo.getGivenName();
        mUser.lastName = userInfo.getFamilyName();
        mUser.acceptedTerms = true; //userInfo.AcceptedTerms; // for some reason, this returns false
        mUser.sync();
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, userInfo.getUnitPreferenceMetric());
        saveUserSettings = false;
        // Drop any previous user's program id; the program fetch that follows sets the new one.
        DataMatrix.putString(DAPIKeys.DATA_MATRIX_METHOD_PROGRAM_ID_STRING, "");
        resetMatrixMethodState();
        // JUMP/OAuth login is also an identity switch; bump generation so a stale save can't land here.
        DataMatrix.putInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT, DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) + 1);
    }

    /**
     * Applies the typed JUMP user metadata. matrix_method_disclaimer_accepted and high_sweat_score are
     * intentionally unused for now (Matrix Method / new Sprint8 respectively).
     */
    private void applyUserMetadata(@NotNull UserMetadata userMetadata) {
        if (userMetadata.getPtp() != null) {
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL, userMetadata.getPtp());
        }
        if (userMetadata.getSkeletalMuscleMassResult() != null) {
            DataMatrix.putFloat(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT, userMetadata.getSkeletalMuscleMassResult());
        }

        PhoenixUserSettings phoenix = userMetadata.getPhoenixUserSettings();
        if (phoenix != null) applyPhoenixUserSettings(phoenix.getMaxHr(), phoenix.getFtp());

        ConnectedDeviceSettings devices = userMetadata.getConnectedDeviceSettings();
        if (devices != null) {
            applyConnectedDeviceSettings(devices.getBleHrMacAddress(), devices.getBleAudioMacAddress(), devices.getAntPlusHrDeviceId());
        }

        HandGripSettings handGrip = userMetadata.getHandGripSettings();
        // fixme: Apply the settings iff actually executing on Project One.
        if (handGrip != null && ProductConfig.isProjectOne()) {
            applyHandGripSettings(handGrip.getUserHapticsOn(), handGrip.getUserLedStyle());
        }
    }

    /** Out of range values (e.g. -1) fall back to defaults; null keeps the currently stored value. */
    private void applyPhoenixUserSettings(@Nullable Integer maxHr, @Nullable Integer ftp) {
        mMaxHr = maxHr != null ? maxHr : DataMatrix.getInt(DAPIKeys.DATA_USER_MAXHR_INT);
        if (mMaxHr < MainAppKeys.TT_DEFAULTS.TT_BPM_MIN_VALUE || mMaxHr > MainAppKeys.TT_DEFAULTS.TT_BPM_MAX_VALUE) {
            mMaxHr = MainAppKeys.TT_DEFAULTS.TT_BPM_DEFAULT_VALUE;
        }
        DataMatrix.putInt(DAPIKeys.DATA_USER_MAXHR_INT, mMaxHr);

        mUserFtp = ftp != null ? ftp : DataMatrix.getInt(DAPIKeys.DATA_USER_FTP_INT);
        if (mUserFtp < MainAppKeys.TT_DEFAULTS.TT_WATTS_MIN_VALUE || mUserFtp > MainAppKeys.TT_DEFAULTS.TT_WATTS_MAX_VALUE) {
            mUserFtp = MainAppKeys.TT_DEFAULTS.TT_WATTS_DEFAULT_VALUE;
        }
        DataMatrix.putInt(DAPIKeys.DATA_USER_FTP_INT, mUserFtp);
    }

    /** Publishes all three device ids, treating absent ones as empty, then auto pairs if any are set. */
    private void applyConnectedDeviceSettings(@Nullable String hrMac, @Nullable String audioMac, @Nullable String antId) {
        bleHR_macAddress = hrMac != null ? hrMac : "";
        bleAudio_macAddress = audioMac != null ? audioMac : "";
        antPlusHR_id = antId != null ? antId : "";
        DataMatrix.putString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, bleHR_macAddress);
        DataMatrix.putString(DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING, bleAudio_macAddress);
        DataMatrix.putString(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING, antPlusHR_id);

        // If we have auto connect mac addresses try to scan for those to connect after logging in
        if (!bleHR_macAddress.isEmpty() || !bleAudio_macAddress.isEmpty() || !antPlusHR_id.isEmpty()) {
            startBackgroundPairing();
        }
    }

    /** Callers gate on Project One. Null values fall back to haptics on and the default LED style. */
    private void applyHandGripSettings(@Nullable Boolean userHapticsOn, @Nullable Integer userLedStyle) {
        hapticsOn = userHapticsOn == null || userHapticsOn;
        ledStyle = LEDStyle.fromValue(userLedStyle != null ? userLedStyle : LEDStyle.DEFAULT.value);
        DataMatrix.putBoolean(DAPIKeys.DATA_USER_HAPTICS_ON_BOOL, hapticsOn);
        DataMatrix.putInt(DAPIKeys.DATA_USER_LED_STYLE_INT, ledStyle.value);
    }

    private void checkGetUserSetting(String value, String key) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "DAPI User Setting checkGetUserSetting key = " + key + " | JSONObject = " + value);
            else Log.d(TAG, "DAPI User Setting checkGetUserSetting key = " + key);
            // Value is just a double without key for SMM, hence created the below if-else block
            if (key.equalsIgnoreCase(DAPIKeys.Constants.TYPE_EVOL_SKELETAL_MUSCLE_MASS)) {
                // This block handles the skeletal muscle mass result for evolt user
                try {
                    DataMatrix.putFloat(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT, (float) Double.parseDouble(value));
                } catch (NumberFormatException exception) {
                    Log.e(TAG, "Parsing error while reading skeletal muscle mass result");
                }
            } else {
                if (key.equalsIgnoreCase(DAPIKeys.Constants.TYPE_USER_SETTING)) {
                    JSONObject jsonObject = new JSONObject(value);
                    applyPhoenixUserSettings(
                            jsonObject.optInt(DAPIKeys.Constants.MAX_HR_USER_KEY, DataMatrix.getInt(DAPIKeys.DATA_USER_MAXHR_INT)),
                            jsonObject.optInt(DAPIKeys.Constants.FTP_USER_KEY, DataMatrix.getInt(DAPIKeys.DATA_USER_FTP_INT)));
                } else if (key.equalsIgnoreCase(DAPIKeys.Constants.TYPE_USER_CONNECTED_DEVICE_SETTING)) {
                    JSONObject jsonObject = new JSONObject(value);
                    applyConnectedDeviceSettings(
                            jsonObject.optString(DAPIKeys.Constants.USER_BLE_HR_MAC_ADDRESS, ""),
                            jsonObject.optString(DAPIKeys.Constants.USER_BLE_AUDIO_MAC_ADDRESS, ""),
                            jsonObject.optString(DAPIKeys.Constants.USER_ANT_HR_DEVICE_ID, ""));
                } else if (key.equalsIgnoreCase(UserSettingsKeys.HandGripSettings.Key)) {
                    // fixme: Apply the settings iff actually executing on Project One.
                    if (ProductConfig.isProjectOne()) {
                        JSONObject jsonObject = new JSONObject(value);
                        applyHandGripSettings(
                                jsonObject.optBoolean(DAPIKeys.Constants.USER_HAPTICS_ON, true),
                                jsonObject.optInt(DAPIKeys.Constants.USER_LED_STYLE, LEDStyle.DEFAULT.value));
                    }
                }
            }
        } catch (JSONException e) {

            // This handles the case where the DAPI key-value pair for bluetooth devices only contained the element of one BLE MAC address
            // And that address was not in a JSON format and is assumed to be an HR device.   ITC compatibility. This code can removed in 2023 or later
            if (key.equalsIgnoreCase(DAPIKeys.Constants.TYPE_USER_CONNECTED_DEVICE_SETTING)) {
                bleAudio_macAddress = "";
                antPlusHR_id = "";
                bleHR_macAddress = value;
                DataMatrix.putString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, bleHR_macAddress);
                DataMatrix.putString(DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING, "");
                DataMatrix.putString(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING, "");
                if(value.contains(":")) {
                    startBackgroundPairing();
                }
            }

            Log.e(TAG, "Error occurred in parsing JSON! " + value, e);
        }
    }

    //This is also a temporary method until the user info can be retrieved through the JUMP token
    private void checkJumpDapiFinished(){
        //User info is finished once DAPI and Jump finish, or just DAPI if not using JUMP
        if((jumpFinished && !fallbackUseDAPI) || (jumpFinished && dapiFinished)){
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, false);
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, false);
        }
    }

    private void saveUserSettingAsJSON () {
        String jsonString = getUserSettingJson();
        if (jsonString != null) {
            sendUserSettingsToDAPI(DAPIKeys.Constants.TYPE_USER_SETTING, jsonString);
        }
    }

    private void saveUserDeviceSettingAsJSON () {
        String jsonString = getUserConnectedDeviceSettingJson();
        if (jsonString != null) {
            sendUserSettingsToDAPI(DAPIKeys.Constants.TYPE_USER_CONNECTED_DEVICE_SETTING, jsonString);
        }
    }

    private void saveUserHandgripSettingsAsJSON() {
        String jsonString = getUserHandgripSettingJson();
        if (jsonString != null) {
            sendUserSettingsToDAPI(UserSettingsKeys.HandGripSettings.Key, jsonString);
        }
    }

    public void updateUserMaxHeartRate(int maxHr) {
        Log.d(TAG, "DAPI User Setting updateUserMaxHeartRate attempting " + maxHr);
        this.mMaxHr = maxHr;
        saveUserSettings = true;
    }

    public void updateUserFtp(int userFtp) {
        Log.d(TAG, "DAPI User Setting updateUserFtp attempting " + userFtp);
        this.mUserFtp = userFtp;
        saveUserSettings = true;
    }

    public void updateUserAudioBLEMacAddr(String bleAudioMacAddress) {
        Log.d(TAG, "DAPI User Setting updateUserAudioBLEMacAddr attempting " + bleAudioMacAddress);
        this.bleAudio_macAddress = bleAudioMacAddress;
        saveUserSettings = true;
    }

    private void updateUserHRBLEMacAddr(String bleHRMacAddress) {
        Log.d(TAG, "DAPI User Setting updateUserHRBLEMacAddr attempting " + bleHRMacAddress);
        this.bleHR_macAddress = bleHRMacAddress;
        saveUserSettings = true;
    }

    private void updateUserAntHRDeviceID(String antHR_deviceID) {
        Log.d(TAG, "DAPI User Setting updateUserAntHRDeviceID attempting " + antHR_deviceID);
        this.antPlusHR_id = antHR_deviceID;
        saveUserSettings = true;
    }

    public void updateUserHapticsOn(boolean hapticsOn) {
        if (this.hapticsOn == hapticsOn) return;
        Log.d(TAG, "DAPI User Setting updateUserHapticsOn attempting " + hapticsOn);
        this.hapticsOn = hapticsOn;
        saveUserSettings = true;
    }

    public void updateUserLedStyle(final int style) {
        LEDStyle ledStyle = LEDStyle.fromValue(style);
        if (this.ledStyle.equals(ledStyle)) return;
        Log.d(TAG, "DAPI User Setting updateUserLedStyle set " + ledStyle.name());
        this.ledStyle = ledStyle;
        if (!this.ledStyle.equals(LEDStyle.DEFAULT)) { // If it doesn't equal the default value.
            saveUserSettings = true;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void sendUserSettingsToDAPI(String name, String value) {
        if (!DapiUser.isGuestUser() && getUserToken().compareTo(DapiUser.GUEST_USER_TOKEN) != 0) {
            Log.i(TAG, "DAPI User Setting setUserSetting = " + mUser.userToken + " key" + name + "  value=" + value);
            DapiComm.Instance.UserManagement.setUserSetting(mUser.userToken, name, value);
        }
    }

    private String getUserSettingJson() {
        try {
            Log.d(TAG, "DAPI User Setting getUserSettingJson 1");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(DAPIKeys.Constants.MAX_HR_USER_KEY, mMaxHr);
            jsonObject.put(DAPIKeys.Constants.FTP_USER_KEY, mUserFtp);
            Log.d(TAG, "DAPI User Setting getUserSettingJson 2 " + jsonObject.toString());
            return jsonObject.toString();
        } catch (JSONException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private String getUserConnectedDeviceSettingJson() {
        try {
            Log.d(TAG, "DAPI User Setting getUserSettingJson 1");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(DAPIKeys.Constants.USER_BLE_AUDIO_MAC_ADDRESS, bleAudio_macAddress);
            jsonObject.put(DAPIKeys.Constants.USER_BLE_HR_MAC_ADDRESS, bleHR_macAddress);
            jsonObject.put(DAPIKeys.Constants.USER_ANT_HR_DEVICE_ID, antPlusHR_id);
            Log.d(TAG, "DAPI User Setting getUserSettingJson 2 " + jsonObject.toString());
            return jsonObject.toString();
        } catch (JSONException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private String getUserHandgripSettingJson() {
        try {
            Log.d(TAG, "DAPI User Setting getUserHandgripSetting 1");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(DAPIKeys.Constants.USER_HAPTICS_ON, hapticsOn);
            jsonObject.put(DAPIKeys.Constants.USER_LED_STYLE, ledStyle.value);
            Log.d(TAG, "DAPI User Setting getUserHandgripSetting 2 " + jsonObject.toString());
            return jsonObject.toString();
        } catch (JSONException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private void logout() {
        backgroundPairing.stopAutoPairing(); // Stop scanning if the user is logged out

        if (!ProductConfig.hasUserProfiles()) { // user profiles stay logged in, just need to switch to guest profile
            if (!mUser.isLocalUser() && !mUser.userToken.equalsIgnoreCase(DapiUser.GUEST_USER_TOKEN)) {
                if (saveUserSettings) {
                    saveUserSettingAsJSON();
                    saveUserDeviceSettingAsJSON();
                    saveUserHandgripSettingsAsJSON();
                }

                DapiComm.Instance.UserManagement.logOut(mUser.userToken, DapiUtil.getMachineId());
            }
        }
        setUserToGuest(); // there has to be a user selected, so make it guest until a successful login
    }

    private void checkSetUserSetting(DapiResult result) {
        if (result != Success) {
            Log.e(TAG, "DAPI User Setting SetUserSetting failure: " + result.name());
        }
    }

    public String getUserToken() {
        Log.i(TAG, "getUserToken:  " + mUser.userToken);
        return mUser.userToken;
    }

    private static class SingletonHolder {
        private static final DAPIUserManagement INSTANCE = new DAPIUserManagement();
    }

    public static DAPIUserManagement getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void umHandler(CborCommands cborCommands, DapiResult dapiResult) {
        Log.d(TAG, "==> " + cborCommands + " | " + dapiResult);
        switch (cborCommands) {
            case CheckXidExists:
                // handle event check xid exists response
                DataMatrix.putInt(DapiLogin.Keys.EVENT_CHECK_XID_EXISTS_RESPONSE_INT, dapiResult.Value);
                break;
            case CheckEmailExists:
                // handle event check email exists response
                DataMatrix.putInt(DapiCreateUser.Keys.EVENT_CHECK_EMAIL_EXISTS_RESPONSE_INT, dapiResult.Value);
                break;
            case UnlinkUser:
                DataMatrix.putInt(DapiCreateUser.Keys.EVENT_REMOVE_PROFILE_RESPONSE_INT, dapiResult.Value);
                break;
            case Logout:
                // do nothing
                break;
            case SetUserSetting:
                checkSetUserSetting(dapiResult);
                break;
            case UpdateUserInfo:
                if (dapiResult == Success) {
                    updatePendingUserProfile(); //syncTimerKick(); // update user data right away instead of waiting for background sync
                } else {
                    pendingUserUpdate = null; // clear it out for next attempt
                }
                DataMatrix.putInt(DapiCreateUser.Keys.EVENT_UPDATE_USER_RESPONSE_INT, dapiResult.Value);
                //DataMatrix.putInt(mResponseKey, dapiResult.Value);
                break;
        }
    }

    @Override
    public void umGetUserSettingsHandler(CborCommands cborCommands, DapiResult dapiResult, ArrayList<UserSetting> arrayList) {
        // Do not need this for Chimera right now.
        Log.d(TAG, "DAPI User Setting Got umGetUserSettingHandler UNEXPECTED response");
    }

    @Override
    public void umSyncUserHandler(CborCommands cborCommands, DapiResult dapiResult, DapiUserInfo dapiUserInfo, String userToken, int userSlot, int userSetup) {

        Log.w(TAG, "umSyncUserHandler slot=" + userSlot + ", userSetup=" + userSetup + ", result=" + dapiResult);
        // 2021-02-11 SYNC no longer used due to security issues
    }

    public void syncTimerStart() { if (ProductConfig.hasUserProfiles() && syncTimer != null) syncTimer.start(); }
    public void syncTimerStop() { if (ProductConfig.hasUserProfiles() && syncTimer != null) syncTimer.interrupt(); }

    private void startBackgroundPairing() {
        Log.d(TAG, "Starting bluetooth auto connect scan");
        backgroundPairing.startAutoPairing(5, 60);
        mDataMatrix.createEventAdapter(DAPIKeys.ACTION_STOP_BACKGROUND_BLUETOOTH_EVENT, keyName -> backgroundPairing.stopAutoPairing());
        mDataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, keyName -> backgroundPairing.stopAutoPairing() );
    }

    private void setUserToGuest() {
        //TODO: set ptp to false
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL, false);
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
        DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, false);
        mUser = new DapiUser();
        mXid = mUser.xid;
        DataMatrix.putInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT, DAPIKeys.Constants.DAPI_GUEST_INDEX); // always do slot before other user info that might trigger events
        mUser.sync();
        mUser.syncDisplayUnits();
        // sync() doesn't clear progression (syncExtendedInfo skips empty progressionData), and it's
        // keyed by the shared GUEST_USER_UUID, so reset it explicitly or the next guest inherits it.
        DataMatrix.putString(DAPIKeys.DATA_PROGRESSION_PROGRAM_STRING, "");
        // Must clear with the program, or a later save would patch the previous user's JUMP program.
        DataMatrix.putString(DAPIKeys.DATA_MATRIX_METHOD_PROGRAM_ID_STRING, "");
        resetMatrixMethodState();
        MatrixProgressionModelKt.resetGuestUser();
        // Bump generation so an in-flight save from the previous guest is dropped as stale.
        DataMatrix.putInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT, DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) + 1);
        DataMatrix.putInt(DAPIKeys.DATA_USER_FTP_INT, DefaultEquations.getDefaultFtp(mUser.genderIdentity));
        DataMatrix.putInt(DAPIKeys.DATA_USER_MAXHR_INT, -1);
        DataMatrix.putBoolean(DAPIKeys.DATA_USER_HAPTICS_ON_BOOL, DataMatrix.getBoolean(DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL));
        DataMatrix.putInt(DATA_LED_STYLE_SYSTEM_INT, LEDStyle.DEFAULT.value);
        DataMatrix.putInt(DAPIKeys.DATA_USER_LED_STYLE_INT, DataMatrix.getInt(DATA_LED_STYLE_SYSTEM_INT));
        DataMatrix.putFloat(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT, 0f);
        // an empty session rather than null, because parcelable listeners skip null values
        DataMatrix.putParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, new JumpSession());
        DataMatrix.putParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, null);
        DataMatrix.putString(DAPIKeys.DATA_USER_CREATE_WALLET_PASS_STRING, "");
        jumpToken = null;
    }

    private boolean setUserToProfile(int slot) {
        UserProfileManager.ProfileUser user = profileManager.getUserBySlot(slot);
        return setUserToProfile(user);
    }

    private boolean setUserToProfile(UserProfileManager.ProfileUser user) {
        if (user == null) return false; // user = UserProfileManager.guestProfile;
        mUser.copyFrom(user); // DO NOT ASSIGN profile user directly to mUser as this service modifies it!
        mXid = mUser.xid;
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL, false);
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
        user.sync(); // ProfileUser.sync() includes updating DATA_USER_PROFILE_SELECTED_SLOT_INT
        user.syncDisplayUnits();
        // Bump generation here too so an in-flight save from the previous user is dropped as stale.
        // Also runs for a same-user refresh (updatePendingUserProfile) - harmless, just a resend.
        DataMatrix.putInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT, DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) + 1);
        return true;
    }

    private void selectLastProfileUser() {
        UserProfileManager.ProfileUser user = profileManager.getCurrentProfile();
        Log.d(TAG, "last user profile slot " + user.slotNumber);
        if (user.isGuest()) setUserToGuest();
        else setUserToProfile(user);
    }

    private void removeAllProfiles(boolean force) {
        if (profileManager == null) return;
        int curDapiEnv = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT);
        boolean dapiChanged = (curDapiEnv >= 0 && profileManager.getProfileData().dapiEnvironment != curDapiEnv);

        if (dapiChanged) profileManager.getProfileData().dapiEnvironment = curDapiEnv; // better here or in UserProfileManager save? here seems safer

        if (force || dapiChanged) {
            for (int i = 1; i <= UserProfileManager.MAX_USER_LIMIT; ++i) {
                //removeProfile(i); // this would unlink, but environ changed, from where would it unlink? just remove locally as only dev/test change environ
                synchronized (profileManagerLock) {
                    profileManager.removeProfile(i);
                }
            }
            UserProfileManager.sendChangeEvent();
            setUserToGuest();
        }
    }

    private void removeProfile(int slot) {
        UserProfileManager.ProfileUser user;
        synchronized (profileManagerLock) {
            user = profileManager.removeProfile(slot);
        }
        UserProfileManager.sendChangeEvent();
        //if (user == null || DapiUser.isLocalUser(user.xid)) {
            DataMatrix.putInt(DapiCreateUser.Keys.EVENT_REMOVE_PROFILE_RESPONSE_INT, (user != null ? Success.Value : InvalidRequest.Value));
        /*} else {
            DapiComm.Instance.UserManagement.unlinkUser(user.slotNumber, DapiUtil.getMachineId()); // since we don't use slots anymore, does this have value? do we leave it a while longer in case we need to disassociate old machines?
        }*/
    }

    private void updatePendingUserProfile() {
        if (profileManager != null && pendingUserUpdate != null) {
            UserProfileManager.ProfileUser user;
            synchronized (profileManagerLock) {
                user = profileManager.updateUser(pendingUserUpdate);
            }
            UserProfileManager.sendChangeEvent();
            // only if updating the current user!
            if (pendingUserUpdate.xid.equals(mUser.xid)) {
                setUserToProfile(user);
            }
        }
        pendingUserUpdate = null; // let that memory go
    }

    /*
     * Populate a DapiUser object for custom XareFit login
     */
    public void populateXareFitUserProfile(String userName, int year, int gender, float weight) {
        mUser.firstName = userName;
        mUser.birthDay = 1;
        mUser.birthMonth = 1;
        mUser.birthYear = year;
        mUser.genderIdentity = GenderIndentity.fromInt(gender);
        mUser.setWeight(new Weight(weight, WeightUnit.KILOGRAM));
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
        mUser.sync();
        mUser.syncDisplayUnits();
    }


    @Deprecated
    @Override
    public void umStringHandler(CborCommands cborCommands, DapiResult dapiResult, String value) {
        Log.i(TAG, "umStringHandler: " + cborCommands.toString() + " " + dapiResult.Value);
        switch (cborCommands) {
            case Login:
                //mResponseKey = DapiLogin.Keys.EVENT_LOGIN_RESPONSE_INT.name();
                if (dapiResult == Success) {
                    handleLogin(value);
                } else {
                    if (dapiResult == InvalidUsernameOrPassword) {
                        if (profileManager != null) {
                            synchronized (profileManagerLock) { profileManager.setPassBounce(mLoginXid, true); } // use xid or slot, does it matter?
                        }

                    }

                    DataMatrix.putInt(DapiLogin.Keys.EVENT_LOGIN_RESPONSE_INT, dapiResult.Value);
                }
                break;
            case NewUserRegistration:
                Log.e(TAG, "umStringHandler result: " + dapiResult);
                if (dapiResult == Success) {
                    //mResponseKey = DapiCreateUser.Keys.EVENT_REGISTER_RESPONSE_INT.name();
                    if (mLoginSwitchUser) {
                        mUser.userToken = value;
                    }
                    if (profileManager != null) {
                        synchronized (profileManagerLock) { profileManager.updateSlotToken(mLoginSlot, value); }
                    }
                    DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, true);
                    jumpFinished = true;
                    DapiComm.Instance.UserManagement.getUserInfo(value, DapiUtil.getMachineId());
                }
                DataMatrix.putInt(DapiCreateUser.Keys.EVENT_REGISTER_RESPONSE_INT, dapiResult.Value);
                break;
        }
    }

    @Deprecated
    @Override
    public void umDapiUserInfoHandler(CborCommands cborCommands, DapiResult dapiResult, DapiUserInfo userInfo) {
        Log.d(TAG, "ptp_timing: Got user info");

        if (dapiResult == Success) {
            if (mLoginSwitchUser) {
                DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
            }
            if (mLoginXid != null && !mLoginXid.isEmpty() && mLoginXid.equals(mXid)) {
                if (ProductConfig.hasUserProfiles()) {
                    synchronized (profileManagerLock) {
                        UserProfileManager.ProfileUser profileUser = profileManager.updateUser(mLoginSlot, userInfo, mLoginXid, mLoginPass);
                        if (profileUser != null) {
                            UserProfileManager.sendChangeEvent();
                            if (mLoginSwitchUser) setUserToProfile(profileUser);
                        }
                    }
                } else if (mLoginSwitchUser) {
                    mUser.uuid = userInfo.DapiUuid;
                    mUser.xid = mLoginXid;
                    //mUser.passcode = mLoginPass; // what are ramifications of this? we only need it for profiles?
                    mUser.genderIdentity = userInfo.Gender;
                    mUser.setWeight(DapiUser.getInfoWeight(userInfo));
                    DataMatrix.putBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL, !(userInfo.Weight > 0));
                    mUser.birthYear = userInfo.Birthday.Year;
                    mUser.birthMonth = userInfo.Birthday.Month;
                    mUser.birthDay = userInfo.Birthday.Day;
                    mUser.age = DapiUser.getAge(mUser.birthYear, mUser.birthMonth, mUser.birthDay);
                    mUser.unitPreferenceMetric = userInfo.UnitsPreferenceMetric;
                    mUser.firstName = userInfo.FirstName;
                    mUser.lastName = userInfo.LastName;
                    mUser.acceptedTerms = true; //userInfo.AcceptedTerms; // for some reason, this returns false
                    mUser.sync();
                    DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, userInfo.UnitsPreferenceMetric);
                    saveUserSettings = false;
                    // No-profiles builds skip setUserToProfile(), so bump generation here too.
                    DataMatrix.putInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT, DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) + 1);
                }
                mLoginPass = ""; // do we need to clear this immediately after we're done with it?
                dapiFinished = true;
                DataMatrix.putBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL, userInfo.connectionsInfo.PTP);
            }
            Log.d(TAG, "ptp_timing: Getting tiers");
            DataMatrix.putBoolean(TiersKeys.DATA_UPDATE_TIERS_BOOL, true);
        } else {
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, false);
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_CUSTOM_WORKOUT_BOOL, false);
        }

        checkJumpDapiFinished();
        DataMatrix.putInt(DapiLogin.Keys.EVENT_GET_INFO_RESPONSE_INT, dapiResult.Value);
    }

        @Deprecated
        @Override
        public void umGetUserSettingHandler(CborCommands cborCommands, DapiResult dapiResult, String value, String key) {
            Log.d(TAG, "DAPI User Setting Got umGetUserSettingHandler response");
            if (cborCommands == CborCommands.GetUserSetting) {
                if(dapiResult == Success) checkGetUserSetting(value, key);
            }
        }

        @Deprecated
        private void handleLogin(String userToken) {
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
        if (mLoginSwitchUser || mUser.xid.equals(mLoginXid)) {
            mUser.userToken = userToken;
        }
        if (profileManager != null) {
            synchronized (profileManagerLock) { profileManager.updateSlotToken(mLoginSlot, userToken); } // this should clear passcode bounce flag as well
        }
        DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, true);
        Log.d(TAG, "Get user info");
        DapiComm.Instance.UserManagement.getUserInfo(userToken, DapiUtil.getMachineId());
        if (!ProductConfig.hasUserProfiles() && mLoginSwitchUser) {
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, true);
            // get the user settings, user values/scores from FTP test, Max HR test etc.
            DapiComm.Instance.UserManagement.getUserSetting(userToken, DAPIKeys.Constants.TYPE_USER_SETTING);
            Log.d(TAG, "Get user connections info");
            DapiComm.Instance.UserManagement.getUserSetting(userToken, DAPIKeys.Constants.TYPE_USER_CONNECTED_DEVICE_SETTING);
            DapiComm.Instance.UserManagement.getUserSetting(userToken, UserSettingsKeys.HandGripSettings.Key);

            // Evolt skeletal muscle mass retrieval
            DapiComm.Instance.UserManagement.getUserSetting(userToken, DAPIKeys.Constants.TYPE_EVOL_SKELETAL_MUSCLE_MASS);
        }

        DataMatrix.putInt(DapiLogin.Keys.EVENT_LOGIN_RESPONSE_INT, Success.Value);
    }
}

// This class is to set the logic for sending login commands of the current selected xID user during booting
class SyncUserThread extends Thread {
    private static final String TAG = "SyncUserThread";
    private final UserProfileManager profileManager = UserProfileManager.getInstance();

    @Override
    public void run() {
        try {
            DataMatrix dataMatrix = new DataMatrix();
            UserProfileManager.ProfileUser user;

            synchronized (profileManager) {
                user = profileManager.getCurrentProfile();
            }

            if (user != null && !user.isGuest() && !user.isLocalUser()) {
                Log.d(TAG, "Sending login commands of the current selected xID: " + user.xid);
                DapiLogin.loginWithInfo(
                        dataMatrix,
                        user.xid,
                        user.passcode,
                        user.slotNumber,
                        false,
                        DapiUtil.LOGIN_TYPE_XID,
                        dapiStatus -> {
                            dataMatrix.finish();
                            if (dapiStatus == 0) {
                                Log.d(TAG, "Sending info success for: " + user.xid);
                            } else {
                                Log.d(TAG, "Sending info failed for: " + user.xid);
                            }
                        }
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Sending login info error: ", e);
        } finally {
            Log.d(TAG, "Sending login sequence completed");
        }
    }
}

