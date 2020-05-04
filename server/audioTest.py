import pyaudio
import wave
def play():
    chunk = 4  # 2014kb
    wf = wave.open(r"test.wav", 'rb')

    p = pyaudio.PyAudio()
    print(f"采样率{wf.getsampwidth()} channel{wf.getnchannels()} rate{wf.getframerate()}")
    stream = p.open(format=p.get_format_from_width(wf.getsampwidth()), channels=wf.getnchannels(),
                    rate=wf.getframerate(), output=True)

    data = wf.readframes(chunk)  # 读取数据

    while True:
        data = wf.readframes(chunk)
        print(len(data))
        if len(data) == 0:
            break
        stream.write(data)
    stream.stop_stream()  # 停止数据流
    stream.close()
    p.terminate()  # 关闭 PyAudio
    print('play函数结束！')


if __name__ == '__main__':
    audio_file = '16k.wav'  # 指定录音文件
    play()