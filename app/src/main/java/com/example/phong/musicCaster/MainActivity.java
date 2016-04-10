package com.example.phong.musicCaster;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.phong.musicCaster.MusicService.MusicBinder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Imports
//More Imports
//MediaControls

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {
    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;


    //MusicController function
    private MusicController controller;
    private SeekBar progressbar;
    private boolean paused=false, playbackPaused=false;
    private ImageButton pauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageView albumArt;
    private TextView minisongTitle;

    //Notification
    private String songTitle="";
    private static final int NOTIFY_ID=1;


    private SongHolder songAdt;

    //Testing Class
    private Intent songIntent;

    private ImageView mSelectedTrackImage;
    private Bitmap songImage = null;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        songList = new ArrayList<Song>();
        songView = (ListView)findViewById(R.id.song_list);

        //Working Code;
        this.getSongList();

//
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getSongTitle().compareTo(b.getSongTitle());
            }
        });

        SongHolderV2 songAdt = new SongHolderV2(this, songList);
        songView.setAdapter(songAdt);

        MediaBarCreation();
        initControllerView();
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
        if (resultCode == Activity.RESULT_CANCELED) {
            playPrev();
        }
    }
        public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        //Searches content from the Primary external storage volume
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                //shuffle
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.action_settings:
                break;
            case R.id.action_broadcast:
                Intent openBroadcast = new Intent(this,BroadcastScreen.class);
                startActivity(openBroadcast);
                break;
            case R.id.action_Receiver:
                Intent openReceiver = new Intent(this,ReceiverScreen.class);
                startActivity(openReceiver);
                break;
            case R.id.action_SongSelector:
                Intent openSongSelector = new Intent(this,SongCollectionParcelable.class);
                startActivity(openSongSelector);
        }
        return super.onOptionsItemSelected(item);
    }

    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        displayMusicBar();
    }

    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    private void setController(){
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }


    @Override
    public void start() {
        displayMusicBar();
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPlaying())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPlaying())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPlaying();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    protected void onPause(){
        super.onPause();
        paused=true;
    }

    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused = false;
        }
    }

    protected void onStop() {
        super.onStop();
    }

    public void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            playbackPaused=false;
        }
        displayMusicBar();
    }

    public void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            playbackPaused=false;
        }
        displayMusicBar();
    }

    private void initControllerView(){
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
                } else {
                    pause();
                }
            }
        });

        previousButton = (ImageButton)findViewById(R.id.previous_button);
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

//        minisongTitle = (TextView)findViewById(R.id.miniSong_title);
//        minisongTitle.setText(songList.get(musicSrv.getSongIndex()).getSongTitle());
    }

    public void onSongCompletion(){
        albumArt = new ImageView(this);
        minisongTitle = new TextView(this);
        albumArt = (ImageView) findViewById(R.id.miniAlbumArt_Image);
        albumArt.setImageBitmap(songList.get(musicSrv.getSongIndex()).getArtWork());

   //     minisongTitle = (TextView)findViewById(R.id.miniSong_title);
    //    minisongTitle.setText(songList.get(musicSrv.getSongIndex()).getSongTitle());
    }

    private void MediaBarCreation(){
        albumArt = new ImageView(this);
        minisongTitle = new TextView(this);
        previousButton = new ImageButton(this);
        pauseButton = new ImageButton(this);
        nextButton = new ImageButton(this);
    }

    private class fetchAlbumArt extends AsyncTask<Void,Void,Boolean> {
        private Context context;
        public fetchAlbumArt(Context context){
            this.context = context;
        }

        MediaMetadataRetriever mediaMetadataRetriever;
        Bitmap tempImage = null;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mediaMetadataRetriever = new MediaMetadataRetriever();
            tempImage = null;
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            try{
                getSongList();
                return true;
            }catch (Exception e){
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Collections.sort(songList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    return a.getSongTitle().compareTo(b.getSongTitle());
                }
            });

            SongHolderV2 songAdt = new SongHolderV2(context, songList);
            songView.setAdapter(songAdt);

        }
    }
}
