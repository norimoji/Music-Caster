package com.example.phong.musicCaster;

/**
 * Created by Phong on 02/12/2015.
 */
public class BluetoothList {

    private long id;
    private String name;


    public BluetoothList(long bluetoothID, String bluetoothName) {
        id=bluetoothID;
        name=bluetoothName;

    }

    public long getID(){return id;}
    public String getName(){return name;}

}
