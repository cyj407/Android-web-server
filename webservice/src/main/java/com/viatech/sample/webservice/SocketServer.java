package com.viatech.sample.webservice;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

public class SocketServer extends WebSocketServer {

    private Context context;
    private String uploadFileName = "";
    private String requestFile = "";
    private ArrayList<File> allFileList;

    private BufferedReader sizeReader;
    private BufferedReader dataReader;
    private InputStream inputStream;

    private final String file264DataName = "car.h264";
    private final String file264SizeName = "car.txt";

    Thread thread;
    Lock mutex;

    private Timer timer = new Timer();
    private Queue<ByteBuffer> queue;



    public SocketServer(InetSocketAddress addr, Context context) {
        super(addr);
        this.context = context;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // send a message to the new client
        conn.send("Hi, client!");
        Log.e("SocketServer.onOpen", "new connection to " + conn.getRemoteSocketAddress());

        queue = new LinkedList<>();
    }

    public void acceptFrame(ByteBuffer buf) {
        mutex.lock();
        queue.offer(buf);
        mutex.unlock();
    }

    public int parseAVCNALu(byte[] array) {
        int arrayLen = array.length;
        int i = 0;
        int state = 0;
        int count = 0;
        while (i < arrayLen){
            byte value = array[i];
            ++i;
            switch(state) {
                case 0:
                    if(value == 0) {
                        state = 1;
                    }
                    break;
                case 1:
                    state = (value == 0) ? 2 : 0;
                    break;
                case 2: case 3:
                    if(value == 0) {
                        state = 3;
                    } else if(value == 1 && i < arrayLen) {
                        int unitType = array[i] & 0x1f;
                        if(unitType == 7 || unitType == 8) {
                            count += 1;
                        }
                        state = 0;
                    } else {
                        state = 0;
                    }
                    break;
            }
        }
        return count;
    }

    public byte[] retrieveFileData(String filename, int offset, long start) {

        byte[] bytes = new byte[offset];
        try {
            if(start == 0) {
                inputStream = context.getAssets().open(filename);
            }
            inputStream.read(bytes, 0, offset);
        } catch (Exception e) {}

        return bytes;
    }

    public void app264Streaming(WebSocket conn) {

        try {
            sizeReader = new BufferedReader(new InputStreamReader(context.getAssets().open(file264SizeName)));
        }catch (IOException e) {
            Log.e("app264Streaming", "Read files error!");
            return;
        }

        MyTimerTask task = new MyTimerTask(conn);
        timer.schedule(task, 30,30);
    }


    class MyTimerTask extends TimerTask {

        private WebSocket conn;
        private long fileStart = 0;
        private boolean flag = true;

        public MyTimerTask(WebSocket conn) {
            this.conn = conn;
        }

        @Override
        public void run() {
            String line;
            try {
                if ((line = sizeReader.readLine()) != null) {
                    long offs = Long.parseLong(line, 10);

                    int off = (int) offs;
                    byte[] b = retrieveFileData(file264DataName, off, fileStart);

                    Log.e("RUN()", "size = " + b.length);

                    boolean sendFlag = true;
                    // --------------------------------------
                    byte[] smallArray = new byte[off];
                    System.arraycopy(b, 0, smallArray, 0, off);
                    if(off < 100) {
                        int count = parseAVCNALu(smallArray);
                        if (count > 2) {
                            sendFlag = false;
                        }
                    }
                    if(sendFlag) {
                        try {
                            conn.send(smallArray);

                        // error handling
                        }catch (IllegalArgumentException iae) {
                            Log.e("app264Streaming","conn.WriteMessage ERROR!!!");
                            flag = false;
                        } catch (NotYetConnectedException nyc) {
                            Log.e("app264Streaming","conn.WriteMessage ERROR!!!");
                            flag = false;
                        }
                    }
                    // ---------------------------------------
                    fileStart += offs;
                }
            } catch (IOException ioe) {}

            if(!flag) {
                timer.cancel();
            }
        }

    }


    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conn.close();
        Log.e("SocketServer.onClose", "closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(final WebSocket conn, String message) {
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
        else {  // streaming
            try {
                JSONObject obj = new JSONObject(message);
                if(obj.get("t").equals("open")) {

                    thread = new Thread(new Runnable() {

                        @Override
                        public void run() {

//                            if (queue.isEmpty()) {
//                                try {
//                                    thread.sleep(30);
//                                } catch (Exception e) {
//                                }
//                            } else {
//                                mutex.lock();
//                                ByteBuffer buf = queue.poll();
//                                mutex.unlock();
//                            }

                            // TODO: use buf to do streaming
                            app264Streaming(conn);
                        }
                    });

                    thread.start();
                }
            } catch (Throwable t) {}
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
