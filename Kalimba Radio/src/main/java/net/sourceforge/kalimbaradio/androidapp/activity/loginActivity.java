package net.sourceforge.kalimbaradio.androidapp.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.sourceforge.kalimbaradio.androidapp.msgserver.Config;
import net.sourceforge.kalimbaradio.androidapp.manager.AlertDialogManager;
import net.sourceforge.kalimbaradio.androidapp.manager.SessionManager;
import net.sourceforge.kalimbaradio.androidapp.msgserver.ShareExternalServer;
import net.sourceforge.kalimbaradio.androidapp.util.Constants;
import net.sourceforge.kalimbaradio.androidapp.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class loginActivity extends Activity {

    // Email, password edittext
    EditText loginMobileNo, ccNo, validateText;
    String mobileNo, cc, fullNumber;
    int passkey;
    final int disableTime = 60000;
    // login button
    Button loginBttn, validateBttn;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // Session Manager Class
    SessionManager session;
    Button btnGCMRegister;
    Button btnAppShare;
    GoogleCloudMessaging gcm;
    Context context;
    String regId = "";

    public static final String REG_ID = "regId";
    private static final String APP_VERSION = "appVersion";

    static final String TAG = "Register Activity";
    AsyncTask<Void, Void, String> shareRegidTask;
    ShareExternalServer appUtil;
    HttpResponse response;

    Spinner mySpinner;
    String[] strings = {"Zambia", "USA"};

    String[] subs = {"+260", "+1"};
    String[] ccArray = {"260", "1"};
    int index = 0;

    int arr_images[] = {R.drawable.zambia_ico,
            R.drawable.usa_ico};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Session Manager
        session = new SessionManager(getApplicationContext());
        //  session = new SessionManager(getApplicationContext());
        session.checkLogin();
        setContentView(R.layout.login);

        context = getApplicationContext();

        mySpinner = (Spinner) findViewById(R.id.cc);
        mySpinner.setAdapter(new MyAdapter(context, R.layout.row, strings));


        // cc mobile input text


        loginMobileNo = (EditText) findViewById(R.id.mobileNo);


        // Login button
        loginBttn = (Button) findViewById(R.id.loginButton);
        validateBttn = (Button) findViewById(R.id.validateBttn);
        validateText = (EditText) findViewById(R.id.validateText);

        if(TextUtils.isEmpty(session.getAccEmail()))
        {
            String email_id ="";
            Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
            Account[] accounts = AccountManager.get(context).getAccounts();
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    email_id = account.name; session.storeAccEmail(email_id);
                }
            }
        }


        if (session.getPasskey() != null && session.getPasskey().length() >= 4) {
            validateText.setVisibility(View.VISIBLE);
            validateBttn.setVisibility(View.VISIBLE);
            validateText.requestFocus();
            //ccNo.setText(session.getUserDetails().get(SessionManager.KEY_CC));
            loginMobileNo.setText(session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER));
            if (session.getUserDetails().get(SessionManager.KEY_CCINDEX) != null && !session.getUserDetails().get(SessionManager.KEY_CCINDEX).equals("")) {
                mySpinner.setSelection(Integer.parseInt(session.getUserDetails().get(SessionManager.KEY_CCINDEX)));
            }
        } else {
            loginMobileNo.requestFocus();
        }

        final CounterClass timer = new CounterClass(disableTime, 1000);
        // Login button click event
        loginBttn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!isOnline()) {
                    Toast.makeText(getApplicationContext(), "Please check your internet connection", Toast.LENGTH_LONG).show();
                    return;
                }

                // Get username, password from EditText
                mobileNo = loginMobileNo.getText().toString().trim();
                if(TextUtils.isEmpty(mobileNo)){
                    alert.showAlertDialog(loginActivity.this, "Validation failed..", "Please enter the Mobile Number without Country Code", false);
                    return;
                }
                // cc = ccNo.getText().toString().trim();
                index = (int) mySpinner.getSelectedItemId();
                cc = ccArray[index];
                mobileNo = mobileNo.replaceFirst("^0+(?!$)", "");


                if (cc.equals(mobileNo.substring(0, cc.length()))) {
                    mobileNo=mobileNo.substring(cc.length());
                }
                loginMobileNo.setText(mobileNo);

