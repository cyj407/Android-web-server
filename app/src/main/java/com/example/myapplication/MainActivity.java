package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.example.myapplication.WebServer.MyServer;
import com.example.myapplication.WebServer.SocketServer;

import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final static String ip = "192.168.137.184";
    private final static int port = 3000;

    // web server -> used to build Android Web Server
    private MyServer myWebServer;

    // web socket(server) -> used to transmit data as the server
    private SocketServer myWebSocketServer;

    @Override
    public void onResume() {
        super.onResume();

        try {
            myWebServer = new MyServer(this);
            Log.e("MainActivity.onResume", "WebServer started");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MainActivity.onResume", "WebServer start failed" + e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            myWebSocketServer.stop();
        }
        catch (Exception e) {
            Log.e("MainActivity.OnPause", "Fail to stop");
        }

        if(myWebServer != null) {
            myWebServer.closeAllConnections();
            myWebServer = null;
            Log.e("MainActivity.onPause", "app pause, so web server close");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            myWebSocketServer = new SocketServer(new InetSocketAddress(ip, port));
            myWebSocketServer.start();
            // handle the accidental close
            myWebSocketServer.setReuseAddr(true);
        } catch(Exception e) {
            Log.e("MainActivity.onCreate", "Exception");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            myWebSocketServer.stop();
        }
        catch (Exception e) {
            Log.e("MainActivity.OnPause", "Fail to stop");
        }
    }
}
