package com.cry.cry.appprocessdemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketManager {
    private static Socket socket;

    private static OutputStream os;
    private static InputStream is;

    public static void Connect(String ipi, Integer porti) {
//        try {
//            socket = new Socket(ipi, porti);
//            System.out.println("socket 连接成功");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//
//            os = socket.getOutputStream();
//
//
//            System.out.println("socket OutputStream创建成功");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public static OutputStream getOs() {
        return os;
    }

    public static InputStream getIs() {
        return is;
    }
}

