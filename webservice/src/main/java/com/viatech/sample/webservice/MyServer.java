package com.viatech.sample.webservice;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fi.iki.elonen.NanoHTTPD;

public class MyServer extends NanoHTTPD {
    private final static int PORT = 8080;
    private Context _mainContext;

    // turn on the http service
    public MyServer(Context context) throws IOException {
        super(PORT);
        _mainContext = context;
        start();
        Log.e("MyServer", "Android web server is running ...");
    }

    // the entrance of the command and the exit from here
    @Override
    public Response serve(IHTTPSession session) {

        // return the path part of the URL
        String uri = session.getUri();
        Log.e("MyServer", "load: "+ uri);
        String filename = uri.substring(1);

        if(uri.equals("/")) {
            filename = "index.html";
        }

        boolean isAscii = true;
        String mimetype = "text/html";
        if(filename.contains(".html") || filename.contains("htm")) {
            mimetype = "text/html";
            isAscii = true;
        }
        else if(filename.contains(".js")) {
            mimetype = "text/javascript";
            isAscii = true;
        }
        else if(filename.contains(".css")) {
            mimetype = "text/css";
            isAscii = true;
        }
        else if(filename.contains(".gif")) {
            mimetype = "text/gif";
            isAscii = false;
        }
        else if(filename.contains(".jpeg") || filename.contains(".jpg")) {
            mimetype = "text/jpeg";
            isAscii = false;
        }
        else if(filename.contains(".png")) {
            mimetype = "text/png";
            isAscii = false;
        }
        else {
            filename = "index.html";
            mimetype = "text/html";
        }


        if(isAscii) {
            String response = "";
            String line = "";
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(_mainContext.getAssets().open(filename)));

                while ((line = reader.readLine()) != null) {
                    response += line;
                    response += "\n";
                }

                reader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimetype, response);
        }
        else {
            InputStream isr;
            try {
                isr = _mainContext.getAssets().open(filename);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimetype, isr, isr.available());
            } catch (IOException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimetype, "");
            }
        }

    }
}