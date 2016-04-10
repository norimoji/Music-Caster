package com.example.phong.musicCaster;


import android.graphics.Bitmap;

/**
 * Created by Phong on 19/11/2015.
 */
public class Song {

    private long songID;
    private String songTitle;
    private String songArtist;
    private Bitmap albumArtwork = null;

    public Song(long ID, String title, String artist, Bitmap artwork){
        songID = ID;
        songTitle = title;
        songArtist = artist;
        albumArtwork = artwork;

    }

    public long getSongID() {
        return songID;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public Bitmap getArtWork(){return albumArtwork;}
}


