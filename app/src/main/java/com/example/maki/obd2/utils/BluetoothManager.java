package com.example.maki.obd2.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Maki on 2017. 01. 29..
 */

public class BluetoothManager {

    private static final String TAG = BluetoothManager.class.getName();
    private static final UUID UUID_SELF = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Nullable
    public static BluetoothSocket Connect(BluetoothDevice btDevice)
    {
        BluetoothSocket socket = null;

        try {
            socket=btDevice.createRfcommSocketToServiceRecord(UUID_SELF);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return socket;
    }
}
