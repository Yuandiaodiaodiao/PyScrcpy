import sys
import numpy as np
from PyQt5 import QtWidgets
from PyQt5.QtGui import QImage,QPixmap
from PyQt5.uic import loadUi
import cv2
class mywin(QtWidgets.QDialog):
    def __init__(self):
        super(mywin,self).__init__()
        loadUi('img.ui',self)
        self.load_btn.clicked.connect(self.loadclicked)

    def loadclicked(self):
        self.loadimage()

    # def loadimage(self,path):
    #     self.image = cv2.imread(path)
    #     self.showimage()

    def loadimage(self):
        height = 640
        width = 360
        with open(f'bitmap.bm', 'rb') as f:
            s = f.read()
            image = np.frombuffer(s, 'uint8')

        print("读取完成")
        npBuff = np.empty((height * width * 3,), dtype='uint8')
        # data[0]=data[0][:height*width]
        # data[1]=data[1][:height*width//4]
        # data[2]=data[2][:height*width//4]
        # for index,item in enumerate(data[0]):
        #     data[0][index]=1
        image = image.reshape((640, 360, 3)).astype('uint8')

        image=cv2.cvtColor(image,cv2.COLOR_BGR2RGB)
        self.qimg = QImage(image.data, image.shape[1], image.shape[0], QImage.Format_RGB888)

        # self.qimg = self.qimg.rgbSwapped()
        self.img_label.setPixmap(QPixmap.fromImage(self.qimg))

if __name__ == "__main__":
    app = QtWidgets.QApplication(sys.argv)
    window = mywin()
    window.show()
    window.setWindowTitle("window")
    #window.setGeometry(100,100,400,300)
    sys.exit(app.exec_())