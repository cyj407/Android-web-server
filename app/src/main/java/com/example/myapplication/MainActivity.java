package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.viatech.sample.webservice.ServerService;


public class MainActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();
        Log.e("MainActivity.onResume", "WebServer resumes.");
        ServerService.resume = true;
        Intent intent = new Intent(this, ServerService.class);
        startService(intent);
//        try {
//            myWebServer = new MyServer(this);
//            Log.e("MainActivity.onResume", "WebServer resumes.");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e("MainActivity.onResume", "WebServer start failed" + e.getMessage());
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Intent intent = new Intent(this, ServerService.class);
        stopService(intent);
        Log.e("MainActivity.onPause", "WebServer pauses.");

//        try {
//            myWebSocketServer.stop();
//        }
//        catch (Exception e) {
//            Log.e("MainActivity.OnPause", "WebServer pauses.");
//        }
//
//        if(myWebServer != null) {
//            myWebServer.closeAllConnections();
//            myWebServer = null;
//            Log.e("MainActivity.onPause", "app pause, so web server close");
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, ServerService.class);
        this.startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, ServerService.class);
        stopService(intent);
    }
}
