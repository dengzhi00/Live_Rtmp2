//
// Created by 83642 on 2017/8/28.
//

//#include <include/x264/x264.h>
#include "Stream.h"
extern "C"{

void Stream::throwNativeInfo(JNIEnv *env, jmethodID methodId, jint code) {
    if(env && methodId && jobj){
        env->CallVoidMethod(jobj,methodId,code);
    } else
        LOGD("通知java层失败:%d",code);
}

void Stream::init(const char* url, int width, int height,int bitrate) {
    LOGD("video pusher path:%s width:%d,height:%d,bitrate:%d", url, width,height, bitrate);
    if(path)
        int m = !strcmp(url,path);
    mWidth = width;
    mHeight = height;
    mBitrate = bitrate;
    path = (char *) new UCHAR[strlen(url) + 1];
//    initVideoEncoder();
    memset(path,0,strlen(url) + 1);
    memcpy(path,url,strlen(url));
//    file_264 = fopen("/sdcard/live.h264","w+");

}

void Stream::startPusher() {
    stop();
    while (startpushing);//等待清除内存
    //线程推流
    LOGD("开启推流线程");
    pthread_create(&pushertId,NULL,Stream::pusher,this);
}


void Stream::stop() {
    pthread_mutex_lock(&mutex);
    LOGD("停止推流");
    pushing = 0;
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}

void Stream::initVideoEncoder(int threadSize) {
    LOGD("video encoder setting");
    x264_param_t param;
    //ultrafast cpu占用最小，zerolatency 不缓存帧
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    param.i_csp = X264_CSP_I420;
    //配置x264编码器参数 宽高调换，因为视频解码参数为旋转90度的参数
    param.i_width = mHeight;
    param.i_height = mWidth;
    param.i_keyint_max = 10*2;
    param.i_fps_num = 10;//帧率分子
    param.i_fps_den = 1;//帧率分母
    param.i_threads = 1; // 建议为cpu个数
    param.b_repeat_headers = 1;//复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
//    param.b_cabac = 1; //自适应上下文算术编码，baseline 不支持

    param.rc.i_bitrate = mBitrate/1000;//码率(比特率,单位Kbps)
    param.rc.i_rc_method = X264_RC_ABR;//参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    param.rc.i_vbv_buffer_size = mBitrate/1000;//设置了i_vbv_max_bitrate必须设置此参数，码率控制区大小,单位kbps
    param.rc.i_vbv_max_bitrate = (int) (mBitrate / 1000 * 1.2);//瞬时最大码率
    param.rc.f_rf_constant_max  =45;
    param.rc.b_mb_tree = 0;


    x264_param_apply_profile(&param,"baseline");
    LOGD("open video encoder");
    videoEncHandle = x264_encoder_open(&param);
    pic_in = (x264_picture_t *) malloc(sizeof(x264_picture_t));
    pic_out = (x264_picture_t *) malloc(sizeof(x264_picture_t));
    //配置x264编码器参数 宽高调换，因为视频解码参数为旋转90度的参数
    x264_picture_alloc(pic_in,X264_CSP_I420,param.i_width,param.i_height);
    x264_picture_init(pic_out);
}

void Stream::initAudioEncoder(int sampleRate, int channel) {
    LOGD("init_audio_emcoder   sampleRate :%d    channel:%d",sampleRate,channel);
    canAudioEncode = 0;
    JNIEnv *env;
    jvm->AttachCurrentThread(&env,0);
    jclass clazz = env->GetObjectClass(jobj);
    jmethodID postID = env->GetMethodID(clazz,"onNativeResponse","(I)V");
    if(audioEncHandle){
        canAudioEncode = 1;
        LOGD("音频编码已打开");
        throwNativeInfo(env, postID, -200);
        return;
    }
    /**
 * 初始化aac句柄，同时获取最大输入样本，及编码所需最小字节
 * sampleRate 采样率 channel声道数 nInputSamples输入样本数 nMaxOutputBytes输出所需最大空间
 */
    audioEncHandle = faacEncOpen((unsigned long) sampleRate, (unsigned int) channel, &nInputSamples, &nMaxOutputBytes);

    if(!audioEncHandle){
        LOGD("音频打开失败");
        throwNativeInfo(env, postID, -201);
        return;
    }
    faacEncConfigurationPtr  faac_enc_config = faacEncGetCurrentConfiguration(audioEncHandle);
    faac_enc_config->mpegVersion = MPEG4;
    faac_enc_config->allowMidside = 1;
    //LC编码
    faac_enc_config->aacObjectType = LOW;
    //输出是否包含ADTS头
    faac_enc_config->outputFormat = 0;
    //时域噪音控制,大概就是消爆音
    faac_enc_config->useTns = 1;
    faac_enc_config->useLfe = 0;
    faac_enc_config->inputFormat = FAAC_INPUT_16BIT;
    faac_enc_config->quantqual = 100;
    //频宽
    faac_enc_config->bandWidth = 0;
    faac_enc_config->shortctl = SHORTCTL_NORMAL;
    if(!faacEncSetConfiguration(audioEncHandle,faac_enc_config)){
        LOGD("参数配置失败");
        throwNativeInfo(env, postID, -202);
        return;
    }
    canAudioEncode = 1;
}

void Stream::add_video_yuv420(UCHAR * yuv,jboolean isBack) {
    UCHAR * buf = new UCHAR[mWidth*mHeight*3/2];
    //旋转算法 次算法为顺时针旋转90度算法
    if(isBack){
        n420_spin((char *) buf, (char *) yuv, mWidth, mHeight);
    } else{
        n420_spin2((char *) buf, (char *) yuv, mWidth, mHeight);
    }

    isEncoding = 1;
    if(!readyPushing || !pushing){
        LOGD("pusher thread is not start readyPushing:%d  pushing:%d",readyPushing,pushing);
        pthread_mutex_lock(&encoder_mutex);
        pthread_cond_signal(&encoder_cond);
        pthread_mutex_unlock(&encoder_mutex);
        isEncoding = 0;
        return;
    }
    pthread_mutex_lock(&encoder_mutex);
    memcpy(pic_in->img.plane[0], buf, (size_t) (mWidth * mHeight));
    memcpy(pic_in->img.plane[1],buf+mWidth*mHeight, (size_t) (mWidth * mHeight >> 2));
    memcpy(pic_in->img.plane[2],buf+mWidth*mHeight+(mWidth*mHeight>>2),(size_t) (mWidth * mHeight >> 2));
    free(buf);
    int nNal = -1;
    x264_nal_t *nal = NULL;
    long  l = (long) RTMP_GetTime();
//    LOGD("编码开始");
    if(!x264_encoder_encode(videoEncHandle,&nal,&nNal,pic_in,pic_out)){
        LOGD("encode faile");
        pthread_cond_signal(&encoder_cond);
        pthread_mutex_unlock(&encoder_mutex);
        isEncoding = 0;
        return;
    }
    pic_in->i_pts++;
    pic_out->i_pts++;
    int sps_len,pps_len;
    UCHAR *sps;
    UCHAR *pps;
    for (int i = 0; i < nNal; i++) {
        if(nal[i].i_type == NAL_SPS){
            sps_len = nal[i].i_payload - 4;
            sps = (UCHAR *) malloc((size_t) (sps_len + 1));
            memcpy(sps,nal[i].p_payload+4, (size_t) sps_len);
        } else if(nal[i].i_type == NAL_PPS){
            pps_len = nal[i].i_payload - 4;
            pps = (UCHAR *) malloc((size_t) (pps_len + 1));
            memcpy(pps,nal[i].p_payload+4, (size_t) pps_len);
            add_264_header(pps,sps,pps_len,sps_len);
            free(sps);
            free(pps);
        } else{
            add_264_body(nal[i].p_payload,nal[i].i_payload);
        }
    }
//    LOGD("编码完成");
    pthread_cond_signal(&encoder_cond);
    pthread_mutex_unlock(&encoder_mutex);
    isEncoding = 0;
}

void Stream::n420_spin(char *dstyuv, char *srcdata, int imageWidth, int imageHeight) {
    int i = 0, j = 0;
    int index = 0;
    int tempindex = 0;
    int div = 0;
    for (i = 0; i < imageWidth; i++) {
//        div = i + 1; //逆时针旋转90度
        div = imageWidth -i;//顺时针旋转90度
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

void Stream::n420_spin2(char *dstyuv, char *srcdata, int imageWidth, int imageHeight) {
    int i = 0, j = 0;
    int index = 0;
    int tempindex = 0;
    int div = 0;
    for (i = 0; i < imageWidth; i++) {
        div = i + 1; //逆时针旋转90度
//        div = imageWidth -i;//顺时针旋转90度
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
//        div = uHeight -i;//顺时针旋转90度
        tempindex = start;
        for (j = 0; j < uHeight; j++) {
            tempindex += uHeight;
            dstyuv[index] = srcdata[tempindex - div];
            dstyuv[index + udiv] = srcdata[tempindex - div + udiv];
            index++;
        }
    }
}

void Stream::add_264_header(UCHAR *pps, UCHAR *sps, int pps_len, int sps_len) {
    int body_size = 13+sps_len + 3 + pps_len;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Reset(packet);
    if(!RTMPPacket_Alloc(packet,body_size)){
        free(packet);
        return;
    }
    char *body = packet->m_body;
    int i = 0;
    body[i++] = 0x17;
    body[i++] = 0x00;
    //composition time 0x000000
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = 0x01;
    body[i++] = sps[1];
    body[i++] = sps[2];
    body[i++] = sps[3];
    body[i++] = (char) 0xff;
    //sps
    body[i++] = (char) 0xE1;
    body[i++] = (char) ((sps_len >> 8) & 0xff);
    body[i++] = (char) (sps_len & 0xff);
    memcpy(&body[i], sps, (size_t) sps_len);
    i += sps_len;
    //pps
    body[i++] = 0x01;
    body[i++] = (char) ((pps_len >> 8) & 0xff);
    body[i++] = (char) (pps_len & 0xff);
    memcpy(&body[i], pps, (size_t) pps_len);
    i+=pps_len;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    addPacket(packet);

}

void Stream::add_264_body(UCHAR *buf, int len) {
//    fwrite(buf, 1, (size_t) len, file_264);
    //去掉帧界定符 00 00 00 01
    if(buf[2] == 0x00){
        buf +=4;
        len -=4;
    } else if(buf[2] == 0x01){//00 00 01
        buf += 3;
        len -= 2;
    }
    int body_size = len + 9;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet,body_size);
    char *body = packet->m_body;
    int type = buf[0] & 0x1f;
    body[0] = 0x27;
    if(type == NAL_SLICE_IDR){
        body[0] = 0x17;
    }
    body[1] = 0x01;
    body[2] = 0x00;
    body[3] = 0x00;
    body[4] = 0x00;

    body[5] = (char) ((len >> 24) & 0xff);
    body[6] = (char) ((len >> 16) & 0xff);
    body[7] = (char) ((len >> 8) & 0xff);
    body[8] = (char) (len & 0xff);

    memcpy(&body[9], buf, (size_t) len);
    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nTimeStamp = RTMP_GetTime() - start_time;
    addPacket(packet);

}

void Stream::add_audio_data(UCHAR * data) {
    if(!audioEncHandle || !canAudioEncode){
        LOGD("音频编码未开启");
        return;
    }
    if(!readyPushing || !pushing){
        LOGD("pusher thread is not start readyPushing:%d  pushing:%d",readyPushing,pushing);
        pthread_mutex_lock(&encoder_mutex);
        pthread_cond_signal(&encoder_cond);
        pthread_mutex_unlock(&encoder_mutex);
        isEncoding = 0;
        return;
    }
    unsigned char* bitbuf = (unsigned char *) malloc(nMaxOutputBytes * sizeof(unsigned char*));
    //  描述 : 编码一桢信息
//  hEncoder : faacEncOpen返回的编码器句柄
//  inputBuffer : 输入信息缓冲区
//  samplesInput : faacEncOpen编码后的数据长度，即缓冲区长度
//  outputBuffer ： 编码后输出信息缓冲区
//  bufferSize : 输出信息长度
    int data_len = faacEncEncode(audioEncHandle, (int32_t *) data, nInputSamples, bitbuf, nMaxOutputBytes);
    if(data_len<=0){
        if(bitbuf){
            free(bitbuf);
        }
        LOGD("音频编码失败");
        return;
    }
//    LOGD("音频编码成功");
    sendAacSoftData(bitbuf, data_len);

}

void Stream::sendAacSoftData(UCHAR * bitbuf, int data_len) {
    int body_size = data_len + 2;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    if(!packet){
        return;
    }
    RTMPPacket_Reset(packet);
    if(!RTMPPacket_Alloc(packet, (uint32_t) body_size)){
        RTMPPacket_Free(packet);
        return;
    }
    char *body = packet->m_body;
    body[0] = (char) 0xaf;
    //faac 体为0x01
    body[1] = 0x01;
    memcpy(&body[2], bitbuf, (size_t) data_len);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = RTMP_GetTime() - start_time;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    addPacket(packet);
}

void Stream::add_aac_header() {
//    sendAacSpec(aacSpecificInfos,2);

    unsigned char* data;
    //长度
    unsigned long len;
    faacEncGetDecoderSpecificInfo(audioEncHandle,&data,&len);
    sendAacSpec(data,2);
}


void Stream::sendAacSpec(UCHAR *data,int len) {
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Reset(packet);
    if(!RTMPPacket_Alloc(packet,len+2)){
        free(packet);
        return;
    }
    char *body = packet->m_body;
    body[0] = (char) 0xAF;
    body[1] = 0x00;
    memcpy(&body[2],data,len);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = (uint32_t) (len + 2);
    packet->m_nChannel = 0x05;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmpClient->m_stream_id;

    addPacket(packet);
}

void Stream::sendAacData(UCHAR *data, int len, long timestamp) {
    if(len>0){
        RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
        RTMPPacket_Reset(packet);
        if(!RTMPPacket_Alloc(packet,len+2)){
            free(packet);
            return;
        }
        char *body = packet->m_body;
        body[0] = (char) 0xAF;
        body[1] = 0x01;
        memcpy(&body[2], data, (size_t) len);

        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nBodySize = (uint32_t) (len + 2);
        packet->m_nChannel = 0x05;
        packet->m_nTimeStamp = (uint32_t) timestamp;
        packet->m_hasAbsTimestamp = 0;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        packet->m_nInfoField2 = rtmpClient->m_stream_id;

        addPacket(packet);
    }
}

void Stream::addPacket(RTMPPacket *packet) {
    pthread_mutex_lock(&mutex);
    if(readyPushing){
        queues.push(packet);
    } else{
        RTMPPacket_Free(packet);
        free(packet);
    }
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}


void *Stream::pusher(void *args) {
    LOGD("线程开启");
    Stream *stream = (Stream *) args;
    stream->startpushing = 1;
    stream->pushing = 1;
    JNIEnv *env;
    stream->jvm->AttachCurrentThread(&env,0);
    jclass clazz = env->GetObjectClass(stream->jobj);
    jmethodID postID = env->GetMethodID(clazz,"onNativeResponse","(I)V");
    do {
        stream->rtmpClient = RTMP_Alloc();
        if(!stream->rtmpClient){
            stream->throwNativeInfo(env,postID,-101);
            goto END;
        }
        RTMP_Init(stream->rtmpClient);
        stream->rtmpClient->Link.timeout = 3;
        stream->rtmpClient->Link.flashVer = RTMP_DefaultFlashVer;
        if(!RTMP_SetupURL(stream->rtmpClient,stream->path)){
            stream->throwNativeInfo(env,postID,-102);
            goto END;
        }
        RTMP_EnableWrite(stream->rtmpClient);
        if(!RTMP_Connect(stream->rtmpClient,NULL)){
            stream->throwNativeInfo(env, postID, -103);
            goto END;
        }
        if(!RTMP_ConnectStream(stream->rtmpClient,0)){
            stream->throwNativeInfo(env, postID, -104);
            goto END;
        }

        stream->throwNativeInfo(env,postID,100);
        stream->readyPushing = 1;
        stream->start_time = RTMP_GetTime();
        //添加音频头
//        if(stream->aacSpecificInfos){
//            stream->add_aac_header();
//        }
        if(stream->audioEncHandle){
            LOGD("*************************1");
            stream->add_aac_header();
        }

        while (stream->pushing){
            pthread_mutex_lock(&stream->mutex);
            pthread_cond_wait(&stream->cond,&stream->mutex);
            if(!stream->pushing){
                pthread_mutex_unlock(&stream->mutex);
                goto END;
            }
            if(stream->queues.empty()){
                pthread_mutex_unlock(&stream->mutex);
                continue;
            }
            int size = stream->queues.size();
            for (int i = 0; i < size; i++) {
                if(!stream->pushing){
                    pthread_mutex_unlock(&stream->mutex);
                    goto END;
                }
                RTMPPacket *packet = stream->queues.front();
                stream->queues.pop();
                if(packet){
                    packet->m_nInfoField2 = stream->rtmpClient->m_stream_id;
                    int j = RTMP_SendPacket(stream->rtmpClient,packet,1);
                    if(!j){
                        LOGD("RTMP_SendPacket fail");
                        RTMPPacket_Free(packet);
                        free(packet);
                        stream->throwNativeInfo(env,postID,-105);
                        pthread_mutex_unlock(&stream->mutex);
                        goto END;
                    }
                    RTMPPacket_Free(packet);
                    free(packet);
                }
            }
            pthread_mutex_unlock(&stream->mutex);
        }
        END:
        _RTMP_Close(stream->rtmpClient);
        _RTMP_Free(stream->rtmpClient);
        stream->rtmpClient = 0;
    }while (0);
    if(stream->pic_in){
        free(stream->pic_in);
        x264_picture_clean(stream->pic_in);
    }
    if(stream->pic_out){
        free(stream->pic_out);
    }
    if(stream->videoEncHandle){
        x264_encoder_close(stream->videoEncHandle);
        stream->videoEncHandle = NULL;
    }

    LOGD("----------- stop pusher release ");
    stream->readyPushing = 0;
    delete stream->path;
    stream->path = NULL;
    int size = stream->queues.size();
    LOGD("----------- release queue");
    for (int i = 0; i < size; ++i) {
        RTMPPacket *packet = stream->queues.front();
        stream->queues.pop();
        if (packet) {
            RTMPPacket_Free(packet);
            free(packet);
        }
    }
    LOGD("----------- notify java status");
    stream->throwNativeInfo(env, postID, 101); //java不会再接收数据回调
    LOGD("----------- detach jvm thread");
    stream->jvm->DetachCurrentThread();
    stream->startpushing = 0;
    stream->pushing = 0;
    pthread_mutex_lock(&stream->encoder_mutex);
    if (stream->isEncoding) {
        pthread_cond_wait(&stream->encoder_cond, &stream->encoder_mutex); //等待一轮编码完成
    }
    LOGD("----------- close x264");
    if (stream->videoEncHandle) {
        x264_encoder_close(stream->videoEncHandle);
    }
    stream->videoEncHandle = 0;
    LOGD("----------- clear pic_in");
    if (stream->pic_in) {
        x264_picture_clean(stream->pic_in);
        free(stream->pic_in);
    }
    stream->pic_in = 0;
    LOGD("----------- clear pic_out");
    if (stream->pic_out) {
        free(stream->pic_out);
    }
    stream->canAudioEncode = 0;
//    if(stream->audioEncHandle){
//        faacEncClose(&stream->audioEncHandle);
//    }
    stream->pic_out = 0;
    pthread_mutex_unlock(&stream->encoder_mutex);
    LOGD("pusher exit");
    pthread_exit(NULL);
    return nullptr;
}

}




























































