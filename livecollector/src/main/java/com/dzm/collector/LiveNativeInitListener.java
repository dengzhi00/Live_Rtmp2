package com.dzm.collector;

/**
 * Created by 83642 on 2017/9/2.
 */

public interface LiveNativeInitListener {

    void initNative(LiveBuild build);

    void releaseNative();

}
