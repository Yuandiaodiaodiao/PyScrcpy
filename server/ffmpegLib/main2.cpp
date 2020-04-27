//
// Created by qq295 on 2020/4/23.
//
//#pragma execution_character_set("utf-8")
extern "C" {
#include "libavformat/avformat.h"
#include "libavutil/error.h"
#include "libavutil/pixfmt.h"
};
#include <iostream>
using std::cout;
using std::endl;
#define BUF_SIZE 0x10000
#define EAGAIN 11
#define _CRT_SECURE_NO_WARNINGS
FILE *fin;

int read_socket_buffer( void *opaque, uint8_t *buf, int buf_size ) {
    int count = fread( buf, 1, buf_size, fin );
    cout << "读取" << count << "字节" << endl;
    if ( count == 0 ) {
        return -1;
    }
    return count;
}

//函数作用：将解码出来的YUV数据保存成JPG图像
// out_file_name -- JPEG图像保存路径
// w, h -- 图像宽高
// linesize -- 图像的Y分量宽度（一般4字节对齐）
// Y, U, V -- 指向图像Y，U，V三个平面的指针
//
int EncodeAndSaveJPEG( const char *out_file_name, int m_jpegQuality, int w, int h, int linesize,
                       uint8_t *Y, uint8_t *U, uint8_t *V ) {
    AVFormatContext *pFormatCtx;
    AVOutputFormat *fmt;
    AVStream *video_st;
    AVCodecContext *pCodecCtx;
    AVCodec *pCodec;
    uint8_t *picture_buf;
    AVFrame *picture;
    AVPacket pkt;
    int y_size;
    int got_picture = 0;
    int size;
    int ret = 0;

    // av_register_all();

#if 0
    //Method 1
    pFormatCtx = avformat_alloc_context();
    //Guess format
    fmt = av_guess_format("mjpeg", NULL, NULL);
    pFormatCtx->oformat = fmt;
    //Output URL
    if (avio_open(&pFormatCtx->pb, out_file_name, AVIO_FLAG_READ_WRITE) < 0){
        fprintf(stderr, "Couldn't open output file.\n");
        return -1;
    }
#else
    // Method 2. More simple
    avformat_alloc_output_context2( &pFormatCtx, NULL, NULL, out_file_name );
    fmt = pFormatCtx->oformat;
#endif

    video_st = avformat_new_stream( pFormatCtx, 0 );
    if ( video_st == NULL ) {
        return -1;
    }
    pCodecCtx = video_st->codec;
    pCodecCtx->codec_id = fmt->video_codec;
    pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
    pCodecCtx->pix_fmt = AV_PIX_FMT_YUVJ420P;

    pCodecCtx->width = w;
    pCodecCtx->height = h;

    pCodecCtx->time_base.num = 1;
    pCodecCtx->time_base.den = 25;
    // Output some information
    av_dump_format( pFormatCtx, 0, out_file_name, 1 );

    pCodec = avcodec_find_encoder( pCodecCtx->codec_id );
    if ( !pCodec ) {
        fprintf( stderr, "jpeg Codec not found. \n" );
        return -1;
    }
    if ( avcodec_open2( pCodecCtx, pCodec, NULL ) < 0 ) {
        fprintf( stderr, "Could not open jpeg codec. \n" );
        return -1;
    }
    picture = av_frame_alloc();
    //    picture = avcodec_alloc_frame();
    size = avpicture_get_size( pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height );
    picture_buf = (uint8_t *) av_malloc( size );
    if ( !picture_buf ) {
        avcodec_close( video_st->codec );
        avio_close( pFormatCtx->pb );
        avformat_free_context( pFormatCtx );
        return -1;
    }
    avpicture_fill( (AVPicture *) picture, picture_buf, pCodecCtx->pix_fmt, pCodecCtx->width,
                    pCodecCtx->height );

    if ( m_jpegQuality > 0 ) {
        if ( m_jpegQuality > 100 ) m_jpegQuality = 100;

        pCodecCtx->qcompress = (float) m_jpegQuality / 100.f;  // 0~1.0, default is 0.5
        pCodecCtx->qmin = 2;
        pCodecCtx->qmax = 31;
        pCodecCtx->max_qdiff = 3;

        fprintf( stderr, "JPEG quality is: %d\n", m_jpegQuality );
    }

    // Write Header
    avformat_write_header( pFormatCtx, NULL );

    y_size = pCodecCtx->width * pCodecCtx->height;
    av_new_packet( &pkt, y_size * 3 );

    picture->data[0] = Y;
    picture->data[1] = U;
    picture->data[2] = V;
    picture->linesize[0] = linesize;
    picture->linesize[1] = linesize / 2;
    picture->linesize[2] = linesize / 2;

    // Encode
    ret = avcodec_encode_video2( pCodecCtx, &pkt, picture, &got_picture );
    if ( ret < 0 ) {
        fprintf( stderr, "avcodec_encode_video2 Error.\n" );
        return -1;
    }
    if ( got_picture == 1 ) {
        pkt.stream_index = video_st->index;
        ret = av_write_frame( pFormatCtx, &pkt );
    }

    av_free_packet( &pkt );
    // Write Trailer
    av_write_trailer( pFormatCtx );

    if ( video_st ) {
        avcodec_close( video_st->codec );
        av_free( picture );
        av_free( picture_buf );
    }
    avio_close( pFormatCtx->pb );
    avformat_free_context( pFormatCtx );

    return 0;
}


