package com.dzm.collector;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_MAX_INPUT_SIZE;

/**
 *
 * @author 邓治民
 * date 2017/8/31 17:52
 * 音频硬编码类
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class LiveAudioEncode {


    private MediaCodec aEncoder;
    private long presentationTimeUs;
    private MediaCodec.BufferInfo aBufferInfo = new MediaCodec.BufferInfo();
    private LiveAudioEncodeListener audioListener;

    public void setAudioListener(LiveAudioEncodeListener audioListener) {
        this.audioListener = audioListener;
    }

    static LiveAudioEncode newInstance(){
        return new LiveAudioEncode();
    }

    void startEncode(){
        presentationTimeUs = System.currentTimeMillis() * 1000;
        if(null != aEncoder)
            aEncoder.start();
    }

    /**
     * 初始化音频编码器
     *
     * @param sampleRate  音频采样率
     * @param chanelCount 声道数
     * @throws IOException 创建编码器失败
     */
    void initAudioEncoder(int sampleRate, int chanelCount) throws IOException {
        MediaCodec aencoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate, chanelCount);
        format.setInteger(KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(KEY_BIT_RATE, sampleRate * chanelCount);
        aencoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        aEncoder = aencoder;
    }

    /**
     * 音频解码
     *
     * @param data
     */
    void encodeAudioData(byte[] data) {
        ByteBuffer[] inputBuffers = aEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = aEncoder.getOutputBuffers();
        int inputBufferId = aEncoder.dequeueInputBuffer(-1);
        if (inputBufferId >= 0) {
            ByteBuffer bb = inputBuffers[inputBufferId];
            bb.clear();
            bb.put(data, 0, data.length);
            long pts = new Date().getTime() * 1000 - presentationTimeUs;
            aEncoder.queueInputBuffer(inputBufferId, 0, data.length, pts, 0);
        }

        int outputBufferId = aEncoder.dequeueOutputBuffer(aBufferInfo, 0);
        if (outputBufferId >= 0) {
            // outputBuffers[outputBufferId] is ready to be processed or rendered.
            ByteBuffer bb = outputBuffers[outputBufferId];
            if (audioListener != null) {
                audioListener.audioEncode(bb, aBufferInfo);
            }
            aEncoder.releaseOutputBuffer(outputBufferId, false);
        }
    }

    interface LiveAudioEncodeListener{
        void audioEncode(ByteBuffer bb,MediaCodec.BufferInfo aBufferInfo);
    }
}
