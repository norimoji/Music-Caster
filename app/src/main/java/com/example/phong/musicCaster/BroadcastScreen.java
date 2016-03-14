package com.example.phong.musicCaster;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * Created by Phong on 02/12/2015.
 */

public class BroadcastScreen extends ActionBarActivity{
    /**
     *  Attributes relating to bluetooth and sharing functionality
     **/
    private BluetoothAdapter bluetoothAdapter = null;
    private BroadcastService broadcastService = null;

    /**
     *  Attributes relating to the list of songs
     */
    private ListView songView;
    private ArrayList<Song>queuedSongs;
    private ArrayAdapter<Song>songArrayAdapter;
    private Button submitButton;
    /**
     *  Connected device name & and debugging purposes
     */
    private String ConnectedDeviceName = null;
    private static final String TAG = "From BroadcastScreen";

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;
    private static final int REQUEST_CONNECTION = 1;

    /**
     * Attributes relating to this class
     */
    private Toolbar toolbar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.songcollection);
        //Added the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        //Used to change the title of the ActionBar
        getSupportActionBar().setTitle("Select a song fool!");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Intent listOfDevices = new Intent(this,ListOfDevices.class);
        startActivityForResult(listOfDevices, REQUEST_CONNECTION);

        //Creates a list of songs
        queuedSongs = new ArrayList<Song>();
        songView = (ListView) findViewById(R.id.Song_Collection);
        this.getSongList();


        Collections.sort(queuedSongs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getSongTitle().compareTo(b.getSongTitle());
            }
        });

        SongHolder songAdt = new SongHolder(this, queuedSongs);
        songView.setAdapter(songAdt);

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
    private void setStatus(int subTitle) {
        final ActionBar actionBar = this.getActionBar();
    if(null == actionBar){
        return;
    }
        toolbar.setTitle(subTitle);
    }

    private void setStatusWithString(CharSequence subTitle){
        final ActionBar actionBar = this.getActionBar();
        if(null == actionBar){
            return;
        }
        toolbar.setTitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BroadcastService
     */
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BroadcastService.STATE_CONNECTED:
                            setStatusWithString(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BroadcastService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BroadcastService.STATE_WAITING:
                            setStatus(R.string.title_waiting);
                            break;
                        case BroadcastService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
            }
        }
    };

    /**
     *  list of song functions
     **/
    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                queuedSongs.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    private void findViewsByID() {
        submitButton = (Button) findViewById(R.id.Submit);
    }

    //@Override
    public void onClick(View v) {
        SparseBooleanArray songSelected = songView.getCheckedItemPositions();
        ArrayList<Song> selectedSongList = new ArrayList<Song>();
        for (int i = 0; i < songSelected.size(); i++) {
            //Song Position in the songList
            int songPosition = songSelected.keyAt(i);
            // Add song IF the song is selected
            if (songSelected.valueAt(i))
                selectedSongList.add(songArrayAdapter.getItem(songPosition));


        }

    }

    /**
     * The on-click listener for song picked
     * selectedSongList is used for transmitting multiple songs
     */
    public void songPicked(View view) {
        //ArrayList<String> selectedSongList = new ArrayList<String>();
        Uri allsongsuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String songPath = allsongsuri.getPath();
        //selectedSongList.add(songPath);

        Intent returnToBroadcastScreen = new Intent();
        returnToBroadcastScreen.putExtra("chosenSong", songPath);

        //Set a result for another activity to receive and finish this activity
        setResult(RESULT_OK, returnToBroadcastScreen);
        Log.d(TAG, "Packaging Media Path: " + songPath);

    //  startActivity(new Intent(this,BroadcastScreen.class));
    //  finish();
        Log.d(TAG,"Switching to Broadcasting screen with : " + songPath);
    }
}