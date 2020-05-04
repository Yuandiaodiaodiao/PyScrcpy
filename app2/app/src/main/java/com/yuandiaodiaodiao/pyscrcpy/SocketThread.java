package com.yuandiaodiaodiao.pyscrcpy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class SocketThread extends Thread {
    private String ip;
    private Integer port;
    private ArrayBlockingQueue aq;
    private byte[] bytes = new byte[8192];

    SocketThread(String ipi, Integer porti, ArrayBlockingQueue queue) {
        aq = queue;
        port = porti;
        ip = ipi;
    }

    @Override
    public void run() {
        Socket socket=null;
        OutputStream os=null;

        boolean connected = false;
        while (!connected) {
            try {
                socket = new Socket(ip, port);
                System.out.println("socket 连接成功");
                connected = true;
            } catch (IOException e) {
                System.out.println("失败 重连");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {

                }

            }
        }
        try {
            os = socket.getOutputStream();
            System.out.println("socket OutputStream创建成功");
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                ByteBuffPackage bk = (ByteBuffPackage) aq.take();
                ByteBuffer codecBuffer = bk.codecBuffer;
                int remaining = codecBuffer.remaining();
//                    System.out.println("remaining="+remaining);
                int all = 0;
                while (remaining > 0) {
                    int read = Math.min(remaining, bytes.length);

                    codecBuffer.get(bytes, 0, read);
                    remaining -= read;
                    os.write(bytes, 0, read);
                    os.flush();
                    all += read;
                }
                if (all > 4 * bytes.length) {
                    System.out.println("扩容到" + bytes.length * 2);
                    bytes = new byte[bytes.length * 2];
                }
                bk.codec.releaseOutputBuffer(bk.index, false);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
