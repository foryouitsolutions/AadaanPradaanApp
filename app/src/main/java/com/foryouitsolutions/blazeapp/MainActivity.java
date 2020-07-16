package com.foryouitsolutions.blazeapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity implements profileDialog.profileDialogListener, FileAdapter.ActionListener {
    private static final int STORAGE_PERMISSION_CODE = 200;
    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;

    private static final String TAG = "sdsd";
    Button btnSend, btnDiscover;
    TextView useradd, clientHost, conndev;
    private BottomSheetBehavior bottomSheetBehavior;

    private Fetch fetch;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    Handler handler = new Handler();
    boolean handler_running = false;
    final HashMap<String, String> buddies = new HashMap<>();
    HashMap<String, String> buddy_ips = new HashMap<>();
    HashMap<String, Long> buddy_times = new HashMap<String, Long>();
    ListView listView;
    public static final String MyPREFERENCES = "MyPrefs";
    SharedPreferences sharedPreferences;
    private WebServer server;
    private Map<String, Uri> files = new HashMap<>();
    private Map<String, String> downloads = new HashMap<>();
    String devicename;
    String host_server;
    String download_file_url;
    FileAdapter fileAdapter;

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            if (wifiP2pInfo.groupFormed) {
                if (wifiP2pInfo.isGroupOwner) {
                    //Toast.makeText(MainActivity.this, "We are connected to a P2P group as host", Toast.LENGTH_SHORT).show();
                } else if (wifiP2pInfo.groupFormed) {
                    //Toast.makeText(MainActivity.this, "We are connected to a P2P group as client", Toast.LENGTH_SHORT).show();
                }

                host_server = wifiP2pInfo.groupOwnerAddress.getHostAddress();
            }
        }
    };

    void ping_server() {
        new FetchURL().execute(new String[]{"http://" + host_server + ":8080/ping/?devicename=" + devicename});
        new FetchURL().execute(new String[]{"http://" + host_server + ":8080/peers"});
    }

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

    Runnable download_runner(ClipData clipData) {
        return () -> {
            if (host_server.length() == 0) {
                return;
            }

            if (clipData == null) {
                return;
            }

            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new GetUrlContentTask().execute(new Object[]{"http://" + host_server + ":8080/peers", item.getUri()});
                    }
                }, i * 5000);
            }
        };
    }

    Runnable runner2 = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            int group_clients = buddy_ips.size() - 1;
            if (group_clients == -1) {
                group_clients = 0;
            }

            if (group_clients > 0) {
                Map<String, String> map = (Map<String, String>) buddy_ips.clone();
                map.remove(devicename);
                String receiever_name = "";
                for (String name : map.keySet()) {
                    receiever_name = name;
                    break;
                }

                conndev.setText(group_clients + " (" + receiever_name + ")");
            } else {
                conndev.setText(group_clients + "");
            }

            mManager.requestConnectionInfo(mChannel, connectionInfoListener);
            if(host_server != null){
                ping_server();
            }

            handler.postDelayed(this, 5000);
        }
    };

    WifiP2pManager.ActionListener ActionListenerBuilder(final String text, String error) {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: " + text);

            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(MainActivity.this, "Adding service to manager failed with code " + code, Toast.LENGTH_SHORT).show();
            }
        };
    }

    @SuppressLint("MissingPermission")
    void init_discovery() {
        permissionCheck();
        buddies.clear();
        buddy_ips.clear();

        ((TextView) findViewById(R.id.empty)).setText("Long press on the device name to connect to it");

        if (wifiManager == null) {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
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
        // setup connection listener
        if (!handler_running) {
            handler.post(runner2);
            handler_running = true;
            startRegistration(mManager, mChannel, "8081", devicename);
        }

        // discover nearby Wifi Direct services
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up_fast);
        listView = findViewById(R.id.peerListView);
        String[] dataArray = new String[1];
//        TextView emptyText = (TextView) findViewById(R.id.empty);
//        listView.setEmptyView(emptyText);
        listView.setAnimation(scaleUp);
        dataArray[0] = "Searching Nearby Devices...";
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.listview_item, dataArray);
        listView.setAdapter(adapter);


        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance("AadaanPradaanService", "_presence._tcp");
        mManager.addServiceRequest(mChannel,
                serviceRequest,
                ActionListenerBuilder("Service discovery properties set...", "Setting service discovery properties to manager failed with code "));


        mManager.discoverServices(mChannel,
                ActionListenerBuilder("Looking for nearby devices...", "Adding service to manager failed with code "));
    }

    //permission Check
    private void permissionCheck() {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                //Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
                .check();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.custom_actionbar);
        getSupportActionBar().setElevation(0);
        //Initializations
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(MainActivity.this)
                .setDownloadConcurrentLimit(10)
                .enableFileExistChecks(false)
                .enableHashCheck(false)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        btnSend = findViewById(R.id.send);
        btnDiscover = findViewById(R.id.discover);
        useradd = findViewById(R.id.search);
        clientHost = findViewById(R.id.clientHost);
        conndev = findViewById(R.id.connectedDevice);

        final View yourview = findViewById(R.id.yourview);
        LinearLayout linearLayout = findViewById(R.id.bottom_sheet);

        //  ProgressBar progressBarCircle = findViewById(R.id.progressbarcircle);

        //getting SharedPrefs
        sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        devicename = (sharedPreferences.getString(MyPREFERENCES, ""));
        if (devicename.length() == 0) {
            devicename = getString(R.string.app_name) + " " + getRandomNo();
            sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(MyPREFERENCES, devicename);
            editor.apply();
        }

        useradd.setText("Device Name - " + devicename);

        // start webserver
        server = new WebServer();

        //Bottom Sheet
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        fileAdapter = new FileAdapter(MainActivity.this);
        recyclerView.setAdapter(fileAdapter);
        recyclerView.setVisibility(View.VISIBLE);
        fetch.getDownloadsInGroup(0, downloads -> {
            final ArrayList<Download> list = new ArrayList<>(downloads);
            Collections.sort(list, (first, second) -> Long.compare(second.getCreated(), first.getCreated()));
            for (Download download : list) {
                fileAdapter.addDownload(download);
            }
        }).addListener(fetchListener);

        bottomSheetBehavior = BottomSheetBehavior.from(linearLayout);
        //Custom Action Bar
        ActionBar.LayoutParams p = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        p.gravity = Gravity.CENTER;

        //Send Button
        btnSend.setOnClickListener(v -> {

            Toast.makeText(getApplicationContext(), "Choose File", Toast.LENGTH_SHORT).show();
            openFile();
        });
        //Discover Button
        btnDiscover.setOnClickListener(v -> init_discovery());

        //
        // All
        ImageView btndelete = findViewById(R.id.deleteLog);
        btndelete.setOnClickListener(v -> {
            if (deleteDownloads()) {
                Toast.makeText(getApplicationContext(), "Cleared Transfers", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Nothing to Clear in Transfers", Toast.LENGTH_SHORT).show();
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
            buddies.put(GetPreparedDeviceName(device_name), device_address);

            String[] dataArray = buddies.keySet().toArray(new String[0]);
            ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.listview_item, dataArray);
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
            if (!buddies.containsKey(device_name)) {
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

    String GetPreparedDeviceName(String device_name) {
        for (String name : buddy_ips.keySet()) {
            if (name.equals(device_name)) {
                return "[Connected] " + device_name;
            }
        }

        return device_name;
    }

    WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                            WifiP2pDevice resourceType) {

            // Update the device name with the human-friendly version from
            // the DnsTxtRecord, assuming one arrived.
            String device_name = resourceType.deviceName;
            String device_address = resourceType.deviceAddress;
            buddies.put(GetPreparedDeviceName(device_name), device_address);

            String[] dataArray = buddies.keySet().toArray(new String[0]);
            ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, dataArray);
            listView.setAdapter(adapter);
            listView.setOnItemLongClickListener(connectPeer);
            Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
        }
    };

    @SuppressLint("MissingPermission")
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
            case R.id.setDirectory:
                Toast.makeText(this, "Select Transfer Folder", Toast.LENGTH_LONG).show();
                selectDir();
                return true;
            case R.id.about:
                Intent i = new Intent(MainActivity.this,about.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void selectDir() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999);
        }
    }

    private void profile() {
        profileDialog profileDialog = new profileDialog();
        profileDialog.show(getSupportFragmentManager(), "Profile Dialog");
    }

    @Override
    public void applyText(String username) {
        //Store in SharedPrefs
        sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MyPREFERENCES, username);
        editor.apply();
        useradd.setText("Device Name - " + username);
        devicename = username;

    }

    int getRandomNo() {
        return (int) (Math.random() * 1000);
    }

    void download_request_handler(String url, String file_name) {


        File file = new File(file_name);
        saveToFile(url, file);
    }

    final int SAVE_FILE_RESULT_CODE = 15;
    final int OPEN_FILE_RESULT_CODE = 16;

    void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(intent, OPEN_FILE_RESULT_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveToFile(String url, File aFile) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, aFile.getPath());
        Log.d(TAG, "saveToFile: " + aFile.getPath());
        download_file_url = url;
        try {
            startActivityForResult(intent, SAVE_FILE_RESULT_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, requestCode, data);
        switch (requestCode) {
            case SAVE_FILE_RESULT_CODE: {
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    Uri file = data.getData();
                    String file_str = file.getPath();
                    String file_name = file_str.substring(file_str.lastIndexOf('/') + 1);
                    Log.d(TAG, "onActivityResult: " + file_name + downloads.get(file_name));
                    download_file(download_file_url, data.getData(), getApplicationContext());
                }
                break;
            }

            case OPEN_FILE_RESULT_CODE:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    new GetUrlContentTask().execute(new Object[]{"http://" + host_server + ":8080/peers", data.getData()});
                } else if(resultCode == RESULT_OK && data.getClipData() != null){
                    handler.post(download_runner(data.getClipData()));
                }
             break;
            case 9999:
                Log.i("Test", "Result URI " + data.getData());
                break;
        }
    }


    private class GetUrlContentTask extends AsyncTask<Object, Integer, Object[]> {
        protected Object[] doInBackground(Object... args) {
            URL url = null;
            try {
                url = new URL((String) args[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String content = "", line;
                while ((line = rd.readLine()) != null) {
                    content += line;
                }
                return new Object[]{content, args[1]};
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new Object[]{};
        }


        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Object[] result) {
            if (result.length > 0) {
                uploadFile((String) result[0], (Uri) result[1]);
            }
        }
    }


    private class FetchURL extends AsyncTask<String, Integer, String> {
        private boolean update_buddies = false;

        protected String doInBackground(String... args) {
            if (args[0].contains("/peers")) {
                update_buddies = true;
            }

            URL url = null;
            try {
                url = new URL(args[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String content = "", line;
                while ((line = rd.readLine()) != null) {
                    content += line;
                }

                return content;
            } catch (IOException e) {
                e.printStackTrace();
                buddy_ips.clear();
            }

            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(String result) {
            if (update_buddies && result != null) {
                updateBuddiesFromText(result);
            }
        }
    }

    void updateBuddiesFromText(String hosts) {
        Log.d(TAG, "updateBuddiesFromText: " + hosts);
        String[] parts = hosts.split("::?");
        buddy_ips.clear();
        for (int i = 0; i < parts.length; i += 2) {
            buddy_ips.put(parts[i], parts[i + 1]);
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    void uploadFile(String hosts, Uri file_uri) {
        Log.d(TAG, "uploadFile: " + hosts);
        Map<String, String> map = new HashMap<String, String>();
        String[] parts = hosts.split("::?");
        for (int i = 0; i < parts.length; i += 2) {
            map.put(parts[i], parts[i + 1]);
        }

        if (map.size() < 2) {
            Toast.makeText(this, "Group does not have 2 peers!", Toast.LENGTH_SHORT).show();
            return;
        }

        String file_name = getFileName(file_uri);
        file_name = file_name.trim();
        String hash = UUID.randomUUID().toString();
        files.put(hash, file_uri);
        Log.d(TAG, "uploadFile: " + hash + file_uri);

        String sender_ip = map.get(devicename);
        String receiever_ip = "";
        map.remove(devicename);
        for (String name : map.keySet()) {
            receiever_ip = map.get(name);
            break;
        }

        String get_url = "http://" + receiever_ip + ":8080/getfiles/?file_name=" + file_name + "&file_url=http://" + sender_ip + ":8080/file/" + hash;
        Log.d(TAG, "uploadFile: making call " + get_url);
        Toast.makeText(MainActivity.this, "Download request sent!", Toast.LENGTH_SHORT).show();
        new FetchURL().execute(new String[]{get_url});
    }

    void download_file(String fileURL, Uri file_uri, Context activity) {
        try {
            final Request request = new Request(fileURL, file_uri);
            request.setPriority(Priority.HIGH);
            request.setNetworkType(NetworkType.ALL);
            request.setGroupId(0);
            fetch.enqueue(request, updatedRequest -> {
                Toast.makeText(activity, "Starting Transfer", Toast.LENGTH_SHORT).show();
                //Request was successfully enqueued for download.
            }, error -> {
                Toast.makeText(activity, "Transfer Error!" + error.getValue(), Toast.LENGTH_SHORT).show();
                error.getThrowable().printStackTrace();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private class WebServer extends NanoHTTPD {

        public WebServer() {
            super(8080);
            try {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Response serve(IHTTPSession session) {
            String url = session.getUri();

            if (url.contains("/getfiles/")) {
                Map params = session.getParameters();
                List<String> list = (List<String>) params.get("file_url");
                String file_url = list.get(0);
                list = (List<String>) params.get("file_name");
                String file_name = list.get(0);
                download_request_handler(file_url, file_name);
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");
            } else if (url.contains("/file/")) {
                String hash = url.substring(url.lastIndexOf('/') + 1);
                Log.d(TAG, "serving hash " + hash);
                Uri file = files.get(hash);
                ContentResolver provider = getApplicationContext().getContentResolver();

                String range = null;
                Map<String, String> headers = session.getHeaders();
                for (String key : headers.keySet()) {
                    Log.d(TAG, "Header for request " + key + ":" + headers.get(key));
                    if ("range".equals(key)) {
                        range = headers.get(key);
                    }
                }

                try {
                    if (file == null) {
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
                    }

                    InputStream inputStream = provider.openInputStream(file);
                    if (range == null) {
                        return newFixedLengthResponse(Response.Status.OK, "*/*", inputStream, inputStream.available());
                    } else {
                        Log.d(TAG, "serve: " + inputStream.available());
                        return getPartialResponse(range, inputStream, inputStream.available());
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
            } else if (url.contains("/ping/")) {
                String ip = session.getRemoteIpAddress();
                Map params = session.getParameters();
                List<String> names = (List<String>) params.get("devicename");
                Log.d(TAG, "serve:  ping" + names.get(0));
                buddy_ips.put(names.get(0), ip);
                Date date = new Date();
                buddy_times.put(names.get(0), date.getTime());
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok");
            } else if (url.contains("/peers")) {
                String hosts = "";
                Date date = new Date();
                for (String name : buddy_ips.keySet()) {
                    if (date.getTime() - buddy_times.get(name) < 30000) {
                        if (hosts.length() > 0) {
                            hosts += "::";
                        }
                        hosts += name + ":" + buddy_ips.get(name);
                    }
                }

                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, hosts);
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
        }
    }

    NanoHTTPD.Response getPartialResponse(String rangeHeader, InputStream fileInputStream, long fileLength) throws IOException {
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        long start, end;
        boolean do_trick = false;
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1
                    - Long.parseLong(rangeValue.substring("-".length()));
        } else {
            do_trick = true;
            String[] range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = range.length > 1 ? Long.parseLong(range[1])
                    : fileLength - 1;
        }

        if (end > fileLength - 1) {
            end = fileLength - 1;
        }

        if (end < 0 && do_trick) {
            end = 1000000000;
        }

        if (start <= end) {
            long contentLength = end - start + 1;
            fileInputStream.skip(start);
            NanoHTTPD.Response response = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, "*/*", fileInputStream);
            response.addHeader("Content-Length", contentLength + "");
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.addHeader("Content-Type", "*/*");
            return response;
        } else {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, "*/*", rangeHeader);
        }
    }

    private final FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onAdded(@NotNull Download download) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            fileAdapter.addDownload(download);
        }

        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            fileAdapter.update(download, etaInMilliseconds, downloadedBytesPerSecond);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onRemoved(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onDeleted(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }
    };

    @Override
    public void onPauseDownload(int id) {
        fetch.pause(id);
    }

    @Override
    public void onResumeDownload(int id) {
        fetch.resume(id);
    }

    @Override
    public void onRemoveDownload(int id) {
        fetch.remove(id);
    }

    @Override
    public void onRetryDownload(int id) {
        fetch.retry(id);
    }


    public boolean deleteDownloads() {
        List<FileAdapter.DownloadData> downloadsNew = fileAdapter.getDownloads();
        if (downloadsNew.size() == 0) {
            return false;
        }
        for (int i = 0; i < downloadsNew.size(); i++) {
            FileAdapter.DownloadData downloadData = downloadsNew.get(i);
            switch (downloadData.download.getStatus()){
                case PAUSED:
                case QUEUED:
                case ADDED:
                case DOWNLOADING:
                case NONE:
                    break;

                default:
                    fetch.remove(downloadData.download.getId());
            }
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Exit app?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }

}