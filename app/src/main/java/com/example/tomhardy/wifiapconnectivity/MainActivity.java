package com.example.tomhardy.wifiapconnectivity;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.lang.reflect.InvocationTargetException;


public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager = null;
    private boolean deviceWifiState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        deviceWifiState = wifiManager.isWifiEnabled();
        Log.d("Main","onCreate");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Main","onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Main","onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("Main","onDestroy");

        try {   // to RESTORE hotspot configuration, before exiting
            wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(wifiManager, null, false);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        wifiManager.setWifiEnabled(deviceWifiState);
        wifiManager = null;

        System.gc();
        System.exit(0);
    }

    public void Sender(View view) {
        startActivity(new Intent(MainActivity.this, Sender.class));
    }

    public void Receiver(View view) {
        startActivity(new Intent(MainActivity.this, Receiver.class));
    }
}