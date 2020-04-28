import cv2
import numpy as np


height=640
width=360

with open(f'bitmap.bm', 'rb') as f:
    s=f.read()
    image = np.frombuffer(s, 'uint8')


print("读取完成")
npBuff=np.empty((height*width*3,), dtype = 'uint8')
# data[0]=data[0][:height*width]
# data[1]=data[1][:height*width//4]
# data[2]=data[2][:height*width//4]
# for index,item in enumerate(data[0]):
#     data[0][index]=1

img = image.reshape((640,360,3)).astype('uint8')


bgr_img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
cv2.imshow("a",bgr_img)
cv2.waitKey(0)