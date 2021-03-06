package com.example.dummy_android_arduino_app;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Arrays;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {
    Button mButton;
    private static Context mContext;
    protected Spinner baudRateSpinner;

    // **** USB **** //
    protected UsbConnection usbConnection;
    protected boolean usbConnected;
    public int[] BaudRates = {9600, 14400, 19200, 38400, 57600, 115200, 230400, 460800, 921600};
    private int baudRate = 9600;
    protected SwitchCompat connectionSwitchCompat;

    public static Context getContext() {
        return mContext;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.settings_activity);
        baudRateSpinner = findViewById(R.id.baud_rate_spinner);
        mButton = (Button) findViewById(R.id.mButton);
        mButton.setEnabled(true);
        connectionSwitchCompat = findViewById(R.id.connection_switch);
        baudRateSpinner.setSelection(Arrays.binarySearch(BaudRates, baudRate));
        toggleConnection(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void connectUsb() {
        usbConnection = new UsbConnection(this, baudRate);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //if (parent == baudRateSpinner) {
        this.baudRate = Integer.parseInt(parent.getItemAtPosition(position).toString());
        Toast.makeText(getContext(), "Set baudrate: " +  this.baudRate, Toast.LENGTH_SHORT).show();
        //}
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void onClick(View view) {
        Toast.makeText(getContext(), "Sending 6.", Toast.LENGTH_SHORT).show();
        usbConnection.send("6");
    }
}



