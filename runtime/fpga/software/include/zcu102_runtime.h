#pragma once

#include "runtime.h"

class ZCU102Runtime : public Runtime {
public:
    virtual void set_init_pc(address_t pc);
    virtual void set_reset(int value);
    virtual void set_base_address(address_t baseAddress);
    ZCU102Runtime();

protected:
    virtual void _allocate_host_memory(address_t start, size_t size);

private:
    uint32_t* reset;
    uint32_t* initPCHi, *initPCLo;
    uint32_t* baseAddrHi, *baseAddrLo;
};


extern "C" {
#include <libxlnk_cma.h>

void _xlnk_reset();
void* sds_alloc_cacheable(uint32_t);
void* sds_alloc_non_cacheable(uint32_t);
void *sds_mmap(void *phy_addr, size_t size, void *virtual_addr);
void *sds_munmap(void *virtual_addr);

}
extern int fd;
