package com.viatech.sample.webservice;

import android.os.Environment;
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
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

public class SocketServer extends WebSocketServer {

    private String uploadFileName = "";
    private String requestFile = "";
    private ArrayList<File> allFileList;

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

        if(message.equals("hi, server")) {
            Log.e("SocketServer.onMessage","get the sdcard structure.");

            allFileList = new ArrayList<>();
            File sdcardPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            getFileList(sdcardPath);

            removeDirFromList();

            String listMsg = allFileList.toString().
                    replace(Environment.getExternalStorageDirectory().getAbsolutePath(),"/sdcard");
            conn.send("option:" + listMsg);
        }
        else if(Pattern.matches("file: .*", message)) {
            uploadFileName = message.substring(6);
        }
        else if(Pattern.matches("client request: .*", message)) {
            conn.send("Server receives the request.");
            requestFile = message.substring(16);

            try {
                conn.send("File starts.");

                File inFile = new File(requestFile);
                FileInputStream fis = new FileInputStream(inFile);

                ByteBuffer buf = ByteBuffer.allocate(8192);
                byte byteArray[] = new byte[8192];
                int readLen;
                while((readLen = fis.read(byteArray)) != -1) {
                    buf.put(byteArray, 0, readLen);

                    buf.flip();         // set position to 0
                    conn.send(buf);     // Synchronize
                }

                conn.send("File ends.");
            }catch (Exception e) {
                e.printStackTrace();
                conn.send("Server sent failed.");
            }
        }
    }

    public void removeDirFromList() {
        Vector<File> removeFileList = new Vector<>();

        // get remove index
        for(Integer i = 0;i < allFileList.size(); ++i) {
            if(allFileList.get(i).isDirectory()) {
                removeFileList.add(allFileList.get(i));
            }
        }

        // remove file from allFileList
        for(int i = 0;i < removeFileList.size(); ++i) {
            allFileList.remove(removeFileList.get(i));
        }
    }


    public ArrayList<File> getFileList(File dirName) {
        File curDirfileList[] = dirName.listFiles();
        if(curDirfileList != null && curDirfileList.length > 0) {
            for(int i = 0;i < curDirfileList.length; ++i) {
                if(curDirfileList[i].isDirectory()) {
                    if(curDirfileList[i].getName().equals("crash")) {
                        continue;
                    }
                    allFileList.add(curDirfileList[i]);
                    getFileList(curDirfileList[i]);
                }
                else {
                    if(curDirfileList[i].getName().endsWith(".png") ||
                    curDirfileList[i].getName().endsWith(".jpg") ||
                    curDirfileList[i].getName().endsWith(".xml") ||
                    curDirfileList[i].getName().endsWith(".txt") ||
                    curDirfileList[i].getName().endsWith(".mp4") ||
                    curDirfileList[i].getName().endsWith(".webm") ||
                    curDirfileList[i].getName().endsWith(".ogg")) {

                        allFileList.add(curDirfileList[i]);
                    }
                }
            }
        }
        return allFileList;

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
