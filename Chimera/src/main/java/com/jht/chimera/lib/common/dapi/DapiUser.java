package com.jht.chimera.lib.common.dapi;

import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.i18n.Unit;
import com.jht.chimera.lib.common.unit.WeightUnit;
import com.jht.chimera.lib.common.value.Weight;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.um.DapiUserInfo;
import com.jhtp.dapiservice.um.GenderIndentity;

import org.jetbrains.annotations.NonNls;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class DapiUser {
    private static final String TAG ="dapiUserConnectionsLog";
    public final static String GUEST_USER_TOKEN = "00000000-0000-0000-0000-000000000000";
    public final static String GUEST_USER_UUID = "00000000-0000-0000-0000-000000000000";

    // NOTE: renaming these could impact json serialization of ProfileUser (could create decoupling data class for ProfileUser if renaming was needed)
    public String email;
    public String xid;
    public String passcode;
    public GenderIndentity genderIdentity;
    private float weightKg;
    public int birthYear;
    public int birthMonth;
    public int birthDay;
    public int age;
    public boolean unitPreferenceMetric;
    public String firstName;
    public String lastName;
    public boolean acceptedTerms;
    public String userToken;
    public String uuid;
    public DapiUserExtendedInfo dapiUserExtendedInfo;
    public boolean extendedInfoReceived;

    public DapiUser() {
        email = "";
        xid = "";
        passcode = "";
        genderIdentity = GenderIndentity.fromInt (DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT));
        weightKg = DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT);
        birthYear = Calendar.getInstance().get(Calendar.YEAR) - DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT);
        birthMonth = Calendar.getInstance().get(Calendar.MONTH);
        birthDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        unitPreferenceMetric = LocaleUtil.getDefaultUnitIsMetric();
        firstName = ContextUtil.getApplicationContext().getString(R.string.guest_tc);
        lastName = "";
        acceptedTerms = true;
        userToken = GUEST_USER_TOKEN;
        uuid = GUEST_USER_UUID;
        dapiUserExtendedInfo = new DapiUserExtendedInfo();
        extendedInfoReceived = false;
        age = DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT);
    }

    public DapiUser(DapiUser other) { copyFrom(other); }
    public void copyFrom(DapiUser other) {
        email = other.email;
        xid = other.xid;
        passcode = other.passcode;
        genderIdentity = other.genderIdentity;
        weightKg = other.weightKg;
        birthYear = other.birthYear;
        birthMonth = other.birthMonth;
        birthDay = other.birthDay;
        unitPreferenceMetric = other.unitPreferenceMetric;
        firstName = other.firstName;
        lastName = other.lastName;
        acceptedTerms = other.acceptedTerms;
        userToken = other.userToken;
        uuid = other.uuid;
        dapiUserExtendedInfo = new DapiUserExtendedInfo(other.dapiUserExtendedInfo);
        extendedInfoReceived = other.extendedInfoReceived;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DapiUser)) return false;
        DapiUser other = (DapiUser)o;
        if (!email.equals(other.email)) return false;
        if (!xid.equals(other.xid)) return false;
        if (!passcode.equals(other.passcode)) return false;
        if (genderIdentity != other.genderIdentity) return false;
        if (weightKg != other.weightKg) return false;
        if (birthYear != other.birthYear) return false;
        if (birthMonth != other.birthMonth) return false;
        if (birthDay != other.birthDay) return false;
        if (unitPreferenceMetric != other.unitPreferenceMetric) return false;
        if (!firstName.equals(other.firstName)) return false;
        if (!lastName.equals(other.lastName)) return false;
        if (acceptedTerms != other.acceptedTerms) return false;
        if (!userToken.equals(other.userToken)) return false;
        if (!uuid.equals(other.uuid)) return false;
        if (!dapiUserExtendedInfo.equals(other.dapiUserExtendedInfo)) return false;
        if (extendedInfoReceived != other.extendedInfoReceived) return false;
        return true;
    }
    @Override
    public int hashCode() {
        // can we get away with just the unique ids?
        return Objects.hash(xid, userToken, uuid);
    }

    /*
        Local user xid should contain known prefix. Also should use GUEST_USER_TOKEN.
        The local user is conceptually between a DAPI user and a guest. They have data, but it doesn't exist outside the console.
     */
    @NonNls private final static String XID_LOCAL_KEY = "local-"; // need to be url safe, normal xid is just numbers
    public static boolean isLocalUser(String user_xid) { return (user_xid != null && user_xid.startsWith(XID_LOCAL_KEY)); }
    public boolean isLocalUser() { return isLocalUser(xid); }
    public static String createLocalXid(int id) { return XID_LOCAL_KEY + id; }
    public void setLocalUser(int id) {
        xid = createLocalXid(id);
        userToken = GUEST_USER_TOKEN; // should local users use guest token?
        uuid = xid;  // can't use GUEST_USER_UUID as user will be detected as guest, but for SSO should be unique
    }

    public String getInitials() {
        if (isGuest()) return firstName; // don't initialize guest
        String initials = "";
        if (firstName != null && firstName.length() > 0) initials += firstName.substring(0,1).toUpperCase(Locale.getDefault());
        if (lastName != null && lastName.length() > 0) initials += lastName.substring(0,1).toUpperCase(Locale.getDefault());
        return initials;
    }

    public String getFullName() { return (firstName + " " + lastName).trim(); }

    public static int getAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        dob.set(year, month - 1, day);

        int year1 = today.get(Calendar.YEAR);
        int year2 = dob.get(Calendar.YEAR);
        int age = year1 - year2;
        int month1 = today.get(Calendar.MONTH);
        int month2 = dob.get(Calendar.MONTH);
        if (month2 > month1) {
            age--;
        } else if (month1 == month2) {
            int day1 = today.get(Calendar.DAY_OF_MONTH);
            int day2 = dob.get(Calendar.DAY_OF_MONTH);
            if (day2 > day1) {
                age--;
            }
        }

        return age;
    }

    public void sync() {

        DataMatrix.putString(DAPIKeys.DATA_USER_FIRST_NAME_STRING, firstName);
        DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, genderIdentity.Value);
        DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, age);

        // Display units are not pushed here - sync() runs on every user data save, and
        // unitPreferenceMetric only refreshes on identity change. See syncDisplayUnits().

        DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, weightKg);
        DataMatrix.updateString(DAPIKeys.DATA_USER_DAPI_UUID_STRING, uuid);
        DataMatrix.putString(DAPIKeys.DATA_USER_DAPI_USER_XID_STRING, xid);
        DataMatrix.putString(DAPIKeys.DATA_USER_DAPI_USER_TOKEN_SESSION_STRING, userToken);

        syncExtendedInfo();

        Log.e(TAG, "after update uuid");

    }

    /**
     * Pushes this user's unit preference to the display units key (separate from the one that can
     * change during a workout). Only call where the user identity changes - on a plain data save it
     * would revert a unit change made since, e.g. picking a language with different default units.
     */
    public void syncDisplayUnits() {
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, unitPreferenceMetric);
    }

    public void syncExtendedInfo() {
        String customWorkoutJson = dapiUserExtendedInfo.customWorkouts.get(FrameConfigManager.getInstance().getFrameConfig().machineClassType.name());
        if(customWorkoutJson == null) {
            DataMatrix.putString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, "");
        } else {
            DataMatrix.putString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, customWorkoutJson);
        }

        String progressionData = dapiUserExtendedInfo.progressionData;
        if (!progressionData.isEmpty()) {
            DataMatrix.updateString(DAPIKeys.DATA_PROGRESSION_PROGRAM_STRING, progressionData);
        }

        DataMatrix.putString(DAPIKeys.DATA_USER_EXTENDED_DATA_JSON_STRING, DapiUserExtendedInfo.toJsonString(dapiUserExtendedInfo));
        DataMatrix.putBoolean(DAPIKeys.DATA_EXTENDED_INFO_RECEIVED_BOOL, extendedInfoReceived);
    }

    public static boolean isGuestUser() {
        return DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING).equals(GUEST_USER_UUID);
    }
    public boolean isGuest() { return uuid.equals(GUEST_USER_UUID); }

    public void setWeight(Weight weight) {
        this.weightKg = (float)weight.getValue(WeightUnit.KILOGRAM);
    }
    public Weight getWeight() {
        return new Weight(this.weightKg, WeightUnit.KILOGRAM);
    }

    public static String getWebUuid() {
        String uuid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
        if (isLocalUser(uuid)) uuid = GUEST_USER_UUID;  // At this time assumes that local user uuid is same as xid, but this could change?
        return uuid;
    }
    public static boolean isOfflineUser() {
        String uuid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
        return isLocalUser(uuid);  // At this time assumes that local user uuid is same as xid, but this could change?
    }

    //// Get/set weight on DAPI objects where units are unclear

    static WeightUnit weightUnitUsedBy(DapiUserInfo userInfo) {
        // THERE IS SOME CONFUSION ABOUT THIS!
        // On the one hand, the DapiUserInfo weight field is called WeightPounds.
        // On the other hand, https://jhtnaee.atlassian.net/wiki/spaces/CCS/pages/763855110/5+Register+User
        // says that the value in the packet's meaning changes depending on the value of the
        // UnitsPreferenceMetric field.
        // This function exists so that if we change our minds we can do it in a single place.
        return userInfo.UnitsPreferenceMetric ? WeightUnit.KILOGRAM : WeightUnit.POUND;
    }

    // Use these functions instead of directly manipulating the weight/unit fields of DapiUserInfo:
    public static void setInfoUnitPreferenceAndWeight(DapiUserInfo user, Unit unitPreference, Weight weight) {
        switch(unitPreference) {
            case METRIC: user.UnitsPreferenceMetric = true; break;
            case IMPERIAL: user.UnitsPreferenceMetric = false; break;
            default: throw new RuntimeException("Unit preference "+unitPreference+" not representable in DapiUserInfo");
        }
        user.Weight = (float) weight.getValue(weightUnitUsedBy(user)); // user preferred weight commands still integers, but artifact will round if needed
    }
    public static Weight getInfoWeight(DapiUserInfo user) {
        return new Weight(user.Weight, weightUnitUsedBy(user));
    }

}
