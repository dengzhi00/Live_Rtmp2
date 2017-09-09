package com.dzm.collector;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.List;

/**
 *
 * @author 邓治民
 * date 2017/8/29 9:41
 */

public class LiveRop implements LiveRopMain{

    private LiveBuild build;
    private static LiveRop instance;
    private LiveEncode liveEncode;
    private LiveNativeInitListener nativeInitListener;

    private boolean isStart;

    public static LiveRop getInstance(){
        if(null == instance){
            synchronized (LiveRop.class){
                if(null == instance)
                    instance = new LiveRop();
            }
        }
        return instance;
    }

    private LiveRop(){
        liveEncode = LiveEncode.newInstance();
    }

    public LiveRop setEncodeListener(LiveEncodeListener encodeListener){
        liveEncode.setEncodeListener(encodeListener);
        return this;
    }

    public LiveRop setNativeInitListener(LiveNativeInitListener nativeInitListener) {
        this.nativeInitListener = nativeInitListener;
        return this;
    }

    public LiveRop build(Activity act) {
        build = new LiveBuild();
        build.setActivity(act);
        return this;
    }

    @Override
    public LiveRop setBitrate(int bitrate) {
        build.setBitrate(bitrate);
        return this;
    }

    @Override
    public LiveRop setFps(int fps) {
        build.setFps(fps);
        return this;
    }

    @Override
    public LiveRop setVideoEncodeType(VideoEncodeType videoEncodeType) {
        build.setVideoEncodeType(videoEncodeType);
        return this;
    }

    @Override
    public LiveRop setRtmpUrl(String rtmpUrl) {
        build.setRtmpUrl(rtmpUrl);
        return this;
    }

    @Override
    public LiveRop setVideoWidth(int videoWidth) {
        build.setVideoWidth(videoWidth);
        return this;
    }

    @Override
    public LiveRop setVideoHeight(int videoHeight) {
        build.setVideoHeight(videoHeight);
        return this;
    }

    @Override
    public LiveRop setHolder(SurfaceHolder holder) {
        build.setHolder(holder);
        return this;
    }

    public List<Camera.Size> getCameraSize() {
        return liveEncode.getCameraSize();
    }


    @Override
    public void initEncode() {
        List<Camera.Size> sizes = getCameraSize();
        boolean hasSize = false;
        for(Camera.Size size :sizes){
            if(size.width == build.getVideoWidth()){
                build.setVideoHeight(size.height);
                hasSize = true;
                break;
            }
        }
        if(!hasSize){
            int wd = 0;
            boolean fr = true;
            int width = 0;
            int height = 0;
            for(Camera.Size size :sizes){
                int m = size.width - build.getVideoWidth();
                if(m>0){
                    if(fr){
                        fr = false;
                        wd = m;
                    }else {
                        if(wd>m){
                            wd = m;
                            width = size.width;
                            height = size.height;
                        }
                    }
                }
            }
            build.setVideoWidth(width);
            build.setVideoHeight(height);
        }
        Log.d("width_height:","width:"+build.getVideoWidth()+"     height:"+build.getVideoHeight());
        build.setSampleRate(liveEncode.getSampleRate());
        build.setChannelConfig(liveEncode.getChannelConfig());
        if(null != nativeInitListener){
            nativeInitListener.initNative(build);
        }

        liveEncode.initEncode(build);
    }

    @Override
    public void startEncode() {
        liveEncode.startEncode();
    }

    @Override
    public void releaseEncode() {
        liveEncode.releaseEncode();
        if(null != nativeInitListener){
            nativeInitListener.releaseNative();
        }
    }
}
