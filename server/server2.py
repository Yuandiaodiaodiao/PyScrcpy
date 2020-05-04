import tornado.websocket
import tornado.ioloop
import tornado.web
import cv2
import numpy as np
import time
import ctypes
import threading
import multiprocessing
import random
import sys
from PyQt5 import QtWidgets
from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtGui import QImage, QPixmap
from PyQt5.uic import loadUi
import qtvideo as qv

connect_users = set()
tickx = 0
timestart = 0
ticks = 0


def solveQps():
    global tickx
    global timestart
    tickx += 1
    if tickx == 60:
        sec = time.time() - timestart
        fps = round(tickx / sec, 1)
        timestart = time.time()
        tickx = 0
        print(f'fps={fps}')
        return True


def saveabp(message):
    with open(f'./frame/message.bin', 'ab+') as f:
        f.write(message)


websocketPackageNum = 0
drawNum = 0

maxNum = 0


class MyWebSocketHandler(tornado.websocket.WebSocketHandler):
    connect_users = set()

    def check_origin(self, origin: str):
        '''重写同源检查 解决跨域问题'''
        return True

    def open(self):
        print("WebSocket opened")
        # 打开连接时将用户保存到connect_users中
        self.connect_users.add(self)

    def solveMessage(self, message):
        print(message)

    def on_message(self, message):

        global websocketPackageNum
        global dll
        messageType = type(message)
        # if messageType!=type(b""):
        #     self.solveMessage(message)
        #     return
        lenmessage = len(message)
        global maxNum
        if lenmessage > maxNum:
            print(f"大包{lenmessage}")
            maxNum = lenmessage
        # print(f"websocket len={lenmessage}")
        inputTime1 = time.time()
        dll.inputBuff(message, len(message))
        websocketPackageNum += 1
        if random.random() > 0.99:
            print(f"buffinput耗时={round((time.time() - inputTime1) * 1000)}")
        # frame = np.frombuffer(message, 'uint8')
        # length = len(frame)
        # print(length)
        # if length > 20:
        #

        # else:
        #     print("jump")
        # print(f"message len {length}")
        # print(frame)
        # print(image)

        # print('收到的信息为：')
        # exit(0)

    def on_close(self):
        print("WebSocket closed")
        # 关闭连接时将用户从connect_users中移除
        self.connect_users.remove(self)

    def check_origin(self, origin):
        # 此处用于跨域访问
        return True

    @classmethod
    def send_demand_updates(cls, message):
        # 使用@classmethod可以使类方法在调用的时候不用进行实例化
        # 给所有用户推送消息（此处可以根据需要，修改为给指定用户进行推送消息）
        for user in cls.connect_users:
            user.write_message(message)


class Application(tornado.web.Application):
    def __init__(self):
        handlers = [
            (r'/ws', MyWebSocketHandler)
        ]
        tornado.web.Application.__init__(self, handlers)


dll = ctypes.CDLL('lib/server8.dll')

# height = 1280
# width = 720

# height = 2220
# width = 1080
FPS = 11
SHAPE = 444

import cmath
import matplotlib.pyplot as plt
import win32api, win32con

screenX = win32api.GetSystemMetrics(win32con.SM_CXSCREEN)  # 获得屏幕分辨率X轴

screenY = win32api.GetSystemMetrics(win32con.SM_CYSCREEN)


def autosize(screenY, screenX, picw, pich):
    minsize = min(screenY, screenX)*0.9
    maxsize = max(picw, pich)
    if maxsize > minsize:
        rate = minsize / maxsize
        return (int(picw * rate//2*2), int(pich * rate//2*2))
    else:
        return (picw, pich)


from qtvideo import mywin

from socketserver3 import MysocketServer

threadImg = None


class Mythread(QThread):
    breakSignal = pyqtSignal(int)

    def __init__(self, queue, size):
        super().__init__()
        # 下面的初始化方法都可以，有的python版本不支持
        # super(Mythread, self).__init__()
        self.queue = queue
        self.scrSize = size

    def run(self):
        global dll
        # 启动socket通信
        while True:
            size = self.queue.get()
            if size.get("size"):
                size = size.get("size")
                break
            else:
                self.queue.put(size)

        print("ImageThread启动")
        w, h = size
        print(f"拿到了w={w}h={h}")
        scrw, scrh = autosize(self.scrSize[0], self.scrSize[1], w, h)
        print(f"自适应分辨率{scrw}x{scrh}")
        # scrw,scrh=w,h
        global drawNum
        global threadImg
        dll.init(0, w, h, scrw, scrh)
        lastTime = time.time()
        print(screenX, screenY)
        # cv2.namedWindow("1", flags=cv2.WINDOW_NORMAL | cv2.WINDOW_KEEPRATIO)
        # cv2.resizeWindow("1", scrw, scrh)

        tick = 0
        bufflen = scrw * scrh * 3
        global FPS
        buff = ctypes.c_buffer(bufflen)

        while True:
            try:
                drawNum += 1
                tick += 1
                buffLen = dll.getBuff(buff)
                frame = np.frombuffer(buff.raw, 'uint8', count=bufflen)
                lenx = len(frame)
                solveQps()
                img = frame.reshape((scrh, scrw, 3)).astype('uint8')
                qv.imageT = img
                self.breakSignal.emit(1)


            except Exception as e:
                print(e)
                pass


def hackSocket():
    id = 0
    with open(f'./frame/message.bin', 'rb') as f:
        while f.readable():
            message = f.read(200000)
            time.sleep(10)
            if len(message) <= 1:
                break
            dll.inputBuff(message, len(message))
            id += 1
            print(f"包id{id}")


hack = 0


def mainx():
    if hack == 2:
        pass
    elif hack == 1:

        t = threading.Thread(target=hackSocket)
        t.start()
        # cvT.join()
        # t.join()
    else:
        # cvT = threading.Thread(target=cvThread, args=(0,))
        # cvT.start()
        # app = Application()
        # app.listen(20482)
        # tornado.ioloop.IOLoop.current().start()
        from socketserver3 import MysocketServer
        server = MysocketServer("", 20481, dll)
        server.start()

        app = QtWidgets.QApplication(sys.argv)
        window = mywin()
        threadx = Mythread()
        threadx.breakSignal.connect(window.loadimage)
        threadx.start()

        window.show()

        # window.setGeometry(100,100,400,300)
        sys.exit(app.exec_())
        #
        #
        # print("cvThread函数")
        # cvThread(0)
        server.join()
    # cvT.join()
    dll.wait()
    # else:


if __name__ == "__main__":

    if hack == 0:

        import buildAndroid2

        mul2 = multiprocessing.Process(target=buildAndroid2.mainx)
        mul2.start()

        mainx()
    else:
        mainx()
