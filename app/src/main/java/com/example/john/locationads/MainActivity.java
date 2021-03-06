package com.example.john.locationads;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private String[] NavArray =  { "Current Location", "Ad Location", "Frequent Ad", "Settings", "Register", "Exit" };
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;
    private SessionManager mSessionManager;
    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.content_frame) != null) {
            if (savedInstanceState != null) {
                return;
            }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        drawerFragment = (FragmentDrawer)
        getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener((FragmentDrawer.FragmentDrawerListener) this);

            mSessionManager = new SessionManager(getApplicationContext());
            mSessionManager.get_logged();
            mSessionManager.get_registered();
            mSessionManager.set_session();

              AsyncTask<Void, Void, Void> DirectionManager_object;
              DirectionManager_object = new DirectionManager(getApplicationContext()).execute();

              startService(new Intent(this, FrequencyUpdater.class));
              create_fragments(new Map_Fragment(0));

//            start_NotificationManager_service();

            user_status();

        }
    }

    private void start_NotificationManager_service(){
            Intent myIntent = new Intent(this, FrequencyUpdater.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, myIntent, 0);

            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.SECOND, 20);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 5*1000, pendingIntent);
    }

    /** action menu function not implemented yet **/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        user_status();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDrawerItemSelected(View view, int position) {
        selectItem(position);
    }

    /** navigation drawer listener swaps fragments in the main content view */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void user_status(){
        if(GlobalVar.getLoggedIn() && GlobalVar.getRegister())
            NavArray = new String[]{"Current Location", "Ad Location", "Frequent Ads", "Settings", "Logout"};
        else if (GlobalVar.getRegister())
            NavArray = new String[]{"Current Location", "Ad Location", "Frequent Ads", "Settings", "Login"};
        else
            NavArray = new String[]{"Current Location", "Ad Location", "Frequent Ads", "Settings", "Register"};

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, NavArray);
    }

    private void selectItem(int position) {
        String title = getString(R.string.app_name);
        switch (position) {
            case 0:
                create_fragments(new Map_Fragment(0));
                break;
            case 1:
                create_fragments(new Map_Fragment(1));
                break;
            case 2:
                create_fragments(new Map_Fragment(2));
                break;
            case 3:
                create_fragments(new SettingsFragment(this));
                break;
            case 4:
                if(GlobalVar.getLoggedIn() && GlobalVar.getRegister())
                    create_logout_dialog();
                else if (GlobalVar.getRegister() && GlobalVar.getLoggedIn() == false)
                    create_login_dialog();
                else
                    create_register_dialog();
                break;
           default:
               break;
           }

        getSupportActionBar().setTitle(title);

    }

    private void create_logout_dialog(){
        final Dialog logout = new Dialog(this);

        logout.setContentView(R.layout.logout_dialog);
        logout.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        Button btnLogout = (Button) logout.findViewById(R.id.btnYes);
        Button btnCancel = (Button) logout.findViewById(R.id.btnNo);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences settings_sp = getApplicationContext().getSharedPreferences(GlobalVar.getSharedPreferenceName(), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings_sp.edit();
                editor.putString("AUTH_TOKEN", null);
                editor.putBoolean("LOGGED_IN", false);
                editor.commit();
                GlobalVar.setUserToken(null);
                GlobalVar.setLoggedIn(false);
                mSessionManager.set_session();

                logout.dismiss();
                Toast.makeText(MainActivity.this, "Logged out Successfully", Toast.LENGTH_LONG).show();

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               logout.dismiss();
            }
        });
        logout.show();
    }

    private void create_login_dialog(){

        final Dialog login = new Dialog(this);

        login.setContentView(R.layout.login_dialog);
        login.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        Button btnLogin = (Button) login.findViewById(R.id.btnSingIn);
        final EditText txtPassword = (EditText)login.findViewById(R.id.etPass);
        final EditText txtEmail = (EditText)login.findViewById(R.id.etEmail);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtEmail.getText().toString().trim().length() > 0 && txtPassword.getText().toString().trim().length() > 0)
                {
                    String password = txtPassword.getText().toString();
                    String email = txtEmail.getText().toString();

                    AsyncTask<Void, Void, Void> AuthenticatorTasker_object;
                    AuthenticatorTasker_object = new AuthenticatorTasker(password,email,login,getApplicationContext(),MainActivity.this).execute();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Enter Email and Password", Toast.LENGTH_LONG).show();
                }
            }
        });
        login.show();
    }

    private void create_register_dialog() {

        final Dialog register = new Dialog(this);

        register.setContentView(R.layout.register_dialog);
        register.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        Button btnLogin = (Button) register.findViewById(R.id.btnSingIn);
//        Button btnCancel = (Button) login.findViewById(R.id.btnCancel);
        final EditText txtEmail = (EditText)register.findViewById(R.id.etEmail);
        final EditText txtPassword = (EditText)register.findViewById(R.id.etPass);
        final EditText txtUsername = (EditText)register.findViewById(R.id.etUserName);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtUsername.getText().toString().trim().length() > 0 && txtEmail.getText().toString().trim().length() > 0 && txtPassword.getText().toString().trim().length() > 0)
                {
                    String email = txtEmail.getText().toString();
                    String password = txtPassword.getText().toString();
                    String username = txtUsername.getText().toString();

                    AsyncTask<Void, Void, Void> AuthenticatorTasker_object;
                    AuthenticatorTasker_object = new AuthenticatorTasker(email,password,username,register,getApplicationContext(),MainActivity.this).execute();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Enter Email and Password", Toast.LENGTH_LONG).show();
                }
            }
        });

        register.show();

    }

    private void create_fragments(Fragment fragment){
        Bundle data = new Bundle();
        fragment.setArguments(data);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        fragmentManager.beginTransaction().addToBackStack(null);
    }

    /** for sync on navigation drawer actions and animations **/
    private void setupDrawer() {
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        mDrawerToggle.onConfigurationChanged(newConfig);
    }


}