import sys
import numpy as np
from PyQt5 import QtWidgets
from PyQt5.QtGui import QImage, QPixmap
from PyQt5.QtWidgets import QDialog, QPushButton, QWidget, QHBoxLayout, QApplication, QLabel, QVBoxLayout, QGridLayout
from PyQt5.uic import loadUi
import cv2

imageT = None
import windowSize


class mywin(QtWidgets.QDialog):
    def __init__(self):
        super(mywin, self).__init__()
        # loadUi('img.ui',self)
        self.desktop = QApplication.desktop()
        self.screenRect = self.desktop.screenGeometry()
        self.height = self.screenRect.height()
        self.width = self.screenRect.width()
        self.setWindowTitle("PyScrcpy")
        print("pc屏幕分辨率为", (self.height, self.width))
        minAxis = min(self.width, self.height) * 0.9 // 2 * 2
        minAxis = int(minAxis)
        self.resize(minAxis * 9 // 16, minAxis)

        layout = QHBoxLayout()

        frameLayout = QHBoxLayout()
        buttonLayout = QVBoxLayout()

        self.all_btn = all_btn = QPushButton()
        all_btn.setText("一键启动")
        self.start_btn = start_btn = QPushButton()
        start_btn.setText("启动server")
        self.androit_btn = androit_btn = QPushButton()
        androit_btn.setText("启动android")
        self.qrShow_btn=qrShow_btn=QPushButton()
        qrShow_btn.setText("显示二维码")
        # self.load_btn.clicked.connect(self.loadimage)
        # self.
        buttonLayout.addWidget(all_btn)
        buttonLayout.addWidget(start_btn)
        buttonLayout.addWidget(androit_btn)
        buttonLayout.addWidget(qrShow_btn)
        buttonLayout.addStretch()

        self.img_label = QLabel()
        # self.img_label.setMinimumWidth(720)
        frameLayout.addWidget(self.img_label)
        layout.addLayout(buttonLayout)
        layout.addLayout(frameLayout)
        self.setLayout(layout)
        # self.setLayout(wLayout)

    def loadimage(self):
        global imageT
        # image = cv2.cvtColor(imageT, cv2.COLOR_BGR2RGB)

        self.qimg = QImage(imageT.data, imageT.shape[1], imageT.shape[0], QImage.Format_RGB888)
        # QPixmap.loadFromData()
        # self.qimg = self.qimg.rgbSwapped()
        self.img_label.setPixmap(QPixmap.fromImage(self.qimg))


if __name__ == "__main__":
    app = QtWidgets.QApplication(sys.argv)
    window = mywin()
    window.show()
    window.setWindowTitle("window")
    # window.setGeometry(100,100,400,300)
    sys.exit(app.exec_())
