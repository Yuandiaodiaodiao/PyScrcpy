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

        super().__init__(application, request, **kwargs)
        self.byteType = type(b"")

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

        messageType = type(message)
        if messageType != self.byteType:
            self.solveMessage(message)
            return

        lenmessage = len(message)
        global stream
        stream.write(message)

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
    def __init__(self):

        handlers = [
            (r'/ws', MyWebSocketHandler)
        ]
        tornado.web.Application.__init__(self, handlers)
        print("websocket listening")



import tornado.platform.asyncio
import pyaudio
p = pyaudio.PyAudio()
stream = p.open(format=p.get_format_from_width(2), channels=1,
                    rate=44100, output=True)

        # asyncio.set_event_loop_policy(tornado.platform.asyncio.AnyThreadEventLoopPolicy())
app = Application()
app.listen(20482)
tornado.ioloop.IOLoop.current().start()
