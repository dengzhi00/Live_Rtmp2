package com.dzm.collector;

import android.app.Activity;
import android.view.SurfaceHolder;

import static com.dzm.collector.VideoEncodeType.SOFT;

/**
 *
 * @author 邓治民
 * date 2017/8/28 16:48
 */

public class LiveBuild {


    /**推流地址*/
    private String rtmpUrl;
    /**推流视频宽度*/
    private int videoWidth;
    /**推流视频高度*/
    private int videoHeight;
    /**编码方式*/
    private VideoEncodeType videoEncodeType;
    /***/
    private int fps;
    /**帧率*/
    private int bitrate;
    /**上下文*/
    private Activity activity;
    /**头像显示holder*/
    private SurfaceHolder holder;
    /**音频采样率*/
    private int sampleRate;
    /**音频声道数*/
    private int channelConfig;

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public void setChannelConfig(int channelConfig) {
        this.channelConfig = channelConfig;
    }

    public void setHolder(SurfaceHolder holder) {
        this.holder = holder;
    }

    public SurfaceHolder getHolder() {
        return holder;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public LiveBuild(){
        this.videoEncodeType = SOFT;
        this.fps = 30;
        this.bitrate = 700 * 1000;
    }

    public VideoEncodeType getVideoEncodeType() {
        return videoEncodeType;
    }

    public void setVideoEncodeType(VideoEncodeType videoEncodeType) {
        this.videoEncodeType = videoEncodeType;
    }

    public String getRtmpUrl() {
        return rtmpUrl;
    }

    public void setRtmpUrl(String rtmpUrl) {
        this.rtmpUrl = rtmpUrl;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }
}
