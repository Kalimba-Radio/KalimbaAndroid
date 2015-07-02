package net.sourceforge.kalimbaradio.androidapp.manager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import net.sourceforge.kalimbaradio.androidapp.activity.MainActivity;
import net.sourceforge.kalimbaradio.androidapp.activity.SelectPlaylistActivity;
import net.sourceforge.kalimbaradio.androidapp.activity.loginActivity;

import java.util.HashMap;

public class SessionManager {
    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "AndroidHivePref";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String IS_USER_CREATED = "IsUserCreated";
    private static final String IS_USER_REGISTERED = "IsUserReg";

    // User name (make variable public to access from outside)
    public static final String KEY_MOBILENUMBER = "mobileNo";

    // CC (make variable public to access from outside)
    public static final String KEY_CC = "cc";

    public static final String KEY_PASS = "passkey";

    public static final String KEY_SUBUSER = "subsonicUser";

    public static final String KEY_CCINDEX = "ccIndex";

    public static final String KEY_ACC_EMAIL = "acc_email";
    // Constructor
    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create User
     */
    public void createUserWithoutSession(String mobileNo, String cc, int ccIndex) {
        // Storing login value as TRUE


        // Storing mobile no in pref
        editor.putString(KEY_MOBILENUMBER, mobileNo);


        // Storing cc in pref
        editor.putString(KEY_CC, cc);
        String subUser = new StringBuilder().append(cc).append("-").append(mobileNo).toString();
        editor.putString(KEY_SUBUSER, subUser);
        editor.putString(KEY_CCINDEX, String.valueOf(ccIndex));
        // commit changes
        editor.commit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String mobileNo, String cc) {
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);

        // Storing mobile no in pref
        editor.putString(KEY_MOBILENUMBER, mobileNo);


        // Storing cc in pref
        editor.putString(KEY_CC, cc);

        editor.putString(KEY_SUBUSER, cc + "-" + mobileNo);

        editor.putString(KEY_CCINDEX, "0");
        // commit changes
        editor.commit();
    }

    /**
     * Check login method wil check user login status
     * If false it will redirect user to login page
     * Else won't do anything
     */
    public void checkLogin() {
        // Check login status
        if (this.isLoggedIn()) {
            // user is not logged in redirect him to Login Activity
            Intent i = new Intent(_context, SelectPlaylistActivity.class);
            // Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add new Flag to start new Activity
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Staring Login Activity
            _context.startActivity(i);


        }

    }

    public void checkNotLogin() {
        // Check login statusm
        if (!this.isLoggedIn()) {
            // user is not logged in redirect him to Login Activity
            Intent i = new Intent(_context, loginActivity.class);
            // Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add new Flag to start new Activity
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Staring Login Activity
            _context.startActivity(i);


        }

    }


    /**
     * Get stored session data
     */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        // user name
        user.put(KEY_MOBILENUMBER, pref.getString(KEY_MOBILENUMBER, null));

        // user CC
        user.put(KEY_CC, pref.getString(KEY_CC, null));
        user.put(KEY_SUBUSER, pref.getString(KEY_SUBUSER, null));
        user.put(KEY_CCINDEX, pref.getString(KEY_CCINDEX, "0"));


        // return user
        return user;
    }

    /**
     * Clear session details
     */
    public void logoutUser() {
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.commit();

        // After logout redirect user to Loing Activity
        Intent i = new Intent(_context, loginActivity.class);
        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Staring Login Activity
        _context.startActivity(i);
    }

    public void cleanSession() {
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.commit();


    }

    /**
     * Quick check for login
     * *
     */
    // Get Login State
    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }


    /*passkey to shared prefrence*/

    public void storePasskey(String passKey) {
        // Storing login value as TRUE

        // Storing cc in pref
        editor.putString(KEY_PASS, passKey);

        // commit changes
        editor.commit();
    }

    public String getPasskey() {
        //getting passkey  from pref
        return pref.getString(KEY_PASS, null);
    }

    public void setUserCreation() {
        // Storing login value as TRUE

        // Storing cc in pref
        editor.putBoolean(IS_USER_CREATED, true);

        // commit changes
        editor.commit();
    }

    public boolean isUserCreated() {
        return pref.getBoolean(IS_USER_CREATED, false);
    }

    public void setUserReg() {
        // Storing login value as TRUE

        // Storing cc in pref
        editor.putBoolean(IS_USER_REGISTERED, true);

        // commit changes
        editor.commit();
    }

    public boolean isUserReg() {
        return pref.getBoolean(IS_USER_REGISTERED, false);
    }

    public void storeAccEmail(String email_id) {
        // Storing login value as TRUE

        // Storing cc in pref
        editor.putString(KEY_ACC_EMAIL, email_id);

        // commit changes
        editor.commit();
    }

    public String getAccEmail() {
        //getting passkey  from pref
        return pref.getString(KEY_ACC_EMAIL, null);
    }
}
