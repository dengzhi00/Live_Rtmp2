package com.dzm.collector;

import android.view.SurfaceHolder;

/**
 *
 * @author 邓治民
 * date 2017/8/29 9:37
 */

public interface LiveRopMain {

    LiveRopMain setEncodeListener(LiveEncodeListener encodeListener);


    LiveRopMain setBitrate(int bitrate);

    LiveRopMain setFps(int fps);

    LiveRopMain setVideoEncodeType(VideoEncodeType videoEncodeType);

    LiveRopMain setRtmpUrl(String rtmpUrl);

    LiveRopMain setVideoWidth(int videoWidth);

    LiveRopMain setVideoHeight(int videoHeight);

    LiveRopMain setHolder(SurfaceHolder holder);

    void initEncode();

    void startEncode();

    void releaseEncode();
}
