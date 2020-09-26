//
// Created by qq295 on 2020/4/25.
//
#include <direct.h>
#include <stdio.h>
#include <iostream>
#include <memory>
#include <thread>
#include "BlockingQueue.h"
#include "Decoder.h"

using std::cout, std::cin, std::endl, std::thread;

extern "C" {
__declspec( dllexport ) int getBuff( uint8_t *buffer );
__declspec( dllexport ) bool inputBuff( uint8_t *buffer, int size );
__declspec( dllexport ) void init( int hack, int width, int height,int outputw,int outputh );
__declspec( dllexport ) void wait();
}
int id = 0;
Decoder decoder;
//AVPixelFormat ap = AV_PIX_FMT_YUV444P;



int getBuff( uint8_t *buffer ) {

    while(decoder.outputQueue.size()>=3){//直接扔掉
        cout<<"扔掉"<<endl;
        decoder.outputQueue.take();
    }
    buffWithSize buff = decoder.outputQueue.take();
    //    cout<<"output大小="<<buff.size<<endl;
    //    cout<<"output队列长度="<<decoder.inputQueue.size()<<endl;
    memcpy( buffer, buff.buffer, buff.size );
    delete[] buff.buffer;
    return decoder.inputQueue.size();
}
bool inputBuff( uint8_t *buffer, int size ) {
    auto *buf = new uint8_t[size];
    memcpy( buf, buffer, size );
    decoder.inputQueue.put( buffWithSize{buf, size, 0} );
    id++;
//        cout<<"input队列"<<decoder.inputQueue.size()<<endl;

    return true;
}
void DecoderThread( Decoder &d, int hack, int width, int height,int outputw,int outputh) {

    cout << 1 << endl;
    if ( hack == 1 ) {
        cout << "hack模式启动" << endl;
    }

    try {
        d.init( hack, width, height,outputw,outputh );
        while(true){
            try{
                d.run();
            }catch (...){
            }
        }
    } catch ( ... ) {
        cout << "error 重启Decoder" << endl;
    }
    //    while(1){
    //
    //    }

    //    d.run();
}
std::unique_ptr<thread> threads;
// thread* threadx;
void init( int hack, int width, int height,int outputw,int outputh ) {
    char buffer[500];
    getcwd( buffer, 500 );
    cout << "工作路径" << buffer << endl;
    threads = std::make_unique<thread>( DecoderThread, std::ref( decoder ), hack, width, height,outputw,outputh );
    // threadx=new thread(DecoderThread,std::ref(decoder));
}
void wait() {
    threads->join();
}
int main() {
    system( "chcp 65001" );
    printf("aaa");
    init( 1, 360, 640,360,640 );
    //    Decoder d = Decoder();
    wait();
    return 0;
}