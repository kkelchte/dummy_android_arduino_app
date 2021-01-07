package com.example.dummy_android_arduino_app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class UsbConnection {
    private static final int USB_VENDOR_ID = 6790; // 0x2341; // 9025
    private static final int USB_PRODUCT_ID = 29987; // 0x0001;

    private UsbManager usbManager;
    PendingIntent usbPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "UsbConnection.USB_PERMISSION";

    private UsbDeviceConnection connection;
    private UsbSerialDevice serialDevice;
    private LocalBroadcastManager mLocalBroadcastManager;
    private String buffer = "";
    private Context context;
    private int baudRate;
    private boolean busy;
    private int vendorId;
    private int productId;
    private String productName;
    private String deviceName;
    private String manufacturerName;

    public UsbConnection(Context context, int baudRate) {
        this.context = context;
        this.baudRate = baudRate;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this.context);
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        usbPermissionIntent =
                PendingIntent.getBroadcast(this.context, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    private UsbSerialInterface.UsbReadCallback callback =
            new UsbSerialInterface.UsbReadCallback() {
                @Override
                public void onReceivedData(byte[] data) {
                    try {
                        String dataUtf8 = new String(data, "UTF-8");
                        buffer += dataUtf8;
                        int index;
                        while ((index = buffer.indexOf('\n')) != -1) {
                            final String dataStr = buffer.substring(0, index).trim();
                            buffer = buffer.length() == index ? "" : buffer.substring(index + 1);

                            AsyncTask.execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            onSerialDataReceived(dataStr);
                                        }
                                    });
                        }
                    } catch (UnsupportedEncodingException e) {

                    }
                }
            };

    private final BroadcastReceiver usbPermissionReceiver =
            new BroadcastReceiver() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (ACTION_USB_PERMISSION.equals(action)) {
                        synchronized (this) {
                            UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if (usbDevice != null) {
                                    // call method to set up device communication
                                    startSerialConnection(usbDevice);
                                }
                            } else {
                                Toast.makeText(
                                        UsbConnection.this.context,
                                        "USB Host permission is required!",
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        }
                    }
                }
            };

    private final BroadcastReceiver usbDetachedReceiver =
            new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            stopUsbConnection();
                        }
                    }
                }
            };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean startUsbConnection() {
        IntentFilter usbPermissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
        mLocalBroadcastManager.registerReceiver(usbPermissionReceiver, usbPermissionFilter);
        // Detach events are sent as a system-wide broadcast
        IntentFilter usbDetachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mLocalBroadcastManager.registerReceiver(usbDetachedReceiver, usbDetachedFilter);

        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
        if (!connectedDevices.isEmpty()) {
            for (UsbDevice usbDevice : connectedDevices.values()) {
                // if (usbDevice.getVendorId() == USB_VENDOR_ID && usbDevice.getProductId() ==
                // USB_PRODUCT_ID) {
                if (usbManager.hasPermission(usbDevice)) {
                    return startSerialConnection(usbDevice);
                } else {
                    usbManager.requestPermission(usbDevice, usbPermissionIntent);
                    Toast.makeText(context, "Please allow USB Host connection.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                // }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean startSerialConnection(UsbDevice device) {
        connection = usbManager.openDevice(device);
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        boolean success = false;
        if (serialDevice != null) {
            if (serialDevice.open()) {
                vendorId = device.getVendorId();
                productId = device.getProductId();
                productName = device.getProductName();
                deviceName = device.getDeviceName();
                manufacturerName = device.getManufacturerName();
                serialDevice.setBaudRate(baudRate);
                serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialDevice.read(callback);
                success = true;
            }
        } else {
        }
        return success;
    }

    private void onSerialDataReceived(String data) {
        // Add whatever you want here
        mLocalBroadcastManager.sendBroadcast(
                new Intent("usb.data_received")
                        .putExtra("from", "usb")
                        .putExtra("data", data));
        Toast.makeText(this.context, "Received " + data,
                Toast.LENGTH_SHORT).show();
    }

    public void stopUsbConnection() {
        try {
            if (serialDevice != null) {
                serialDevice.close();
            }

            if (connection != null) {
                connection.close();
            }
        } finally {
            serialDevice = null;
            connection = null;
        }
        mLocalBroadcastManager.unregisterReceiver(usbPermissionReceiver);
        mLocalBroadcastManager.unregisterReceiver(usbDetachedReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void send(String msg) {
        busy = true;
        serialDevice.write(msg.getBytes(UTF_8));
        busy = false;
    }

    public boolean isOpen() {
        return connection != null;
    }

    public boolean isBusy() {
        return busy;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }
}
