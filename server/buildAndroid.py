import os
import subprocess

cmd1 = "buildStep1.bat"

# os.system(cmd1)

os.chdir(r'D:\Github\androidScreenShare')
# okhttpjar=r'C:\Users\qq295\.gradle\caches\modules-2\files-2.1\com.squareup.okhttp3\okhttp\4.5.0\cfd127ae6de4862daa93e15ceae9291108eaabc5\okhttp-4.5.0.jar'
# cmd2 = r'D:\Android\Sdk\build-tools\28.0.3\dx.bat --dex --output=Main.dex D:\Github\androidScreenShare\shareandcontrollib\build\intermediates\packaged-classes\debug\classes.jar '
# os.system(cmd2)
# print('cmd2finish')
os.system(r'adb push D:\Github\androidScreenShare\shareandcontrollib\build\outputs\apk\debug\shareandcontrollib-debug.apk /sdcard/server.jar')
os.system('adb shell "export CLASSPATH=/sdcard/server.jar && exec app_process /sdcard com.wanjian.puppet.Main"')
exit(0)

shell = subprocess.Popen(["adb",'shell'], stdin=subprocess.PIPE, stdout=subprocess.PIPE)

shell.stdin.write('export CLASSPATH=/sdcard/Main.dex\n'.encode('utf-8'))
shell.stdin.write('exec app_process /sdcard com.wanjian.puppet.Main\n'.encode('utf-8'))
info,err=shell.communicate()
print(info.decode('gbk'))
shell.stdin.write(bytes("export CLASSPATH=/sdcard/Main.dex", encoding="utf8"))
shell.stdin.write(bytes("exec app_process /sdcard com.wanjian.puppet.Main", encoding="utf8"))

# shell=os.popen('adb shell','rw')
# shell.write("export CLASSPATH=/sdcard/Main.dex")
# shell.write("exec app_process /sdcard com.wanjian.puppet.Main")
while True:
    # r = shell.stdout.read()
    r = shell.stdout.readlines()
    print(r)

# os.system('adb shell export CLASSPATH=/sdcard/Main.dex')
# os.system('adb shell exec app_process /sdcard com.wanjian.puppet.Main')
