package com.example.phong.musicCaster;


/**
 * Created by Phong on 19/11/2015.
 */
public class Song {

    private long songID;
    private String songTitle;
    private String songArtist;

    public Song(long ID, String title, String artist) {
        songID = ID;
        songTitle = title;
        songArtist = artist;

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

}


