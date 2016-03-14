package com.example.phong.musicCaster;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast; //Enable toast popups

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.logging.LogRecord;


/**
 * Created by Phong on 02/12/2015.
 */

public class BroadcastScreen extends ActionBarActivity{
    private BluetoothAdapter bluetoothAdapter = null;
    private ListView availableBluetoothList;
    private List bluetoothList;
    private ArrayList<Song>queuedSongs;
    private ArrayList<String>anotherList;
    private BroadcastService broadcastService = null;
    private ReceiverScreen receiverScreen;
    private Toolbar toolbar = null;
    /**
     *  Connected device name
     */
    private String ConnectedDeviceName = null;

    private static final String TAG = "From BroadcastScreen";
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;
    private static final int REQUEST_CONNECTION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.broadcast_list);
        //Added the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.broadcastToolBar);
        setSupportActionBar(toolbar);
        toolbar.setSubtitle("BOOM");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Intent listOfDevices = new Intent(this,ListOfDevices.class);
        startActivityForResult(listOfDevices, REQUEST_CONNECTION);

        //Enable new activity for selecting songs
//        Intent getSongList = new Intent(this, SongCollectionParcelable.class);
//        final int result = 1;
//        getSongList.putExtra("selectedSongs", "BroadcastScreen");
//        startActivityForResult(getSongList, result);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!bluetoothAdapter.isEnabled()){
            //Enable new activity turning on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        broadcastService = new BroadcastService(this,handler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastService != null){
            if(broadcastService.getState() == BroadcastService.STATE_NONE){

                broadcastService.start();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String a = Integer.toString(requestCode);
        if(requestCode == REQUEST_CONNECTION) {
            Log.d(TAG, a);
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data);
                Log.d(TAG, "just received intent " + data.getPackage() + " " + data.getDataString() + "From ListOfDevice");
            }
        }
       }


    private void connectDevice(Intent data) {
        // Get the device MAC address
        String deviceAddress = data.getExtras()
                .getString(ListOfDevices.EXTRA_DEVICE_ADDRESS);
        Log.v(TAG,deviceAddress);
        //Check if bluetooth address is correct
        Log.v(TAG, "Attempt to connect " + String.valueOf(bluetoothAdapter.checkBluetoothAddress(deviceAddress)));
        Log.v(TAG, "Attempting to connect to device: " + deviceAddress);
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        // Attempt to connect to the device
        Log.v(TAG, "Attempting to try connect and connect with device object: " + deviceAddress);
        if(bluetoothAdapter.checkBluetoothAddress(deviceAddress)){
            Log.v(TAG, "Device object value: " + device.toString() + " " + device.getName());
            broadcastService.connect(device);
        }
        Log.v(TAG, "Finished connecting device with : " + deviceAddress);

    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(String subTitle) {
      if (BroadcastScreen.this== null){
        return;
        }
      final ActionBar actionBar = this.getActionBar();
    if(null == actionBar){
        return;
    }
        toolbar.setSubtitle(subTitle);
    }


    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BroadcastService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BroadcastService.STATE_CONNECTING:
                            setStatus("connecting...");
                            break;
                        case BroadcastService.STATE_WAITING:
                            setStatus("waiting...");
                            break;
                        case BroadcastService.STATE_NONE:
                            setStatus("not connected");
                            break;
                    }
            }
        }
    };
}
