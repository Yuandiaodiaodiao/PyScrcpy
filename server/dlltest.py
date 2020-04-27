import ctypes

# dll = windll.LoadLibrary('lib/dllcore.dll')
dll=ctypes.CDLL('lib/server2.dll')
# print(dll)
# dll.HelloWorld('WDNMD')
# print(dll.printX())
# a=dll.Double(123)
# print(dll.printX())
#
# print(type(a))
# print(a)
dll.init()
dll.wait()
# buff=dll.returnBuff(buffer)
# print(buff)