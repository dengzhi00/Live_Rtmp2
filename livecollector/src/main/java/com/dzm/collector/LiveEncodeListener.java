package com.dzm.collector;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by 83642 on 2017/9/2.
 */

public interface LiveEncodeListener {

    void videoToYuv420sp(byte[] yuv);

    void videoToHard(ByteBuffer bb, MediaCodec.BufferInfo info);

    void audioToHard(ByteBuffer bb, MediaCodec.BufferInfo info);

    void audioToSoft(byte[] data);

}
