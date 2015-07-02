/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */

package net.sourceforge.kalimbaradio.androidapp.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.sourceforge.kalimbaradio.androidapp.domain.Playlist;
import net.sourceforge.kalimbaradio.androidapp.manager.SessionManager;
import net.sourceforge.kalimbaradio.androidapp.msgserver.ShareExternalServer;
import net.sourceforge.kalimbaradio.androidapp.service.MusicService;
import net.sourceforge.kalimbaradio.androidapp.util.BackgroundTask;
import net.sourceforge.kalimbaradio.androidapp.util.Constants;
import net.sourceforge.kalimbaradio.androidapp.util.PopupMenuHelper;
import net.sourceforge.kalimbaradio.androidapp.util.TabActivityBackgroundTask;
import net.sourceforge.kalimbaradio.androidapp.R;
import net.sourceforge.kalimbaradio.androidapp.service.MusicServiceFactory;
import net.sourceforge.kalimbaradio.androidapp.util.PlaylistAdapter;
import net.sourceforge.kalimbaradio.androidapp.util.Util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

public class SelectPlaylistActivity extends SubsonicTabActivity implements AdapterView.OnItemClickListener {

    private static final int MENU_ITEM_PLAY_ALL = 1;

    private ListView list;
    private View emptyTextView;

    GoogleCloudMessaging gcm;
    Context context;

    ShareExternalServer appUtil;
    String regId="";
    AsyncTask<Void, Void, String> shareRegidTask;
    public static final String REG_ID = "regId";
    private static final String APP_VERSION = "appVersion";
    SessionManager session;

    static final String TAG = "Register Activity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_playlist);
        //  appUtil = new ShareExternalServer();

        //  regId = getRegistrationId();

        session = new SessionManager(getApplicationContext());
        //regId = getIntent().getStringExtra("regId");
        final SharedPreferences prefs = getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        regId = prefs.getString(REG_ID, "");

        Log.d("MainActivity", "regId: " + regId);
        //if(!session.isUserCreated()) {
        validateSubsonicUser();
        // }
        /*if(!session.isUserReg()) {
            validateSubsonicUserReg();
        }*/


        final Context context = this;
       /* if (!TextUtils.isEmpty(regId)) {
            shareRegidTask = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    String result = appUtil.shareRegIdWithAppServer(context, regId);
                    return result;
                }

                @Override
                protected void onPostExecute(String result) {
                    shareRegidTask = null;
                  *//*  Toast.makeText(getApplicationContext(), result,
                            Toast.LENGTH_LONG).show();*//*
                }

            };
            shareRegidTask.execute(null, null, null);
        }
*/


        list = (ListView) findViewById(R.id.select_playlist_list);
        emptyTextView = findViewById(R.id.select_playlist_empty);
        list.setOnItemClickListener(this);
        registerForContextMenu(list);

        // Title: Playlists
        setTitle(R.string.playlist_label);

