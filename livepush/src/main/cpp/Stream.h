//
// Created by 83642 on 2017/8/28.
//

#ifndef LIVE_RTMP2_STREAM_H
#define LIVE_RTMP2_STREAM_H

#include <android/log.h>
#define LOGD(...) __android_log_print(3,"NDK",__VA_ARGS__)

#include <pthread.h>
#include <queue>
#include <strings.h>
#include <jni.h>
#include <stdio.h>

extern "C"{
#include "include/x264/x264.h"
#include "librtmp/rtmp.h"
#include "include/faac/faac.h"
#define _RTMP_Free(_rtmp)  if(_rtmp) {RTMP_Free(_rtmp); _rtmp = NULL;}
#define _RTMP_Close(_rtmp)  if(_rtmp && RTMP_IsConnected(_rtmp)) RTMP_Close(_rtmp);


using namespace std;

typedef unsigned char UCHAR;

class Stream {

public:
    int pushing;
private:
    JavaVM* jvm;
    jobject jobj;

    int mWidth;
    int mHeight;
    int mBitrate;
    int isEncoding;
    int readyPushing;
    int start_time;
    int startpushing;

    UCHAR* aacSpecificInfos;
    char* path;

    RTMP *rtmpClient;

    queue<RTMPPacket*> queues;

    x264_t *videoEncHandle;
    x264_picture_t* pic_in;
    x264_picture_t* pic_out;

    faacEncHandle audioEncHandle;
    unsigned long nInputSamples;
    unsigned long nMaxOutputBytes;
    int canAudioEncode;
    FILE* file_264;

    pthread_t pushertId;
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
    pthread_mutex_t encoder_mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t encoder_cond = PTHREAD_COND_INITIALIZER;

public:
    Stream(JavaVM* vm, jobject obj):
            jvm(vm),jobj(obj),pushing(0),mWidth(0),mHeight(0),isEncoding(0),readyPushing(0),start_time(0)
    ,startpushing(0),aacSpecificInfos(NULL),audioEncHandle(NULL){}

    virtual ~Stream(){
        if(aacSpecificInfos){
            delete aacSpecificInfos;
            aacSpecificInfos = 0;
        }
    }

    void setAacSpecificInfos(UCHAR* infos){
        aacSpecificInfos = new UCHAR[2];
        memset(aacSpecificInfos,0,2);
        memcpy(aacSpecificInfos,infos,2);
    }


public:
    void init(const char* rtmpPath,int width,int heitht,int bitrate);

    void startPusher();

    void initVideoEncoder(int threadSize);
    void initAudioEncoder(int sampleRate, int channel);

    void stop();

    void add_video_yuv420(UCHAR*,jboolean);
    void add_264_header(UCHAR*, UCHAR*, int, int);
    void add_264_body(UCHAR *, int);

    void add_audio_data(UCHAR*);
    void sendAacSpec(UCHAR*,int);
    void sendAacData(UCHAR *data, int len,long timestamp);

private:

    void addPacket(RTMPPacket *packet);

    void throwNativeInfo(JNIEnv *, jmethodID, jint);

    void add_aac_header();

    static void *pusher(void*);

    void n420_spin(char *dstyuv, char *srcdata, int imageWidth, int imageHeight);

    void n420_spin2(char *dstyuv, char *srcdata, int imageWidth, int imageHeight);

    void sendAacSoftData(UCHAR*, int );
};

}




#endif //LIVE_RTMP2_STREAM_H
