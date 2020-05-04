import qrcode
import numpy as np
import cv2
from PIL import Image


# qr = qrcode.QRCode()
# qr.add_data("11111")
# qr.print_ascii()
# matrix=qr.get_matrix()
#
# image = qrcode.make('11111')
# image2=image.get_image()
#
# image2=image2.convert("L")
#
# img = cv2.cvtColor(np.array(image2),cv2.COLOR_GRAY2RGB)
# print(img)
# cv2.imshow("1",img)
# cv2.waitKey(0)
def renderQR(content):
    qr = qrcode.make(content)
    image = qr.get_image().convert("L")
    cvImg = cv2.cvtColor(np.array(image), cv2.COLOR_GRAY2RGB)
    cvImg = cv2.resize(cvImg, (400, 400), interpolation=cv2.INTER_NEAREST)
    # cvImg=cv2.cvtColor(np.array(image),cv2.C)
    return cvImg


if __name__ == "__main__":
    import util

    img = renderQR(util.getIp())
    cv2.imshow("1", img)
    cv2.waitKey(0)
