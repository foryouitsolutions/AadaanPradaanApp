package com.foryouitsolutions.aadaanpradaan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pConfig;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity extends AppCompatActivity implements profileDialog.profileDialogListener {
    private static final String TAG = "sdsd";
    Button btnSend, btnReceive;
    TextView useradd;
    private BottomSheetBehavior bottomSheetBehavior;
    WifiManager manager;
    private WifiManager.LocalOnlyHotspotReservation mReservation;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.custom_actionbar);

        //Initializations
        btnSend = findViewById(R.id.send);
        btnReceive = findViewById(R.id.receive);
        useradd = findViewById(R.id.search);
        final View yourview = findViewById(R.id.yourview);
        LinearLayout linearLayout = findViewById(R.id.bottom_sheet);

        //Bottom Sheet
        bottomSheetBehavior = BottomSheetBehavior.from(linearLayout);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {

            }

            @Override
            public void onSlide(@NonNull View view, float v) {
                yourview.setRotation(v * 180);
            }
        });

        //Custom Action Bar
        ActionBar.LayoutParams p = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        p.gravity = Gravity.CENTER;

        //Send Button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Discovering...", Toast.LENGTH_SHORT).show();
            }
        });
        //Receive Button
        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                mChannel = mManager.initialize(getApplicationContext(), getMainLooper(), null);

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Device is ready to accept incoming connections from peers.

                        Toast.makeText(MainActivity.this, "hotspot ready", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "hotspot");
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup group) {
                                String groupPassword = group.getPassphrase();
                                Log.d(TAG, "LOGGG" + groupPassword + group.getNetworkName());
                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "P2P group creation failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: " +reason);
                    }
                });



            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.profile:
                profile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void profile() {
        profileDialog profileDialog = new profileDialog();
        profileDialog.show(getSupportFragmentManager(),"Profile Dialog");
    }

    @Override
    public void applyText(String username) {
        useradd.setText(username);
    }
}