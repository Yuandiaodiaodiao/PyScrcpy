import win32api,win32con

screenX=win32api.GetSystemMetrics(win32con.SM_CXSCREEN)   #获得屏幕分辨率X轴

screenY=win32api.GetSystemMetrics(win32con.SM_CYSCREEN)

import ctypes.wintypes

def get_current_size(hwnd=0):
    try:
        f = ctypes.windll.dwmapi.DwmGetWindowAttribute
    except WindowsError:
        f = None
    if f:
        rect = ctypes.wintypes.RECT()
        DWMWA_EXTENDED_FRAME_BOUNDS = 9
        f(ctypes.wintypes.HWND(hwnd),
          ctypes.wintypes.DWORD(DWMWA_EXTENDED_FRAME_BOUNDS),
          ctypes.byref(rect),
          ctypes.sizeof(rect)
          )
        size = (rect.right - rect.left, rect.bottom - rect.top)
        return size