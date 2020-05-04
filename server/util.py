import socket
#获取本机电脑名
myname = socket.getfqdn(socket.gethostname(  ))
#获取本机ip
myaddr = socket.gethostbyname(myname)
def getIp():
    return myaddr

if __name__=="__main__":
    print(getIp())