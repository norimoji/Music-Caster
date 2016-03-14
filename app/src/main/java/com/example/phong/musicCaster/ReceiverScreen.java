package com.example.phong.musicCaster;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by Phong on 02/03/2016.
 */
public class ReceiverScreen extends Activity{
    private BroadcastService broadcastService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.broadcast_list);
        broadcastService = new BroadcastService(this,handler);
    }

    private final Handler handler = new Handler() {};

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastService != null){
            if(broadcastService.getState() == BroadcastService.STATE_NONE){

                broadcastService.start();
            }
        }
    }
}



