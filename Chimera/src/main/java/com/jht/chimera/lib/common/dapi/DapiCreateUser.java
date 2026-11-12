package com.jht.chimera.lib.common.dapi;

import static com.jht.chimera.lib.common.unit.WeightUnit.KILOGRAM;
import static com.jht.chimera.lib.common.unit.WeightUnit.POUND;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.Unit;
import com.jht.chimera.lib.common.unit.WeightUnit;
import com.jht.chimera.lib.common.value.Weight;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.DapiResult;
import com.jhtp.dapiservice.um.GenderIndentity;

import org.jetbrains.annotations.NonNls;

public class DapiCreateUser {
    @NonNls private static final String TAG = "DapiCreateUser";

    public enum Keys {
        ACTION_CHECK_EMAIL_EXISTS_STRING,
        EVENT_CHECK_EMAIL_EXISTS_RESPONSE_INT,
        ACTION_REGISTER_USER_PARCEL,
        EVENT_REGISTER_RESPONSE_INT,
        // DapiRegisterParcelable.class
        ACTION_UPDATE_USER_PARCEL,
        EVENT_UPDATE_USER_RESPONSE_INT,
        // Local profiles
        ACTION_REMOVE_PROFILE_SLOT_INT,
        EVENT_REMOVE_PROFILE_RESPONSE_INT,
        ;
    }


    public interface OnCheckEmailExistsListener {
        void onCheckEmailExists(@Nullable Boolean emailExists); // if we could not determine, we send null
    }

    public interface OnCreateUserListener {
        void onCreateUser(int dapiStatus);
    }

    public interface OnUpdateUserListener {
        void OnUpdateUser(int dapiStatus);
    }

    public interface OnRemoveProfileListener {
        void OnRemoveProfile(int dapiStatus);
    }

    private DapiCreateUser() {
        // prevent instantiation of utility class
    }

