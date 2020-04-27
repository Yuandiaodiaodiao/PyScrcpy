import cv2
import threading
import time
import win32gui,win32con


class Producer(threading.Thread):
	"""docstring for Producer"""

	def __init__(self, rtmp_str):

		super(Producer, self).__init__()

		self.rtmp_str = rtmp_str

		# 通过cv2中的类获取视频流操作对象cap
		self.cap = cv2.VideoCapture(self.rtmp_str)

		# 调用cv2方法获取cap的视频帧（帧：每秒多少张图片）
		# fps = self.cap.get(cv2.CAP_PROP_FPS)
		self.fps = self.cap.get(cv2.CAP_PROP_FPS)
		print(self.fps)

		# 获取cap视频流的每帧大小
		self.width = int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH))
		self.height = int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
		self.size = (self.width, self.height)
		print(self.size)

		# 定义编码格式mpge-4
		self.fourcc = cv2.VideoWriter_fourcc('M', 'P', '4', '2')

		# 定义视频文件输入对象
		self.outVideo = cv2.VideoWriter('saveDir1.avi', self.fourcc, self.fps, self.size)

	def run(self):

		print('in producer')

		ret, image = self.cap.read()

		while ret:
			# if ret == True:

			self.outVideo.write(image)

			cv2.imshow('video', image)

			cv2.waitKey(int(1000 / int(self.fps)))  # 延迟

			if cv2.waitKey(1) & 0xFF == ord('q'):

				self.outVideo.release()

				self.cap.release()

				cv2.destroyAllWindows()

				break

			ret, image = self.cap.read()


if __name__ == '__main__':

	print('run program')
	# rtmp_str = 'rtmp://live.hkstv.hk.lxdns.com/live/hks'  # 经测试，已不能用。可以尝试下面两个。
	# rtmp_str = 'rtmp://media3.scctv.net/live/scctv_800'  # CCTV
	# rtmp_str = 'rtmp://58.200.131.2/livetv/hunantv'  # 湖南卫视
	rtmp_str = 'rtmp://192.168.31.39/live/STREAM_NAME'

	producer = Producer(rtmp_str)  # 开个线程
	producer.start()