package com.cry.cry.appprocessdemo;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketController {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();
    private static WebSocket mWebSocket;
    private static CountDownLatch countDownLatch;
    private static String url;

    public static void Connect(String url, CountDownLatch count) {
        countDownLatch = count;
        Connect(url);
    }

    public static WebSocket getmWebSocket() {
        return mWebSocket;
    }

    private static void Connect(String urli) {
        url = urli;
        try{
            client.newWebSocket(new Request.Builder().url(url).build(), createwsListener());
        }catch (Exception e){
            System.out.println(e);
        }
    }

    private static void ReConnect() {
        System.out.println("重连"+url);
        Connect(url);
    }

    public static void Write(byte[] buffer) {
        mWebSocket.send(ByteString.of(buffer));
    }

    public static void Write(ByteBuffer buffer) {
        mWebSocket.send(ByteString.of(buffer));
    }

    private static WebSocketListener createwsListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                mWebSocket = webSocket;
                countDownLatch.countDown();
                System.out.println("连接完成");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                System.out.println("连接失败");
                try{
                    Thread.sleep(1000);
                }catch (Exception e){}
                ReConnect();

            }
        };
    }
}
