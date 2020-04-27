import cv2
import numpy as np

# Vt = np.zeros(shape=(140, 10), dtype='uint8')
# Vt=Vt.reshape(-1)
# print(Vt)

# image=cv2.imread("image.jpg")
# image2=cv2.cvtColor(image,cv2.COLOR_RGB2YUV)
# cv2.imshow("a",image2)
# cv2.waitKey(0)
datas=["Y","U","V"]
rowByte=[384,192,192]
height=640
width=360
data=[]
for strs in datas:
    with open(f'../../AppRemote/{strs}.bin', 'rb') as f:
        s=f.read()
        image = np.frombuffer(s, 'uint8')
        data.append(image)

print("读取完成")

# data[0]=data[0][:height*width]
# data[1]=data[1][:height*width//4]
# data[2]=data[2][:height*width//4]
# for index,item in enumerate(data[0]):
#     data[0][index]=1
img = np.concatenate((data[0],data[1],data[2]))
img = img.reshape((height*3 // 2, width)).astype('uint8')
bgr_img = cv2.cvtColor(img, cv2.COLOR_YUV420P2RGB)
cv2.imshow("a",bgr_img)
cv2.waitKey(0)