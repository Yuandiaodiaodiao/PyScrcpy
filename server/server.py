import tornado.websocket
import tornado.ioloop
import tornado.web
import cv2
import numpy as np
import time
connect_users = set()
tick=0
timestart=0
class MyWebSocketHandler(tornado.websocket.WebSocketHandler):
    connect_users=set()
    def check_origin(self):
        '''重写同源检查 解决跨域问题'''
        return True

    def open(self):
        print("WebSocket opened")
        # 打开连接时将用户保存到connect_users中
        self.connect_users.add(self)


    def on_message(self, message):
        # with open('image.jpg','wb') as f:
        #     f.write(message)
        image=np.frombuffer(message, 'uint8')
        image = cv2.imdecode(image, 1)
        #
        # # imgg = cv2.imencode('.png', img)
        #
        #
        # # 缓存数据保存到本地
        #
        cv2.imshow('URL2Image', image)
        cv2.waitKey(1)
        global tick
        global timestart

        tick+=1
        if tick==60:
            sec=time.time()-timestart
            fps=round(tick/sec,1)
            timestart=time.time()
            tick=0
            print(f'fps={fps}')
        del image
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




class Application(tornado.web.Application) :
    def __init__(self) :
        handlers = [
            (r'/ws', MyWebSocketHandler)
        ]
        tornado.web.Application.__init__(self, handlers)
if __name__ == "__main__" :
    app = Application()
    app.listen(20481)
    tornado.ioloop.IOLoop.current().start()