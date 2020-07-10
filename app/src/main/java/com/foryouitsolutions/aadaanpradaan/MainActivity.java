package com.foryouitsolutions.aadaanpradaan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pConfig;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static android.content.Context.*;

public class MainActivity extends AppCompatActivity implements profileDialog.profileDialogListener {
    private static final String TAG = "sdsd";
    Button btnSend, btnReceive;
    TextView useradd ,clientHost;
    private BottomSheetBehavior bottomSheetBehavior;
    WifiManager manager;
    private WifiManager.LocalOnlyHotspotReservation mReservation;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    Handler handler = new Handler();
    boolean handler_running = false;
    final HashMap<String, String> buddies = new HashMap<>();
    ListView listView;
    public static final String MyPREFERENCES = "MyPrefs" ;
    SharedPreferences sharedPreferences;


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
//            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                //Toast.makeText(MainActivity.this, "We are connected to a P2P group as host", Toast.LENGTH_SHORT).show();
                clientHost.setText("Host");
            } else if (wifiP2pInfo.groupFormed) {
                //Toast.makeText(MainActivity.this, "We are connected to a P2P group as client", Toast.LENGTH_SHORT).show();
                clientHost.setText("Client");
            }
        }
    };

    WifiP2pManager.PeerListListener peerlistener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            Collection<WifiP2pDevice> list = peers.getDeviceList();
            Iterator<WifiP2pDevice> i = list.iterator();
            while (i.hasNext()) {
                WifiP2pDevice device = i.next();
                Log.d(TAG, "onPeersAvailable: " + device.deviceAddress + device.deviceName);
            }
        }
    };

    Runnable runner = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group == null) {
                        Log.d(TAG, "onGroupInfoAvailable: no group!");
                        return;
                    }
                    String groupPassword = group.getPassphrase();
                    Log.d(TAG, "LOGGG" + groupPassword + group.getNetworkName());
                }
            });
            mManager.requestPeers(mChannel, peerlistener);
            handler.postDelayed(this, 5000);
        }
    };



    Runnable runner2 = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            mManager.requestConnectionInfo(mChannel, connectionInfoListener);
            handler.postDelayed(this, 5000);
        }
    };

    WifiP2pManager.ActionListener ActionListenerBuilder(final String text, String error) {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(MainActivity.this, "Adding service to manager failed with code " + code, Toast.LENGTH_SHORT).show();
            }
        };
    }

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
        clientHost = findViewById(R.id.clientHost);
        final View yourview = findViewById(R.id.yourview);
        LinearLayout linearLayout = findViewById(R.id.bottom_sheet);


        //getting SharedPrefs
        sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final String devicename = (sharedPreferences.getString(MyPREFERENCES, ""));
        useradd.setText(devicename);



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

        //init service button
        ((Button) findViewById(R.id.service_init)).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if(wifiManager == null){
                    wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
                    mChannel = mManager.initialize(getApplicationContext(), getMainLooper(), new WifiP2pManager.ChannelListener() {
                        @Override
                        public void onChannelDisconnected() {
                            Toast.makeText(MainActivity.this, "Lost connection to P2P channel", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                // make sure wifi is initially on
                wifiManager.setWifiEnabled(true);
                // add local service
                Toast.makeText(getApplicationContext(), "Inciting discovery service...", Toast.LENGTH_SHORT).show();
                // setup connection listener
                if(!handler_running){
                    handler.post(runner2);
                    handler_running = true;
                    startRegistration(mManager, mChannel, "8081" , devicename);
                }

                // discover nearby Wifi Direct services
                listView = findViewById(R.id.peerListView);
                String[] dataArray = new String[1];
                dataArray[0] = "No devices. Working...";
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, dataArray);
                listView.setAdapter(adapter);

                Toast.makeText(getApplicationContext(), "Setting listeners...", Toast.LENGTH_SHORT).show();
                mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

                WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance("AadaanPradaanService", "_presence._tcp");
                mManager.addServiceRequest(mChannel,
                        serviceRequest,
                        ActionListenerBuilder("Service discovery properties set...", "Setting service discovery properties to manager failed with code "));

                mManager.discoverServices(mChannel,
                        ActionListenerBuilder("Discovering services called...", "Adding service to manager failed with code "));            }
        });


        //Send Button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Discovering...", Toast.LENGTH_SHORT).show();

                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
                mChannel = mManager.initialize(getApplicationContext(), getMainLooper(), null);

                mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, MainActivity.this);
                mIntentFilter = new IntentFilter();
                // Indicates a change in the Wi-Fi P2P status.
                mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                // Indicates a change in the list of available peers.
                mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                // Indicates the state of Wi-Fi P2P connectivity has changed.
                mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                // Indicates this device's details have changed.
                mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
                Log.d("TAG", "initialWork: done");

                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Discovering Nearby Devices...", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(runner, 1000);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Discovering Nearby Devices Failed...", Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
        //Receive Button
        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                Toast.makeText(getApplicationContext(), "starting..." + wifiManager.isP2pSupported(), Toast.LENGTH_SHORT).show();
                wifiManager.setWifiEnabled(true);
                mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
                mChannel = mManager.initialize(getApplicationContext(), getMainLooper(), null);

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "missing perms...", Toast.LENGTH_SHORT).show();

                    return;
                }
                mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Device is ready to accept incoming connections from peers.

                        Toast.makeText(MainActivity.this, "hotspot ready", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "hotspot");
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getApplicationContext(), "missing perms...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        handler.postDelayed(runner, 1000);


                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "P2P group creation failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: " + reason);
                    }
                });


            }
        });


    }

    WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        @Override
        /* Callback includes:
         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */

        public void onDnsSdTxtRecordAvailable(
                String fullDomain, Map record, WifiP2pDevice device) {
            Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
            String device_name = (String) record.get("device_name");
            String device_address = device.deviceAddress;
            buddies.put(device_name, device_address);

            String[] dataArray =  buddies.keySet().toArray(new String[0]);
            ArrayAdapter adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1, dataArray );
            listView.setAdapter(adapter);
            listView.setOnItemLongClickListener(connectPeer);
        }
    };

    ListView.OnItemLongClickListener connectPeer = new AdapterView.OnItemLongClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(200);
            }

            Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
            final String device_name = (String) ((TextView) view).getText();
            String device_address;
            if(!buddies.containsKey(device_name)){
                Toast.makeText(MainActivity.this, "Cannot connect as device is no longer available", Toast.LENGTH_SHORT).show();
                return true;
            }

            device_address = buddies.get(device_name);
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device_address;
            // setup connection success listener
            mManager.requestConnectionInfo(mChannel, connectionInfoListener);
            mManager.connect(mChannel, config,
                    ActionListenerBuilder("Sent connection request to " + device_name, "Setting service discovery properties to manager failed with code ")
            );
            return true;
        }
    };

    WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                            WifiP2pDevice resourceType) {

            // Update the device name with the human-friendly version from
            // the DnsTxtRecord, assuming one arrived.
            String device_name = resourceType.deviceName;
            String device_address = resourceType.deviceAddress;
            buddies.put(device_name, device_address);

            String[] dataArray =  buddies.keySet().toArray(new String[0]);
            ArrayAdapter adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1, dataArray );
            listView.setAdapter(adapter);
            listView.setOnItemLongClickListener(connectPeer);
            Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
        }
    };

    private void startRegistration(WifiP2pManager manager, WifiP2pManager.Channel channel, String SERVER_PORT, String devicename) {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", SERVER_PORT);
        record.put("device_name", devicename);
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("AadaanPradaanService", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No permission to location. Failing...", Toast.LENGTH_LONG).show();
            return;
        }

        manager.addLocalService(channel, serviceInfo,
                ActionListenerBuilder("Added service to manager", "Adding service to manager failed with code ")
        );
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
        //Store in SharedPrefs
        sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MyPREFERENCES,username);
        editor.apply();
        useradd.setText(username);

    }
}