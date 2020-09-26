import ctypes
import sys
import os
p=os.path.abspath(os.path.curdir)
sys.path.append(p)
print(sys.path)
print(p)
d=['swresample-3.dll',"swscale-5.dll","avutil-56.dll","avcodec-58.dll","avformat-58.dll",'server8.dll']
for dd in d:
    funcs=[ctypes.windll.LoadLibrary,ctypes.cdll.LoadLibrary,ctypes.CDLL,ctypes.WinDLL]
    for fun in funcs:
        try:
            fun(dd)
            print(f"{dd}成功")
            break
        except:
            pass
