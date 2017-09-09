
extern "C"
{
#include "Stream.h"

Stream *stream;

JNIEXPORT void JNICALL
Java_com_dzm_ffmpeg_LivePusher_init(JNIEnv *env, jobject instance, jint width, jint height,
                                    jstring rtmpPath_) {


    stream = new Stream();
    if(stream){
        const char *rtmpPath = env->GetStringUTFChars(rtmpPath_, 0);
        stream->init_ffmpeg((char *) rtmpPath, width, height);
        env->ReleaseStringUTFChars(rtmpPath_, rtmpPath);
    }
}

JNIEXPORT void JNICALL
Java_com_dzm_ffmpeg_LivePusher_addVideo(JNIEnv *env, jobject instance, jbyteArray data_,
                                        jboolean isBack) {
    if(!stream){
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    stream->add_yuv_data((UCHAR *) data, isBack);

    env->ReleaseByteArrayElements(data_, data, 0);
}



}