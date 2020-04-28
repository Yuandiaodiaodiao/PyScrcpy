package com.cry.cry.appprocessdemo;

import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import com.cry.cry.appprocessdemo.refect.SurfaceControl;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import okio.ByteString;

public class ScreenRecorder {
    private static final int DEFAULT_FRAME_RATE = 30; // fps
    private static final int DEFAULT_I_FRAME_INTERVAL = 1; // seconds
    private static final int DEFAULT_BIT_RATE = (int) 50e6; // 8Mbps
    private static final int DEFAULT_TIME_OUT = 10 * 1000; // 2s

    private static final int REPEAT_FRAME_DELAY = 6; // repeat after 6 frames
    private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;
    private static final int NO_PTS = -1;

    private boolean sendFrameMeta = false;

    private final ByteBuffer headerBuffer = ByteBuffer.allocate(12);
    private long ptsOrigin;

    private volatile boolean stop;
    private MediaCodec encoder;

    public void setStop(boolean stop) {
        this.stop = stop;
//        encoder.signalEndOfInputStream();
    }

    //进行录制的循环，录制得到的数据，都写到fd当中
    public void record(int width, int height, FileDescriptor fd) {
        //对MediaCodec进行配置
        boolean alive = false;
        try {
            do {
                try {

                    MediaFormat mediaFormat = createMediaFormat(DEFAULT_BIT_RATE, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);
                    mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
                    mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
                    encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                    //输入输出的surface 这里是没有
                    encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    Surface inputSurface = encoder.createInputSurface();
                    IBinder surfaceClient = setDisplaySurface(width, height, inputSurface);
                    encoder.start();
                    System.out.println("encoder.start()1");
                    try {
                        alive = encode(encoder, fd);
                        System.out.println("encode(encoder, fd)结束");
                        alive = alive && !stop;
                        System.out.println("alive =" + alive + ", stop=" + stop);
                    } finally {
                        System.out.println("encoder.stop");
                        //为什么调用stop会block主呢？
//                    encoder.stop();
                        System.out.println("destroyDisplaySurface");
                        destroyDisplaySurface(surfaceClient);
                        System.out.println("encoder release");
                        encoder.release();
                        System.out.println("inputSurface release");
                        inputSurface.release();
                        System.out.println("end");
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("e");
                } finally {
                    System.out.println("finally");
                }

            } while (alive);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("end record");
    }

    //创建录制的Surface
    private IBinder setDisplaySurface(int width, int height, Surface inputSurface) {
        Rect deviceRect = new Rect(0, 0, width, height);
        Rect displayRect = new Rect(0, 0, width, height);
        IBinder surfaceClient = SurfaceControl.createDisplay("recorder", false);
        //设置和配置截屏的Surface
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(surfaceClient, inputSurface);
            SurfaceControl.setDisplayProjection(surfaceClient, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(surfaceClient, 0);
        } finally {
            SurfaceControl.closeTransaction();
        }
        return surfaceClient;
    }

    private void destroyDisplaySurface(IBinder surfaceClient) {
        SurfaceControl.destroyDisplay(surfaceClient);
    }

    //创建MediaFormat
    private MediaFormat createMediaFormat(int bitRate, int frameRate, int iFrameInterval) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);

        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
//        mediaFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, MICROSECONDS_IN_ONE_SECOND * REPEAT_FRAME_DELAY / frameRate);//us 200ms 没有新帧重复帧出包
        mediaFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 400_000);//us 100ms 没有新帧重复帧出包
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            System.out.println("安卓10 设置帧率=" + frameRate);
            mediaFormat.setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, frameRate);
        }
        return mediaFormat;
    }

    //进行encode
    private boolean encode(MediaCodec codec, FileDescriptor fd) throws IOException {
        System.out.println("encode");
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int tick = 0;
        long tickstarttime = System.currentTimeMillis();
        int packNum = 0;
        while (!eof) {

            Long t1 = System.currentTimeMillis();
            tick++;
            packNum++;
//            System.out.println("pakNum="+packNum);
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, -1);
            eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

            Long t2 = System.currentTimeMillis();


            try {

                if (outputBufferId >= 0) {
                    ByteBuffer codecBuffer = codec.getOutputBuffer(outputBufferId);
//                    if (sendFrameMeta) {
//                        writeFrameMeta(fd, bufferInfo, codecBuffer.remaining());
//                    }
//                    System.out.println(bufferInfo.flags);
//                        WebSocketController.Write(codecBuffer);
//                        ByteString bs = ByteString.of(codecBuffer);
//                        byte[] ba = bs.toByteArray();
//                        System.out.println("图片大小1=" + ba.length);
                        SocketManager.getOs().write(ByteString.of(codecBuffer).toByteArray());
                        SocketManager.getOs().flush();
//                    }
//                    System.out.println("before flush");
//                    Long t3 = System.currentTimeMillis();
//
//
//                    Long t4 = System.currentTimeMillis();
//                    if (tick > 60) {
//                        long fps = (System.currentTimeMillis() - tickstarttime);
//                        fps = Math.round(1.0 * tick / fps * 1000);
//                        System.out.println("android fps=" + fps);
//                        tickstarttime = System.currentTimeMillis();
//                        tick = 0;
//                    }
                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false);
                }
            }

        }
        return !eof;
    }

    private void writeFrameMeta(FileDescriptor fd, MediaCodec.BufferInfo bufferInfo, int packetSize) throws IOException {
        headerBuffer.clear();

        long pts;
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            pts = NO_PTS; // non-media data packet
        } else {
            if (ptsOrigin == 0) {
                ptsOrigin = bufferInfo.presentationTimeUs;
            }
            pts = bufferInfo.presentationTimeUs - ptsOrigin;
        }

        headerBuffer.putLong(pts);
        headerBuffer.putInt(packetSize);
        headerBuffer.flip();
        IO.writeFully(fd, headerBuffer);
    }

}
