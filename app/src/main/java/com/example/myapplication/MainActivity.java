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
        Intent intent = new Intent(this, ServerService.class);
        startService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        Intent intent = new Intent(this, ServerService.class);
        stopService(intent);
        Log.e("MainActivity.onPause", "WebServer pauses.");

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
