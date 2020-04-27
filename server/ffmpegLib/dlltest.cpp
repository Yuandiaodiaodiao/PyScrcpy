//
// Created by qq295 on 2020/4/24.
//
#include <stdio.h>
#include <iostream>
#include <memory>
#include <thread>
#include "BlockingQueue.h"
using std::cout, std::cin, std::endl, std::thread;
extern "C" {
__declspec( dllexport ) int Double();
__declspec( dllexport ) float floatAdd( float a, float b );
__declspec( dllexport ) void HelloWorld( char *str );
__declspec( dllexport ) void Ints( int *arr, int n );
__declspec( dllexport ) int printX();
__declspec( dllexport ) bool returnBuff( char *buffer );
__declspec( dllexport ) bool inputBuff( char *buffer );
__declspec( dllexport ) void init();
}
int x = 1;
BlockingQueue<char *> buffQueue;

void Eater() {
    while ( true ) {
        buffQueue.take();
        cout << "嗝" << endl;
    }
}
thread *threadEater;
std::unique_ptr<thread>threads= std::make_unique<thread>(Eater);
void init() {
    //启动线程
    threadEater = new thread( Eater );
}


bool inputBuff( char *buffer ) {
    //    buffer[0]=(char)99;
    buffQueue.put( buffer );
    return true;
}
bool returnBuff( char *buffer ) {
    buffer = buffQueue.take();
    buffer[0] += 1;
    //    for(int a=1;a<100;a++){
    //        buffer[a]=(char)(a);
    //    }
    return true;
}

int Double() {
    x = 2;
    return x * 2;
}
int printX() {
    return x;
}
float floatAdd( float a, float b ) {
    return a + b;
}

void HelloWorld( char *str ) {
    printf( "%s", str );
}

void Ints( int *arr, int n ) {
    for ( int i = 0; i < n; i++ ) {
        printf( "%d ", arr[i] );
    }
    puts( "" );
}