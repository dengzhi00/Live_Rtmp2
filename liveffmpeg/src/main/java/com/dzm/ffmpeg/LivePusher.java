package com.dzm.ffmpeg;

/**
 *
 * @author 邓治民
 * date 2017/9/5 15:39
 */

class LivePusher {

    static {
        System.loadLibrary("native-lib");
    }

    private static LivePusher livePush;

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

    public native void init(int width,int height,String rtmpPath);

    public native void addVideo(byte[] data,boolean isBack);

}
