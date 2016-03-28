package com.example.phong.musicCaster;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Phong on 24/02/2016.
 */
public class BroadcastService {
    // Debugging
    private static final String TAG = "BroadcastService";

    // Unique UUID for this application
    private static final UUID A_UNIQUE_INDENTIFER = UUID.fromString("884ffcb7-01f6-45f9-af59-699ac9c9d9b2");

    // Member fields
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler threadHandler;
    private AcceptThread acceptThread;
    private ConnectToDeviceThread connectToDeviceThread;
    private ConnectedThread connectedThread;
    /**
     * States for defining the current stage the application is in
     */
    private int currentState;

    //Constants for various of states
    public static final int STATE_NONE = 0;       //Neither waiting or connected to a connection
    public static final int STATE_WAITING = 1;    //Awaiting for a incoming connection
    public static final int STATE_CONNECTING = 2; //Preparing for initiation with outgoing connection;
    public static final int STATE_CONNECTED = 3;  //Successfully connected with slave device;

    private byte[] convertedByteArray = null;
    private BroadcastScreen broadcastScreen = null;

    private int currentHandlingState;

    public static final int NOT_HANDLING_PACKAGE = 0;
    public static final int HANDLING_PACKAGE = 1;

    public BroadcastService(Context context, Handler handler) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        currentState = STATE_NONE;
        threadHandler = handler;
        currentHandlingState = NOT_HANDLING_PACKAGE;
    }
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + currentState + " -> " + state);
        currentState = state;

        // Give the new state to the Handler so the UI Activity can update
       threadHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return currentState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start " + " @synchronized void start");
        // Cancel any thread currently running a connection
        if (connectToDeviceThread != null) {
            connectToDeviceThread.cancel();
            connectToDeviceThread = null;
            Log.v(TAG,"Canceled connectToDeviceThread");
        }

        setState(STATE_WAITING);
        Log.v(TAG, "Set State to WAITING");

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null) {
           acceptThread = new AcceptThread();
           acceptThread.start();
            Log.v(TAG, "Restarting acceptThread");
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     //param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if(currentState == STATE_CONNECTING){
            if(connectToDeviceThread != null){
              connectToDeviceThread.cancel();
                connectToDeviceThread = null;
        }

    }
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

