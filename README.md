# Live_Rtmp2
android视音频硬编码，软编码rtmp推流器


项目包含三大模块：
livecollector模块：视音频采集模块
                LiveVideoGet：视频采集类
                LiveAudioGet：音频采集类
                LiveVideoEncode：视频编码nv21转yuv420p，视频硬编码，视频90度旋转算法
                LiveAudioEncode：音频硬编码类
                LiveEncode：视音频采集参数编码控制类
                LiveRop：参数控制，数据回调类
                
livepush模块：视音频编码推流模块，视频编码：x264编码；音频编码：faac编码；推流：rtmp推流
liveffmpeg模块：ffmpeg编码推流模块，ffmpeg正在学习中，此模块将持续更新...

app模块：测试demo
