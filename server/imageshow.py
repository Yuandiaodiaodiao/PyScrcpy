import cv2
import numpy as np

with open('image.jpg','rb')as f:
    x=f.read()
    image = np.frombuffer(x, 'uint8')
    image= cv2.imdecode(image, 0)
    while True:
        cv2.imshow('URL2Image', image)
        cv2.waitKey(1)
        print("show")