AVFormatContext *format_ctx;
AVIOContext *avio_ctx;
AVCodec *codec;
AVCodecContext *codec_ctx;
AVPacket *packet;
int main() {
    system( "chcp 65001" );
    cout << 1 << endl;
    avformat_network_init();

    format_ctx = avformat_alloc_context();
    unsigned char *buffer = static_cast<unsigned char *>( av_malloc( BUF_SIZE ) );

    fin = fopen( "message.bin", "rb" );

    avio_ctx = avio_alloc_context( buffer, BUF_SIZE, 0, NULL, read_socket_buffer, NULL, NULL );
    format_ctx->pb = avio_ctx;
    int ret = avformat_open_input( &format_ctx, NULL, NULL, NULL );
    codec = avcodec_find_decoder( AV_CODEC_ID_H264 );
    codec_ctx = avcodec_alloc_context3( codec );
    codec_ctx->width = 360;
    codec_ctx->height = 640;
    ret = avcodec_open2( codec_ctx, codec, NULL );
    packet = av_packet_alloc();
    printf( "编码器打开成功\n" );

    // loop

    while ( av_read_frame( format_ctx, packet ) >= 0 ) {
        printf( "decoder w = %d ,h =%d \n", codec_ctx->width, codec_ctx->height );

        while ( true ) {
            ret = avcodec_send_packet( codec_ctx, packet );
            if ( ret == 0 ) {
                printf( "avcodec_send_packet success\n" );
                //成功找到了
                break;
            } else if ( ret == AVERROR( EAGAIN ) ) {
                printf( "avcodec_send_packet EAGAIN\n" );
                break;
            } else {
                printf( "avcodec_send_packet error:%s\n" );
                av_packet_unref( packet );
            }
        }
        AVFrame *decode_frame = av_frame_alloc();
        ret = avcodec_receive_frame( codec_ctx, decode_frame );
        FILE *foutY = fopen( "Y.bin", "wb" );
        FILE *foutU = fopen( "U.bin", "wb" );
        FILE *foutV = fopen( "V.bin", "wb" );

        for ( int i = 0; i < decode_frame->height; i++ ) {
            fwrite( decode_frame->data[0] + i * decode_frame->linesize[0], 1, decode_frame->width,
                    foutY );
            //            t_file.write((char*)(pFrame->data[0]+i*t_yPerRowBytes),t_frameWidth);
        }

        for ( int i = 0; i < decode_frame->height / 2; i++ ) {
            fwrite( decode_frame->data[1] + i * decode_frame->linesize[1], 1,
                    decode_frame->width / 2, foutU );

            //            t_file.write((char*)(pFrame->data[1]+i*t_uPerRowBytes),t_frameWidth/2);
        }

        for ( int i = 0; i < decode_frame->height / 2; i++ ) {
            fwrite( decode_frame->data[2] + i * decode_frame->linesize[2], 1,
                    decode_frame->width / 2, foutV );

            //            t_file.write((char*)(pFrame->data[2]+i*t_vPerRowBytes),t_frameWidth/2);
        }

        //        fwrite(decode_frame->buf[0]->data,1,decode_frame->buf[0]->size,foutY);
        //        fwrite(decode_frame->buf[1]->data,1,decode_frame->buf[1]->size,foutU);
        //        fwrite(decode_frame->buf[2]->data,1,decode_frame->buf[2]->size,foutV);
        fclose( foutY );
        fclose( foutU );
        fclose( foutV );
        EncodeAndSaveJPEG( "image.jpg", 100, decode_frame->width, decode_frame->height,
                           decode_frame->linesize[0], decode_frame->data[0], decode_frame->data[1],
                           decode_frame->data[2] );
        if ( ret == 0 ) {
        } else if ( ret == AVERROR( EAGAIN ) ) {
        } else {
            printf( "avcodec_receive_frame error:%s\n" );
        }
        //送现
        //    SDL_bool consumer_preview = cache->product_frame();
        //        sc->send_frame(pFrame);
        //如果上一个还没显示，则不需要发送事件，等上一个显示了。在发送
        //    if (consumer_preview == SDL_FALSE) {
        //
        //    } else {
        //      screen->push_frame_event();
        //    }

        //如果已经读完，就GG
        if ( avio_ctx->eof_reached ) {
            break;
        }
        av_packet_unref( packet );
    }
}
