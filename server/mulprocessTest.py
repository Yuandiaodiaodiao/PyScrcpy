import os
import multiprocessing



class AndroidStart(multiprocessing.Process):
    def __init__(self):
        super().__init__()

    def run(self) -> None:

        os.system(r'adb push ../app/app/build/outputs/apk/debug/app-debug.apk /sdcard/server2.jar')
        os.system(
            'adb shell "export CLASSPATH=/sdcard/server2.jar && exec app_process /sdcard com.cry.cry.appprocessdemo.HelloWorld"')



if __name__=="__main__":
    multiprocessing.freeze_support()
    process = AndroidStart()
    process.start()
    process.join()