package com.example.phong.musicCaster;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.phong.musicCaster.MusicService.MusicBinder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_ID_PERMISSIONS = 1;
    private boolean storageNotGranted = true;
    private ArrayList<Song> songList;
    private ListView songView;
    private BluetoothAdapter bluetoothAdapter = null;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    //MediaPlayer Controls
    private boolean playbackPaused = false;
    private ImageButton pauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageView albumArt;
    private TextView minisongTitle;
    private SongHolderV2 songAdt;

    //AlbumArt Related
    private ImageView mSelectedTrackImage;
    private Bitmap songImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(checkStoragePermission()){
            startMusicPlayer();
        }
    }

    public boolean checkStoragePermission(){
        int permissionReadStorageMessage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if(permissionReadStorageMessage != PackageManager.PERMISSION_GRANTED){
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            startMusicPlayer();
        }
        if(!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_PERMISSIONS);
            return false;
        }
        return true;

    }
    public void startMusicPlayer(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        songList = new ArrayList<Song>();
        songView = (ListView)findViewById(R.id.song_list);

        this.getSongList();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getSongTitle().compareTo(b.getSongTitle());
            }
        });

        SongHolderV2 songAdt = new SongHolderV2(this, songList);
        songView.setAdapter(songAdt);

        MediaBarCreation();
        playbackController();
       if(storageNotGranted) {
           storageNotGranted = false;
           finish();
           startActivity(getIntent());
       }
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setListOfSongs(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == 4){
            if(bluetoothAdapter.isEnabled()) {
                Intent openBroadcast = new Intent(this, BroadcastScreen.class);
                startActivity(openBroadcast);
            }
        }
        if(requestCode == 3) {
            if (bluetoothAdapter.isEnabled()){
                Intent openReceiver = new Intent(this, ReceiverScreen.class);
                startActivity(openReceiver);
            }
        }

    }

    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(musicSrv.isShuffle()){
            menu.getItem(1).setIcon(R.drawable.shuffleonbutton);
        }else{
            menu.getItem(1).setIcon(R.drawable.shuffleoffbutton);
        }

        if(musicSrv.isRepeating()){
            menu.getItem(0).setIcon(R.drawable.repeatonbutton);
        }else{
            menu.getItem(0).setIcon(R.drawable.repeatoffbutton);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                    musicSrv.setShuffle();
                invalidateOptionsMenu();
                break;
            case R.id.action_repeat:
                musicSrv.setRepeat();
                invalidateOptionsMenu();
                break;
            case R.id.action_exit:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.action_broadcast:
                if (!bluetoothAdapter.isEnabled()) {
                    //Enable new activity turning on Bluetooth
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 4);
                }else if(bluetoothAdapter.isEnabled()) {
                    Intent openBroadcast = new Intent(this, BroadcastScreen.class);
                    startActivity(openBroadcast);
                }
                break;
            case R.id.action_Receiver:
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 3);
                }else if(bluetoothAdapter.isEnabled()) {
                    Intent openReceiver = new Intent(this, ReceiverScreen.class);
                    startActivity(openReceiver);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        //Searches content from the Primary external storage volume
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        System.gc();

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
                long thisAlbumArtwork = musicCursor.getLong(musicCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

                Bitmap bitmap = null;
                Uri sArtworkUri = Uri
                        .parse("content://media/external/audio/albumart");
                Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, thisAlbumArtwork);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), albumArtUri);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 90, 90, true);

                } catch (FileNotFoundException exception) {
                    exception.printStackTrace();
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.invalidcoverart);
                } catch (IOException e) {

                    e.printStackTrace();
                }
                songList.add(new Song(thisId, thisTitle, thisArtist, bitmap));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        pauseButton.setImageResource(R.drawable.pausebutton);
        displayMusicBar();
        musicSrv.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (musicSrv.isRepeating()) {
                    mp.setLooping(true);
                    mp.start();
                } else {
                    mp.stop();
                    playNext();
                    displayMusicBar();
                }
            }
        });
    }

    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }


    public void start() {
        displayMusicBar();
        musicSrv.go();
    }

    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPlaying();
        return false;
    }

    public void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            playbackPaused = false;
        }
        displayMusicBar();
        pauseButton.setImageResource(R.drawable.pausebutton);
        musicSrv.getMediaPlayer().setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(musicSrv.isRepeating()){
                    mp.setLooping(true);
                    mp.start();
                }
            }
        });
    }



    public void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            playbackPaused = false;
        }
        displayMusicBar();
        pauseButton.setImageResource(R.drawable.pausebutton);
        musicSrv.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (musicSrv.isRepeating()) {
                    mp.setLooping(true);
                    mp.start();
                }
            }
        });
    }


    private void playbackController(){
        nextButton = (ImageButton) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                playNext();
            }
        });

        pauseButton = (ImageButton) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying() != true) {
                    start();
                    pauseButton.setImageResource(R.drawable.pausebutton);
                } else {
                    pause();
                    pauseButton.setImageResource(R.drawable.play);
                }
            }
        });

        previousButton = (ImageButton) findViewById(R.id.previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
    }

    private void displayMusicBar(){
        albumArt = (ImageView) findViewById(R.id.miniAlbumArt_Image);
        albumArt.setImageBitmap(songList.get(musicSrv.getSongIndex()).getArtWork());

        minisongTitle = (TextView)findViewById(R.id.miniSong_title);
        minisongTitle.setText(songList.get(musicSrv.getSongIndex()).getSongTitle());
    }

    private void MediaBarCreation(){
        albumArt = new ImageView(this);
        minisongTitle = new TextView(this);
        previousButton = new ImageButton(this);
        pauseButton = new ImageButton(this);
        nextButton = new ImageButton(this);
    }
}
