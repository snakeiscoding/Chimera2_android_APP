package com.jht.chimera.lib.common.dapi;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.login.WalletPass;
import com.jht.data.QrAuthorizationResponse;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.DapiResult;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Semaphore;


public class DapiLogin {
    @NonNls private static final String TAG = "DapiLogin";
    public static final Semaphore mutex = new Semaphore(1, true); // need sync across threads

    public enum Keys {
        ACTION_LOGIN_PARCEL,
        ACTION_LOGIN_USER_TOKEN,
        EVENT_LOGIN_RESPONSE_INT,
        EVENT_GET_INFO_RESPONSE_INT,
        ACTION_LOGOUT,
        ACTION_CHECK_XID_EXISTS_STRING,
        EVENT_CHECK_XID_EXISTS_RESPONSE_INT,
        ACTION_ASSOCIATE_RFID_PARCELABLE,
        ACTION_DISASSOCIATE_RFID_PARCELABLE,
        ACTION_LOGIN_WALLET_PASS_PARCELABLE,
        ACTION_LOGIN_QR_INT,
        EVENT_QR_CODE_PARCELABLE,
        EVENT_QR_LOGIN_RESPONSE_INT,
    }

    /** Control codes posted to ACTION_LOGIN_QR_INT. */
    public static final class QrAction {
        public static final int START = 0;
        public static final int STOP = 1;

        private QrAction() {}

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({START, STOP})
        public @interface Value {}
    }

    /** Outcome codes posted to EVENT_QR_LOGIN_RESPONSE_INT. */
    public static final class QrResult {
        public static final int SUCCESS = 0;
        public static final int ERROR = 1;
        public static final int EXPIRED = 2;

        private QrResult() {}

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({SUCCESS, ERROR, EXPIRED})
        public @interface Value {}
    }

    public interface OnQrLoginListener {
        /** Device code ready; the UI can render the QR image. */
        void onCodeReady(QrAuthorizationResponse response);
        /** Final outcome of the QR login. */
        void onResult(@QrResult.Value int qrResult);
    }

    /**
     * Drives the JUMP QR flow via DataMatrix.
     */
    public static void startQrSession(DataMatrix dataMatrix, final OnQrLoginListener listener) {
        dataMatrix.createParcelableAdapter(Keys.EVENT_QR_CODE_PARCELABLE, QrAuthorizationResponse.class, response -> {
            if (listener != null && response != null) listener.onCodeReady(response);
        });
        dataMatrix.createIntAdapter(Keys.EVENT_QR_LOGIN_RESPONSE_INT, qrResult -> {
            if (listener != null) listener.onResult(qrResult);
        });
        DataMatrix.putInt(Keys.ACTION_LOGIN_QR_INT, QrAction.START);
    }

    public static void stopQrSession() {
        DataMatrix.putInt(Keys.ACTION_LOGIN_QR_INT, QrAction.STOP);
    }

    public static void logout() {
        DataMatrix.sendEvent(Keys.ACTION_LOGOUT);
    }

    public interface OnCheckXidExistsListener {
        void onCheckXidExists(@Nullable Boolean xidExists); // if we could not determine, we send null
    }

