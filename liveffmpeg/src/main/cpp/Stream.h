//
// Created by 83642 on 2017/9/2.
//

#ifndef LIVE_RTMP2_STREAM_H
#define LIVE_RTMP2_STREAM_H

#include <strings.h>
#include <jni.h>
#include <stdio.h>

#include <android/log.h>

#define LOGD(...) __android_log_print(3,"NDK",__VA_ARGS__)
extern "C"
{
#include "include/libavformat/avformat.h"
#include "include/libavutil/opt.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/time.h"

typedef unsigned char UCHAR;

class Stream {

private:

    int mWidth;
    int mHeight;

    //输出
    AVOutputFormat              *pOutFormt;
    AVFormatContext             *pOfmtCtx;

    //编码
    //编码设置
    AVFormatContext             *pEncodeFmtCtx;
    AVCodec                     *pEncodeCodec;
    AVCodecContext              *pEncodeCtx;
    AVStream                    *video_st;
    int64_t start_time;
    AVPacket                     enc_pkt;
    int                          framecnt;


public:
    void init_ffmpeg(char*rtmp_path,int width,int height);

    void add_yuv_data(UCHAR * yuv,jboolean isBack);

private:
    int init_encode(int width,int height);

    void n420_spin(char *dstyuv, char *srcdata, int imageWidth, int imageHeight);

    void n420_spin2(char *dstyuv, char *srcdata, int imageWidth, int imageHeight);
};


};




#endif //LIVE_RTMP2_STREAM_H
