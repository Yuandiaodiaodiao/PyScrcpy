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

    def check_origin(self):
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


dll = ctypes.CDLL('lib/server3.dll')

# height = 1280
# width = 720

height = 2220
width = 1080
FPS = 11
SHAPE = 444
shapeA = height * 3 // 2
shapeB = width
import cmath
import matplotlib.pyplot as plt
import win32api,win32con

screenX=win32api.GetSystemMetrics(win32con.SM_CXSCREEN)   #获得屏幕分辨率X轴

screenY=win32api.GetSystemMetrics(win32con.SM_CYSCREEN)

def autosize(screenY,screenX,picw,pich):
    minsize=min(screenY,screenX)*0.9
    maxsize=max(picw,pich)
    if maxsize>minsize:
        rate=minsize/maxsize
        return (round(picw*rate),round(pich*rate))
    else:
        return (picw,pich)



def cvThread(hack):
    print("cvThread启动")
    scrw,scrh=autosize(screenX,screenY,width,height)
    global dll
    global drawNum
    dll.init(hack, width, height)
    lastTime = time.time()
    print(screenX,screenY)
    cv2.namedWindow("1", flags=cv2.WINDOW_NORMAL | cv2.WINDOW_KEEPRATIO)
    cv2.resizeWindow("1", scrw, scrh)

    tick = 0
    bufflen = height * width * 3
    global FPS
    buff = ctypes.c_buffer(bufflen)
    npBuff=np.empty((height*width*3), dtype = 'uint8')

    while True:
        try:
            drawNum += 1
            show = random.random()
            tick += 1
            timebuff = time.time()
            buffLen = dll.getBuff(buff)
            print("取出")
            timebuffused = time.time() - timebuff
            print(f"解码耗时={round(timebuffused * 1000, 1)}ms")
            # if timebuffused>0:
            #     FPS-=1
            #     print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            # if tick%(FPS*2)==0:
            #     FPS+=1
            # thistime = time.time()
            # lastUse = thistime - lastTime
            # if lastUse < 1/FPS :
            #     print(f"sleep {1/FPS - lastUse}")
            #     # time.sleep(1/FPS - lastUse)
            # lastTime = time.time()

            frame = np.frombuffer(buff.raw, 'uint8',count=height*width*3)
            # print("start!!!!!!!!!!!!!")
            # for i in range(100):
            #     print(frame[i],end=" ")
            # print('end!!!!!!!')
            lenx = len(frame)
            print(f"len={lenx}")

            # if lenx != bufflen:
            #     continue
            solveQps()
            # pixels = lenx // 3
            # # x*x/16*9=1280*720
            # pixels = pixels // 9 * 16
            # pixels=cmath.sqrt(pixels).real
            # x = round(pixels)
            # y = x // 16 * 9
            # print(f"RGB {x}:{y}")
            img = frame.reshape((height, width, 3)).astype('uint8')
            # rgb_img = cv2.cvtColor(img, cv2.COLOR_YUV420p2RGB)
            cvTime = time.time()

            cv2.imshow("1", img)
            cv2.waitKey(1)
            # plt.imshow(rgb_img)
            # plt.show()
            cvTimeUsed = time.time() - cvTime
            # if show>0.99:
            # print(f"open cv time={round(cvTimeUsed * 1000, 1)}ms")

            # print(f"wsNum={websocketPackageNum} drawNum={drawNum}")
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
        cvThread(1)

    elif hack == 1:

        t = threading.Thread(target=hackSocket)
        t.start()
        cvThread(0)
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
        print("cvThread函数")
        cvThread(0)
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
