import os
import subprocess
import multiprocessing


def mainx():
    multiprocessing.freeze_support()

    os.system(r'adb push ../app/app/build/outputs/apk/debug/app-debug.apk /sdcard/server2.jar')
    os.system('adb shell "export CLASSPATH=/sdcard/server2.jar && exec app_process /sdcard com.cry.cry.appprocessdemo.HelloWorld"')

if __name__ == "__main__":

    mainx()