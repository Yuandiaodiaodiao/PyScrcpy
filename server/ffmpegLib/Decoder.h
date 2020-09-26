//
// Created by qq295 on 2020/4/25.
//

#ifndef ASREMOTE_DECODER_H
#define ASREMOTE_DECODER_H
extern "C" {
#include "libavformat/avformat.h"
#include "libavutil/error.h"
#include "libavutil/imgutils.h"
#include "libavutil/pixfmt.h"
#include "libswscale/swscale.h"
};
#include <algorithm>
#include <iostream>
#include "BlockingQueue.h"
#include "buffer.h"
using std::cout;
using std::endl;
FILE *fin = nullptr;
FILE *fin2 = nullptr;
// extern AVPixelFormat ap;

int read_socket_buffer( void *opaque, uint8_t *buf, int buf_size );
int read_socket_buffer2( void *opaque, uint8_t *buf, int buf_size );

int hackRead( void *opaque, uint8_t *buf, int buf_size ) {
    if ( fin == nullptr ) {
        fin = fopen( "message.bin", "rb" );
    }
    int count = fread( buf, 1, 65535, fin );
    //        cout << "读取" << count << "字节" << endl;
    if ( count == 0 ) {
        return -1;
    }
    return count;
}
AVPixelFormat ap = AV_PIX_FMT_YUV420P;

class Decoder {
   public:
    BlockingQueue<buffWithSize> inputQueue;
    BlockingQueue<buffWithSize> outputQueue;
    buffWithSize haventReadFinish;
    const static size_t BUF_SIZE = (int)1e6;
    uint8_t *bufferx;
    int width, height;
    int outputw,outputh;
    const static AVPixelFormat RGB_Fmt = AV_PIX_FMT_RGB24;
    Decoder() : haventReadFinish( buffWithSize{nullptr, 0, 0} ) {
        avformat_network_init();
        format_ctx = avformat_alloc_context();
        bufferx = static_cast<uint8_t *>( av_malloc( BUF_SIZE ) );
        codec = avcodec_find_decoder( AV_CODEC_ID_H264 );
        codec->pix_fmts = &ap;
        codec_ctx = avcodec_alloc_context3( codec );
        codec_ctx->pix_fmt=ap;
        codec_ctx->sw_pix_fmt=ap;
    }
    void init( bool hack, int widthx, int heightx,int outputwi,int outputhi ) {
        outputw=outputwi;
        outputh=outputhi;
        //会预读取然后阻塞
        width = widthx;
        height = heightx;
        avio_ctx = avio_alloc_context( bufferx, BUF_SIZE, 0, this,
                                       hack ? hackRead : read_socket_buffer2, nullptr, nullptr );

        format_ctx->pb = avio_ctx;
        int ret = avformat_open_input( &format_ctx, nullptr, nullptr, nullptr );
        codec_ctx->width = width;
        codec_ctx->height = height;
        ret = avcodec_open2( codec_ctx, codec, nullptr );
        packet = av_packet_alloc();
        cout << "编码器打开成功" << endl;
    }
    void run() {
        int frames=1;
        printf( "decoder w = %d ,h =%d \n", codec_ctx->width, codec_ctx->height );
        AVFrame *decode_frame = av_frame_alloc();
        AVFrame *RGB_frame = av_frame_alloc();
        RGB_frame->height = outputh;
        RGB_frame->width = outputw;
        RGB_frame->format = RGB_Fmt;
        uint8_t *m_rgbBuffer;
        int numBytes;
        numBytes = av_image_get_buffer_size( RGB_Fmt, outputw, outputh, 1 );
        cout << "RGB图片大小=" << numBytes << endl;
        m_rgbBuffer = (uint8_t *) av_malloc( numBytes * sizeof( uint8_t ) );
        int rest = av_image_fill_arrays( RGB_frame->data, RGB_frame->linesize, m_rgbBuffer, RGB_Fmt,
                                         outputw, outputh, 1 );
        struct SwsContext *m_img_convert_ctx;

        //特别注意sws_getContext内存泄露问题，
        //注意sws_getContext只能调用一次，在初始化时候调用即可，另外调用完后，在析构函数中使用sws_freeContext，将它的内存释放。
        //设置图像转换上下文
        m_img_convert_ctx = sws_getContext( width, height, ap, outputw, outputh, RGB_Fmt, SWS_BICUBIC,
                                            NULL, NULL, NULL );
        int isinit = 0;
        while ( av_read_frame( format_ctx, packet ) >= 0 ) {
            int ret;
            while ( true ) {
                ret = avcodec_send_packet( codec_ctx, packet );
                if ( ret == 0 ) {
                    //                    printf( "avcodec_send_packet success\n" );
                    //成功找到了
                    break;
                } else if ( ret == AVERROR( EAGAIN ) ) {
                    printf( "avcodec_send_packet EAGAIN\n" );
                    break;
                } else {
                    printf( "avcodec_send_packet error\n" );
                    av_packet_unref( packet );
                }
            }

            ret = avcodec_receive_frame( codec_ctx, decode_frame );
            if ( isinit == 0 ) {
                isinit = 1;
                int codedW = codec_ctx->coded_width, codedH = codec_ctx->coded_height;

//                cout << "初始化图片大小= " << numBytes << " linesize= " << decode_frame->linesize[0]
//                     << endl;
                cout << "解码width= " << codedW << " 解码heitht= " << codedH
                     << endl;
              
            }
            frames++;
            if(frames%1000==0){
            frames=0;
             cout << "decode width= " << decode_frame->width
                 << " decode height= " << decode_frame->height << endl;
            }

            //转换+裁剪
            sws_scale( m_img_convert_ctx, (uint8_t const *const *) decode_frame->data,
                       decode_frame->linesize, 0, height, RGB_frame->data, RGB_frame->linesize );
            buffWithSize buff2 = {new uint8_t[numBytes], numBytes, 0};

            for ( int i = 0; i < outputh; i++ ) {
                memcpy( buff2.buffer + buff2.readIndex, m_rgbBuffer + i * RGB_frame->linesize[0],
                        outputw * 3 );
                buff2.readIndex += outputw * 3;
            }
            buff2.size = buff2.readIndex;
//
//            FILE *fout1 = fopen( "../bitmap.bm", "wb" );
//            fwrite( buff2.buffer, 1, buff2.size, fout1 );
//            fclose( fout1 );
            //
            //            for(int i=0;i<=1000;++i){
            //                cout<<(int)*(m_rgbBuffer+i)<<" ";
            //            }
            //            cout<<"输出 buffsize= "<<buff2.readIndex<<"size="<<buff2.size;

            if ( ret == 0 ) {
                outputQueue.put( buff2 );
            } else if ( ret == AVERROR( EAGAIN ) ) {
                break;
            } else {
                break;
                printf( "avcodec_receive_frame error\n" );
            }
            if ( avio_ctx->eof_reached ) {
                break;
            }
            av_packet_unref( packet );
        }
        sws_freeContext( m_img_convert_ctx );
        av_frame_free( &decode_frame );
        av_frame_free( &RGB_frame );
        cout << "endwhile" << endl;
    }

