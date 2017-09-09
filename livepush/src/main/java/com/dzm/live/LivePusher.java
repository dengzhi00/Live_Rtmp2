package com.dzm.live;

import android.os.Handler;
import android.util.Log;

/**
 *
 * @author 邓治民
 * date 2017/8/28 9:31
 */

class LivePusher {

    static {
        System.loadLibrary("Dvr");
    }

    private static LivePusher livePush;
    private Handler hander;

    static LivePusher getInscance(){
        if(null == livePush){
            synchronized (LivePusher.class){
                if(null == livePush){
                    livePush = new LivePusher();
                }
            }
        }
        return livePush;
    }

    private LivePusher(){
        hander = new Handler();
    }

    private OnRtmpConnectListener listener;

    void setListener(OnRtmpConnectListener listener) {
        this.listener = listener;
    }

    public void onNativeResponse(final int status){
        Log.d("onNativeResponse",""+status);
        if(null == listener)
            return;
        hander.post(new Runnable() {
            @Override
            public void run() {
                switch (status){
                    case -101:
                    case -102:
                    case -103:
                    case -104:
                        listener.rtmpConnect("rtmp连接失败",status);
                        break;
                    case -105:
                        listener.rtmpConnect("rtmp包推流失败",status);
                        break;
                    case 100:
                        listener.rtmpConnect("rtmp连接成功",status);
                        break;
                    default:
                        listener.rtmpConnect("未知",status);
                        break;
                }
            }
        });
    }

    /**
     * 初始化参数
     * @param rtmpUrl 推流地址
     * @param width 宽度
     * @param height 高度
     */
    public native void initLive(String rtmpUrl,int width,int height,int bitrate);

    /**
     * 初始化视频编码器
     */
    public native void initVedioEncode(int threadSize);

    /**
     * 初始化音频软编码
     * @param sampleRate 采样率
     * @param channel 声道数
     */
    public native void initAudioEncode(int sampleRate, int channel);

    /**
     * 开启推流线程
     */
    public native void startPusher();

    /**
     * 硬软编吗编码推流视频内容数据
     * @param vide0 视频内容
     */
    public native void pushVideo(byte[] vide0,boolean isback);

    /**
     * 硬编码 推流sps pps 视频信息帧
     * @param sps sps
     * @param sps_lenth sps_lenth
     * @param pps pps
     * @param pps_lenth pps_lenth
     */
    public native void send_sps_pps(byte[] sps,int sps_lenth,byte[] pps,int pps_lenth);

    /**
     * 硬编码 推流视频内容
     * @param body body
     * @param body_lenth body_lenth
     */
    public native void send_video_body(byte[] body,int body_lenth);

    /**
     * 硬编码 音频aac 头
     * @param aac_spec aac_spec
     * @param len len
     */
    public native void sendAacSpec(byte[] aac_spec,int len);

    /**
     * 硬编码 音频aac体
     * @param data data
     * @param len len
     * @param timestamp timestamp
     */
    public native void sendAacData(byte[] data, int len, long timestamp);

    /**
     * 软编码 自定义音频aac 头
     * @param data data
     */
    public native void setAacSpecificInfos(byte[] data);

    /**
     * 软编吗添加音频pcm 体
     * @param data pcm格式音频数据
     */
    public native void addAudioData(byte[] data);

    /**
     * 释放内存
     */
    public native void release();

}
