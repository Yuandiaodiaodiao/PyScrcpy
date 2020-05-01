from typing import Any

import tornado.websocket
import tornado.web
import tornado.ioloop
import time
import random
import threading

import asyncio

from tornado import httputil


class MyWebSocketHandler(tornado.websocket.WebSocketHandler):
    connect_users = set()
    def __init__(self, application: tornado.web.Application, request: httputil.HTTPServerRequest, **kwargs: Any):
        self.queue=kwargs["queue"]
        kwargs.pop("queue")
        super().__init__(application, request, **kwargs)

    def check_origin(self, origin: str):
        '''重写同源检查 解决跨域问题'''
        return True

    def open(self):
        print("WebSocket opened")
        # 打开连接时将用户保存到connect_users中
        self.connect_users.add(self)

    def solveMessage(self, message):
        print(message)
        msArray=message.split(" ")
        if len(msArray)>1 and msArray[0]=='size':
            size=(int(msArray[1]),int(msArray[2]))
            print(f"设备分辨率为{size}")
            self.queue.put(dict(size=size))

    def on_message(self, message):

        global websocketPackageNum
        global dll
        messageType = type(message)
        if messageType != type(b""):
            self.solveMessage(message)
            return
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

    @classmethod
    def send_demand_updates(cls, message):
        # 使用@classmethod可以使类方法在调用的时候不用进行实例化
        # 给所有用户推送消息（此处可以根据需要，修改为给指定用户进行推送消息）
        for user in cls.connect_users:
            user.write_message(message)


class Application(tornado.web.Application):
    def __init__(self,queue):
        handlers = [
            (r'/ws', MyWebSocketHandler,dict(queue=queue))
        ]
        tornado.web.Application.__init__(self, handlers)


import tornado.platform.asyncio


class WebsocketThread(threading.Thread):
    def __init__(self,queue):
        super().__init__()
        self.queue=queue

    def run(self) -> None:

        asyncio.set_event_loop_policy(tornado.platform.asyncio.AnyThreadEventLoopPolicy())
        app = Application(self.queue)
        app.listen(20482)
        tornado.ioloop.IOLoop.current().start()

