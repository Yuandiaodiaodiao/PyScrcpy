//
// Created by qq295 on 2020/4/25.
//

#ifndef ASREMOTE_BUFFER_H
#define ASREMOTE_BUFFER_H

struct buffWithSize {
    uint8_t *buffer;
    int size;
    int readIndex;
};

#endif  // ASREMOTE_BUFFER_H
