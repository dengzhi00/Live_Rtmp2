package com.dzm.collector;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author 邓治民
 *         date 2017/8/28 17:13
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class LiveEncode {

    //视频
    private LinkedBlockingQueue<byte[]> videoQueue;
    private LiveVideoGet liveVideo;
    private LiveVideoEncode liveVideoEncode;
    private Thread videoEncodeThread;
    private boolean videoEncodeStart;
    private int colorFormat;
    //音频
    private LiveAudioGet liveAudioGet;
    private LinkedBlockingQueue<byte[]> audioQueue;
    private Thread audioEncodeThread;
    private boolean audioEncodeStart;
    private LiveAudioEncode liveAudioEncode;



    private LiveBuild build;
    private LiveEncodeListener encodeListener;

    void setEncodeListener(LiveEncodeListener encodeListener) {
        this.encodeListener = encodeListener;
    }

    static LiveEncode newInstance(){
        return new LiveEncode();
    }

    private LiveEncode() {
        colorFormat = -1;
        videoQueue = new LinkedBlockingQueue<>();
        //视频编码类
        liveVideoEncode = LiveVideoEncode.newInstance();
        liveVideoEncode.setVideoListener(new LiveVideoEncode.OnVideoEncodeListener() {
            @Override
            public void videoToYuv420(byte[] yuv) {
                if(null != encodeListener){
                    encodeListener.videoToYuv420sp(yuv);
                }

            }

            @Override
            public void videoToHard(ByteBuffer bb, MediaCodec.BufferInfo vBufferInfo) {
                if(null != encodeListener && null != bb){
                    encodeListener.videoToHard(bb,vBufferInfo);
                }
            }

            @Override
            public void addCallbackBuffer(byte[] old) {
                liveVideo.addCallbackBuffer(old);
            }
        });
        //视频采集类
        liveVideo = LiveVideoGet.newInstance();
        liveVideo.setVideoListener(new LiveVideoGet.LiveVideoListener() {
            @Override
            public void onPreviewFrame(byte[] data) {
                try {
                    if (videoEncodeStart) videoQueue.put(data);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        //音频采集
        audioQueue = new LinkedBlockingQueue<>();
        liveAudioGet = LiveAudioGet.newInstance();
        liveAudioGet.initAudio();
        liveAudioGet.setListener(new LiveAudioGet.LiveAudioListener() {
            @Override
            public void audioRead(byte[] audio) {
                audioQueue.add(audio);
            }
        });
        //音频编码
        liveAudioEncode = LiveAudioEncode.newInstance();
        liveAudioEncode.setAudioListener(new LiveAudioEncode.LiveAudioEncodeListener() {
            @Override
            public void audioEncode(ByteBuffer bb, MediaCodec.BufferInfo aBufferInfo) {
                if(null != encodeListener)
                encodeListener.audioToHard(bb,aBufferInfo);
            }
        });
    }

    int getSampleRate(){
        return liveAudioGet.sampleRate;
    }

    int getChannelConfig(){
        return liveAudioGet.channelConfig;
    }

    List<Camera.Size> getCameraSize(){
        return liveVideo.getCameraSize();
    }

    void initEncode(LiveBuild builds) {
        this.build = builds;
        liveVideoEncode.initVideoEncode(build);
        try {
            if (build.getVideoEncodeType() == VideoEncodeType.HARD) {
                colorFormat = liveVideoEncode.initVideoEncoder(build.getVideoWidth(), build.getVideoHeight(), build.getFps(),build.getBitrate());
            }
            liveAudioEncode.initAudioEncoder(liveAudioGet.sampleRate,liveAudioGet.channelConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startEncode(){
        startVideoEncode();
        startAudioEncode();
    }

    private void startAudioEncode(){
        liveAudioGet.startAudio();
        audioEncodeThread = new Thread(){
            @Override
            public void run() {
                liveAudioEncode.startEncode();
                while (audioEncodeStart && !Thread.interrupted()){
                    try {
                        Log.d("audio_queue_size",audioQueue.size()+"");
                        byte[] data = audioQueue.take();
                        if(build.getVideoEncodeType() == VideoEncodeType.HARD){
                            liveAudioEncode.encodeAudioData(data);
                        }else {
                            if(null != encodeListener){
                                encodeListener.audioToSoft(data);
                            }
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        audioEncodeStart = true;
        audioEncodeThread.start();
    }

    private void startVideoEncode(){
        liveVideo.startCamera(build);
        videoEncodeThread = new Thread() {
            @Override
            public void run() {
                if (build.getVideoEncodeType() == VideoEncodeType.HARD) {
                    liveVideoEncode.startEncoder();
                }
                while (videoEncodeStart && !Thread.interrupted()) {
                    try {
                        Log.d("vedio_queue_size",videoQueue.size()+"");
                        byte[] bytes = videoQueue.take();
                        liveVideoEncode.encodeData(bytes,colorFormat);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        videoEncodeStart = true;
        videoEncodeThread.start();
    }

    void releaseEncode(){
        liveVideo.releaseCamera();
        liveVideoEncode.releaseVideo();
        stopVideoEncode();
        liveAudioGet.stopAudio();
        stopAudioEncode();
    }


    private void stopVideoEncode() {
        videoEncodeStart = false;
        videoEncodeThread.interrupt();
        videoQueue.clear();
    }

    public void stopAudioEncode(){
        audioEncodeStart = false;
        audioEncodeThread.interrupt();
        audioQueue.clear();
    }

}
