package com.jht.chimera.service.dapi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiCreateUser;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.interfaces.models.MatrixProgressionModelKt;
import com.jht.oauthlib.auth.BksCredentialProvider;
import com.jhtp.dapiservice.um.DapiUserInfo;
import com.jhtp.dapiservice.um.GenderIndentity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/*
    Refer to com/jht/parker/service/dapi/UserManager.java from original ParkerApp for original implementation
 */

//import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.MAX_USER_LIMIT;

/**
 * Manages local user profiles and their storage.
 * Needs to coordinate with DapiUserManagement for user selection, but profiles contain more data than just DapiUser.
 */
public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    public static final int MAX_USER_LIMIT = 4;
    public static final ProfileUser guestProfile = new ProfileUser();

    // We don't want to save/load these (could be marked "transient" ?)
    //private DataMatrix mDataMatrix = new DataMatrix();
    private Context mContext;
    //private final Object profilesLock = new Object();
    // We do want to save/load these (we'll just collect that data into a single class)
    private ProfileDataStore mData = new ProfileDataStore();

    private static class SingletonHolder {
        private static final UserProfileManager INSTANCE = new UserProfileManager();
    }
    public static UserProfileManager getInstance() {
        return UserProfileManager.SingletonHolder.INSTANCE;
    }

    public enum Keys {
        EVENT_PROFILE_UPDATED,
        EVENT_REFRESH_CURRENT_SLOT,
    }

    private UserProfileManager() {
        init();
    }
    private void init() {
        Log.d(TAG, "running init");
        mContext = ContextUtil.getApplicationContext();
        BksCredentialProvider credentialProvider = new BksCredentialProvider(mContext);
        String password = credentialProvider.getString("USER_PROFILE_MANAGEMENT_PASSWORD");
        mSecretKey = new SecretKeySpec(password.getBytes(), "AES");
        int curDapiEnv = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT);

        reloadData();
        if (mData.dapiEnvironment < 0 && curDapiEnv >= 0) mData.dapiEnvironment = curDapiEnv; // if file has no env, set to current one

        // This reset should be moved to DapiUserManagement with change event so it is only triggered on that process ?
        // All XID users need to be unlinked ?
        //mDataMatrix.createEventAdapter(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS, keyReturnedBack -> resetUsers());
        //mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, pos -> { if (mData.dapiEnvironment!=pos) resetUsers(); });
        //Log.d(TAG, "file=" + getUsersFile().getAbsolutePath());

        //mDataMatrix.createEventAdapter(Keys.EVENT_PROFILE_UPDATED, keyReturnedBack -> reloadData());
    }

    // leave manual to allow more control of when emitted? or would automatic be cleaner?
    // any value in passing the id of the profile that changed?
    public static void sendChangeEvent() { DataMatrix.sendEvent(Keys.EVENT_PROFILE_UPDATED); }

    public void reloadData() {
        // Attempt to load data from backing store
        loadData();

        setupSlotFolders();

        // Should we generate default local users? New user adds as needed, only do this for TESTING
        /*if (BuildConfig.DEBUG) {
            String names[] = {
                    "Patty",
                    "Andrew",
                    "Bob",
                    //"Boris"
            };
            if (mData.users.size() == 0) {
                int slot = 1;
                for (String name : names) {
                    ProfileUser user = new ProfileUser();
                    user.setLocalUser(slot++);
                    user.firstName = name;
                    user.lastName = "Johnson";
                    mData.users.add(user);
                }
            }
        }*/
    }

    /**
     * Clears the currently stored users
     */
    /*private void resetUsers() {
        // just do top level user tracking or wipe everything?
        mData.users.clear();
        saveData();
        purgeUsers();
        selectGuestUser();
    }*/
    private void selectGuestUser() {
        //DAPIUserManagement.getInstance().setUserToGuest(); // careful, you might create multiple instances across processes
        //DataMatrix.putInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT, DAPIKeys.Constants.DAPI_GUEST_INDEX);
        DataMatrix.sendEvent(DapiLogin.Keys.ACTION_LOGOUT); // should cause Dapi service to switch back to guest
    }

    // NOTE: cause profile sync does not have xid, adding a profile is the ONLY time we have a unique xid !!
    // After profile is added, xid will likely be blank or a local variant.
    public ProfileUser getUserByXid(String xid) {
        for (ProfileUser user : mData.users) {
            if (user.xid != null && user.xid.equals(xid)) return user;
        }
        return null;
    }
    public ProfileUser getUserBySlot(int slot) {
        return mData.findBySlot(slot);
    }
    public ProfileDataStore getProfileData() { return mData; }
    public int getNextAvailableSlotNumber() { return mData.findNextSlotNumber(); }

    public ProfileUser getCurrentProfile() {
        int slot = DataMatrix.getInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT);
        ProfileUser user = mData.findBySlot(slot);
        if (user == null) user = guestProfile;
        return user;
    }
    public static boolean isCurrentSlot(int slot) {
        int currentSlot = DataMatrix.getInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT);
        return (currentSlot == slot);
    }

    private ProfileUser addIfNotExist(ProfileUser profileUser, int slot) {
        if (profileUser==null) {
            if (mData.users.size() < MAX_USER_LIMIT) {
                profileUser = new ProfileUser();
                profileUser.slotNumber = (slot > 0 ? slot : mData.findNextSlotNumber());
                if (profileUser.slotNumber < 0) {
                    Log.w(TAG, "Cannot find next slot number");
                    return null;
                }
                mData.users.add(profileUser);
            }
        }
        return profileUser;
    }

    public void updateUser(DapiUser user) {
        ProfileUser profileUser = getUserByXid(user.xid);
        if (profileUser == null || profileUser.isGuest()) return;
        if (user.equals(profileUser)) return; // has same data, don't bother the file system
        //int savedSlot = profileUser.slotNumber; // make sure we preserve slot number !
        profileUser.copyFrom(user); // dapi user copy doesn't include slot?
        //profileUser.slotNumber = savedSlot;
        saveData();
    }

    public void setPassBounce(String xid, boolean bounce) {
        ProfileUser profileUser = getUserByXid(xid);
        if (profileUser == null || profileUser.isGuest()) return;
        int value = profileUser.passBounce;
        if (bounce) {
            if (value < ProfileUser.PassBounceThreshold) ++value;
        } else {
            value = 0; // if no bounce, reset count
        }
        if (profileUser.passBounce == value) return;
        profileUser.passBounce = value;
        saveData();
    }

    // update user from registration data
    public ProfileUser updateUser(DapiCreateUser.DapiRegisterParcelable registerParcelable) {
        ProfileUser profileUser = getUserBySlot(registerParcelable.slotNumber);

        // if new user, attempt to add
        profileUser = addIfNotExist(profileUser, registerParcelable.slotNumber);
        if (profileUser != null) {
            registerParcelable.copyTo(profileUser);
            // make sure local user has the correct fields set for local
            if (DapiUser.isLocalUser(registerParcelable.xid)) {
                profileUser.setLocalUser(profileUser.slotNumber);
            }
            saveData();
        }
        return profileUser;
    }

    // update user from DAPI data (DapiUser doesn't presently have passcode set, so pass it separately?)
    /*public ProfileUser updateUser(int slot, DapiUser user, String pass) {
        // TODO - searching by DAPI user uuid would be FAR safer!
        ProfileUser profileUser = slot > 0 ? getUserBySlot(slot) : getUserByXid(user.xid); // if no valid slot given, try xid
        //Log.d(TAG, "updateUser from DapiUser existing " + (profileUser == null ? "null" : profileUser.toString()) );
        profileUser = addIfNotExist(profileUser, slot);
        if (profileUser == null) {
            return null;
        }
        int savedSlot = profileUser.slotNumber; // make sure we preserve slot number !
        profileUser.copyFrom(user);
        profileUser.slotNumber = savedSlot;
        if (pass != null && !pass.isEmpty()) profileUser.passcode = pass;
        saveData();
        return profileUser;
    }*/

    public ProfileUser updateUser(int slot, DapiUserInfo userInfo, String xid, String pass) {
        Log.d(TAG, "updateUser from DapiUserInfo slot=" + slot);
        ProfileUser profileUser = getUserBySlot(slot);
        profileUser = addIfNotExist(profileUser, slot);
        if (profileUser == null) {
            Log.w(TAG, "updateUser: No user to update slot=" + slot);
            return null;
        }
        // Now that we are not adding xID users via web, protect local slots from being overwritten
        if (profileUser.isLocalUser()) {
            Log.w(TAG, "updateUser: slot contains local user slot=" + slot);
            return null;
        }
        profileUser.xid = xid;
        if (pass != null && !pass.isEmpty()) profileUser.passcode = pass; // does this matter? not returned from sync command

        //profileUser.userToken = userToken; // would be set during login/register, wouldn't be in DapiUserInfo
        profileUser.uuid = userInfo.DapiUuid;
        profileUser.genderIdentity = userInfo.Gender;
        profileUser.setWeight(DapiUser.getInfoWeight(userInfo));
        profileUser.birthYear = userInfo.Birthday.Year;
        profileUser.birthMonth = userInfo.Birthday.Month;
        profileUser.birthDay = userInfo.Birthday.Day;
        profileUser.unitPreferenceMetric = userInfo.UnitsPreferenceMetric;
        profileUser.firstName = userInfo.FirstName;
        profileUser.lastName = userInfo.LastName;
        profileUser.acceptedTerms = true;
        saveData();
        return profileUser;
    }

    public ProfileUser updateSlotToken(int slot, String userToken) {
        Log.d(TAG, "updateSlotToken for slot=" + slot);
        ProfileUser profileUser = getUserBySlot(slot);
        profileUser = addIfNotExist(profileUser, slot);
        if (profileUser == null) {
            Log.w(TAG, "updateSlotToken: No user to update slot=" + slot);
            return null;
        }
        profileUser.userToken = userToken;
        profileUser.passBounce = 0; // if we got a user token, our passcode works
        saveData();
        return profileUser;
    }

    /*public static DapiUserInfo getUserInfo(DapiUser user) {
        DapiUserInfo userInfo = new DapiUserInfo();
        if (user != null) {
            userInfo.AcceptedTerms = user.acceptedTerms;
            userInfo.AgeYears = user.getAge();
            userInfo.Birthday = new DapiDate(user.birthYear, user.birthMonth, user.birthDay);
            userInfo.DapiUuid = user.uuid;
            userInfo.IsMale = user.isMale;
            userInfo.FirstName = user.firstName;
            userInfo.LastName = user.lastName;
            userInfo.LanguageId = 0;
            userInfo.ShareWorkoutData = true;
            WeightConvertUtil.setUnitPreferenceAndWeight(userInfo,
                    user.unitPreferenceMetric ? Unit.METRIC : Unit.IMPERIAL,
                    user.getWeight());
        }
        return userInfo;
    }*/

    public ProfileUser removeProfile(int userSlot) {
        Log.d(TAG, "removeProfile userSlot=" + userSlot);
        ProfileUser user = getUserBySlot(userSlot);
        if (user != null) {
            // remove user from list
            mData.users.remove(user);
            saveData();
            // purge profile data folder?
            purgeUserFolder(user.slotNumber);
            if (user.isLocalUser()) MatrixProgressionModelKt.eraseLocalUserData(user.uuid);
            setupSlotFolders();
            // if deleting current user, need to log user off? Leave that to the caller or better here?
            int slot = DataMatrix.getInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT);
            if (slot == user.slotNumber) selectGuestUser();
        }
        return user;
    }

    private File getUsersFile() {
        return new File(mContext.getFilesDir(), "users.json");
    }
    private File getUsersBackupFile() {
        return new File(mContext.getFilesDir(), "users.bak");
    }
    public File getUserFolder(int slot) {
        return new File(mContext.getFilesDir(), "user" + slot);
    }
    public File getUserFolder() {
        int slot = DataMatrix.getInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT);
        return getUserFolder(slot);
    }

    private void purgeUsers() {
        try {
            File usersFile = getUsersFile();
            if (usersFile.exists()) {
                if (!usersFile.delete()) throw new Exception("delete() returned false");
            }
            File usersFileBak = getUsersBackupFile();
            if (usersFileBak.exists()) {
                if (!usersFileBak.delete()) throw new Exception("delete() returned false");
            }
        }
        catch (Exception ex) { Log.w(TAG, "Failed to delete users file! ", ex); }
        for (int i=0; i <= MAX_USER_LIMIT; ++i) {
            purgeUserFolder(i);
        }
    }

    private void purgeUserFolder(int slot) {
        try {
            File slotFolder = getUserFolder(slot);
            delete(slotFolder);
            //if (!slotFolder.exists()) slotFolder.mkdir();
        }
        catch (Exception ex) { Log.w(TAG, "Failed to delete user slot " + slot + " folder! ", ex); }
    }


    private boolean delete(File file) {
        if (file == null || !file.exists()) return false;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) delete(child);
            }
        }
        file.delete();
        return !file.exists();
    }

    private void setupSlotFolders() {
        // check/create slot folders
        for (int i=0; i <= MAX_USER_LIMIT; ++i) {
            File slotFolder = getUserFolder(i);
            if (!slotFolder.exists()) {
                if (!slotFolder.mkdir()) Log.w(TAG, "Failed to mkdir " + slotFolder.getAbsolutePath());
            }
            else if (!slotFolder.isDirectory()) Log.w(TAG, "Slot folder path is NOT a directory " + slotFolder.getAbsolutePath());
        }
    }

    /**
     * Loads users from the file to the list
     */
    private void loadData() {
        if (!loadData(getUsersFile())) {
            loadData(getUsersBackupFile());
        }

        //updateUserLists();
        Log.d(TAG, "Loading users done, there are " + mData.users.size() + " users");
    }
    private boolean loadData(File users) {
        Log.d(TAG, "Loading users from " + users.getAbsolutePath());

        if (!users.exists()) return false;
        try {
            // Read file and return data
            byte[] data = FileUtils.readFile(users);
            if (data == null || data.length == 0) {
                Log.w(TAG, "Users file missing or empty");
                return false;
            }
            String jsonString = decryptMsg(data, mSecretKey);
            Gson gson = createGson();
            ProfileDataStore newData = gson.fromJson(jsonString, ProfileDataStore.class);
            if (newData != null) {
                newData = ProfileDataStore.checkFixData(newData);
                mData.copyFrom(newData);
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load users json ", e);
            return false;
        }
    }

    /**
     * Saves the users that are currently in the user list to the file.
     */
    private void saveData() {
        File outFile = getUsersFile();
        File outFileBak = getUsersBackupFile();
        if (!outFileBak.exists()) backupData(); // preserve the old in case file write is interrupted
        Log.d(TAG, "Saving users to " + outFile.getAbsolutePath());
        //mData.sort(); // do we want to keep the list in slot order?
        try ( BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(outFile)) ) {
            Gson gson = createGson();
            bw.write(encryptMsg(gson.toJson(mData), mSecretKey));
        } catch (Exception e) {
            Log.w(TAG, "Failed to save users json ", e);
        }
        ExecuteAsRoot.runSyncCommand(); // ensure filesystem gets sync right away in case of power loss (do we need to keep a backup file as well?)
        backupData(); // assuming write finished, just update the backup so it is current? or is that too risky?
    }

    private void backupData() {
        File outFile = getUsersFile();
        File outFileBak = getUsersBackupFile();
        if (outFile.exists()) {
            try {
                FileUtils.copyFile(outFile, outFileBak);
                ExecuteAsRoot.runSyncCommand();
            } catch (Exception e) { Log.w(TAG, "Unable to make users backup ", e); }
        }
    }

    private Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(GenderIndentity.class, new GenderIdentitySerializer() );
        gsonBuilder.registerTypeAdapterFactory(new ProfileUserTypeAdapterFactory());
        return gsonBuilder.create();
    }



    SecretKey mSecretKey;

    byte[] encryptMsg(String message, SecretKey secret)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        /* Encrypt the message. */
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); //NON-NLS
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        byte[] cipherText = cipher.doFinal(message.getBytes("UTF-8"));
        return cipherText;
    }

    String decryptMsg(byte[] cipherText, SecretKey secret)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        /* Decrypt the message, given derived encContentValues and initialization vector. */
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); //NON-NLS
        cipher.init(Cipher.DECRYPT_MODE, secret);
        String decryptString = new String(cipher.doFinal(cipherText), "UTF-8");
        return decryptString;
    }


    // Should we create a ProfileUser as an extension of DapiUser ? or as a purely separate duplicate of DapiUser?
    // Default constructor should create a fully functional guest user
    @Keep
    public static class ProfileUser extends DapiUser implements Comparable<ProfileUser> {
        // Add any additional profile info beyond that required by DapiUser (for legacy fields see ProfileUserTypeAdapterFactory)
        public int slotNumber = DAPIKeys.Constants.DAPI_GUEST_INDEX; // For viafit we need an established slot number
        public int passBounce = 0; // indicate if passcode has bounced when it was used last
        // constants
        public static final int PassBounceThreshold = 3;

        public ProfileUser() {
            super();
        }
        public ProfileUser(ProfileUser other) {
            super(other);
            slotNumber = other.slotNumber;
            passBounce = other.passBounce;
        }

        public boolean isPassBounceThresholdHit() {
            return (passBounce >= PassBounceThreshold);
        }

        @Override
        public void setLocalUser(int id) { super.setLocalUser(id); slotNumber = id; }

        /*public void copyFrom(ProfileUser o) {
            super.copyFrom(o);
            //slotNumber = o.slotNumber; // do we want to block copying slot numbers? or does that make this function counter intuitive?
        }*/
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!super.equals(o)) return false;
            if (!(o instanceof ProfileUser)) return false;
            ProfileUser other = (ProfileUser)o;
            if (slotNumber != other.slotNumber) return false;
            if (passBounce != other.passBounce) return false;
            return true;
        }
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), slotNumber);
        }

        @Override
        public void sync() {
            DataMatrix.putInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT, slotNumber);
            super.sync(); // user's UUID should be last thing updated since it triggers SSO events
        }

        @Override
        public int compareTo(ProfileUser o) {
            return Integer.compare(slotNumber, o.slotNumber); // if we need to sort them by slot number ? should we fill in the full member comparisons?
        }

        @Override
        public String toString() {
            return "ProfileUser{xid=" + xid + ",slot=" + slotNumber + ",name='" + super.getFullName() + "'}";
        }
    }

    // This is the data we want to save/load from local storage
    @Keep
    public static class ProfileDataStore {
        // would this be more efficient as fixed size array with unused slots being null ?
        public List<UserProfileManager.ProfileUser> users = new LinkedList<>();
        public int dapiEnvironment = -1;

        public void copyFrom(ProfileDataStore other) {
            this.users = new LinkedList<>(other.users);
            this.dapiEnvironment = other.dapiEnvironment;
        }
        public static ProfileDataStore checkFixData(ProfileDataStore data) {
            if (data == null) data = new ProfileDataStore();
            if (data.users == null) data.users = new LinkedList<>(); // in case json is missing users list
            return data;
        }

        public UserProfileManager.ProfileUser findBySlot(int slot) {
            for (UserProfileManager.ProfileUser user : users) {
                if (user.slotNumber == slot) return user;
            }
            return null;
        }
        public int findNextSlotNumber() {
            // slot zero is reserved for guest in DAPI
            for (int i = 1; i <= MAX_USER_LIMIT; ++i) {
                UserProfileManager.ProfileUser user = findBySlot(i);
                if (user == null) return i;
            }
            return -1;  // return negative for error so we don't accidentally issue guest slot?
        }

        // Do we want to sort by slot number or just switch to fixed position array?
        /*public void sort() {
            Collections.sort(users);
        }*/
    }

    // Store GenderIdentity enum as integer in JSON (by default gson appears to use a string name)
    static class GenderIdentitySerializer implements JsonSerializer<GenderIndentity>, JsonDeserializer<GenderIndentity> {
        @Override
        public JsonElement serialize(GenderIndentity src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.Value);
        }

        @Override
        public GenderIndentity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try { return GenderIndentity.fromInt(json.getAsNumber().intValue()); }
            catch (Exception ignored) { return GenderIndentity.NotSpecified; }
        }
    }

    // Need special handling of legacy JSON fields
    // (Alternatives available in gson derived libraries such as https://github.com/julman99/gson-fire)
    @SuppressWarnings("unchecked")
    static class ProfileUserTypeAdapterFactory implements TypeAdapterFactory {
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!ProfileUser.class.isAssignableFrom(type.getRawType())) return null; // this class only serializes 'ProfileUser' and its subtypes
            final TypeAdapter<ProfileUser> userAdapter = gson.getDelegateAdapter(this, TypeToken.get(ProfileUser.class));
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
            TypeAdapter<ProfileUser> result = new TypeAdapter<ProfileUser>() {
                public void write(JsonWriter out, ProfileUser value) throws IOException {
                    userAdapter.write(out, value);
                }
                public ProfileUser read(JsonReader in) throws IOException {
                    JsonObject object = elementAdapter.read(in).getAsJsonObject();
                    ProfileUser profile = userAdapter.fromJsonTree(object);
                    // apply legacy fields to current fields, if found and parsable
                    try { profile.genderIdentity = object.get("isMale").getAsBoolean() ? GenderIndentity.Male : GenderIndentity.Female; }
                    catch (Exception ignored) {}
                    return profile;
                }
            }.nullSafe();
            return (TypeAdapter<T>) result;
        }
    }

    // We need a way to track the SSO version of the zips being stored locally
    @Keep
    public static class LocalSsoData {
        public String ssoVersion; // SSO version for the zip file
        public String md5;  // MD5 of the zip file (so you know it belongs to this meta data)

        public boolean isValid(File zipFile, String requiredSsoVersion) {
            if (!zipFile.exists()) return false;
            if (this.ssoVersion == null || !this.ssoVersion.equals(requiredSsoVersion)) return false;
            String zipMd5 = FileUtils.computeMd5(zipFile);
            return zipMd5 != null && zipMd5.equalsIgnoreCase(this.md5);
        }

        @NonNull
        @Override
        public String toString() { return "md5=" + md5 + ", ssoVersion=" + ssoVersion; }

        private static Gson createGson() {
            GsonBuilder gsonBuilder = new GsonBuilder();
            return gsonBuilder.create();
        }

        public static boolean write(LocalSsoData data, File file) {
            try {
                Gson gson = createGson();
                String content = gson.toJson(data);
                return FileUtils.writeToFile(content, file);
            } catch (Exception e) {
                Log.e(TAG, "LocalSsoData.write", e);
                return false;
            }
        }

        public static LocalSsoData read(File file) {
            if (!file.exists()) return null;
            try {
                Gson gson = createGson();
                String content = FileUtils.readFileToString(file);
                return gson.fromJson(content, LocalSsoData.class);
            } catch (Exception e) {
                Log.e(TAG, "LocalSsoData.read", e);
                return null;
            }
        }
    }

}


