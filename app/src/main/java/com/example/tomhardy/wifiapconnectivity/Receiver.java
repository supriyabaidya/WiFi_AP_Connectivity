package com.example.tomhardy.wifiapconnectivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Receiver extends AppCompatActivity {

    final private int appVersion = Build.VERSION.SDK_INT;
    final private int HOTSPOT_SETTINGS_REQUEST_CODE = 1;

    private HomeWatcher mHomeWatcher = null;
    private boolean isHomePressed;

    WifiManager wifiManager = null;
    WifiConfiguration wifiApConfiguration = null, deviceWifiApConfiguration = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                isHomePressed = true;
            }

            @Override
            public void onHomeLongPressed() {
            }
        });
        mHomeWatcher.startWatch();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        try {       // getting device hotspot Configuration,no need to memory allocation
            deviceWifiApConfiguration = (WifiConfiguration) wifiManager.getClass().getMethod("getWifiApConfiguration").invoke(wifiManager);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        wifiApConfiguration = new WifiConfiguration();      // must needed for restoring old hotspot configuration , memory allocation is needed
        wifiApConfiguration.SSID = deviceWifiApConfiguration.SSID + "My_Hotspot";
        wifiApConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        setWifiApEnabled(true);

        waitingForSender();

//        gotoTransferIntent();

        Log.d("Receiver", "onCreate");
    }

    private void gotoTransferIntent(Socket socket) {
        Intent dataTransferIntent = new Intent(Receiver.this, DataTransfer.class);
        dataTransferIntent.putExtra("TYPE", "Receiver");
        SocketHandler.setSocket(socket);
        startActivity(dataTransferIntent);
    }

    private void waitingForSender() {
        new ServerAsyncTask().execute();

//        new Thread(new Runnable() {     // ServerSocket is getting started in thread
//            @Override
//            public void run() {
//                try {
//                    ServerSocket serverSocket = new ServerSocket(13337);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((TextView) findViewById(R.id.textView)).setText("serverSocket is created.\nWaiting For Sender");
//                        }
//                    });
//                    Log.d("serverSocket", "is created");
//                    while (true) {
//                        //Server is waiting for client here
//                        Socket socket = serverSocket.accept();
//                        gotoTransferIntent(socket);
//                        break;
//                    }
//                    serverSocket.close();
//                    Log.d("socket", "is created");
//
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((TextView) findViewById(R.id.textView)).setText("serverSocket is not created.");
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((TextView) findViewById(R.id.textView)).setText("serverSocket is not created.");
//                        }
//                    });
//                }
//            }
//        }).start();


    }

    @Override
    protected void onPause() {
        super.onPause();

        mHomeWatcher.stopWatch();

        if (isHomePressed) {
//            Log.d("onPause", String.valueOf(deviceWifiState) + "isHomePressed");
            try {   // to RESTORE Wifi & hotspot configuration, before exiting

                wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class).invoke(wifiManager, deviceWifiApConfiguration);

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        Log.d("Receiver", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHomeWatcher.startWatch();
        if (isHomePressed) {
            try {   // to RESTORE Wifi & hotspot configuration, before exiting

                wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class).invoke(wifiManager, wifiApConfiguration);

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            isHomePressed = false;
        }

        if (DataTransfer.isTransferComplete)
        {
            DataTransfer.isTransferComplete=false;
            finish();
        }

        Log.d("Receiver", "onResume");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("Receiver", "onDestroy");
        try {   // to RESTORE hotspot configuration, before exiting

            wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class).invoke(wifiManager, deviceWifiApConfiguration);
            wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(wifiManager, deviceWifiApConfiguration, false);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setWifiApEnabled(boolean enabled) {        // to enable hotspot when needed
        wifiManager.setWifiEnabled(false);

        if (appVersion >= Build.VERSION_CODES.N) {

            new BackgroundAsyncTask(HOTSPOT_SETTINGS_REQUEST_CODE).execute();      // option 2 , for checking whether 'hotspot' is ON

            try {
                wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class).invoke(wifiManager, wifiApConfiguration);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            final Intent hotspotSettingsIntent = new Intent(Intent.ACTION_MAIN, null);
            final ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");

            hotspotSettingsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            hotspotSettingsIntent.setComponent(componentName);
            hotspotSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            showDialog("", "Android 7 or above prohibits auto start hotspot,set manually", hotspotSettingsIntent, HOTSPOT_SETTINGS_REQUEST_CODE, this);
        } else {
            try {
                wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(wifiManager, wifiApConfiguration, enabled);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    // can't show dialog on async task or background
    private void showDialog(final String title, final String message, final Intent intent, final int requestCode, final Context context) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(false);
                builder.setTitle(title);
                builder.setMessage(message);

                builder.setPositiveButton("goto setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        startActivityForResult(intent, requestCode);
                    }
                });

                builder.show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_CANCELED) {
            switch (requestCode) {
                case HOTSPOT_SETTINGS_REQUEST_CODE:

                    try {
                        if (!(Boolean) wifiManager.getClass().getMethod("isWifiApEnabled").invoke(wifiManager)) {
                            final Intent hotspotSettingsIntent = new Intent(Intent.ACTION_MAIN, null);
                            final ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");

                            hotspotSettingsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            hotspotSettingsIntent.setComponent(componentName);
                            hotspotSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                            showDialog("", "Android 7 or above prohibits auto start hotspot,set manually", hotspotSettingsIntent, HOTSPOT_SETTINGS_REQUEST_CODE, this);

                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

        return;
    }

    class BackgroundAsyncTask extends AsyncTask<Void, Void, Void> {
        private int option = -1;

        public BackgroundAsyncTask(int option) {
            this.option = option;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected Void doInBackground(Void... params) {

            switch (option) {
                case HOTSPOT_SETTINGS_REQUEST_CODE:

                    try {
                        while (!(Boolean) wifiManager.getClass().getMethod("isWifiApEnabled").invoke(wifiManager) && !isCancelled())
                            ;

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    break;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            finishActivity(option);
        }
    }

    class ServerAsyncTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(13333);
                publishProgress("serverSocket is created");
                Log.d("serverSocket", "is created");
                while (true) {
                    //Server is waiting for client here
                    Socket socket = serverSocket.accept();

                    gotoTransferIntent(socket);
                    break;
                }
                serverSocket.close();
                Log.d("socket", "is created");
                publishProgress("Socket is created");

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            ((TextView) findViewById(R.id.textView)).setText(values[0]);
        }
    }
}