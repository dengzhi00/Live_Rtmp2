package com.dzm.rtmp2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.dzm.collector.LiveRop;
import com.dzm.collector.VideoEncodeType;
import com.dzm.ffmpeg.LiveFfmpegManager;

/**
 *
 * @author 邓治民
 * date 2017/9/2 11:36
 */

public class PushActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private LiveRop liveRop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);
        SurfaceView push_serface = (SurfaceView) findViewById(R.id.push_serface);
        push_serface.setKeepScreenOn(true);
        SurfaceHolder mSurfaceHolder = push_serface.getHolder();
        mSurfaceHolder.addCallback(this);

        Bundle bundle = getIntent().getExtras();
        String path = bundle.getString("path");
        int width = bundle.getInt("width");
        int height = bundle.getInt("height");
        int encodeType = bundle.getInt("encodeType");
        VideoEncodeType type ;
        if(encodeType == 100){
            type = VideoEncodeType.SOFT;
        } else if(encodeType == 200){
            type = VideoEncodeType.HARD;
        }else {
            type = VideoEncodeType.SOFT;
        }

        int bitrate = bundle.getInt("bitrate");

//        liveRop =  LiveManager.build(this, new OnRtmpConnectListener() {
//            @Override
//            public void rtmpConnect(String msg, int code) {
//                Toast.makeText(PushActivity.this,msg+code,Toast.LENGTH_SHORT).show();
//            }
//        })
//                .setHolder(mSurfaceHolder)
//                .setRtmpUrl(path)
//                .setVideoWidth(width)
//                .setVideoEncodeType(type)
//                .setVideoHeight(height)
//                .setBitrate(bitrate*1000);

        liveRop =  LiveFfmpegManager.build(this)
                .setHolder(mSurfaceHolder)
                .setRtmpUrl(path)
                .setVideoWidth(width)
                .setVideoEncodeType(type)
                .setVideoHeight(height)
                .setBitrate(bitrate*1000);
        liveRop.initEncode();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        liveRop.startEncode();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveRop.releaseEncode();
    }
}
