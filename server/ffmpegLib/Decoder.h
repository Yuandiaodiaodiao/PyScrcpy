//
// Created by qq295 on 2020/4/25.
//

#ifndef ASREMOTE_DECODER_H
#define ASREMOTE_DECODER_H
extern "C" {
#include "libavformat/avformat.h"
#include "libavutil/error.h"
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
AVPixelFormat ap = AV_PIX_FMT_YUV444P;

int read_socket_buffer( void *opaque, uint8_t *buf, int buf_size );
int read_socket_buffer2( void *opaque, uint8_t *buf, int buf_size );

int hackRead( void *opaque, uint8_t *buf, int buf_size ) {
    if ( fin == nullptr ) {
        fin = fopen( "message.bin", "rb" );
    }
    int count = fread( buf, 1, buf_size, fin );
        cout << "读取" << count << "字节" << endl;
    if ( count == 0 ) {
        return -1;
    }
    return count;
}

class Decoder {
   public:
    BlockingQueue<buffWithSize> inputQueue;
    BlockingQueue<buffWithSize> outputQueue;
    buffWithSize haventReadFinish;

    Decoder() : haventReadFinish( buffWithSize{nullptr, 0, 0} ) {
        avformat_network_init();
    }
    void init( bool hack, int widthx, int heightx ) {
        format_ctx = avformat_alloc_context();
        auto *buffer = static_cast<unsigned char *>( av_malloc( BUF_SIZE ) );
        avio_ctx = avio_alloc_context( buffer, BUF_SIZE, 0, this,
                                       hack ? hackRead : read_socket_buffer2, nullptr, nullptr );
        format_ctx->pb = avio_ctx;
        int ret = avformat_open_input( &format_ctx, nullptr, nullptr, nullptr );
        codec = avcodec_find_decoder( AV_CODEC_ID_H264 );
        codec->pix_fmts = &ap;
        //        codec->pix_fmts = &ap;

        codec_ctx = avcodec_alloc_context3( codec );
        codec_ctx->width = widthx;
        codec_ctx->height = heightx;
        ret = avcodec_open2( codec_ctx, codec, nullptr );
        packet = av_packet_alloc();
        cout << "编码器打开成功" << endl;
    }
    void run() {
        printf( "decoder w = %d ,h =%d \n", codec_ctx->width, codec_ctx->height );
        AVFrame *decode_frame = av_frame_alloc();
        AVFrame *RGB_frame = av_frame_alloc();
        int numBytes = avpicture_get_size( AV_PIX_FMT_RGB24, codec_ctx->width, codec_ctx->height );
        uint8_t *m_rgbBuffer, *m_yuvBuffer;
        m_rgbBuffer = (uint8_t *) av_malloc( numBytes * sizeof( uint8_t ) );
        int yuvSize = codec_ctx->width * codec_ctx->height * 3 / 2;
        m_yuvBuffer = (uint8_t *) av_malloc( yuvSize );
        struct SwsContext *m_img_convert_ctx;
        //特别注意sws_getContext内存泄露问题，
        //注意sws_getContext只能调用一次，在初始化时候调用即可，另外调用完后，在析构函数中使用sws_freeContext，将它的内存释放。
        //设置图像转换上下文
        m_img_convert_ctx = sws_getContext( codec_ctx->width, codec_ctx->height, AV_PIX_FMT_YUV420P,
                                            codec_ctx->width, codec_ctx->height, AV_PIX_FMT_RGB32,
                                            SWS_BICUBIC, NULL, NULL, NULL );
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
            int Urate = decode_frame->linesize[0] / decode_frame->linesize[1];
            int Vrate = decode_frame->linesize[0] / decode_frame->linesize[2];
            int Ylength = decode_frame->height * decode_frame->width;
            int Ulength = decode_frame->height * decode_frame->width / Urate / Urate;
            int Vlength = decode_frame->height * decode_frame->width / Vrate / Vrate;

            buffWithSize buff = {new uint8_t[Ylength + Ulength + Vlength],
                                 Ylength + Ulength + Vlength, 0};
            //            cout << "输出frame size=" << buff.size << endl;
            for ( int i = 0; i < decode_frame->height; i++ ) {
                memcpy( buff.buffer + buff.readIndex,
                        decode_frame->data[0] + i * decode_frame->linesize[0],
                        decode_frame->width );
                buff.readIndex += decode_frame->width;
            }

            for ( int i = 0; i < decode_frame->height / Urate; i++ ) {
                memcpy( buff.buffer + buff.readIndex,
                        decode_frame->data[1] + i * decode_frame->linesize[1],
                        decode_frame->width / Urate );
                buff.readIndex += decode_frame->width / Urate;
            }
            for ( int i = 0; i < decode_frame->height / Vrate; i++ ) {
                memcpy( buff.buffer + buff.readIndex,
                        decode_frame->data[2] + i * decode_frame->linesize[2],
                        decode_frame->width / Vrate );
                buff.readIndex += decode_frame->width / Vrate;
            }
            avpicture_fill( (AVPicture *) RGB_frame, m_rgbBuffer, AV_PIX_FMT_RGB24,
                            decode_frame->width, decode_frame->height );
            sws_scale( m_img_convert_ctx, (uint8_t const *const *) decode_frame->data,
                       decode_frame->linesize, 0, decode_frame->height, RGB_frame->data,
                       RGB_frame->linesize );
            
            if ( ret == 0 ) {
                outputQueue.put( buff );
            } else if ( ret == AVERROR( EAGAIN ) ) {
                break;
            } else {
                break;
                printf( "avcodec_receive_frame error\n" );
            }
            //如果已经读完，就GG
            if ( avio_ctx->eof_reached ) {
                break;
            }
            av_packet_unref( packet );
        }
        sws_freeContext( m_img_convert_ctx );
        av_frame_free( &decode_frame );
        cout << "endwhile" << endl;
    }

   private:
    AVFormatContext *format_ctx;
    AVIOContext *avio_ctx;
    AVCodec *codec;
    AVCodecContext *codec_ctx;
    AVPacket *packet;
    const static size_t BUF_SIZE = 65535;

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