   private:
    AVFormatContext *format_ctx;
    AVIOContext *avio_ctx;
    AVCodec *codec;
    AVCodecContext *codec_ctx;
    AVPacket *packet;

    BlockingQueue<buffWithSize> outputRecycle;
};

int read_socket_buffer( void *opaque, uint8_t *buf, int buf_size ) {
    //    if ( fin2 == nullptr ) {
    //        fin2 = fopen( "message2.bin", "ab" );
    //    }
    //        buf[0]=1;
    //        return 1;
    //        cout << "read_socket_buffer " << buf_size << endl;
    auto *decoder = static_cast<Decoder *>( opaque );
    int count = 0;
    while ( buf_size > 0 ) {
        //            cout<<"buffwhile"<<buff.size<<buff.readIndex<<endl;
        if ( decoder->haventReadFinish.size - decoder->haventReadFinish.readIndex <= 0 ) {
            //释放内存
            if ( decoder->haventReadFinish.buffer != nullptr ) {
                delete[] decoder->haventReadFinish.buffer;
            }
            decoder->haventReadFinish = decoder->inputQueue.take();
            //            cout<<"下一位"<<endl;
        }
        //            cout<<"当前inputbuff size="<<buff.size<<endl;
        int canRead = std::min(
            buf_size, decoder->haventReadFinish.size - decoder->haventReadFinish.readIndex );
        buf_size -= canRead;
        //        cout<<"这次读"<<canRead<<"剩余"<<buf_size<<"start
        //        from:"<<decoder->haventReadFinish.readIndex<<"数据包size="<<decoder->haventReadFinish.size<<endl;

        memcpy( buf + count, decoder->haventReadFinish.buffer + decoder->haventReadFinish.readIndex,
                canRead );

        //        fwrite(decoder->haventReadFinish.buffer+decoder->haventReadFinish.readIndex,1,canRead,fin2);

        count += canRead;
        decoder->haventReadFinish.readIndex += canRead;
        //            cout<<"haventReadFinish"<<decoder->haventReadFinish.size<<decoder->haventReadFinish.readIndex<<endl;
    }
    //    fclose(fin2);
    //    fin2= nullptr;
    //        cout<<"读取完成"<<endl;

    return count;
}

int read_socket_buffer2( void *opaque, uint8_t *buf, int buf_size ) {

    auto *decoder = static_cast<Decoder *>( opaque );
    int count = 0;
    if ( decoder->haventReadFinish.size - decoder->haventReadFinish.readIndex <= 0 ) {
        //释放内存
        if ( decoder->haventReadFinish.buffer != nullptr ) {
            delete[] decoder->haventReadFinish.buffer;
        }
        decoder->haventReadFinish = decoder->inputQueue.take();
    }
    int canRead =
        std::min( buf_size, decoder->haventReadFinish.size - decoder->haventReadFinish.readIndex );

    memcpy( buf + count, decoder->haventReadFinish.buffer + decoder->haventReadFinish.readIndex,
            canRead );

    count += canRead;
    decoder->haventReadFinish.readIndex += canRead;

    return count;
}
#endif  // ASREMOTE_DECODER_H
