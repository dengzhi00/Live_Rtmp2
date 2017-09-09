package com.dzm.collector;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_COLOR_FORMAT;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;
import static android.media.MediaFormat.KEY_MAX_INPUT_SIZE;

/**
 *
 * @author 邓治民
 * date 2017/8/31 15:27
 * 视频编码
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class LiveVideoEncode {

    private static final String TAG = LiveVideoEncode.class.getName();

    private MediaCodec vEncoder; //视频编码器
    private byte[] videobytes;//yuv数据
    private byte[] videoXZ;//yuv旋转数据
    private LiveBuild build;
    private long presentationTimeUs;
    private MediaCodec.BufferInfo vBufferInfo = new MediaCodec.BufferInfo();
    private OnVideoEncodeListener videoListener;
    private long preTime;

    void setVideoListener(OnVideoEncodeListener videoListener) {
        this.videoListener = videoListener;
    }

    static LiveVideoEncode newInstance(){
        return new LiveVideoEncode();
    }

    void initVideoEncode(LiveBuild builds){
        this.build = builds;
        videobytes = new byte[calculateFraeSize(ImageFormat.NV21)];
        videoXZ = new byte[calculateFraeSize(ImageFormat.NV21)];
    }

    private int calculateFraeSize(int format) {
        return build.getVideoWidth() * build.getVideoHeight() * ImageFormat.getBitsPerPixel(format) / 8;
    }

    void startEncoder(){
        presentationTimeUs = System.currentTimeMillis() * 1000;
        if(null != vEncoder)
            vEncoder.start();
    }

    void releaseVideo(){
        if(null != vEncoder)
            vEncoder.release();
    }

    /**
     * 初始化视频编码器。
     * @param width  视频的宽
     * @param height 视频的高
     * @throws IOException 创建编码器失败
     */

    int initVideoEncoder(int width, int height, int fps,int bitrate) throws IOException {
        // 初始化
        MediaCodecInfo mediaCodecInfo = getMediaCodecInfoByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        int colorFormat = getColorFormat(mediaCodecInfo);
        MediaCodec vencoder = MediaCodec.createByCodecName(mediaCodecInfo.getName());
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height,width);
        format.setInteger(KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(KEY_BIT_RATE, bitrate);
        format.setInteger(KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(KEY_FRAME_RATE, fps);
        format.setInteger(KEY_I_FRAME_INTERVAL, 1);
        vencoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        vEncoder = vencoder;
        return colorFormat;
    }


    private static MediaCodecInfo getMediaCodecInfoByType(String mimeType) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }


    private static int getColorFormat(MediaCodecInfo mediaCodecInfo) {
        int matchedForamt = 0;
        MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        for (int i = 0; i < codecCapabilities.colorFormats.length; i++) {
            int format = codecCapabilities.colorFormats[i];
            if (format >= MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar && format <= MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
                if (format >= matchedForamt) {
                    matchedForamt = format;
                }
            }
        }
        switch (matchedForamt) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                Log.i(TAG, "selected yuv420p");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                Log.i(TAG, "selected yuv420pp");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                Log.i(TAG, "selected yuv420sp");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                Log.i(TAG, "selected yuv420psp");
                break;

        }
        return matchedForamt;
    }

    void encodeData(byte[] data,int colorFormat){
        if (colorFormat == -1) {
            Yuv420Util.Nv21ToYuv420SP(data, videobytes, build.getVideoWidth(), build.getVideoHeight());
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            Yuv420Util.Nv21ToYuv420SP(data, videobytes, build.getVideoWidth(), build.getVideoHeight());
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            Yuv420Util.Nv21ToI420(data, videobytes, build.getVideoWidth(), build.getVideoHeight());
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible) {
            // Yuv420_888
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
            // Yuv420packedPlannar 和 yuv420sp很像
            // 区别在于 加入 width = 4的话 y1,y2,y3 ,y4公用 u1v1
            // 而 yuv420dp 则是 y1y2y5y6 共用 u1v1
            //这样处理的话颜色核能会有些失真。
            Yuv420Util.Nv21ToYuv420SP(data, videobytes, build.getVideoWidth(), build.getVideoHeight());
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
        } else {
            System.arraycopy(data, 0, videobytes, 0, data.length);
        }
        if (build.getVideoEncodeType() == VideoEncodeType.HARD){
            if(LiveConfig.isBack){
                n420_spin(videoXZ,videobytes,build.getVideoWidth(),build.getVideoHeight());
            }else {
                n420_spin2(videoXZ,videobytes,build.getVideoWidth(),build.getVideoHeight());
            }
            encodeVideoData(videoXZ,data);
        } else {
            if(null != videoListener){
                videoListener.videoToYuv420(videobytes);
            }
        }

        //处理完成之后调用 addCallbackBuffer()
        if (preTime != 0) {
            // 延时 防止卡顿 增加视频效果
            int shouldDelay = (int) (1000.0 / build.getFps());
            int realDelay = (int) (System.currentTimeMillis() - preTime);
            int delta = shouldDelay - realDelay;
            if (delta > 0) {
                try {
                    Thread.sleep(delta);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(null != videoListener){
            videoListener.addCallbackBuffer(data);
        }
        preTime = System.currentTimeMillis();
    }

    /**
     * 逆时针旋转
     */
    private void n420_spin(byte[] dstyuv, byte[] srcdata, int imageWidth, int imageHeight) {
        int i = 0, j = 0;
        int index = 0;
        int tempindex = 0;
        int div = 0;

        for (i = 0; i < imageWidth; i++) {
            div = imageWidth -i;//顺时针旋转90度
//           div = i + 1; //逆时针旋转90度
            tempindex = 0;
            for (j = 0; j < imageHeight; j++) {
                tempindex += imageWidth;
                dstyuv[index++] = srcdata[tempindex - div];
            }
        }
        //u起始位置
        int start = imageWidth * imageHeight;
        //u v 数据的长度
        int udiv = start >> 2;
        //u v 数据宽度
        int uWidth = imageWidth >> 1;
        //u v 数据高度
        int uHeight = imageHeight >> 1;
        //数据 下标位置
        index = start;
        for (i = 0; i < uWidth; i++) {
//        div = i + 1; //逆时针旋转90度
            div = uHeight -i;//顺时针旋转90度
            tempindex = start;
            for (j = 0; j < uHeight; j++) {
                tempindex += uHeight;
                dstyuv[index] = srcdata[tempindex - div];
                dstyuv[index + udiv] = srcdata[tempindex - div + udiv];
                index++;
            }
        }
    }

    /**
     * 顺时针旋转
     */
    private void n420_spin2(byte[] dstyuv, byte[] srcdata, int imageWidth, int imageHeight){
        int i = 0, j = 0;
        int index = 0;
        int tempindex = 0;
        int div = 0;

        for (i = 0; i < imageWidth; i++) {
//            div = imageWidth -i;//顺时针旋转90度
           div = i + 1; //逆时针旋转90度
            tempindex = 0;
            for (j = 0; j < imageHeight; j++) {
                tempindex += imageWidth;
                dstyuv[index++] = srcdata[tempindex - div];
            }
        }
        //u起始位置
        int start = imageWidth * imageHeight;
        //u v 数据的长度
        int udiv = start >> 2;
        //u v 数据宽度
        int uWidth = imageWidth >> 1;
        //u v 数据高度
        int uHeight = imageHeight >> 1;
        //数据 下标位置
        index = start;
        for (i = 0; i < uWidth; i++) {
        div = i + 1; //逆时针旋转90度
//            div = uHeight -i;//顺时针旋转90度
            tempindex = start;
            for (j = 0; j < uHeight; j++) {
                tempindex += uHeight;
                dstyuv[index] = srcdata[tempindex - div];
                dstyuv[index + udiv] = srcdata[tempindex - div + udiv];
                index++;
            }
        }
    }


    /**
     * 视频硬编码编码
     */
    private void encodeVideoData(byte[] dstByte,byte[] data) {
        ByteBuffer[] inputBuffers = vEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = vEncoder.getOutputBuffers();
        int inputBufferId = vEncoder.dequeueInputBuffer(-1);
        if (inputBufferId >= 0) {
            // fill inputBuffers[inputBufferId] with valid data
            ByteBuffer bb = inputBuffers[inputBufferId];
            bb.clear();
            bb.put(dstByte, 0, dstByte.length);
            long pts = new Date().getTime() * 1000 - presentationTimeUs;
            vEncoder.queueInputBuffer(inputBufferId, 0, dstByte.length, pts, 0);
        }

        int outputBufferId = vEncoder.dequeueOutputBuffer(vBufferInfo, 0);
        if (outputBufferId >= 0) {
            // outputBuffers[outputBufferId] is ready to be processed or rendered.
            ByteBuffer bb = outputBuffers[outputBufferId];
//            Log.d("encodeData5",dstByte.length+"");
            if (null != videoListener) {
                videoListener.videoToHard(bb, vBufferInfo);
            }
            vEncoder.releaseOutputBuffer(outputBufferId, false);
        }
        if (null != videoListener) {
            videoListener.addCallbackBuffer(data);
        }
    }

    interface OnVideoEncodeListener{

        void videoToYuv420(byte[] yuv);

        void videoToHard(ByteBuffer bb, MediaCodec.BufferInfo vBufferInfo);

        void addCallbackBuffer(byte[] old);

    }

}
