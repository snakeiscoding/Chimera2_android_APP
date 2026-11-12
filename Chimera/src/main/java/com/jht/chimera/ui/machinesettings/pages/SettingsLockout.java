package com.jht.chimera.ui.machinesettings.pages;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.datamatrixlib.DataMatrix;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles logic for entering new passcodes for the settings lockout feature as well as
 * logic for checking that the user enters the passcode correctly prior to entering MES, and locking
 * them out for a specified time if they don't.
 */
public class SettingsLockout {
    private String currentPasscode;
    private boolean settingsLocked = false;
    private static final String TAG = "SettingsLockout";

    private @NonNull PasscodeState passcodeState = PasscodeState.DISABLED;

    /** This is the number of incorrect attempts the user has made to enter a passcode **/
    private int attemptCount = 0;
    /** How many failed attempts to allow before lockout **/
    private static final int MAX_ATTEMPTS = 5;
    /** How many minutes to lock entry after exceeding attempts **/
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    /** Minimum length of passcode **/
    public static final int MIN_PASSCODE_LENGTH = 8;
    /** Maximum length of passcode **/
    public static final int MAX_PASSCODE_LENGTH = 40;

    /**
     * Singleton instance of the SettingsLockout, we only need one
     */
    private static final SettingsLockout instance = new SettingsLockout();

    /**
     * Gets the singleton instance of the SettingsLockout
     * @return the singleton instance
     */
    public static SettingsLockout getInstance() {
        return instance;
    }

    /**
     * Initializes the SettingsLockout. Checks if a passcode is set, and sets state to enabled. Also checks
     * if SettingsLockout instance should start locked, such as if a user locked themselves out and
     * then rebooted the console.
     */
    private SettingsLockout() {
        String passcode = DataMatrix.getString(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_PASSCODE_STRING, "");
        if (!passcode.isEmpty()) {
            currentPasscode = passcode;
            passcodeState = PasscodeState.ENABLED;
        }
        checkLockout();
    }