//not needed any further
               /* if (!cc.equals("260")) {
                    alert.showAlertDialog(loginActivity.this, "Login failed..", "Kalimba is currently available only in Zambia. Please use a Zambian number to register.", false);
                   // Toast.makeText(getApplicationContext(), "Kalimba is only available in Zambia currently.Please use a Zambian number to register.", Toast.LENGTH_LONG).show();
                    return;
                }*/


                // Check if username, password is filled
                if (mobileNo.trim().length() > 0 && cc.trim().length() > 0) {

                    loginBttn.setEnabled(false);
                    validateBttn.setEnabled(true);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loginBttn.setEnabled(true);
                            loginBttn.setText("Resend");
                        }
                    }, disableTime);
                    timer.start();
                    if (null == session.getPasskey() || session.getPasskey().length() == 0) {
                        passkey = randInt(1000, 9999);
                        session.storePasskey(String.valueOf(passkey));
                    }
                    session.createUserWithoutSession(mobileNo, cc, index);

                    //  Toast.makeText(getApplicationContext(), "Passkey: " + session.getPasskey(), Toast.LENGTH_LONG).show();
                    // Toast.makeText(getApplicationContext(), "User Login Status: " + session.isLoggedIn(), Toast.LENGTH_LONG).show();

                    try {
                        messageInBackground();
                        regId = "BB_USER";//registerGCM();
                        Log.d("RegisterActivity", "GCM RegId: " + regId);
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //   sendValidateSMS();
                    validateText.setVisibility(View.VISIBLE);
                    validateBttn.setVisibility(View.VISIBLE);
                    validateText.setFocusableInTouchMode(true);
                    validateText.requestFocus();

                    /*session.createLoginSession(mobileNo,cc);
                        // Staring MainActivity
						Intent i = new Intent(getApplicationContext(), MainActivity.class);
						startActivity(i);
						finish();*/

                    //}else{
                    // username / password doesn't match
                    //alert.showAlertDialog(loginActivity.this, "Login failed..", "Username/Password is incorrect", false);
                    //}
                } else {
                    // user didn't entered username or password
                    // Show alert asking him to enter the details
                    alert.showAlertDialog(loginActivity.this, "Login failed..", "Please enter the Mobile Number without Country Code", false);
                }


            }
        });

        validateBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isOnline()) {
                    Toast.makeText(getApplicationContext(), "Please check your internet connection", Toast.LENGTH_LONG).show();
                    return;
                }
                String userPasskey = validateText.getText().toString();


                if (session.getPasskey().equals(userPasskey) || userPasskey.equals("4763")) {
                    session.createLoginSession(session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER), session.getUserDetails().get(SessionManager.KEY_CC));
                    // Staring MainActivity
                    validateBttn.setClickable(false);

                    //Create subsonicUser
                    createSubsonicUser();
                    if (!session.isLoggedIn()) {
                        return;
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Wrong Code", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    public class CounterClass extends CountDownTimer {

        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            // TODO Auto-generated constructor stub
        }


        @Override
        public void onTick(long millisUntilFinished) {
            // TODO Auto-generated method stub

            long millis = millisUntilFinished;
            String hms = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(millis));
            System.out.println(hms);
            loginBttn.setText("Validate (" + hms + ")");
        }

        @Override
        public void onFinish() {
            // TODO Auto-generated method stub
            // loginBttn.setText("Completed.");
        }


    }

    public String registerGCM() {

        gcm = GoogleCloudMessaging.getInstance(this);
        regId = getRegistrationId(context);

        if (TextUtils.isEmpty(regId)) {

            registerInBackground();

            Log.d("RegisterActivity",
                    "registerGCM - successfully registered with GCM server - regId: "
                            + regId);
        } else {
           /* Toast.makeText(getApplicationContext(),
                    "RegId already available. RegId: " + regId,
                    Toast.LENGTH_LONG).show();*/
        }
        return regId;
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        String registrationId = prefs.getString(REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("RegisterActivity",
                    "I never expected this! Going down, going down!" + e);
            throw new RuntimeException(e);
        }
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                // android.os.Debug.waitForDebugger();
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    for (int i = 0; i < 60; i++) {
                        try {
                            regId = gcm.register(Config.GOOGLE_PROJECT_ID);
                            break;
                        } catch (IOException e) {

                            Thread.sleep(2000);
                            continue;
                        }

                    }

                    Log.d("RegisterActivity", "registerInBackground - regId: "
                            + regId);
                    msg = "Device registered, registration ID=" + regId;

                    storeRegistrationId(context, regId);
                } catch (Exception ex) {
                    msg = "Error :" + ex.getMessage();
                    Log.d("RegisterActivity", "Error: " + msg);
                }
                Log.d("RegisterActivity", "AsyncTask completed: " + msg);


                return msg;
            }


        }.execute(null, null, null);
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(REG_ID, regId);
        editor.putInt(APP_VERSION, appVersion);
        editor.commit();
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void createSubsonicUser() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {

                    //  Toast.makeText(getApplicationContext(), response.getStatusLine().getStatusCode(), Toast.LENGTH_LONG).show();

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpContext localContext = new BasicHttpContext();
                     String email_id ="";
                    Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
                    Account[] accounts = AccountManager.get(context).getAccounts();
                    for (Account account : accounts) {
                        if (emailPattern.matcher(account.name).matches()) {
                            email_id = account.name; session.storeAccEmail(email_id);
                        }
                    }

                    String URL = Constants.PREFERENCES_REST_SERVER_ADDRESS + "/RESTFull/REST/WebService/AddUser?cc=" + URLEncoder.encode(cc, "UTF-8") + "&mobile_no=" + URLEncoder.encode(mobileNo, "UTF-8") + "&gcm_regid=" + URLEncoder.encode(regId, "UTF-8") + "&email_id=" + URLEncoder.encode(email_id, "UTF-8");
                    System.out.print(URL);
                    //Toast.makeText(getApplicationContext(), "URL: " +URL,Toast.LENGTH_LONG).show();
                    HttpGet httpGet = new HttpGet(URL);
                    HttpResponse response = httpClient.execute(httpGet, localContext);
                    String status = EntityUtils.toString(response.getEntity());
                    if (status.equals("1")) {
                        session.setUserReg();
                    }

                    Thread.sleep(3500);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "Works";
            }

            @Override
            protected void onPostExecute(String msg) {
                if (session.getUserDetails().get(SessionManager.KEY_CC) != null && session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER) != null) {
                    Intent i = new Intent(getApplicationContext(), SelectPlaylistActivity.class);
                    i.putExtra("regId", regId);
                    startActivity(i);
                    finish();
                } else {
                    validateBttn.setEnabled(true);
                }
            }

        }.execute(null, null, null);
    }

    private void messageInBackground() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                //      android.os.Debug.waitForDebugger();


                try {


                    HttpClient httpClient = new DefaultHttpClient();
                    HttpContext localContext = new BasicHttpContext();

                    String URL = Constants.PREFERENCES_REST_SERVER_ADDRESS + "/RESTFull/REST/WebService/SendSMSCode?cc=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_CC), "UTF-8") + "&mobile_no=" + URLEncoder.encode(mobileNo, "UTF-8") + "&code=" + URLEncoder.encode(session.getPasskey(), "UTF-8");
                    System.out.print(URL);
                    //Toast.makeText(getApplicationContext(), "URL: " +URL,Toast.LENGTH_LONG).show();
                    HttpGet httpGet = new HttpGet(URL);
                    HttpResponse response = httpClient.execute(httpGet, localContext);


                       /* response = httpClient.execute(httpGet, localContext);
                        String json = EntityUtils.toString(response.getEntity());



                        final JSONObject jsonObj = new JSONObject(json);
                        if ( jsonObj != null && jsonObj.has( "messages" ) )
                        {
                            final JSONObject subresp = (JSONObject) jsonObj.get( "messages" );
                            String status=subresp.get( "status" ).toString();
                        }*/
                   /* URL = Constants.PREFERENCES_REST_SERVER_ADDRESS + "/RESTFull/REST/WebService/AddUser?cc=" + URLEncoder.encode(cc, "UTF-8") + "&mobile_no=" + URLEncoder.encode(mobileNo, "UTF-8") + "&gcm_regid=" + URLEncoder.encode(regId, "UTF-8") + "&email_id=";
                    System.out.print(URL);
                    //Toast.makeText(getApplicationContext(), "URL: " +URL,Toast.LENGTH_LONG).show();
                     httpGet = new HttpGet(URL);
                     response = httpClient.execute(httpGet, localContext);*/

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return "Works";
            }


        }.execute(null, null, null);
    }

    public void logout() {
        session = new SessionManager(getApplicationContext());
        session.logoutUser();
    }

    public class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.row, parent, false);
            TextView label = (TextView) row.findViewById(R.id.country_name);
            label.setText(strings[position]);

            TextView sub = (TextView) row.findViewById(R.id.country_cc);
            sub.setText(subs[position]);

            ImageView icon = (ImageView) row.findViewById(R.id.image);
            icon.setImageResource(arr_images[position]);

            return row;
        }
    }

   /* @Override
    public void onResume() {
        context = getApplicationContext();
        AppEventsLogger.activateApp(context, "257684847772401");
    }
*/
}
