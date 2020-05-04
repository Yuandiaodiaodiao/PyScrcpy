package com.yuandiaodiaodiao.pyscrcpy;

import android.media.MediaCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class SocketManager {
    private static SocketThread thread=null;


    private static ArrayBlockingQueue queue = new ArrayBlockingQueue(10, false);
    public static void Connect(String ipi, Integer porti) {
        if(thread!=null && thread.isAlive()){
            thread.interrupt();
        }
        thread=new SocketThread(ipi,porti,queue);
        thread.start();
    }
    public static void WriteBuffer(MediaCodec codeci, int indexi, ByteBuffer codecBufferi){
        queue.offer( new ByteBuffPackage(codeci,indexi,codecBufferi));
    }

}

