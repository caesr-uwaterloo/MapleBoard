#ifndef _MAPLE_BOARD_COMM_EMU_H
#define _MAPLE_BOARD_COMM_EMU_H

#define _MB_HS 0
#define _MB_DMA 1
#define _MB_RD 2
#define _MB_WR 3

#ifdef __cplusplus
extern "C" {
#endif
struct mb_device_request {
    uint64_t req_type;
    uint64_t address;
    uint64_t data;
    uint64_t size;
    uint64_t dma_len;
    uint8_t dma_payload[];
};
struct mb_device_response {
    uint64_t address;
    uint64_t data;
};

#ifdef __cplusplus
}
#endif

#endif