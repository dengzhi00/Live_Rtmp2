//#include <jni.h>
//#include <stdio.h>

#include "Stream.h"

extern "C" {

JavaVM *jvm = 0;
jobject obj = 0;

Stream *stream;


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
	jvm = vm;
	JNIEnv* env = NULL;
	jint result = -1;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		return result;
	}
	return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL Java_com_dzm_live_LivePusher_initLive(JNIEnv *env,
		jobject thiz, jstring rtmpUrl_,jint width, jint height,jint bitrate) {
	if (!obj) {
		obj = env->NewGlobalRef(thiz);
	}
	stream = new Stream(jvm,obj);
	if(stream){
		const char *rtmpUrl = env->GetStringUTFChars(rtmpUrl_, 0);
		stream->init(rtmpUrl,width,height,bitrate);
		env->ReleaseStringUTFChars(rtmpUrl_, rtmpUrl);
	}
}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_startPusher(JNIEnv *env, jobject instance) {
	if(!stream){
		return;
	}
	stream->startPusher();

}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_pushVideo(JNIEnv *env, jobject instance, jbyteArray vide0_,jboolean isBack) {
	if(!stream)
		return;
	jbyte *vide0 = env->GetByteArrayElements(vide0_, NULL);
	stream->add_video_yuv420((UCHAR*)vide0,isBack);

	env->ReleaseByteArrayElements(vide0_, vide0, 0);
}


JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_send_1sps_1pps(JNIEnv *env, jobject instance, jbyteArray sps_,
											jint sps_lenth, jbyteArray pps_, jint pps_lenth) {
	if(!stream)
		return;
	jbyte *sps = env->GetByteArrayElements(sps_, NULL);
	jbyte *pps = env->GetByteArrayElements(pps_, NULL);

	stream->add_264_header((UCHAR *) pps, (UCHAR *) sps, pps_lenth, sps_lenth);


	env->ReleaseByteArrayElements(sps_, sps, 0);
	env->ReleaseByteArrayElements(pps_, pps, 0);
}


JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_send_1video_1body(JNIEnv *env, jobject instance, jbyteArray body_,
											   jint body_lenth) {
	if(!stream)
		return;
	jbyte *body = env->GetByteArrayElements(body_, NULL);
	stream->add_264_body((UCHAR *) body, body_lenth);
	env->ReleaseByteArrayElements(body_, body, 0);
}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_sendAacSpec(JNIEnv *env, jobject instance, jbyteArray aac_spec_,jint len) {
	if(!stream)
		return;
	jbyte *aac_spec = env->GetByteArrayElements(aac_spec_, NULL);
	stream->sendAacSpec((UCHAR *) aac_spec, len);
	env->ReleaseByteArrayElements(aac_spec_, aac_spec, 0);
}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_sendAacData(JNIEnv *env, jobject instance,
										 jbyteArray data_, jint len, jlong timestamp) {
	if(!stream)
		return;
	jbyte *data = env->GetByteArrayElements(data_, NULL);
	stream->sendAacData((UCHAR *) data, len, (long) timestamp);
	env->ReleaseByteArrayElements(data_, data, 0);
}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_setAacSpecificInfos(JNIEnv *env, jobject instance, jbyteArray data_) {
	if(!stream)
		return;
	jbyte *data = env->GetByteArrayElements(data_, NULL);
	stream->setAacSpecificInfos((UCHAR *) data);
	env->ReleaseByteArrayElements(data_, data, 0);
}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_initVedioEncode(JNIEnv *env, jobject instance,jint threadSize) {

	if(!stream)
		return;
	stream->initVideoEncoder(threadSize);

}


JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_initAudioEncode(JNIEnv *env, jobject instance, jint sampleRate,
											 jint channel) {
	if(!stream)
		return;
	stream->initAudioEncoder(sampleRate,channel);

}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_addAudioData(JNIEnv *env, jobject instance, jbyteArray data_) {
	if(!stream)
		return;
	jbyte *data = env->GetByteArrayElements(data_, NULL);
	stream->add_audio_data((UCHAR *) data);
	env->ReleaseByteArrayElements(data_, data, 0);
}

JNIEXPORT void JNICALL
Java_com_dzm_live_LivePusher_release(JNIEnv *env, jobject instance) {

	LOGD("live pusher release begin");
	if (stream) {
		stream->stop();
		delete stream;
	}
	stream = 0;
	obj = 0;
	LOGD("live pusher release end");
}
}
