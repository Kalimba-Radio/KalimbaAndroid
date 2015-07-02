package net.sourceforge.kalimbaradio.androidapp.msgserver;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by Twerp on 12/28/2014.
 */

public final class GCMHelper {

    static GoogleCloudMessaging gcm = null;

    static Context context= null;

    public GCMHelper (Context context)
    {
        this.context= context;
    }

    public String GCMRegister (String SENDER_ID) throws Exception
    {
        String regid = "";
        //Check if Play store services are available.
        if(!checkPlayServices())
            throw new Exception("Google Play Services not supported. Please install and configure Google Play Store.");

        if (gcm == null) {
            gcm = GoogleCloudMessaging.getInstance(context);
        }
        regid = gcm.register(SENDER_ID);

        return regid;
    }


    private static boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }
}