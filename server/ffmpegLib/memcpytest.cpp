//
// Created by qq295 on 2020/4/24.
//
#include"iostream"
#include"cstdio"
#include "random"

#include "time.h"
int main(){
    int constsize=337e6;
    uint8_t *buff=new uint8_t[constsize];
    uint8_t *buff2=new uint8_t[constsize];
    for(int i=0;i<constsize;i++){
        buff[i]=(uint8_t)(rand())%10;
    }
    clock_t start, finish;
    start=clock();
    for(int i=0;i<=120;++i){
        buff[i]=(uint8_t)(rand())%10;
        memcpy(buff2,buff,constsize);
    }
    finish=clock();
    double  duration;
    duration = (double)(finish - start) / CLOCKS_PER_SEC;
    std::cout<<"timeuse= "<<duration<<std::endl;
    for(int i=0;i<constsize;i++){
        buff[i]=buff2[i]+buff[i];
    }
    for(int i=0;i<constsize;i+=100e3){
        std::cout<<buff2[i];
    }
    return 0;
}