//        // Start the thread to connect with the given device
//        mConnectThread = new ConnectThread(device, secure);
//        mConnectThread.start();
//        setState(STATE_CONNECTING);
//    }

    connectToDeviceThread = new ConnectToDeviceThread(device);
    connectToDeviceThread.start();
    setState(STATE_CONNECTING);}
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected " + " @synchronized void connected");

        // Cancel the thread that completed the connection
        Log.v(TAG,"Canceled connectToDeviceThread");
        if(connectToDeviceThread != null){
            connectToDeviceThread.cancel();
            connectToDeviceThread = null;
        }

        Log.v(TAG, "Canceled connectedThread");
        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
            Log.v(TAG, "Canceled acceptThread");
            if (acceptThread != null) {
                acceptThread.cancel();
                acceptThread = null;
            }


        // Start the thread to manage the connection and perform transmissions
        Log.v(TAG, "Create a connectedThread(socket) and start it");
         connectedThread = new ConnectedThread(socket);
         connectedThread.start();

        // Send the name of the connected device back to the UI Activity

        Message msg = threadHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        threadHandler.sendMessage(msg);


        setState(STATE_CONNECTED);
            Log.v(TAG,"Set State to CONNECTED");
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop " + " @synchronized void stop");
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
            Log.v(TAG,"Canceled connectedThread");
        }
        if(connectToDeviceThread != null) {
           connectToDeviceThread.cancel();
           connectToDeviceThread = null;
            Log.v(TAG,"Canceled connectToDeviceThread");
        }
        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
            Log.v(TAG,"Canceled acceptThread");
        }
        setState(STATE_NONE);
        Log.v(TAG, "Set State to NONE");
    }

    /**
     * Use write method in ConnectedThread for transmission
     *
     * @param out The bytes to write
     //see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (currentState!= STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        Log.d(TAG, "We're writing out");
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = threadHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        threadHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BroadcastService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = threadHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        threadHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BroadcastService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread{
        // Local ServerSocket
        private final BluetoothServerSocket localServerSocket;

        private AcceptThread() {
            BluetoothServerSocket tempServerSocket = null;

            //Creata a new listening serverSocket
            try{
                tempServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(TAG, A_UNIQUE_INDENTIFER);
            }catch (IOException e) {
                Log.e(TAG, "listen failed",e);
            }
            localServerSocket = tempServerSocket;
    }

        public void run(){
            Log.d(TAG,"Begin AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (currentState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = localServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BroadcastService.this) {
                        switch (currentState) {
                            case STATE_WAITING:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.v(TAG, "END AcceptedThread");
        }

        public void cancel() {
            Log.d(TAG, "Socket Type cancel" + this);
            try {
               localServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectToDeviceThread extends Thread{
        private final BluetoothSocket localServerSocket;
        private final BluetoothDevice tempBluetoothDevice;

        public ConnectToDeviceThread(BluetoothDevice device){
            tempBluetoothDevice = device;
            BluetoothSocket tempBluetoothSocket = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tempBluetoothSocket = device.createRfcommSocketToServiceRecord(A_UNIQUE_INDENTIFER);
                } catch (IOException e){
                Log.e(TAG, "Creation failed",e);
            }
            localServerSocket = tempBluetoothSocket;
        }

        public void run(){
            Log.i(TAG, "Begin ConnectToDeviceThread");

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            Log.v(TAG, "About to localServerSocket.connect()");
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                localServerSocket.connect();
                Log.d(TAG, "establishing a localServerSocket");
            } catch (IOException e) {
                // Close the socket
                try {
                    localServerSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG,"socket during connection failure", e2);
                }
                Log.v(TAG, "LocalServerSocket has failed, commencing connectionFailed ");
                connectionFailed();
                return;
            }
            Log.v(TAG, "LocalServerSocket: " + localServerSocket.toString() + " BluetoothDevice: " +
                    tempBluetoothDevice.toString());

            // Reset the ConnectThread because we're done
            synchronized (BroadcastService.this) {
                    connectToDeviceThread = null;
                Log.v(TAG, "Setting connectToDeviceThread to null");
            }

            // Start the connected thread
            connected(localServerSocket, tempBluetoothDevice);
            Log.v(TAG, "Starting the connected Thread");
        }
        public void cancel() {
            try {
                localServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect ", e);
            }
        }
    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread{
        /**
         * Debugging Purpose
         */
        private static final String className = "ConnectedThread";

        /**
         * Bluetooth Attributes
         */
        private final BluetoothSocket connectedSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

       //BluetoothA2dp bluetoothA2dp;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(className, "Creating a connected thread");
            connectedSocket = socket;
            InputStream tempThreadIn = null;
            OutputStream tempThreadOut= null;

            try{
                tempThreadIn = socket.getInputStream();
                tempThreadOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(className, "temp sockets not created", e);
            }

            connectedInputStream = tempThreadIn;
            connectedOutputStream = tempThreadOut;
        }

        @Override
        public void run() {
            Log.i(className, "Commence connected thread");
            byte[] buffer = new byte[16 * 1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                //Intent intent = Intent.getIntent("card-package"); // receive intent from broadcastScreen as file object to be converted into bytearray.
                try {
                    Log.d(TAG,"Start the reading - connectedThread out method");
                    // Read from the InputStream
                    bytes = connectedInputStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    threadHandler.obtainMessage(Constants.MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(className, "disconnected", e);
                    connectionLost();

                    //Restart the broadcastService back to initial state(Accepting)
                    BroadcastService.this.start();
                    break;
                }
            }
        }

    public void write(byte[] buffer){
        try {
            Log.d(TAG,"Started Writing out - connectedThread write method");
            connectedOutputStream.write(buffer);
        } catch (IOException e) {
             Log.e(TAG, "Exception happen during write", e);
        }
    }

        public void cancel(){
            try {
                connectedSocket.close();
            } catch (IOException e) {
                Log.e(className, "Closed due to connect socket failed", e);
            }
        }
    }
}




