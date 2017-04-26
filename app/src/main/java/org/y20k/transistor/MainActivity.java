/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menu bar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.y20k.transistor.helpers.DialogAdd;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.util.Arrays;


/**
 * MainActivity class
 */
public final class MainActivity extends AppCompatActivity  implements NavigationView.OnNavigationItemSelectedListener  {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Main class variables */
    private boolean mTwoPane;
    private File mCollectionFolder;
    private View mContainer;
    private BroadcastReceiver mCollectionChangedReceiver;
    private static final int RC_SIGN_IN = 123; //for Firebase login (can be any Unique number
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private LinearLayout layoutLogin;
    private LinearLayout layoutLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get collection folder
        StorageHelper storageHelper = new StorageHelper(this);
        mCollectionFolder = storageHelper.getCollectionDirectory();
        if (mCollectionFolder == null) {
            Toast.makeText(this, getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
            finish();
        }

        // set layout
        setContentView(R.layout.activity_main);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

        //Mal:toolbar and Drawer
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //Mal:END toolbar and Drawer

        //authentication (login using Firebase components)
        Button btnLogin = (Button) navigationView.getHeaderView(0).findViewById(R.id.btnLogin);
        layoutLogin = (LinearLayout) navigationView.getHeaderView(0).findViewById(R.id.layoutLogin);
        layoutLoggedIn = (LinearLayout) navigationView.getHeaderView(0).findViewById(R.id.layoutLoggedIn);
        mAuth= FirebaseAuth.getInstance();
        //layout
        if (mAuth.getCurrentUser() != null) {
            // already signed in
            fillUserProfileData();
            layoutLoggedIn.setVisibility(View.VISIBLE);
            layoutLogin.setVisibility(View.GONE);
        } else {
            // not signed in
            layoutLoggedIn.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
        }
        btnLogin.setOnClickListener(new View.OnClickListener() {
            // Choose an arbitrary request code value
            @Override
            public void onClick(View v) {
                if (mAuth.getCurrentUser() == null) {
                    // not signed in
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setTheme(R.style.TransistorAppTheme_NoActionBar)
                                    .setProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        });

    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.menu_add) {
            DialogAdd dialog = new DialogAdd(this, mCollectionFolder);
            dialog.show();
            return true;
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
//            AuthUI.getInstance()
//                    .signOut(MainActivity.this)
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        public void onComplete(@NonNull Task<Void> task) {
//                            // user is now signed out
//                            layoutLoggedIn.setVisibility(View.GONE);
//                            layoutLogin.setVisibility(View.VISIBLE);
//                        }
//                    });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check if two pane mode can be used
        mTwoPane = detectTwoPane();

        // tablet mode: show player fragment in player container
        if (mTwoPane && mCollectionFolder.listFiles().length > 1) {
            // hide right pane
            mContainer.setVisibility(View.VISIBLE);
        } else if (mTwoPane) {
            // make room for action call
            mContainer.setVisibility(View.GONE);
        }

        saveAppState(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceivers();
    }


    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        // check if two pane mode can be used
        mTwoPane = detectTwoPane();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // activity opened for second time set intent to new intent
        setIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // make sure that MainActivityFragment's onActivityResult() gets called
        super.onActivityResult(requestCode, resultCode, data);

        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == ResultCodes.OK) {
                Toast.makeText(this, "WelcomeBackActivity", Toast.LENGTH_SHORT).show();

                IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
                fillUserProfileData();

                layoutLoggedIn.setVisibility(View.VISIBLE);
                layoutLogin.setVisibility(View.GONE);

                return;
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    Toast.makeText(this, "sign_in_cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "no_internet_connection", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, "unknown_error", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "unknown_sign_in_response", Toast.LENGTH_SHORT).show();
        }
    }

    private void fillUserProfileData() {
        SimpleDraweeView imageView = (SimpleDraweeView) navigationView.getHeaderView(0).findViewById(R.id.imageView);
        TextView txtUserProfileName = (TextView) navigationView.getHeaderView(0).findViewById(R.id.txtUserProfileName);
        TextView txtProfileEmail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.txtProfileEmail);


        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Name, email address, and profile photo Url
        txtUserProfileName.setText(currentUser.getDisplayName());
        txtProfileEmail.setText(currentUser.getEmail());
        Uri photoUrl = currentUser.getPhotoUrl();

        //set round icon
        if(photoUrl!=null) {
            int color = ContextCompat.getColor(this, R.color.colorPrimary);
            RoundingParams roundingParams = RoundingParams.fromCornersRadius(5f);
            roundingParams.setBorder(color, 1.0f);
            roundingParams.setRoundAsCircle(true);
            imageView.getHierarchy().setRoundingParams(roundingParams);
            imageView.setImageURI(photoUrl);//.setImageBitmap(stationImageSmall);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_main);
        // hand results over to fragment main
        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /* Checks if two-pane mode can be used */
    private boolean detectTwoPane() {
        mContainer = findViewById(R.id.player_container);

        // if player_container is present two-pane layout can be used
        if (mContainer != null) {
            LogHelper.v(LOG_TAG, "Large screen detected. Choosing two pane layout.");
            return true;
        } else {
            LogHelper.v(LOG_TAG, "Small screen detected. Choosing single pane layout.");
            return false;
        }
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        // editor.putInt(PREF_STATION_ID_SELECTED, mStationID);
        editor.putBoolean(TransistorKeys.PREF_TWO_PANE, mTwoPane);
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state. Two Pane = " + mTwoPane);
    }


    /* Unregisters broadcast receivers */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCollectionChangedReceiver);
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {
        // RECEIVER: station added, deleted, or changed
        mCollectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // show/hide player layout container
                if (mTwoPane && mCollectionFolder.listFiles().length == 1) {
                    // make room for action call - hide player container
                    mContainer.setVisibility(View.GONE);
                } else if (mTwoPane) {
                    // show player container
                    mContainer.setVisibility(View.VISIBLE);
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mCollectionChangedReceiver, collectionChangedIntentFilter);
    }

//remove fragment if delete all stations
    public void removePlayFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(TransistorKeys.PLAYER_FRAGMENT_TAG);
        if(fragment!=null) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }
}