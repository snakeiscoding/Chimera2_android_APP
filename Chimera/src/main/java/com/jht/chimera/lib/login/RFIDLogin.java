package com.jht.chimera.lib.login;

import static com.jht.chimera.lib.login.WalletPassCreator.KEY_BARCODE;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_CUSTOM;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_EMAIL;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_EMAIL_PASSWORD;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_PASSCODE;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_PASSWORD;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_PHONE;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_RFID;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_TYPE;
import static com.jht.chimera.lib.login.WalletPassCreator.KEY_XID;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.rfid.PFBarcodeData;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;
import com.jhtp.dapiservice.DapiResult;

import org.json.JSONObject;

import java.math.BigInteger;

/**
 * This class provides RFID login functionality.
 */
public class RFIDLogin {
    private static final String TAG = "RFIDLogin";
    private final ILoginListener listener;
    private boolean rfidLoginEnabled = false;
    private RFIDChip chip;
    private String tag;
    private boolean associate;

    /**
     * Constructs a new instance of the RFIDLogin class.
     * @param dataMatrix DataMatrix object used to setup RFID scan adapters.
     */
    public RFIDLogin(@NonNull DataMatrix dataMatrix, @NonNull ILoginListener listener){
        this.listener = listener;

        dataMatrix.createParcelableAdapter(
                HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE,
                RFIDChip.class,
                chip -> {
                    this.chip = chip;
                    this.tag = StringUtil.byteArray2Hex(chip.uid());
                    this.associate = !SettingKeys.AssetManagement.isAllLoginTypeDisabled();
                    handleRFIDScan(dataMatrix);
                }
        );

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_PF_BARCODE_DETECTED_PARCELABLE, PFBarcodeData.class, barcodeData -> {
            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)) {
                this.chip = barcodeData.chip();
                this.tag = barcodeData.barcode();
                this.associate = false;
                handleRFIDScan(dataMatrix);
            } else {
                this.chip = barcodeData.chip();
                this.tag = StringUtil.byteArray2Hex(barcodeData.chip().uid());
                this.associate = !SettingKeys.AssetManagement.isAllLoginTypeDisabled();
                handleRFIDScan(dataMatrix);
            }
        }, false);

        dataMatrix.createParcelableAdapter(
            HALKeys.EVENT_WALLET_PASS_DETECTED_PARCELABLE,
            WalletPass.class,
            walletPass -> {
                handleWalletPass(dataMatrix, walletPass);
            }
        );
    }

    /**
     * Starts scanning for RFID chips. Scans only once, then turns off.
     */
    public void start(){
        if (isAvailable()){
            rfidLoginEnabled = true;
            DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, true);
        }
        else {
            stop();
        }
    }

    /**
     * Stops scanning for RFID chips.
     */
    public void stop(){
        rfidLoginEnabled = false;
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, false);
    }

    /**
     * Handles RFID login scans when they occur.
     * @param mDataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     */
    private void handleRFIDScan(DataMatrix mDataMatrix){
        if (isAvailable() && rfidLoginEnabled) {
            rfidLoginEnabled = false;

            listener.onLoginAttemptStart();

            switch (getEndpoint()) {
                case DAPI:
                    // Perform DAPI login. Chip, tag, and associate variables must be class-level variables due to DAPI using callback in DataMatrix adapter.
                    DapiLogin.login(mDataMatrix, tag, "" + (int) (chip.rfStandard().toByte()), DapiUtil.LOGIN_TYPE_RFID, result -> {
                        Result rfidLoginResponse;

                        switch (DapiResult.fromInt(result)) {
                            case Success:
                                rfidLoginResponse = Result.SUCCESS;
                                break;
                            case InvalidUsernameOrPassword:
                            case LoginFailed:
                                if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_CUSTOM_RFID_ONLY_BOOL)) {
                                    rfidLoginResponse = Result.INVALID_CREDENTIALS;
                                } else {
                                    rfidLoginResponse = associate ? Result.NO_ASSOCIATED_XID : Result.INVALID_CREDENTIALS;
                                }
                                break;
                            default:
                                rfidLoginResponse = Result.CONNECTION_ERROR;
                        }

                        listener.onLoginAttemptEnd(rfidLoginResponse, chip);
                    });
                    break;
                case ALTERNATE:
                    // If a non-Dapi intranet endpoint is defined in M/E/S, store the UID from the RFID chip as the user token
                    DataMatrix.putString(DAPIKeys.DATA_NON_DAPI_USER_CHIP_UUID_STRING, new BigInteger(chip.uid()).toString());
                    DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
                    listener.onLoginAttemptEnd(Result.SUCCESS, chip);
            }
        }
        else {
            stop();
        }
    }

    /**
     * Handles Wallet login scans when they occur.
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param walletPass WalletPass object
     */
    private void handleWalletPass(DataMatrix dataMatrix, WalletPass walletPass) {

        if (isAvailable() && rfidLoginEnabled) {
            rfidLoginEnabled = false;

            listener.onWalletLoginAttemptStart();

            // Return if the pass data contains the xid/passcode json
            if (handleWalletPassXid(dataMatrix, walletPass.pass)) return;
            if (handleWalletPassRfid(dataMatrix, walletPass.pass)) return;
            if (handleWalletPassBarcode(dataMatrix, walletPass.pass)) return;
            if (handleWalletPassCustom(dataMatrix, walletPass.pass)) return;
            if (handleWalletPassPhone(dataMatrix, walletPass.pass)) return;
            if (handleWalletPassEmail(dataMatrix, walletPass.pass)) return;

            // Perform DAPI wallet login
            DapiLogin.loginWithWallet(dataMatrix, walletPass, loginListener);

        } else {
            stop();
        }
    }


    DapiLogin.OnLoginListener loginListener = new DapiLogin.OnLoginListener() {
        @Override
        public void onLogin(int dapiStatus) {
            Result rfidLoginResponse;

            switch (DapiResult.fromInt(dapiStatus)) {
                case Success:
                    rfidLoginResponse = Result.SUCCESS;
                    break;
                case InvalidUsernameOrPassword:
                case LoginFailed:
                    rfidLoginResponse = Result.INVALID_CREDENTIALS;
                    break;
                default:
                    rfidLoginResponse = Result.CONNECTION_ERROR;
            }

            listener.onWalletLoginAttemptEnd(rfidLoginResponse);
        }
    };

    /**
     * This logic handle pass data that has the xid/passcode json for the wallet pass.
     *
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param pass pass data
     * @return true if the pass data is valid, false otherwise
     */
    private boolean handleWalletPassXid(DataMatrix dataMatrix, String pass) {
        try {
            JSONObject json = new JSONObject(pass);
            String xid = json.optString(KEY_XID, "");
            String passcode = json.optString(KEY_PASSCODE, "");
            if (xid.isEmpty() || passcode.isEmpty()) return false;
            DapiLogin.login(dataMatrix, xid, passcode, DapiUtil.LOGIN_TYPE_XID, loginListener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This logic handle pass data that has the xid json for the wallet pass.
     *
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param pass pass data
     * @return true if the pass data is valid, false otherwise
     */
    private boolean handleWalletPassRfid(DataMatrix dataMatrix, String pass) {
        try {
            JSONObject json = new JSONObject(pass);
            String tagUid = json.optString(KEY_RFID, "");
            String rfStandard = json.optString(KEY_TYPE, "");
            if (rfStandard.isEmpty() || tagUid.isEmpty()) return false;
            DapiLogin.login(dataMatrix, tagUid, rfStandard, DapiUtil.LOGIN_TYPE_RFID, loginListener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This logic handle pass data that has the barcode json for the wallet pass.
     *
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param pass pass data
     * @return true if the pass data is valid, false otherwise
     */
    private boolean handleWalletPassBarcode(DataMatrix dataMatrix, String pass) {
        try {
            JSONObject json = new JSONObject(pass);
            String barcode = json.optString(KEY_BARCODE, "");
            if (barcode.isEmpty()) return false;
            DapiLogin.login(dataMatrix, barcode, "", DapiUtil.LOGIN_TYPE_BARCODE, loginListener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This logic handle pass data that has the custom json for the wallet pass.
     * Email type should be part of custom.
     *
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param pass pass data
     * @return true if the pass data is valid, false otherwise
     */
    private boolean handleWalletPassCustom(DataMatrix dataMatrix, String pass) {
        try {
            JSONObject json = new JSONObject(pass);
            String custom = json.optString(KEY_CUSTOM, "");
            String password = json.optString(KEY_PASSWORD, "");
            if (password.isEmpty() || custom.isEmpty()) return false;
            DapiLogin.login(dataMatrix, custom, password, DapiUtil.LOGIN_TYPE_CUSTOM, loginListener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This logic handle pass data that has the phone json for the wallet pass.
     *
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param pass pass data
     * @return true if the pass data is valid, false otherwise
     */
    private boolean handleWalletPassPhone(DataMatrix dataMatrix, String pass) {
        try {
            JSONObject json = new JSONObject(pass);
            String phone = json.optString(KEY_PHONE, "");
            String password = json.optString(KEY_PASSWORD, "");
            if (password.isEmpty() || phone.isEmpty()) return false;
            DapiLogin.login(dataMatrix, phone, password, DapiUtil.LOGIN_TYPE_PHONE, loginListener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This logic handle pass data that has the email json for the wallet pass.
     *
     * @param dataMatrix DataMatrix object used on active activity. Needed for Dapi login.
     * @param pass pass data
     * @return true if the pass data is valid, false otherwise
     */
    private boolean handleWalletPassEmail(DataMatrix dataMatrix, String pass) {
        try {
            JSONObject json = new JSONObject(pass);
            String email = json.optString(KEY_EMAIL, "");
            String password = json.optString(KEY_EMAIL_PASSWORD, "");
            if (password.isEmpty() || email.isEmpty()) return false;
            DapiLogin.login(dataMatrix, email, password, DapiUtil.LOGIN_TYPE_EMAIL, loginListener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if RFID login should be available to use (i.e. user not logged in and AM / WT enabled).
     * @return boolean indicating if RFID login is available
     */
    public static boolean isAvailable(){
        boolean isAvailable;
        boolean usgAlternateEndpointEnabled = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL);
        boolean isDapiUserGuest = DapiUser.isGuestUser();
        boolean isLoginEnabled = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_RFID_LOGIN_BOOL) ||
                DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL);

        if (usgAlternateEndpointEnabled) {
            isAvailable = isLoginEnabled;
        } else {
            isAvailable = isDapiUserGuest && isLoginEnabled;
        }

        return isAvailable;
    }

    /**
     * Get the endpoint that will be used for RFID authentication
     * @return LoginEndpoint - the endpoint that will be communicated with
     */
    public static LoginEndpoint getEndpoint() {
        LoginEndpoint endpoint;

        if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL)) {
            endpoint = LoginEndpoint.ALTERNATE;
        } else {
            endpoint = LoginEndpoint.DAPI;
        }

        return endpoint;
    }

    /**
     * RFID login callback interface.
     */
    public interface ILoginListener {
        void onLoginAttemptStart();
        void onLoginAttemptEnd(Result result, RFIDChip chip);
        void onWalletLoginAttemptStart();
        void onWalletLoginAttemptEnd(Result result);
    }

    public enum Result {
        SUCCESS,
        NO_ASSOCIATED_XID,
        INVALID_CREDENTIALS,
        CONNECTION_ERROR
    }

    public enum LoginEndpoint {
        DAPI,
        ALTERNATE
    }
}