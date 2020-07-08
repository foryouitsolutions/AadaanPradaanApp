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
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

public class MainActivity extends AppCompatActivity implements profileDialog.profileDialogListener {
    private static final String TAG = "sdsd";
    Button btnSend, btnReceive;
    TextView useradd;
    private BottomSheetBehavior bottomSheetBehavior;
    WifiManager manager;
    private WifiManager.LocalOnlyHotspotReservation mReservation;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    Handler handler = new Handler();
    final HashMap<String, ServiceDevice> buddies = new HashMap<>();
    ListView listView;

    private class ServiceDevice {
        public String device_name;
        public WifiP2pDevice device;
    }


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                Log.d(TAG, "onConnectionInfoAvailable: as HOST");
            } else if (wifiP2pInfo.groupFormed) {
                Log.d(TAG, "onConnectionInfoAvailable: as client");
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

        // init service button
        ((Button) findViewById(R.id.service_init)).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                mChannel = mManager.initialize(getApplicationContext(), getMainLooper(), null);
                // todo cleanup existing services
                // add local service
                Toast.makeText(getApplicationContext(), "Inciting discovery service...", Toast.LENGTH_SHORT).show();
                startRegistration(mManager, mChannel, "8081");

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
                        new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(MainActivity.this, "Service discovery properties set...", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int code) {
                                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                                Toast.makeText(MainActivity.this, "Setting service discovery properties to manager failed with code " + code, Toast.LENGTH_SHORT).show();
                            }
                        });

                mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                                         Toast.makeText(MainActivity.this, "Discovering services called...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        Toast.makeText(MainActivity.this, "Adding service to manager failed with code " + code, Toast.LENGTH_SHORT).show();
                    }
                    });
            }
        });


        //Send Button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Discovering...", Toast.LENGTH_SHORT).show();

                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
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

                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                Toast.makeText(getApplicationContext(), "starting..." + wifiManager.isP2pSupported(), Toast.LENGTH_SHORT).show();
                wifiManager.setWifiEnabled(true);
                mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
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
            ServiceDevice new_device = buddies.containsKey((device.deviceAddress))?buddies.get(device.deviceAddress): new ServiceDevice();
            new_device.device_name = (String) record.get("device_name");
            buddies.put(device.deviceAddress, new_device);

            String[] dataArray = new String[buddies.size()];
            Set keys = buddies.keySet();
            Iterator<String> iterator = keys.iterator();
            int i= 0;
            while(iterator.hasNext()){
                String deviceAddress = iterator.next();
                dataArray[i++] = buddies.get(deviceAddress).device_name;
            }

            ArrayAdapter adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1, dataArray );
            listView.setAdapter(adapter);
        }
    };

    WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                            WifiP2pDevice resourceType) {

            // Update the device name with the human-friendly version from
            // the DnsTxtRecord, assuming one arrived.

            ServiceDevice new_device = buddies
                    .containsKey(resourceType.deviceAddress) ? buddies
                    .get(resourceType.deviceAddress) : new ServiceDevice();
            new_device.device = resourceType;
            buddies.put(resourceType.deviceAddress, new_device);
            String[] dataArray = new String[buddies.size()];
            Set keys = buddies.keySet();
            Iterator<String> iterator = keys.iterator();
            int i= 0;
            while(iterator.hasNext()){
                String deviceAddress = iterator.next();
                dataArray[i++] = buddies.get(deviceAddress).device_name;
            }

            ArrayAdapter adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1, dataArray );
            listView.setAdapter(adapter);
            Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
        }
    };

    private void startRegistration(WifiP2pManager manager, WifiP2pManager.Channel channel, String SERVER_PORT) {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", SERVER_PORT);
        record.put("device_name", "John Doe" + (int) (Math.random() * 1000));
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

        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Added service to manager", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(MainActivity.this, "Adding service to manager failed with code " + arg0, Toast.LENGTH_SHORT).show();
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