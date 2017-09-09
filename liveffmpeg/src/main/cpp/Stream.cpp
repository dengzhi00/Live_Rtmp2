//
// Created by 83642 on 2017/9/2.
//

#include "Stream.h"

extern "C"
{

void Stream::init_ffmpeg(char *rtmp_path, int width, int height) {
    LOGD("开始初始化 rtmp_path:%s",rtmp_path);
    mWidth = width;
    mHeight = height;

    av_register_all();

    avformat_network_init();

    //输出
    avformat_alloc_output_context2(&pOfmtCtx,NULL,"flv",rtmp_path);

    if(!pOfmtCtx){
        LOGD("Could not create output context");
        return;
    }
    //初始化视频编码器
    if(init_encode(width,height) < 0){
        LOGD("init_encode fail!");
        return;
    }

    video_st = avformat_new_stream(pOfmtCtx,pEncodeCodec);

    if(video_st  == NULL){
        LOGD("video_st is NULL!");
        return;
    }

    video_st->time_base.num = 1;
    video_st->time_base.den = 25;
    video_st->codec = pEncodeCtx;

    if(avio_open(&pOfmtCtx->pb,rtmp_path,AVIO_FLAG_READ_WRITE) < 0){
        LOGD("Failed to open output file!\n");
        return;
    }

    avformat_write_header(pOfmtCtx,NULL);

    start_time = av_gettime();
    LOGD("完成初始化");
}

int Stream::init_encode(int width, int height) {
    pEncodeFmtCtx = avformat_alloc_context();

    pEncodeCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if(!pEncodeCodec){
        LOGD("Can not find encoder!\n");
        return -1;
    }
    pEncodeCtx = avcodec_alloc_context3(pEncodeCodec);
    pEncodeCtx->pix_fmt = PIX_FMT_YUV420P;
    pEncodeCtx->width = height;
    pEncodeCtx->height = width;
    pEncodeCtx->time_base.num = 1;
    pEncodeCtx->time_base.den = 25;
    pEncodeCtx->bit_rate = 400000;
    pEncodeCtx->gop_size = 250;

    if(pOfmtCtx->oformat->flags & AVFMT_GLOBALHEADER)
        pEncodeCtx->flags = CODEC_FLAG_GLOBAL_HEADER;
    pEncodeCtx->qmin = 10;
    pEncodeCtx->qmax = 51;

    pEncodeCtx->max_b_frames = 1;

    AVDictionary *parm = 0;

    av_opt_set(pEncodeCtx->priv_data,"preset", "superfast", 0);
    av_opt_set(pEncodeCtx->priv_data,"tune", "zerolatency", 0);

    //打开编码器
    if(avcodec_open2(pEncodeCtx,pEncodeCodec,&parm) < 0){
        LOGD("Failed to open encoder!\n");
        return -1;
    }
    return 1;
}

void Stream::add_yuv_data(UCHAR *yuv, jboolean isBack) {
//    LOGD("开始编码");
    UCHAR * buf = new UCHAR[mWidth*mHeight*3/2];
    //旋转算法 次算法为顺时针旋转90度算法
    if(isBack){
        n420_spin((char *) buf, (char *) yuv, mWidth, mHeight);
    } else{
        n420_spin2((char *) buf, (char *) yuv, mWidth, mHeight);
    }

    AVFrame *pFream = avcodec_alloc_frame();
    uint8_t  *out_buffer = (uint8_t *) av_malloc(
            (size_t) avpicture_get_size(PIX_FMT_YUV420P, pEncodeCtx->width, pEncodeCtx->height));
    avpicture_fill((AVPicture *)pFream,out_buffer,PIX_FMT_YUV420P,pEncodeCtx->width,pEncodeCtx->height);
    memcpy(pFream->data[0], buf, (size_t) (mWidth * mHeight));
    memcpy(pFream->data[1],buf+mWidth*mHeight, (size_t) (mWidth * mHeight >> 2));
    memcpy(pFream->data[2],buf+mWidth*mHeight+(mWidth*mHeight>>2),(size_t) (mWidth * mHeight >> 2));

    pFream->format = AV_PIX_FMT_YUV420P;
    pFream->width = mHeight;
    pFream->height = mWidth;

    enc_pkt.data = NULL;
    enc_pkt.size = 0;
    av_init_packet(&enc_pkt);

    int ret;
    int enc_got_frame = 0;

    ret = avcodec_encode_video2(pEncodeCtx,&enc_pkt,pFream,&enc_got_frame);
    av_frame_free(&pFream);

    if(enc_got_frame == 1){
        framecnt++;

        enc_pkt.stream_index = video_st->index;

        AVRational time_base = pOfmtCtx->streams[0]->time_base;
        AVRational r_framerate1 = {60,2};
        AVRational time_base_q = { 1, AV_TIME_BASE };
        int64_t calc_duration = (int64_t) ((double) (AV_TIME_BASE)
                                           * (1 / av_q2d(r_framerate1)));    //内部时间戳
        enc_pkt.pts = av_rescale_q(framecnt * calc_duration, time_base_q,
                                   time_base);
        enc_pkt.dts = enc_pkt.pts;
        enc_pkt.duration = (int) av_rescale_q(calc_duration, time_base_q, time_base); //(double)(calc_duration)*(double)(av_q2d(time_base_q)) / (double)(av_q2d(time_base));
        enc_pkt.pos = -1;

        //Delay
        int64_t pts_time = av_rescale_q(enc_pkt.dts, time_base, time_base_q);
        int64_t now_time = av_gettime() - start_time;
        if (pts_time > now_time)
            av_usleep((unsigned int) (pts_time - now_time));

        ret = av_interleaved_write_frame(pOfmtCtx, &enc_pkt);
//        LOGD("写入编码");
        av_free_packet(&enc_pkt);
    }

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



}