    public static void checkXidExists(DataMatrix dataMatrix, String xid, final OnCheckXidExistsListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_CHECK_XID_EXISTS_RESPONSE_INT, dapiResult -> {
            if (listener != null) {
                Boolean xidExist = null; // cannot confirm or deny
                if (dapiResult == DapiResult.XidAlreadyTaken.Value) xidExist = true;
                if (dapiResult == DapiResult.Success.Value) xidExist = false;
                listener.onCheckXidExists(xidExist);
            }
        });
        DataMatrix.putString(Keys.ACTION_CHECK_XID_EXISTS_STRING, xid);
    }

    public interface OnLoginListener {
        void onLogin(int dapiStatus);
    }

    public static void login(DataMatrix dataMatrix, String xid, String passcode, String loginType, final OnLoginListener listener) {
        login(dataMatrix, xid, passcode, -1, loginType, listener);
    }

    public static void login(DataMatrix dataMatrix, String xid, String passcode, int slot, String loginType, final OnLoginListener listener) {
        login(dataMatrix, xid, passcode, slot, true, loginType, listener);
    }

    public static void login(DataMatrix dataMatrix, String xid, String passcode, int slot, boolean switchToThisUser, String loginType, final OnLoginListener listener) {
        Log.d(TAG, "ptp_timing: Login user");
        dataMatrix.createIntAdapter(Keys.EVENT_LOGIN_RESPONSE_INT, dapiStatus -> {
            mutex.release();
            if (listener != null) {
                listener.onLogin(dapiStatus);
            }
        });

        new Thread(() -> {
            try {
                mutex.acquire();
                DapiLoginParcelable dapiLoginParcelable = new DapiLoginParcelable();
                dapiLoginParcelable.Xid = xid;
                dapiLoginParcelable.passcode = passcode;
                dapiLoginParcelable.loginType = loginType;
                dapiLoginParcelable.slotNumber = slot;
                dapiLoginParcelable.switchToUser = switchToThisUser;
                DataMatrix.putParcelable(Keys.ACTION_LOGIN_PARCEL, dapiLoginParcelable);
            } catch (Exception e) {
                Log.e(TAG, "user login function failed", e);
                if (listener != null) {
                    listener.onLogin(DapiResult.InternalException.Value);
                }
            }
        }).start();
    }

    public static void loginWithXidUserToken(DataMatrix dataMatrix, String userToken, final OnLoginListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_LOGIN_RESPONSE_INT, dapiStatus -> {
            mutex.release();
            if (listener != null) {
                listener.onLogin(dapiStatus);
            }
        });

        new Thread(() -> {
            try {
                mutex.acquire();
                DataMatrix.putString(Keys.ACTION_LOGIN_USER_TOKEN, userToken);
            } catch (Exception e) {
                Log.e(TAG, "user login function failed", e);
                if (listener != null) {
                    listener.onLogin(DapiResult.InternalException.Value);
                }
            }
        }).start();
    }

    public static void loginWithInfo(DataMatrix dataMatrix, String xid, String passcode, int slot, boolean switchToThisUser, String loginType, final OnLoginListener listener) {
        Log.d(TAG, "Login user");
        dataMatrix.createIntAdapter(Keys.EVENT_LOGIN_RESPONSE_INT, dapiStatus -> {
            if (dapiStatus != DapiResult.Success.Value) { // if login succeeds, DapiUserManagement will move to get info
                mutex.release();
                if (listener != null) {
                    listener.onLogin(dapiStatus);
                }
            }
        });
        dataMatrix.createIntAdapter(Keys.EVENT_GET_INFO_RESPONSE_INT, dapiStatus -> { // transaction is not actually done until user data is received
            mutex.release();
            if (listener != null) {
                listener.onLogin(dapiStatus);
            }
        });

        new Thread(() -> {
            try {
                mutex.acquire();
                DapiLoginParcelable dapiLoginParcelable = new DapiLoginParcelable();
                dapiLoginParcelable.Xid = xid;
                dapiLoginParcelable.passcode = passcode;
                dapiLoginParcelable.loginType = loginType;
                dapiLoginParcelable.slotNumber = slot;
                dapiLoginParcelable.switchToUser = switchToThisUser;
                DataMatrix.putParcelable(Keys.ACTION_LOGIN_PARCEL, dapiLoginParcelable);
            } catch (Exception e) {
                Log.e(TAG, "user login function failed", e);
                if (listener != null) {
                    listener.onLogin(DapiResult.InternalException.Value);
                }
            }
        }).start();
    }

    public static void loginWithWallet(DataMatrix dataMatrix, WalletPass walletPass, final OnLoginListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_LOGIN_RESPONSE_INT, dapiStatus -> {
            mutex.release();
            if (listener != null) {
                listener.onLogin(dapiStatus);
            }
        });

        new Thread(() -> {
            try {
                mutex.acquire();
                DataMatrix.putParcelable(Keys.ACTION_LOGIN_WALLET_PASS_PARCELABLE, walletPass);
            } catch (Exception e) {
                Log.e(TAG, "user login function failed", e);
                if (listener != null) {
                    listener.onLogin(DapiResult.InternalException.Value);
                }
            }
        }).start();
    }

    static public class DapiLoginParcelable implements Parcelable {
        public String Xid = "";
        public String passcode = "";
        public String loginType = "";
        public int slotNumber = -1; // if using profile slots (negative numbers are invalid)
        public boolean switchToUser = true; // should DapiUserManagement automatically switch to this user when logged in


        public DapiLoginParcelable() {
        }

        public DapiLoginParcelable(Parcel parcel) {
            readFromParcel(parcel);
        }

        public static final Creator<DapiLoginParcelable> CREATOR = new Creator<DapiLoginParcelable>() {
            @Override
            public DapiLoginParcelable[] newArray(int size) {
                return new DapiLoginParcelable[size];
            }

            @Override
            public DapiLoginParcelable createFromParcel(Parcel source) {
                return new DapiLoginParcelable(source);
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(Xid);
            dest.writeString(passcode);
            dest.writeString(loginType);
            dest.writeInt(slotNumber);
            dest.writeInt(switchToUser ? 1 : 0);
        }

        public void readFromParcel(Parcel source) {
            Xid = source.readString();
            passcode = source.readString();
            loginType = source.readString();
            slotNumber = source.readInt();
            switchToUser = source.readInt() != 0;
        }
    }
}
