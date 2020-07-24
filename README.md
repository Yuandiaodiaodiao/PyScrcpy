# PyScrcpy
android screen&amp;audio share used PyQt ffmpeg Websocket  
## 演示
https://www.bilibili.com/video/BV1Nt4y117ht
## 性能
安卓视频音频串流  
scrcpy延迟100ms  
PyScrcpy插adb走wifi传输延迟100-160ms  
PyScrcpy无线延迟100-300ms  
帧率 在没有其他应用占用系统硬解码器的情况下 原生分辨率@60fps  

## 使用方法
1. 运行/server/main.py
2. 安装app2/app/build/outputs/apk/debug/app-debug.apk(release中有)
3. 打开app 扫码运行 (需要>=android 8)
## 注意事项
1. server8.dll使用vs2019编译 可能会与您的windows出现不兼容的情况 需要您自己对/server/ffmpegLib中的target=server8进行重新编译  
2.apk兼容性为api>=26 已经发布在release中 想自己build可以使用android studio对app2文件夹进行构建



## 参考文档
二维码扫描  
https://github.com/yipianfengye/android-zxingLibrary   

MediaProjection屏幕录制录音   
https://github.com/yrom/ScreenRecorder  

MediaProjection屏幕录制  
https://github.com/eterrao/ScreenRecorder   

万物起源scrcpy  
https://github.com/Genymobile/scrcpy  

qt版的scrcpy投屏  
https://github.com/barry-ran/QtScrcpy  

MediaProjection录制 RTMP推流  
https://github.com/deepsadness/MediaProjectionDemo  
 
ffmpeg解码   
https://github.com/deepsadness/AppRemote  

adb拉起surfaceApi  
https://github.com/android-notes/androidScreenShare  

MediaProjection录制   
https://github.com/magicsih/AndroidScreenCaster  

用FFmpeg保存JPEG图片  
https://blog.csdn.net/zhoubotong2012/article/details/79342116  

avio_alloc_context 读内存  
https://www.jianshu.com/p/3c95b0471d3a  

ffmpeg lib下载  
https://ffmpeg.zeranoe.com/builds/  

转换yuv 和rgb  
https://blog.csdn.net/shwan_ma/article/details/102482477  

关于linesize的问题  
https://blog.csdn.net/download_73/article/details/53302825  

阻塞队列  
https://blog.csdn.net/big_yellow_duck/article/details/52601543  

utf-8编码问题  
https://blog.csdn.net/u014671962/article/details/101525645  

## LICENSE
```
   Copyright (C) 2018 Genymobile
   Copyright (C) 2020 yuandiaodiaodiao

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
