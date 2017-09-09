package com.dzm.chat;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 *
 * @author 邓治民
 * date 2016/12/28 11:30
 * 播放rtmp 流
 */
public class MediaSerfaceView extends SurfaceView implements SurfaceHolder.Callback, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnVideoSizeChangedListener{

    private IjkMediaPlayer mediaPlayer;

//    private List<String> fileAddress;
    private String mPath;
//    private int listPositon;

//    private Object mutex = new Object();

    private SurfaceHolder surfaceHolder;

    private int streamVolume;
    private AudioManager am;

    private Context context;

    public MediaSerfaceView(Context context) {
        this(context,null);
    }

    public MediaSerfaceView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MediaSerfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//        fileAddress = new ArrayList<>();
//        fileAddress.add("rtmp://live.hkstv.hk.lxdns.com/live/hks");
//        fileAddress.add(FTPFileUtil.SDCARDPATH + "cadey.mp4");
//        fileAddress.add(FTPFileUtil.SDCARDPATH + "shell.mp4");
//        fileAddress.add(FTPFileUtil.SDCARDPATH + "test.mp4");
//        fileAddress.add(FTPFileUtil.SDCARDPATH + "a1.mp4");
//        fileAddress.add(FTPFileUtil.SDCARDPATH + "a2.mp4");
//        fileAddress.add(FTPFileUtil.SDCARDPATH + "a3.mp4");
        initPlayer();
        init();
    }

    public void init() {
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        if (fileAddress.size() > 0) {
//            listPositon = 0;
//            play(fileCheck());
//        }
    }

    public void initPlayer(){
        mediaPlayer = new IjkMediaPlayer();
//        mediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnVideoSizeChangedListener(this);
    }

    public void play(String path) {
        try {
            if (null == mediaPlayer)
                return;
            if (TextUtils.isEmpty(path)) {
                return;
            }
            this.mPath = path;
            mediaPlayer.reset();
            if (null != surfaceHolder)
                mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setDataSource(path);
//            mediaPlayer.setDataSource("rtmp://192.168.1.120/live");
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play(AssetFileDescriptor path) {
        try {
            if (null == mediaPlayer)
                return;
            if (null == path) {
                return;
            }
            mediaPlayer.reset();
            if (null != surfaceHolder)
                mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setDataSource(path.getFileDescriptor());
//            mediaPlayer.setDataSource("rtmp://192.168.1.120/live");
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        streamVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        } catch (Exception e) {
            e.printStackTrace();
            am.setStreamMute(AudioManager.STREAM_MUSIC, true);
//            if (streamVolume != 0)
//                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
//        if (null != mediaPlayer && mediaPlayer.isPlaying()) {
//            mediaPlayer.pause();
//        }
    }

    public void onResume() {
        try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        } catch (Exception e) {
            e.printStackTrace();
            am.setStreamMute(AudioManager.STREAM_MUSIC, false);
//            if (streamVolume != 0)
//                am.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolume, 0);
        }
//        if (null != mediaPlayer && !mediaPlayer.isPlaying()) {
//            mediaPlayer.start();
//        }
    }

    public void stop() {
        if (null != mediaPlayer && mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

//    public void exit() {
//
//        if (fileAddress != null) {
//            fileAddress.clear();
//        }
//    }

    public void playErer(){
        if(null != mediaPlayer){
            mediaPlayer.release();
            mediaPlayer = null;
        }
        initPlayer();
        play(mPath);
    }

    public void onDestroy(){
        if (null != mediaPlayer && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.pause();
        }
        mediaPlayer = null;

    }

//    public void add(String fileName) {
//        fileAddress.add(fileName);
//    }

//    public void addAll(JSONArray jsonArray) {
//        try {
//            synchronized (mutex) {
//                clearList();
//                for (int i = 0; i < jsonArray.length(); i++) {
//                    JSONObject jsonObject = jsonArray.getJSONObject(i);
//                    add(FTPFileUtil.SDCARDPATH + jsonObject.getString("name"));
//                }
//            }
//            play();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//    }

//    public void addAll(List<String> list) {
//        synchronized (mutex) {
//            clearList();
//            fileAddress.addAll(list);
//        }
//        play();
//    }

    public boolean isCurrentPlaying(String path) {
        if(TextUtils.isEmpty(mPath) || TextUtils.isEmpty(path))
            return false;
        if(null == mediaPlayer)
            return false;
        if(TextUtils.equals(mPath,path) && null != mediaPlayer && mediaPlayer.isPlaying())
            return false;
        return true;
    }

    public long getCurrentPosition(){
        return mediaPlayer!=null?mediaPlayer.getCurrentPosition():0;
    }


//    long seekto = 0;
//    public void seekTo(String name,long l){
//        seekto = l;
//        if(TextUtils.equals(FTPFileUtil.SDCARDPATH+name,getCurrent())&&null != mediaPlayer){
//            mediaPlayer.seekTo(l);
//        }else{
//            String fineName = FTPFileUtil.SDCARDPATH+name;
//            if(null == mediaPlayer){
//                initPlayer();
//            }
//            play(fineName);
//            for(int i = 0;i<fileAddress.size();i++){
//                if(TextUtils.equals(fileAddress.get(i),fineName)){
//                    listPositon = i;
//                    break;
//                }
//            }
//        }
//    }

    public long getAllPosition(){
        return mediaPlayer != null?mediaPlayer.getDuration():0;
    }

//    public void clearList() {
//        fileAddress.clear();
//    }

//    private String fileCheck() {
//        boolean fileEx = false;
//        synchronized (mutex) {
//            while (listPositon < fileAddress.size()) {
//                File file = new File(fileAddress.get(listPositon));
//                if (file.exists()) {
//                    fileEx = true;
//                    break;
//                }
//                fileAddress.remove(listPositon);
//            }
//        }
//        if (fileEx) {
//            Log.d("IndexOutOf", "position:" + listPositon + ",size：" + fileAddress.size());
//            return fileAddress.get(listPositon);
//        } else {
//            listPositon = 0;
//            return fileCHeck0();
//        }
//    }

//    private String fileCHeck0() {
//        boolean fileEx = false;
//        synchronized (mutex){
//            while (listPositon < fileAddress.size()) {
//                File file = new File(fileAddress.get(listPositon));
//                if (file.exists()) {
//                    fileEx = true;
//                    break;
//                }
//                fileAddress.remove(listPositon);
//            }
//        }
//        if (fileEx) {
//            return fileAddress.get(listPositon);
//        } else {
//            return "";
//        }
//    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        if (null == mediaPlayer)
            return;
        mediaPlayer.setDisplay(surfaceHolder);

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = null;
        if (null != mediaPlayer)
            mediaPlayer.setDisplay(null);

    }


    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        Log.d("surface", "onCompletion");
//        listPositon++;
//        if (fileAddress.size() > 0) {
//            if (listPositon<0 || listPositon >= fileAddress.size()) {
//                listPositon = 0;
//            }
//            play(fileCheck());
//        }
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        iMediaPlayer.start();
//        if(seekto > 0){
//            iMediaPlayer.seekTo(seekto);
//            seekto = 0;
//        }
    }

    @Override
    public void onSeekComplete(IMediaPlayer iMediaPlayer) {

    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int i, int i1, int i2, int i3) {

    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
        playErer();
        Log.e("","");
        return false;
    }


}
