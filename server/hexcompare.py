f1=open("message.bin",'rb')
f2=open("message2.bin",'rb')
index=0
while True:
    a=f1.read(1)
    b=f2.read(1)
    if a!=b:
        print(f"error in{index}")
        break
    index+=1