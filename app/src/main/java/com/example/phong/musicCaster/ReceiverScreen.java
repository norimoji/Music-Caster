package com.example.phong.musicCaster;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Phong on 02/03/2016.
 */
public class ReceiverScreen extends AppCompatActivity {
    // Debugging
    private static final String TAG = "ReceiverScreen";

    // MediaController
    private MusicController playBackController;

    private BroadcastService broadcastService = null;

    private MediaPlayer bluetoothMediaPlayer = null;

    private String mConnectedDeviceName = null;

    private String uriLink = "";

    private ImageView playPauseController;

    //Constant used to time states
    private int currentSessionStatus;
    public static final int notInSession = 0;
    public static final int inSession = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiver_screen);
        broadcastService = new BroadcastService(this, handler);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        currentSessionStatus = 0;


        playPauseController = (ImageView)findViewById(R.id.togglePlaybackController);

        playPauseController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

    }
    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(int subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    private void setStatusWithString(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BroadcastService.STATE_CONNECTED:
                            String tempName = getString(R.string.title_connected_to) + mConnectedDeviceName;
                            setStatusWithString(tempName);
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
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readbuffer = (byte[]) msg.obj;
                    Log.d(TAG, "Whats is in the buffer? " + readbuffer);
                    String subString = new String(readbuffer);
                    uriLink += subString;
                    Log.d(TAG, "Name of subString" + subString);
                    TextView textView = (TextView)findViewById(R.id.musicplayer);
                    textView.setText(uriLink);
                    Log.d(TAG, "setting up MediaPlayer with subString");
                    setUpMediaPlayer(readbuffer);
                    Log.d(TAG, "MediaPlayer Finished setting up");
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    }
                    break;
            }
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastService != null) {
            if (broadcastService.getState() == BroadcastService.STATE_NONE) {
                broadcastService.start();
            }
        }
    }

    private void togglePlayPause() {
        if (bluetoothMediaPlayer.isPlaying()) {
            bluetoothMediaPlayer.pause();
            playPauseController.setImageResource(R.drawable.play);
        } else {
            bluetoothMediaPlayer.start();
            playPauseController.setImageResource(R.drawable.end);
        }
    }

//    public File convertByteArrayToFile(byte[] bytes) {
//        File file = new File("something.mp3");
//        try {
//            FileOutputStream fileOutputStream = new FileOutputStream(file);
//            fileOutputStream.write(bytes);
//            fileOutputStream.close();
//            return file;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public int getSessionState(){
        return currentSessionStatus;
    }

//    public void go(Context context){
//        Uri trackUri = Uri.parse(uriLink);
//        Log.d(TAG,"This is the Uri link" + trackUri.toString());
//
//        Log.d(TAG,"Initialise the MediaPlayer");
//        bluetoothMediaPlayer = new MediaPlayer();
//        try {
//            Log.d(TAG, "setAudioStreamType to AudioManager.STREAM_MUSIC");
//                 bluetoothMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            Log.d(TAG, "setDataSource with trackUri");
//                 bluetoothMediaPlayer.setDataSource(context, trackUri);
//            Log.d(TAG, "prepare");
//            bluetoothMediaPlayer.prepare();
//        } catch (IOException e) {
//            Log.e(TAG, "Problem in backgroundThread within ReceiverScreen", e);
//        }
//    }

    private class backgroundThread implements Runnable {
        public void run() {
            while(broadcastService.getState() == BroadcastService.STATE_CONNECTED){
                Log.d(TAG,"Is thread running?");

            }
        }
    }

    public void setUpMediaPlayer(byte[] byteArray) {

        try {
            File tempFile = File.createTempFile("temp",".mp3",getCacheDir());
            tempFile.deleteOnExit();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            bufferedOutputStream.write(byteArray);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            Log.d(TAG,"Where is the tempFile located? " + tempFile.getAbsolutePath());
            Log.d(TAG, "How big is the file? " + tempFile.length());

             Log.d(TAG,"Setting PreparedAsync and Data Source with ");
            FileInputStream fileInputStream = new FileInputStream(tempFile);

            if(fileInputStream.getFD() != null) {
                Log.d(TAG, "Creating MediaPlayer");
                bluetoothMediaPlayer = new MediaPlayer();
                bluetoothMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                bluetoothMediaPlayer.setDataSource(fileInputStream.getFD());
                fileInputStream.close();


                bluetoothMediaPlayer.prepare();
                bluetoothMediaPlayer.start();

            }
        } catch (IOException e) {
            Log.e(TAG,"Problem occurred in setUpMediaPlayer",e);
        }
    }
}