        // Button 1: refresh
        ImageButton refreshButton = (ImageButton) findViewById(R.id.action_button_1);
        refreshButton.setImageResource(R.drawable.action_refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });

        // Button 2: search
        ImageButton actionSearchButton = (ImageButton) findViewById(R.id.action_button_2);
        actionSearchButton.setImageResource(R.drawable.action_search);
        actionSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearchRequested();
            }
        });
        // Button 3: overflow
        final View overflowButton = findViewById(R.id.action_button_3);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PopupMenuHelper().showMenu(SelectPlaylistActivity.this, overflowButton, R.menu.main);
            }
        });

        load();
    }

    private void refresh() {
        finish();
        Intent intent = new Intent(this, SelectPlaylistActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_REFRESH, true);
        Util.startActivityWithoutTransition(this, intent);
    }

    private void load() {
        BackgroundTask<List<Playlist>> task = new TabActivityBackgroundTask<List<Playlist>>(this) {
            @Override
            protected List<Playlist> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SelectPlaylistActivity.this);
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                return musicService.getPlaylists(refresh, SelectPlaylistActivity.this, this);
            }

            @Override
            protected void done(List<Playlist> result) {
                list.setAdapter(new PlaylistAdapter(SelectPlaylistActivity.this, PlaylistAdapter.PlaylistComparator.sort(result)));
                emptyTextView.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }
        };
        task.execute();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(Menu.NONE, MENU_ITEM_PLAY_ALL, MENU_ITEM_PLAY_ALL, R.string.common_play_now);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        Playlist playlist = (Playlist) list.getItemAtPosition(info.position);

        switch (menuItem.getItemId()) {
            case MENU_ITEM_PLAY_ALL:
                Intent intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
                Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
                break;
            default:
                return super.onContextItemSelected(menuItem);
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Playlist playlist = (Playlist) parent.getItemAtPosition(position);

        Intent intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
        Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
    }

    private String getRegistrationId() {
        final SharedPreferences prefs = getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        String registrationId = prefs.getString(REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        return registrationId;
    }

    public void validateSubsonicUser() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {


                    if (session.getUserDetails().get(SessionManager.KEY_CC) != null && session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER) != null) {


                       // String email_id =session.getAccEmail();
                        String email_id ="";
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpContext localContext = new BasicHttpContext();

                        String URL = Constants.PREFERENCES_KEY_SERVER_ADDRESS + "/rest/getUser.view?u=admin&p=H@rd2Cr@ck&username=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_SUBUSER), "UTF-8") + "&v=1.10.2&c=myapp&f=json";

                        HttpGet httpGet = new HttpGet(URL);


                        HttpResponse response = httpClient.execute(httpGet, localContext);
                        HttpEntity entity = response.getEntity();
                        if (entity == null) {
                            throw new RuntimeException("No entity received for URL ");
                        }
                        String json = EntityUtils.toString(entity);
                        String status = "";
                        final JSONObject jsonObj = new JSONObject(json);
                        if (jsonObj != null && jsonObj.has("subsonic-response")) {
                            final JSONObject subresp = (JSONObject) jsonObj.get("subsonic-response");
                            status = subresp.get("status").toString();
                        }

                        URL = Constants.PREFERENCES_REST_SERVER_ADDRESS + "/RESTFull/REST/WebService/UpdateDbUser?cc=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_CC), "UTF-8") + "&mobile_no=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER), "UTF-8") + "&gcm_regid=" + URLEncoder.encode(regId, "UTF-8") + "&email_id=" + URLEncoder.encode(email_id, "UTF-8");
                        System.out.print(URL);
                        //Toast.makeText(getApplicationContext(), "URL: " +URL,Toast.LENGTH_LONG).show();
                        httpGet = new HttpGet(URL);
                        httpClient.execute(httpGet, localContext);

                        if (!status.equalsIgnoreCase("ok")) {
                            httpClient = new DefaultHttpClient();
                            localContext = new BasicHttpContext();


                            URL = Constants.PREFERENCES_REST_SERVER_ADDRESS + "/RESTFull/REST/WebService/AddUser?cc=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_CC), "UTF-8") + "&mobile_no=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER), "UTF-8") + "&gcm_regid=" + URLEncoder.encode(regId, "UTF-8") + "&email_id=" + URLEncoder.encode(email_id, "UTF-8");
                            System.out.print(URL);
                            //Toast.makeText(getApplicationContext(), "URL: " +URL,Toast.LENGTH_LONG).show();
                            httpGet = new HttpGet(URL);
                            response = httpClient.execute(httpGet, localContext);
                            status = EntityUtils.toString(response.getEntity());
                            if (status.equals("1")) {
                                session.setUserReg();
                            }
                        } else {
                            session.setUserCreation();
                        }


                    } else {
                        logout();
                        Intent i = new Intent(getApplicationContext(), loginActivity.class);
                        startActivity(i);
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // session.logoutUser();
                }
                return "Works";
            }

           /* @Override
            protected void onPostExecute(String msg) {
                if(session.getUserDetails().get(SessionManager.KEY_CC)!=null && session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER)!=null) {
                    Intent i = new Intent(getApplicationContext(), SelectPlaylistActivity.class);
                    i.putExtra("regId", regId);
                    startActivity(i);
                    finish();
                }
                else{
                    validateBttn.setEnabled(true);
                }
            }*/

        }.execute(null, null, null);
    }

    public void validateSubsonicUserReg() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {


                    if (session.getUserDetails().get(SessionManager.KEY_CC) != null && session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER) != null) {
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpContext localContext = new BasicHttpContext();

                        httpClient = new DefaultHttpClient();
                        localContext = new BasicHttpContext();

                        String URL = Constants.PREFERENCES_REST_SERVER_ADDRESS + "/RESTFull/REST/WebService/AddUser?cc=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_CC), "UTF-8") + "&mobile_no=" + URLEncoder.encode(session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER), "UTF-8") + "&gcm_regid=" + URLEncoder.encode(regId, "UTF-8");
                        System.out.print(URL);
                        //Toast.makeText(getApplicationContext(), "URL: " +URL,Toast.LENGTH_LONG).show();
                        HttpGet httpGet = new HttpGet(URL);
                        HttpResponse response = httpClient.execute(httpGet, localContext);
                        String status = EntityUtils.toString(response.getEntity());
                        if (status.equals("1")) {
                            session.setUserReg();
                        }


                    } else {
                        logout();
                        Intent i = new Intent(getApplicationContext(), loginActivity.class);
                        startActivity(i);
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // session.logoutUser();
                }
                return "Works";
            }

           /* @Override
            protected void onPostExecute(String msg) {
                if(session.getUserDetails().get(SessionManager.KEY_CC)!=null && session.getUserDetails().get(SessionManager.KEY_MOBILENUMBER)!=null) {
                    Intent i = new Intent(getApplicationContext(), SelectPlaylistActivity.class);
                    i.putExtra("regId", regId);
                    startActivity(i);
                    finish();
                }
                else{
                    validateBttn.setEnabled(true);
                }
            }*/

        }.execute(null, null, null);
    }

}