package com.viatech.sample.webservice;

import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;

public class SocketServer extends WebSocketServer {

    private String uploadFileName = "";
    private String requestFile = "";
    public SocketServer(InetSocketAddress addr) {
        super(addr);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // send a message to the new client
        conn.send("Hi, client!");
        Log.e("SocketServer.onOpen", "new connection to " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conn.close();
        Log.e("SocketServer.onClose", "closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.e("SocketServer.onMessage","received message from " + conn.getRemoteSocketAddress() + ": " + message);

        if(Pattern.matches("file: .*", message)) {
            uploadFileName = message.substring(6);
        }
        else if(Pattern.matches("client request: .*", message)) {
            conn.send("Server receives the request.");
            requestFile = message.substring(16);

            try {
                File inFile = new File(requestFile);
                FileInputStream fis = new FileInputStream(inFile);
                ByteBuffer buf = ByteBuffer.allocate((int) inFile.length());
                int _byte;
                while((_byte = fis.read()) != -1) {
                    buf.put((byte) _byte);
                }
                buf.flip();         // set position to 0
                conn.send(buf);     // Synchronize
            }catch (Exception e) {
                e.printStackTrace();
                conn.send("Server sent failed.");
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message)
    {
        super.onMessage(conn, message);

        if(message != null) {
            // show the buffer info
            Log.e("onMessage", message.toString());
        }

        // save files from client
        try {
            File file = new File("/sdcard/" + uploadFileName);
            FileChannel fc = new FileOutputStream(file, false).getChannel();
            fc.write(message);
            fc.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        conn.send("server saved the file!");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if(conn != null) {
            Log.e("Socket Server", "an error occurred on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        }
        Log.e("SocketServer.onError", "an error occurred on connection: " + ex);
    }

    @Override
    public void onStart() {
        Log.e("SocketServer.onStart", "Server started successfully.");
    }

}
