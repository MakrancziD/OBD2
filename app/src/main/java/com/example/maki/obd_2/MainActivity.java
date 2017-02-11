package com.example.maki.obd_2;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.maki.obd_2.utils.BluetoothManager;
import com.example.maki.obd_2.utils.CustomObdCommand;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnSend;
    private EditText editCommand;
    private ListView commandList;
    private final List<String> items = new ArrayList<>();

    private static final int REQUEST_ENABLE_BT = 1111;
    private static final String TAG = "MyActivity";
    private BluetoothSocket btSocket;
    private BluetoothAdapter btAdapter;

    private ArrayAdapter<String> itemsAdapter=null;

    private boolean isBluetoothOn=false;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        InitBluetoothConnection();

        commandList = (ListView) findViewById(R.id.listView);

        items.add("Test");
        itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        commandList.setAdapter(itemsAdapter);
        btnSend = (Button) findViewById(R.id.button);
        editCommand = (EditText) findViewById(R.id.editText);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                items.add(editCommand.getText().toString());
                runCommand(new CustomObdCommand(editCommand.getText().toString()));
            }
        });


    }

    private void runCommand(ObdCommand command)
    {
        try {
            command.run(btSocket.getInputStream(), btSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        items.add(command.getResult());
        itemsAdapter.notifyDataSetChanged();
    }

    private void initObdService()
    {
        runCommand(new ObdResetCommand());
        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
        runCommand(new EchoOffCommand());
        runCommand(new LineFeedOffCommand());
        runCommand(new TimeoutCommand(62));
        runCommand(new SelectProtocolCommand(ObdProtocols.AUTO));
        runCommand(new AmbientAirTemperatureCommand());

    }

    private void InitBluetoothConnection()
    {
        if(CheckBluetoothAdapter())
        {
            SelectBtDevice();
        }
        else
        {
            ShowToast("Unable to access Bluetooth!");
        }
    }

    private boolean CheckBluetoothAdapter()
    {
        Context context = getApplicationContext();

        if(btAdapter==null)
        {
            ShowToast("Bluetooth not supported on your device! Go Fuck yourself!");
        }
        else if(!btAdapter.isEnabled()) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                    dialog.setTitle("Bluetooth turned off")
                    .setMessage("Do you want to enable Bluetooth?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(turnOn, REQUEST_ENABLE_BT);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
        else { isBluetoothOn=true;}

        return isBluetoothOn;
    }

    private boolean SelectBtDevice() {
        //TODO: store default device and try to connect to it automatically on startup

        List<String> deviceInfo = new ArrayList<>();

        final List<BluetoothDevice> pairedDevices = new ArrayList<>();
        pairedDevices.addAll(btAdapter.getBondedDevices());

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceInfo.add(device.getName() + " - " + device.getAddress());
            }
        }

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceInfo.toArray(new String[deviceInfo.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition(); //isn't it the which parameter??
                BluetoothDevice chosenDevice = pairedDevices.get(position);

                if (btSocket != null && btSocket.getRemoteDevice().equals(chosenDevice)) {
                    ShowToast("Device already connected!");
                    dialog.dismiss();
                } else {
                    new LoadBtSocketAsync(chosenDevice).execute(null, null, null);
                }
            }
        }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                initObdService();
            }
        }).setNeutralButton("Discover devices", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (btAdapter.isDiscovering()) {
                            btAdapter.cancelDiscovery();
                        }

                        btAdapter.startDiscovery();

                        final BroadcastReceiver bReciever = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String action = intent.getAction();
                                if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                    pairedDevices.add(device);
                                }
                            }
                        };

                    }
                }
        );

        alertDialog.show();

        return true;
    }



//
//    private void SaveDeviceAddress(String deviceAddress) throws IOException {
//
//
//        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
//
//        new ConnectThread(device).start();
//
//        //waiting for socket... HACK
//        for(int i=0;i<30;i++)
//        {
//            if(btSocket!=null)
//            {
//                break;
//            }
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        ShowToast("Connection socket acquired");
//
////        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
////
////        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
////
////        socket.connect();
//    }
//
//    private class ConnectThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final BluetoothDevice mmDevice;
//
//        public ConnectThread(BluetoothDevice device) {
//            // Use a temporary object that is later assigned to mmSocket
//            // because mmSocket is final.
//            BluetoothSocket tmp = null;
//            mmDevice = device;
//
//            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//
//            try {
//                // Get a BluetoothSocket to connect with the given BluetoothDevice.
//                // MY_UUID is the app's UUID string, also used in the server code.
//                tmp = device.createRfcommSocketToServiceRecord(uuid);
//            } catch (IOException e) {
//                Log.e(TAG, "Socket's create() method failed", e);
//            }
//            mmSocket = tmp;
//        }
//
//        public void run() {
//            // Cancel discovery because it otherwise slows down the connection.
//            btAdapter.cancelDiscovery();
//
//            try {
//                // Connect to the remote device through the socket. This call blocks
//                // until it succeeds or throws an exception.
//                mmSocket.connect();
//            } catch (IOException connectException) {
//                // Unable to connect; close the socket and return.
//                try {
//                    mmSocket.close();
//                } catch (IOException closeException) {
//                    Log.e(TAG, "Could not close the client socket", closeException);
//                }
//                return;
//            }
//
//            // The connection attempt succeeded. Perform work associated with
//            // the connection in a separate thread.
//
//            btSocket=mmSocket;
//        }
//
//        // Closes the client socket and causes the thread to finish.
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the client socket", e);
//            }
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode==REQUEST_ENABLE_BT)
        {
            if(resultCode==RESULT_OK)
            {
                isBluetoothOn=true;
            }
        }
    }

    private void ShowToast(String msg)
    {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private class LoadBtSocketAsync extends AsyncTask<String, String, String>
    {
        private BluetoothDevice bt;
        private ProgressDialog loadingDialog;

        public LoadBtSocketAsync(BluetoothDevice bt)
        {
            this.bt=bt;
        }

        @Override
        protected void onPreExecute()
        {
            loadingDialog = ProgressDialog.show(MainActivity.this, "Connecting to device", "Please wait...", true);
            loadingDialog.setCancelable(true);
        }

        @Override
        protected String doInBackground(String... params) {
            BluetoothSocket tmpSocket =  BluetoothManager.Connect(bt);
            return tmpSocket==null?"REKT":"OK";
        }

        @Override
        protected void onPostExecute(String s)
        {
            loadingDialog.dismiss();
            ShowToast(s.equals("OK")?"Connected":"REKT");
        }
    }
}
