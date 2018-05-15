package com.example.tomhardy.wifiapconnectivity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;

import android.net.wifi.ScanResult;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Sender extends AppCompatActivity {

    boolean isConnected = false;
    private OnItemLongClickListener onItemLongClickListener;

    ArrayList<String> devices = new ArrayList<>();
    List<ScanResult> scanResults;

    private ListView listView = null;
    private WifiManager wifiManager = null;
    private BroadcastReceiver broadcastReceiver = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        onItemLongClickListener = new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                String ssid = devices.get(position);

                WifiConfiguration wifiConfiguration = new WifiConfiguration();

                wifiConfiguration.SSID = "\"" + ssid + "\"";
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                int netId = wifiManager.addNetwork(wifiConfiguration);

                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);

                if (wifiManager.reconnect()) {
                    Log.d("calling", "ClientAsyncTask");
                    new ClientAsyncTask().execute();
                }

                return true;
            }
        };

        listView = (ListView) findViewById(R.id.list);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        listView.setOnItemLongClickListener(onItemLongClickListener);

        if (!wifiManager.isScanAlwaysAvailable()) {
            startActivity(new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE));
        }

        if (!wifiManager.isWifiEnabled())
            setWifiEnabled(true);

        broadcastReceiver = new WifiApReceiver();

        wifiManager.startScan();

        Log.d("sender", "onCreate");
    }

    private void gotoTransferIntent(Socket socket) {

        Intent dataTransferIntent = new Intent(Sender.this, DataTransfer.class);
        dataTransferIntent.putExtra("TYPE", "Sender");
        SocketHandler.setSocket(socket);
        startActivity(dataTransferIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.unregisterReceiver(broadcastReceiver);
        Log.d("sender", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();

//        wifiManager.startScan();
        this.registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (DataTransfer.isTransferComplete)
        {
            DataTransfer.isTransferComplete=false;
            finish();
        }
        Log.d("sender", "onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("Sender","onDestroy");

//        wifiManager.disconnect();

        listView = null;
        wifiManager = null;
        broadcastReceiver = null;
    }

    public void refresh(View view) {
//        Log.d("con info in main",wifiManager.getConnectionInfo()+"");
//        new ClientAsyncTask().execute();
        wifiManager.startScan();
        this.registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));            // again scan on refresh is pressed
//        Toast.makeText(getApplicationContext(), "Scanning....", Toast.LENGTH_SHORT).show();
    }

    private void setWifiEnabled(boolean enabled)        // to enable wifi when needed
    {

        try {
            if ((Boolean) wifiManager.getClass().getMethod("isWifiApEnabled").invoke(wifiManager)) {
                wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(wifiManager, null, false);
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        wifiManager.setWifiEnabled(enabled);
    }

    //
    public class WifiApReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            scanResults = wifiManager.getScanResults();     // declaration must be in class
//            scanResults = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults();       // declaration must be in class
            int size = scanResults.size();
            devices.clear();

            Log.d("sender", String.valueOf(size));
            while (size > 0) {
                size--;
                final String ssid = scanResults.get(size).SSID;

//                    HashMap<String, String> item = new HashMap<String, String>();
//                    item.put(ITEM_KEY,  + "  " + scanResults.get(size).capabilities + " , " +scanResults.get(size).BSSID );
                if (ssid.contains("My_Hotspot")) {
                    Log.d("sender", ssid);
                    devices.add(ssid);     // declaration and memory allocation must be in class
                }

            }

            Log.d("devices", String.valueOf(devices));
            listView.setAdapter(new ArrayAdapter(Sender.this, android.R.layout.simple_list_item_1, devices.toArray()));     // alternative to notifydatasetchange (using new ArrayAdapter for setting adapter)
//            devices.clear();
            scanResults = null;
            Log.d("devices", String.valueOf(devices));
        }
    }

    class ClientAsyncTask extends AsyncTask<Void, Boolean, Void> {

        @Override
        protected Void doInBackground(Void... params) {
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                ConnectivityManager connManager = null;
//                Network[] networks = null;
//                boolean connectedToWifi = false;
//
//                while (!connectedToWifi) {          // waiting a to get connected with Receiver's Hotspot before establishment of socket
//                    connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//                    networks = connManager.getAllNetworks();
//
//                    for (Network network : networks) {
////                        Log.d("statusFinal", "" + connManager.getNetworkInfo(network).getTypeName() + " , " + connManager.getNetworkInfo(network).isConnected());
//                        if (connManager.getNetworkInfo(network).getTypeName().equalsIgnoreCase("wifi") && connManager.getNetworkInfo(network).isConnected()) {
//                            connectedToWifi = true;
//
//                            publishProgress(true);
//                            try {
//                                Thread.sleep(4000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//            } else {
                publishProgress(true);
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//            }


            try {
                Socket socket = new Socket("192.168.43.1", 13333);
                Log.d("sender", "socket is created");
                publishProgress(false,false);
                gotoTransferIntent(socket);

            } catch (UnknownHostException e) {
                e.printStackTrace();
                publishProgress(false,true);
            } catch (IOException e) {
                e.printStackTrace();
                publishProgress(false,true);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            listView.setEnabled(!values[0]);
            if (values[0]) {
                ((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.VISIBLE);
            } else {
                ((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.GONE);
                ((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.GONE);
                if (values[1])
                {
                    ((TextView) findViewById(R.id.textStatus)).setText("connection time out");
                }
            }
        }
    }
}