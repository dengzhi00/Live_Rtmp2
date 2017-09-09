package com.dzm.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.dzm.collector.LiveRop;
import com.dzm.collector.VideoEncodeType;
import com.dzm.live.LiveManager;
import com.dzm.live.OnRtmpConnectListener;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private LiveRop liveRop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPlayer();

        MediaSerfaceView msv_media = (MediaSerfaceView) findViewById(R.id.msv_media);
        msv_media.play("rtmp://ip:1935/live/abctest1");

        SurfaceView my_seruface = (SurfaceView) findViewById(R.id.my_seruface);
        my_seruface.setZOrderOnTop(true);
        SurfaceHolder surfaceHolder = my_seruface.getHolder();
        surfaceHolder.addCallback(this);
        liveRop =  LiveManager.build(this, new OnRtmpConnectListener() {
            @Override
            public void rtmpConnect(String msg, int code) {
                Toast.makeText(MainActivity.this,msg+code,Toast.LENGTH_SHORT).show();
            }
        })
                .setHolder(surfaceHolder)
                .setRtmpUrl("rtmp://ip:1935/live/abctest1")
                .setVideoWidth(480)
                .setVideoEncodeType(VideoEncodeType.HARD)
                .setVideoHeight(480);
        liveRop.initEncode();

    }

    private void initPlayer(){
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
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