    public static void checkEmailExists(DataMatrix dataMatrix, String email, final OnCheckEmailExistsListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_CHECK_EMAIL_EXISTS_RESPONSE_INT, dapiResult -> {
            if (listener != null) {
                Boolean emailExist = null; // cannot confirm or deny
                if (dapiResult == DapiResult.EmailAlreadyInUse.Value) emailExist = true;
                if (dapiResult == DapiResult.DuplicateEmail.Value) emailExist = true;
                if (dapiResult == DapiResult.Success.Value) emailExist = false;
                listener.onCheckEmailExists(emailExist);
            }
        });
        DataMatrix.putString(Keys.ACTION_CHECK_EMAIL_EXISTS_STRING, email);
    }

    public static void createUser(DataMatrix dataMatrix, DapiRegisterParcelable registerParcelable, final OnCreateUserListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_REGISTER_RESPONSE_INT, dapiStatus -> {
            DapiLogin.mutex.release();
            if (listener != null) {
                listener.onCreateUser(dapiStatus);
            }
        });
        new Thread(() -> {
            try {
                DapiLogin.mutex.acquire(); // both login and register user leverage the get info path in DapiUserManagement which has to be done one at time to keep context
                DataMatrix.putParcelable(Keys.ACTION_REGISTER_USER_PARCEL, registerParcelable);
            } catch (Exception e) {
                Log.e(TAG, "user create user function failed", e);
                if (listener != null) {
                    listener.onCreateUser(DapiResult.InternalException.Value);
                }
            }
        }).start();
    }

    public static void createUserWithInfo(DataMatrix dataMatrix, DapiRegisterParcelable registerParcelable, final OnCreateUserListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_REGISTER_RESPONSE_INT, dapiStatus -> {
            if (dapiStatus != DapiResult.Success.Value) { // if register succeeds, DapiUserManagement will move to get info
                DapiLogin.mutex.release();
                if (listener != null) {
                    listener.onCreateUser(dapiStatus);
                }
            }
        });
        dataMatrix.createIntAdapter(DapiLogin.Keys.EVENT_GET_INFO_RESPONSE_INT, dapiStatus -> { // transaction is not actually done until user data is received
            DapiLogin.mutex.release();
            if (listener != null) {
                listener.onCreateUser(dapiStatus);
            }
        });
        new Thread(() -> {
            try {
                DapiLogin.mutex.acquire(); // both login and register user leverage the get info path in DapiUserManagement which has to be done one at time to keep context
                DataMatrix.putParcelable(Keys.ACTION_REGISTER_USER_PARCEL, registerParcelable);
            } catch (Exception e) {
                Log.e(TAG, "user create user function failed", e);
                if (listener != null) {
                    listener.onCreateUser(DapiResult.InternalException.Value);
                }
            }
        }).start();
    }

    public static void removeProfile(DataMatrix dataMatrix, int slot, final OnRemoveProfileListener listener) {
        dataMatrix.createIntAdapter(Keys.EVENT_REMOVE_PROFILE_RESPONSE_INT, dapiStatus -> {
            if (listener != null) {
                listener.OnRemoveProfile(dapiStatus);
            }
        });
        DataMatrix.putInt(Keys.ACTION_REMOVE_PROFILE_SLOT_INT, slot);
    }

    // TODO: Find out if this is only used internally
    // If so, we could do away with all the weight conversion stuff
    // and just have a 'float weightKg'
    public static class DapiRegisterParcelable implements Parcelable {
        public String email;
        public String xid;
        public String passcode;
        public GenderIndentity genderIndentity;
        private int weight;
        /** What are the units of the weight field? */
        private WeightUnit getWeightUnit() {
            // return POUND; // WRONG!?  https://jhtnaee.atlassian.net/wiki/spaces/CCS/pages/763625841/3+Get+User+Info
            return unitPreferenceMetric ? KILOGRAM : POUND;
        }
        public Weight getWeight() {
            return new Weight(this.weight, this.unitPreferenceMetric ? KILOGRAM : POUND);
        }
        public int birthYear;
        public int birthMonth;
        public int birthDay;
        private boolean unitPreferenceMetric;
        // Since the internal value to represent weight depends on the unit,
        // force them to always be set together:
        public void setUnitPreferenceAndWeight(Unit unit, Weight weight) {
            switch(unit) {
                case METRIC: this.unitPreferenceMetric = true; break;
                case IMPERIAL: this.unitPreferenceMetric = false; break;
                default: this.unitPreferenceMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL); break;

                // I don't think we want to have a crash on the console because DAPI returned an unexpected value.
                // Rather let's use the system default.
                //default: throw new RuntimeException("Unit preference "+unit+" not representable in DapiRegisterParcelable");
            }
            this.weight = (int)Math.round(weight.getValue(getWeightUnit()));
        }
        public Unit getUnitPreference() {
            return unitPreferenceMetric ? Unit.METRIC : Unit.IMPERIAL;
        }

        public String firstName;
        public String lastName;
        public boolean acceptedTerms;
        public int slotNumber = 0;  // slot zero is guest (only used for retail product on viafit)
        public String userToken = null; // if you want to update a user that is not the current user

        public DapiRegisterParcelable() {
        }

        public DapiRegisterParcelable(Parcel parcel) {
            readFromParcel(parcel);
        }

        public void copyTo(DapiUser user) {
            user.email = email;
            user.xid = xid;
            user.passcode = passcode;
            user.genderIdentity = genderIndentity;
            user.setWeight(getWeight());
            user.birthYear = birthYear;
            user.birthMonth = birthMonth;
            user.birthDay = birthDay;
            user.unitPreferenceMetric = unitPreferenceMetric;
            user.firstName = firstName;
            user.lastName = lastName;
            user.acceptedTerms = acceptedTerms;
        }

        public static final Creator<DapiRegisterParcelable> CREATOR = new Creator<DapiRegisterParcelable>() {
            @Override
            public DapiRegisterParcelable[] newArray(int size) {
                return new DapiRegisterParcelable[size];
            }

            @Override
            public DapiRegisterParcelable createFromParcel(Parcel source) {
                return new DapiRegisterParcelable(source);
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(email);
            dest.writeString(xid);
            dest.writeString(passcode);
            dest.writeInt(genderIndentity.Value);
            dest.writeInt(weight);
            dest.writeInt(birthYear);
            dest.writeInt(birthMonth);
            dest.writeInt(birthDay);
            dest.writeInt(unitPreferenceMetric ? 1 : 0);
            dest.writeString(firstName);
            dest.writeString(lastName);
            dest.writeInt(acceptedTerms ? 1 : 0);
            dest.writeInt(slotNumber);
            dest.writeString(userToken);
        }

        public void readFromParcel(Parcel source) {
            email = source.readString();
            xid = source.readString();
            passcode = source.readString();
            genderIndentity = GenderIndentity.fromInt(source.readInt());
            weight = source.readInt();
            birthYear = source.readInt();
            birthMonth = source.readInt();
            birthDay = source.readInt();
            unitPreferenceMetric = source.readInt() != 0;
            firstName = source.readString();
            lastName = source.readString();
            acceptedTerms = source.readInt() != 0;
            slotNumber = source.readInt();
            userToken = source.readString();
        }
    }
}
