package com.yuandiaodiaodiao.pyscrcpy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static MediaProjectionManager mediaProjectionManager;
    private String TAG="MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bt = (Button) findViewById(R.id.button);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });

        MediaCodecInfo[] mi=findEncodersByType("video/avc");
        Log.d(TAG,mi.toString());
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

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
//        try {
//            WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//            DisplayMetrics metrics = new DisplayMetrics();
//            mWindowManager.getDefaultDisplay().getMetrics(metrics);
//        } catch (Exception e){
//            Log.e(TAG, "MediaProjection error");
//        }
//        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        Intent service = new Intent(this, ScreenService.class);
        service.putExtra("code", resultCode);
        service.putExtra("data", data);
        startForegroundService(service);
//        MediaProjection mp= mediaProjectionManager.getMediaProjection(resultCode,data);
    }
}
