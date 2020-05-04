package com.yuandiaodiaodiao.pyscrcpy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {
    public static MediaProjectionManager mediaProjectionManager;
    private String TAG = "MainActivity";
    private static String WS_URL = "ws://192.168.31.39:20482/ws";
    private static Integer SERVER_PORT = 20481;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bt = (Button) findViewById(R.id.button);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CountDownLatch countDownLatch = new CountDownLatch(1);//创建锁
                WebSocketController.Connect(WS_URL, countDownLatch);
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                start();
            }
        });
        Button bt2 = (Button) findViewById(R.id.button3);
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(),  ScreenService.class);
                stopService(intent);
//                Intent service = new Intent(getApplicationContext(), ScreenService.class);
//                service.putExtra("open", 1);
//                startForegroundService(service);
            }
        });
        MediaCodecInfo[] mi = findEncodersByType("video/avc");
        Log.d(TAG, mi.toString());


        ZXingLibrary.initDisplayOpinion(this);
        String[] permissions = new String[]{Manifest.permission.
                WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
        requestPermissions(permissions, 200);

        Button button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try{
//                    mWebSocket.send("aaaaa");
//                }catch (Exception e){
//                    Log.d("e", e.toString());
//
//                }
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                intent.setAction("com.google.zxing.client.android.SCAN");
                try {

                    startActivityForResult(intent, 2);

                } catch (Exception e) {
                    Log.d("e", e.toString());
                }
            }

        });

    }

    private MediaCodecInfo[] findEncodersByType(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        List<MediaCodecInfo> infos = new ArrayList<>();
        for (MediaCodecInfo info : codecList.getCodecInfos()) {
            if (!info.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities cap = info.getCapabilitiesForType(mimeType);
                if (cap == null) continue;
            } catch (IllegalArgumentException e) {
                // unsupported
                continue;
            }
            infos.add(info);
        }

        return infos.toArray(new MediaCodecInfo[infos.size()]);
    }

    private void start() {

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2) {
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    TextView tv=(TextView)findViewById(R.id.textView2);
                    WS_URL="ws://" + result + ":20482/ws";
                    tv.setText(WS_URL);
                    CountDownLatch countDownLatch = new CountDownLatch(1);//创建锁
                    SocketManager.Connect(result, SERVER_PORT);

                    WebSocketController.Connect(WS_URL, countDownLatch);
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    Toast.makeText(this, "解析结果:" + result, Toast.LENGTH_LONG).show();
                    start();
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MainActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }

        } else if (requestCode == 1) {
            Intent service = new Intent(this, ScreenService.class);
            service.putExtra("code", resultCode);
            service.putExtra("data", data);
            startForegroundService(service);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
