package com.example.dummy_android_arduino_app;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends Activity {
    private static Context mContext;

    // **** USB **** //
    protected UsbConnection usbConnection;
    protected boolean usbConnected;
    protected SwitchCompat connectionSwitchCompat;

    public static Context getContext() {
        return mContext;
    }

    // **** Steering buttons ******* //
    Button straightButton, leftButton, rightButton;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.settings_activity);
        straightButton = (Button) findViewById(R.id.straightButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void connectUsb() {
        usbConnection = new UsbConnection(this, 9600);
        usbConnected = usbConnection.startUsbConnection();
    }

    private void disconnectUsb() {
        if (usbConnection != null) {
            usbConnection.stopUsbConnection();
            usbConnection = null;
        }
        usbConnected = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public synchronized void onDestroy() {
        toggleConnection(false);
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void toggleConnection(boolean isChecked) {
        if (isChecked) {
            connectUsb();
        } else {
            disconnectUsb();
        }

        if (usbConnected) {
            connectionSwitchCompat.setText(usbConnection.getProductName());
            Toast.makeText(getContext(), "Connected.", Toast.LENGTH_SHORT).show();
        } else {
            connectionSwitchCompat.setText("No Device");
            // Tried to connect but failed
            if (isChecked) {
                Toast.makeText(getContext(), "Please check the USB connection.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Disconnected.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendStraight(View view) {
        if (usbConnected){
            Toast.makeText(getContext(), "Sending 1.", Toast.LENGTH_SHORT).show();
            usbConnection.send("1");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendLeft(View view) {
        if (usbConnected){
            Toast.makeText(getContext(), "Sending 0.", Toast.LENGTH_SHORT).show();
            usbConnection.send("0");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendRight(View view) {
        if (usbConnected){
            Toast.makeText(getContext(), "Sending 2.", Toast.LENGTH_SHORT).show();
            usbConnection.send("2");
        }
    }


}



