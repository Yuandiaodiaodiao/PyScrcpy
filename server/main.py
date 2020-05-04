from PyQt5 import QtWidgets, QtGui
import qtvideo
import sys
import mulprocessTest
import multiprocessing
import server2
import socketserver3
import WebsocketServer
import util
import qrGen
class UiServer(qtvideo.mywin):

    def __init__(self, dll):
        super().__init__()
        self.dllHandle=dll._handle
        self.queue = multiprocessing.Queue()
        self.dll = dll
        self.all_btn.clicked.connect(self.allStart)
        self.start_btn.clicked.connect(self.startServer)
        self.androit_btn.clicked.connect(self.startAndroid)
        self.qrShow_btn.clicked.connect(self.showQr)
        # socket线程 用于接收图片数据
        self.socketServer = socketserver3.MysocketServer("", 20481, self.dll)

        # 取图片线程 从dll中取出图片并通知qt显示
        self.serverThread = server2.Mythread(self.queue,(self.width,self.height))
        self.serverThread.breakSignal.connect(self.loadimage)

        # android adb shell
        self.androidProcess = mulprocessTest.AndroidStart()

        # 消息通知
        self.wsThread = WebsocketServer.WebsocketThread(self.queue,self.dll)
        self.showQr()

        self.startServer()

    def showQr(self):
        self.ip = util.getIp()
        qrImg = qrGen.renderQR(self.ip)
        qtvideo.imageT = qrImg
        self.loadimage()

    def startServer(self):
        self.serverThread.start()
        self.socketServer.start()
        self.wsThread.start()

    def startAndroid(self):
        self.androidProcess.start()

    def allStart(self):
        # self.startServer()
        self.startAndroid()
    def __del__(self):
        print("析构")
    def closeEvent(self, a0: QtGui.QCloseEvent) -> None:
        self.dll.dclose(self.dllHandle)
        print("卸载dll")

if __name__ == "__main__":

    app = QtWidgets.QApplication(sys.argv)
    window = UiServer(server2.dll)
    window.show()
    sys.exit(app.exec_())
