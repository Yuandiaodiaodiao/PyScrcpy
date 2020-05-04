package com.yuandiaodiaodiao.pyscrcpy;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class ByteBuffPackage {
    public MediaCodec codec;
    public int index;
    public ByteBuffer codecBuffer;
    ByteBuffPackage(MediaCodec codeci, int indexi, ByteBuffer codecBufferi){
        codec=codeci;
        index=indexi;
        codecBuffer=codecBufferi;
    }
}
