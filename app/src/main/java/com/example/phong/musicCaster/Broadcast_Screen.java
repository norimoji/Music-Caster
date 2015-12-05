package com.example.phong.musicCaster;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by Phong on 02/12/2015.
 */

public class Broadcast_Screen extends BaseAdapter {
    private ArrayList<BluetoothList> listOfBlueooth;
    private LayoutInflater bluetoothInf;

    public Broadcast_Screen(Context c, ArrayList<BluetoothList> bluetoothUsers) {
        listOfBlueooth = bluetoothUsers;
        bluetoothInf = LayoutInflater.from(c);

    }

    @Override
    public int getCount() {
        return listOfBlueooth.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }


//      public View getView(int position, View convertView, ViewGroup parent) {
//        LinearLayout songLay = (LinearLayout)bluetoothInf.inflate
//                (R.layout.song, parent, false);
//        //get title and artist views
//        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
//        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
//        //get song using position
//        Song currSong = songs.get(position);
//        //get title and artist strings
//        songView.setText(currSong.getTitle());
//        artistView.setText(currSong.getArtist());
//        //set position as tag
//        songLay.setTag(position);
//        return songLay;
//    }
}

