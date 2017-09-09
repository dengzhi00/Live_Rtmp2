package com.dzm.live;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.os.Build;
import android.util.Log;

import com.dzm.collector.*;

import java.io.File;
import java.io.FileFilter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author 邓治民
 * date 2017/8/28 16:12
 */

public class LiveManager {

    private static final String TAG = LiveManager.class.getName();



    private static final int NAL_SLICE = 1;
    private static final int NAL_SLICE_DPA = 2;
    private static final int NAL_SLICE_DPB = 3;
    private static final int NAL_SLICE_DPC = 4;
    private static final int NAL_SLICE_IDR = 5;
    private static final int NAL_SEI = 6;
    private static final int NAL_SPS = 7;
    private static final int NAL_PPS = 8;
    private static final int NAL_AUD = 9;
    private static final int NAL_FILLER = 12;

    private LiveManager(){}

    public static LiveRop build(Activity activity){
        return build(activity,null);
    }

    public static LiveRop build(Activity activity, OnRtmpConnectListener listener){
        if(null != listener){
            LivePusher.getInscance().setListener(listener);
        }
        return LiveRop.getInstance().build(activity).setEncodeListener(new LiveEncodeListener() {
            @Override
            public void videoToYuv420sp(byte[] yuv) {
                LivePusher.getInscance().pushVideo(yuv,LiveConfig.isBack);
            }

            @Override
            public void videoToHard(ByteBuffer bb, MediaCodec.BufferInfo info) {
                onEncodedAvcFrame(bb,info);
            }

            @Override
            public void audioToHard(ByteBuffer bb, MediaCodec.BufferInfo info) {
                onEncodeAacFrame(bb,info);
            }

            @Override
            public void audioToSoft(byte[] data) {
                LivePusher.getInscance().addAudioData(data);
            }
        }).setNativeInitListener(new LiveNativeInitListener() {
            @Override
            public void initNative(LiveBuild build) {
                LivePusher.getInscance().initLive(build.getRtmpUrl(),build.getVideoWidth(),build.getVideoHeight(),build.getBitrate());
                if(build.getVideoEncodeType() == VideoEncodeType.SOFT){
                    LivePusher.getInscance().initVedioEncode(LiveManager.getNumCores());
                    LivePusher.getInscance().initAudioEncode(build.getSampleRate(),build.getChannelConfig());
                    LivePusher.getInscance().setAacSpecificInfos(AudioSpecificConfig.getSpecificInfo());
                }
                LivePusher.getInscance().startPusher();
            }

            @Override
            public void releaseNative() {
                LivePusher.getInscance().release();
            }
        });
    }



    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void onEncodedAvcFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
        int offset = 4;
        //判断帧的类型
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = bb.get(offset) & 0x1f;
//        Log.d(TAG, "type=" + type +"    size:"+vBufferInfo.size);
        if (type == NAL_SPS) {
            //[0, 0, 0, 1, 103, 66, -64, 13, -38, 5, -126, 90, 1, -31, 16, -115, 64, 0, 0, 0, 1, 104, -50, 6, -30]
            //打印发现这里将 SPS帧和 PPS帧合在了一起发送
            // SPS为 [4，len-8]
            // PPS为后4个字节
            final byte[] pps = new byte[4];
            final byte[] sps = new byte[vBufferInfo.size - 12];
            bb.getInt();// 抛弃 0,0,0,1
            bb.get(sps, 0, sps.length);
            bb.getInt();
            bb.get(pps, 0, pps.length);
//            Log.d(TAG, "解析得到 sps:" + Arrays.toString(sps) + ",PPS=" + Arrays.toString(pps));
            LivePusher.getInscance().send_sps_pps(sps, sps.length, pps, pps.length);
        } else if (type == NAL_SLICE || type == NAL_SLICE_IDR) {
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            LivePusher.getInscance().send_video_body(bytes, bytes.length);
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void onEncodeAacFrame(ByteBuffer bb, final MediaCodec.BufferInfo aBufferInfo) {
        if (aBufferInfo.size == 2) {
            // 我打印发现，这里应该已经是吧关键帧计算好了，所以我们直接发送
            final byte[] bytes = new byte[2];
            bb.get(bytes);
            LivePusher.getInscance().sendAacSpec(bytes,2);
        } else {
            final byte[] bytes = new byte[aBufferInfo.size];
            bb.get(bytes);
            LivePusher.getInscance().sendAacData(bytes, bytes.length, aBufferInfo.presentationTimeUs / 1000);
        }
    }

    public static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            Log.d(TAG, "CPU Count: "+files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Print exception
            Log.d(TAG, "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return 1;
        }
    }

}
