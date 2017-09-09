package com.dzm.ffmpeg;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaCodec;

import com.dzm.collector.LiveBuild;
import com.dzm.collector.LiveConfig;
import com.dzm.collector.LiveEncodeListener;
import com.dzm.collector.LiveNativeInitListener;
import com.dzm.collector.LiveRop;

import java.nio.ByteBuffer;


/**
 *
 * @author 邓治民
 * date 2017/9/5 15:46
 */

public class LiveFfmpegManager {

    public static LiveRop build(Activity activity){
        return LiveRop.getInstance().build(activity).setEncodeListener(new LiveEncodeListener() {
            @Override
            public void videoToYuv420sp(byte[] yuv) {
                LivePusher.getInscance().addVideo(yuv, LiveConfig.isBack);
            }

            @Override
            public void videoToHard(ByteBuffer bb, MediaCodec.BufferInfo info) {
            }

            @Override
            public void audioToHard(ByteBuffer bb, MediaCodec.BufferInfo info) {
            }

            @Override
            public void audioToSoft(byte[] data) {

            }
        }).setNativeInitListener(new LiveNativeInitListener() {
            @Override
            public void initNative(LiveBuild build) {
                LivePusher.getInscance().init(build.getVideoWidth(),build.getVideoHeight(),build.getRtmpUrl());
            }

            @Override
            public void releaseNative() {

            }
        });
    }

}
