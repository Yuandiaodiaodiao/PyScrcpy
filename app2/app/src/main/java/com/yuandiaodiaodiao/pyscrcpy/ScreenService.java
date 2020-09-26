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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static android.media.AudioRecord.RECORDSTATE_RECORDING;

public class ScreenService extends Service {
    private static String TAG = "ScreenService";
    private Thread thread;
    private static String WS_URL = "ws://192.168.31.39:20482/ws";
    private static String SERVER_IP = "192.168.31.39";
    private static Integer SERVER_PORT = 20481;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MediaProjection mp;
    private Thread audiothread;

    @Override
    public void onDestroy() {
//        if(thread.isAlive()){
//            thread.interrupt();
//        }
        ar.stop();
        audiothread.interrupt();
        mEncoder.stop();
        stopForeground(true);
        super.onDestroy();
    }

    public int onStartCommand(final Intent intent, int flags, int startId) {
        int openCode = intent.getIntExtra("open", -1);
        if (openCode == 1) {
            ar.stop();
            mEncoder.stop();
            audiothread.interrupt();
            stopForeground(true);

            return super.onStartCommand(intent, flags, startId);
        }
        createNotificationChannel();

        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        WebSocketController.getmWebSocket().send("size " + screenWidth + " " + screenHeight);

        Intent it = Objects.requireNonNull((Intent) (intent.getParcelableExtra("data")));
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mp = mediaProjectionManager.getMediaProjection(intent.getIntExtra("code", -1), it);
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
        audioRecord();
        createMediaCodec(screenWidth, screenHeight);
        return super.onStartCommand(intent, flags, startId);
    }

    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int BIT_RATE = (int) 10e6;
    public static  int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 5;
    private VirtualDisplay vd;

    private AudioRecord createAudioRecord() {
        for (int sampleRate : new int[]{44100}) {
            for (short audioFormat : new short[]{
                    AudioFormat.ENCODING_PCM_16BIT
//                    , AudioFormat.ENCODING_PCM_8BIT
            }) {
                for (short channelConfig : new short[]{
                        AudioFormat.CHANNEL_IN_MONO
//                        , AudioFormat.CHANNEL_IN_STEREO //立体声
                }) {

                    // Try to initialize
                    try {
                        int recBufSize = AudioRecord.getMinBufferSize(sampleRate,
                                channelConfig, audioFormat);

                        if (recBufSize < 0) {
                            continue;
                        }

                        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                sampleRate, channelConfig, audioFormat,
                                recBufSize * 2);

                        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {

                            return audioRecord;
                        }

                        audioRecord.release();
                        audioRecord = null;
                    } catch (Exception e) {
                        // Do nothing
                    }
                }
            }
        }

        throw new IllegalStateException(
                "getInstance() failed : no suitable audio configurations on this device.");

    }

    private AudioRecord ar;

    private void audioRecord() {
        ar = createAudioRecord();
        audiothread = new Thread(new Runnable() {

            @Override
            public void run() {
                byte[] audiodata = new byte[1000];
                ar.startRecording();
                while (ar.getRecordingState() == RECORDSTATE_RECORDING && !Thread.interrupted()) {
                    int readsize = ar.read(audiodata, 0, 1000);
                    WebSocketController.Write(audiodata);
//                    System.out.println("读了" + readsize);

                }
                ar.release();
            }
        });
        audiothread.start();

    }

    private void createMediaCodec(int width, int height) {

        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 400_000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            System.out.println("安卓10 设置帧率=" + FRAME_RATE);
            format.setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, FRAME_RATE);
        }
        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
//            mEncoder = MediaCodec.createByCodecName("c2.android.avc.encoder");
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            vd = mp.createVirtualDisplay("display-", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, null, null, null);
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
                            SocketManager.WriteBuffer(codec, outputBufferId, codecBuffer);
//                            WebSocketController.Write(codecBuffer);
//                            Log.d(TAG, "输出buff");
                        }
                    } finally {
//                        if (outputBufferId >= 0) {
//                            codec.releaseOutputBuffer(outputBufferId, false);
//                        }
                    }
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    System.out.println("channged!!!!!!!!!!1!!!!");
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