    /**
     * Lockout timer which lasts the specified duration and unlocks and resets attempts when finished
     */
    private final JHTTimer lockoutTimer = new JHTTimer((int) TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES), () -> {
        settingsLocked = false;
        attemptCount = 0;
    });

    /**
     * This enum provides possible passcode entry results. This can be used to provide feedback to the user.
     */
    private enum PasscodeResults {
        /** Passcode matches, or passcode was successfully updated **/
        SUCCESS,
        /** Generic failure condition **/
        FAILURE,
        /** Passcodes were not equivalent **/
        DID_NOT_MATCH,
        /** Passcode was shorter than the specified minimum **/
        TOO_SHORT,
        /** Passcode was longer than the specified maximum **/
        TOO_LONG,
        /** Passcode does not contain a letter **/
        MISSING_LETTER,
        /** Passcode does not contain a number **/
        MISSING_NUMBER,
        /** Passcode does not contain at least one of the following special characters: !@#$%&*()_+=|<>?{}[]~- **/
        MISSING_SPECIAL_CHARACTER
    }

    /**
     * This enum tracks the state of entering the passcode in MES
     */
    public enum PasscodeState {
        DISABLED, //checkbox is unchecked
        PASSCODE_ENTERED, //the first text field is populated
        CONFIRMATION_ENTERED, //the confirmation text field is populated
        ENABLED, //the passcode feature is engaged and the passcode matches the rules
    }

    /**
     * Gets the current state of entering the passcode in MES
     * @return the current state of entering the passcode in MES
     */
    @NonNull
    PasscodeState getPasscodeState() {
        return passcodeState;
    }

    /**
     * Checks if the SettingsLockout should start off in a locked state, such as rebooting after lockout
     */
    public void checkLockout() {
        long lockoutTimestamp = DataMatrix.getLong(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_TIMESTAMP_LONG, 0);
        long currentTimestamp = System.currentTimeMillis();
        if (lockoutTimestamp==0) { //no timestamp set
            settingsLocked = false;
        } else if ((currentTimestamp - lockoutTimestamp) > TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES)) { //settings were locked but are now unlocked by timeout
            settingsLocked = false;
        } else { //settings are still locked, start the timer based on when the lockout happened
            int timeRemainingMillis = (int) (lockoutTimestamp + TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES) - currentTimestamp);
            settingsLocked = true;
            attemptCount = MAX_ATTEMPTS; //This is required because there is logic to update the settingsLocked boolean based on this value
            lockoutTimer.stop();
            lockoutTimer.start(timeRemainingMillis);
        }
    }

    /**
     * Based on the current state of entering the passcode, this method will update the PasscodeState
     * of the SettingsLockout
     * @param passcodeText The text entered in the passcode entry field in MES
     * @param confirmationText The text entered in the passcode confirmation field in MES
     * @param isChecked Whether or not the SettingsLockout checkbox is checked in MES
     * @param view The view to attach dialog messages for user feedback
     * @param fromApply Whether or not the call is coming from the Apply Passcode button
     * @return the PasscodeState of SettingsLockout after calling this method
     */
    public @NonNull PasscodeState updateState(String passcodeText, String confirmationText, boolean isChecked, View view, boolean fromApply) {
        if (!isChecked) { //only need to check the checkbox for disable, any state can go to disabled by unchecking
            passcodeState = PasscodeState.DISABLED;
            DataMatrix.putString(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_PASSCODE_STRING, "");
        } else if (passcodeState == PasscodeState.DISABLED) { //only advance to passcode entered if we have passcode text
            boolean hasPasscodeText = !passcodeText.isEmpty();
            if (hasPasscodeText) {
                passcodeState = PasscodeState.PASSCODE_ENTERED;
            }
        } else if (passcodeState == PasscodeState.PASSCODE_ENTERED) {
            boolean hasPasscodeText = !passcodeText.isEmpty();
            boolean hasConfirmationText = !confirmationText.isEmpty();
            if (hasPasscodeText && hasConfirmationText) { //only advance to confirmation entered if we have both texts
                passcodeState = PasscodeState.CONFIRMATION_ENTERED;
            }
        } else if (passcodeState == PasscodeState.CONFIRMATION_ENTERED) {
            PasscodeResults evaluationResult = changePasscode(passcodeText, confirmationText, view, fromApply);
            boolean passcodeFollowsRules = evaluationResult == PasscodeResults.SUCCESS;
            boolean hasPasscodeText = !passcodeText.isEmpty();
            boolean hasConfirmationText = !confirmationText.isEmpty();
            if (hasPasscodeText && hasConfirmationText && passcodeFollowsRules && fromApply) { //only advance if we have all fields, follow rules, and user pressed apply
                passcodeState = PasscodeState.ENABLED;
            }
        }
        return passcodeState;
    }

    /**
     * Checks if the two provided passcodes match each other
     * @param currentPasscode Text of the first passcode field in MES
     * @param confirmPasscode Text of the confirmation passcode field in MES
     * @return SUCCESS if they match, DID_NOT_MATCH otherwise
     */
    private PasscodeResults passcodeMatches(final @NonNull String currentPasscode, final @NonNull String confirmPasscode) {
        if (currentPasscode.equals(confirmPasscode)){
            return PasscodeResults.SUCCESS;
        }
        return PasscodeResults.DID_NOT_MATCH;
    }

    /**
     * Checks if the provided passcode is the correct length
     * @param currentPasscode Text of the first passcode field in MES
     * @return SUCCESS if passcode is correct length, TOO_SHORT or TOO_LONG for if the passcode
     * is too short or too long respectively
     */
    private PasscodeResults passcodeCorrectLength(String currentPasscode) {
        if (currentPasscode.length() < MIN_PASSCODE_LENGTH) {
            return PasscodeResults.TOO_SHORT;
        } else if (currentPasscode.length() > MAX_PASSCODE_LENGTH){
            return PasscodeResults.TOO_LONG;
        }
        return PasscodeResults.SUCCESS;
    }

    /**
     * Checks if the provided passcode has a letter, number, and special character
     * @param currentPasscode Text of the first passcode field in MES
     * @return SUCCESS if the passcode meets requirements, MISSING_LETTER, MISSING_NUMBER, or
     * MISSING_SPECIAL_CHARACTER if the passcode is missing a letter, number, or special character respectively
     */
    private PasscodeResults passcodeHasLetterNumberCharacter(String currentPasscode) {
        Pattern letter = Pattern.compile("[a-zA-Z]");
        Pattern number = Pattern.compile("[0-9]");
        Pattern special = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]"); //what special characters should be allowed?
        Matcher hasLetter = letter.matcher(currentPasscode);
        Matcher hasNumber = number.matcher(currentPasscode);
        Matcher hasSpecial = special.matcher(currentPasscode);

        if (!hasLetter.find()) {
            return PasscodeResults.MISSING_LETTER;
        } else if (!hasNumber.find()) {
            return PasscodeResults.MISSING_NUMBER;
        } else if (!hasSpecial.find()) {
            return PasscodeResults.MISSING_SPECIAL_CHARACTER;
        }
        return PasscodeResults.SUCCESS;
    }

    /**
     * Evaluates all of the required rules for the passcode and returns the result of that evaluation
     * @param currentPasscode Text of the first passcode field in MES
     * @param confirmPasscode Text of the confirmation passcode field in MES
     * @return SUCCESS if the passcodes follow all the rules. Otherwise, a PasscodeResult
     * indicating which of the rules failed
     */
    private PasscodeResults passcodeFollowsRules(String currentPasscode, String confirmPasscode) {
        PasscodeResults result;
        //check if passcode matches
        result = passcodeMatches(currentPasscode,confirmPasscode);
        if (result != PasscodeResults.SUCCESS) {
            return result;
        }
        //check if passcode is correct length
        result = passcodeCorrectLength(currentPasscode);
        if (result != PasscodeResults.SUCCESS) {
            return result;
        }
        //check if passcode has letter, number, and special character
        result = passcodeHasLetterNumberCharacter(currentPasscode);
        if (result != PasscodeResults.SUCCESS) {
            return result;
        }
        return result;
    }


    /**
     * Attempts to set the passcode
     * @param newPasscode Text of the first passcode field in MES
     * @param newConfirmPasscode Text of the confirm passcode field in MES
     * @param view The view to attach dialog messages for user feedback
     * @param fromApply Whether or not the call is coming from the Apply Passcode button
     * @return SUCCESS if the passcode updated. Otherwise, a PasscodeResult indicating why
     * the update failed
     */
    PasscodeResults changePasscode(String newPasscode, String newConfirmPasscode, View view, boolean fromApply) {
        PasscodeResults result = passcodeFollowsRules(newPasscode, newConfirmPasscode);
        if (result == PasscodeResults.SUCCESS) {
            //storing in datamatrix for persistence
            DataMatrix.putString(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_PASSCODE_STRING, newPasscode);
            currentPasscode = newPasscode;
            attemptCount = 0;
            return PasscodeResults.SUCCESS;
        }
        logResult(result, view, fromApply);
        return result;
    }

    /**
     * Checks that an entered passcode matches the set passcode
     * @param passcode Passcode entered by the user prior to opening MES
     * @return true if the passcode matched, false if the passcode did not match or locked out
     */
    public boolean checkPasscode(String passcode) {
        if (!settingsLocked && passcode.equals(currentPasscode)) {
            return true;
        } else {
            attemptCount++;
            if(attemptCount > (MAX_ATTEMPTS - 1)) { //-1 is so it locks on the maxAttempt failed login, not one after
                settingsLocked = true;
                long lockoutTimestamp = System.currentTimeMillis();
                DataMatrix.putLong(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_TIMESTAMP_LONG, lockoutTimestamp);
                lockoutTimer.stop();
                lockoutTimer.start();
            }
            return false;
        }
    }

    /**
     * Logs a message to give feedback on a PasscodeResult
     * @param result the PasscodeResult
     * @param view The view to attach dialog messages for user feedback
     * @param fromApply Whether or not the call is coming from the Apply Passcode button
     */
    void logResult(PasscodeResults result, View view, boolean fromApply) {
        Context context = view.getContext();
        String message = "";
        switch (result) {
            case SUCCESS:
                Log.d(TAG,"Success");
                break;
            case DID_NOT_MATCH:
                message = context.getString(R.string.passcodes_did_not_match_tc);
                Log.d(TAG, message);
                break;
            case TOO_SHORT:
                message = context.getString(R.string.passcode_too_short_tc);
                Log.d(TAG, message);
                break;
            case TOO_LONG:
                message = context.getString(R.string.passcode_too_long_tc);
                Log.d(TAG, message);
                break;
            case MISSING_LETTER:
                message = context.getString(R.string.passcode_missing_letter_tc);
                Log.d(TAG, message);
                break;
            case MISSING_NUMBER:
                message = context.getString(R.string.passcode_missing_number_tc);
                Log.d(TAG, message);
                break;
            case MISSING_SPECIAL_CHARACTER:
                message = context.getString(R.string.passcode_missing_special_character_tc);
                Log.d(TAG, message);
                break;
            case FAILURE:
            default:
                Log.d(TAG,"Failure/Default");
        }
        if (!message.isEmpty() && fromApply) ProjectDialogFactory.message(view, message, () -> {}, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
    }

    public boolean isSettingsLocked() {
        return settingsLocked;
    }
}
