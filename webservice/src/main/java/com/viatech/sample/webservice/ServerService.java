package com.viatech.sample.webservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.net.InetSocketAddress;

public class ServerService extends Service {

    private final static String ip = "192.168.137.184";
    private final static int port = 9000;

    // web server -> used to build Android Web Server
    private MyServer myWebServer;

    // web socket(server) -> used to transmit data as the server
    private SocketServer myWebSocketServer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("MyServerService", "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MyServerService", "onStartCommand()");

        if(myWebServer == null) {
            try {
                myWebServer = new MyServer(this);
                myWebSocketServer = new SocketServer(new InetSocketAddress(ip, port), this);

                myWebSocketServer.start();

                // handle the accidental close
                myWebSocketServer.setReuseAddr(true);

            } catch (Exception e) {
                Log.e("MainActivity.onCreate", "Exception");
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("MyServerService", "onDestroy()");

        // stop web socket service
        try {
            myWebSocketServer.stop();
        } catch (Exception e) {
            Log.e("MyServerService", "Fail to stop");
        }

        // close web server connections
        if(myWebServer != null) {
            myWebServer.closeAllConnections();
            myWebServer = null;
            Log.e("MyServerService", "App pauses, so web server close");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
