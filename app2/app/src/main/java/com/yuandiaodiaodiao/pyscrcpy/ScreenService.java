package com.yuandiaodiaodiao.pyscrcpy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class ScreenService extends Service {
    private static String TAG = "ScreenService";
    private Thread thread;
    private static String WS_URL = "ws://192.168.31.39:20482/ws";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MediaProjection mp;



    public int onStartCommand(final Intent intent, int flags, int startId) {
        createNotificationChannel();


        CountDownLatch countDownLatch = new CountDownLatch(1);//创建锁
        WebSocketController.Connect(WS_URL, countDownLatch);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WebSocketController.getmWebSocket().send("size "+720+" "+1080);

        Intent it= Objects.requireNonNull((Intent) (intent.getParcelableExtra("data")));
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mp = mediaProjectionManager.getMediaProjection(intent.getIntExtra("code", -1),it);
        Log.e(TAG, "mMediaProjection created: " + mp);
//        thread=new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        while(true){
//                            Log.d(TAG,"aaa");
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                    }
//                }
//        );
////        thread.run();
        createMediaCodec();
        return super.onStartCommand(intent, flags, startId);
    }

    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int BIT_RATE = 800000;
    private static final int FRAME_RATE = 15;
    private static final int IFRAME_INTERVAL = 5;
    private VirtualDisplay vd;

    private void createMediaCodec() {
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, 720, 1280);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 400_000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
//            mEncoder = MediaCodec.createByCodecName("c2.android.avc.encoder");
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            vd = mp.createVirtualDisplay("display-", 720, 1280, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, null, null, null);
            vd.setSurface(mInputSurface);
            mEncoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    // not important for us, since we're using Surface
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {

                    int outputBufferId = index;
                    try {
                        if (outputBufferId >= 0) {
                            ByteBuffer codecBuffer = codec.getOutputBuffer(outputBufferId);
                            Log.d(TAG, "输出buff");
                        }
                    } finally {
                        if (outputBufferId >= 0) {
                            codec.releaseOutputBuffer(outputBufferId, false);
                        }
                    }
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            mEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);

    }
}
