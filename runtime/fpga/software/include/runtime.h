#pragma once
/**
 * The runtime is used to setup the memory content and expose RISC-V core control to
 * the host program.
 * The runtime also maintains memory mangagement values of the loaded elf
 */
#include "common.h"

#include <string>
#include <set>
#include <map>
#include <elfio/elfio.hpp>

class Runtime {
public:
  /**
   * sets the registers in the RISC-V core
   * in simulation, this is done by calling exported dpi tasks 
   * in real FPGA board, this is normally achieved by writing to memory mapped addresses
   */
  virtual void set_init_pc(address_t pc) = 0;
  virtual void set_reset(int value) = 0;
  virtual void set_base_address(address_t baseAddress) = 0;
  // Only used for VU9P board for coherent accesses
  virtual void setup_post(address_t start) {}
  virtual uint64_t get_stats(int offset) = 0;

  /**
   * gets the underlying physical address, which will be used in the AXI transactions
   */
  virtual address_t get_phys_start() { return this->phys_start; };
  /**
   * loads the elf specified by the path, note that no relocation will be applied.
   * program and sections will be loaded as per the elf file specifies.
   */
  void load_elf(std::string& path, size_t size);

  /**
   * loads the stack content in the end of the memory
   * the content includes argc, argv, envp and aux vector
   * for riscv-tests, these content are not used but still initialized
   */
  void load_stack();

  /**
   * Set whether there is a core for 
   */
  void set_mon_core(unsigned long mcore) { 
    this->mon_core_value = mcore;
    this->has_mon_value = 1;
  }
  void unset_mon_core() {
    this->has_mon_value = 0;
  }

  /**
   *  maps a chunk of memory, emulates the mmap2 system call
   */
  address_t create_mmap2(address_t addr, size_t length, int prot, int flags, int fd, off_t pgoffset);

  address_t remove_mmap(address_t addr, size_t length);


 protected:
  /**
   * allocates host memory, the memory addresses will start as the parameter
   * suggests the host and the core shares the same address space: [start,
   * start + size) note that in hardware, this chunk of memory should be
   * mapped to contiguous area, for example, the area used for DMA, if the
   * core does not support virtual memory
   */
  virtual void _allocate_host_memory(address_t start, size_t size) = 0;

  /**
   * copies a segment to the memory
   * addresses and sizes are encoded in the segment class
   */
  void _load_segment(ELFIO::segment* seg);

  /**
   * copies elf header to the memory
   */
  void _load_elfheader(ELFIO::elfio& reader);
  /**
   * copies program header table to the memory
   */
  void _load_phdr_table(ELFIO::elfio& reader);

  /**
   * copies the envp array to the end of the stack
   * and returns the start of env array
   */
  address_t _copy_env(address_t end);
  /**
   * copies the args array to the end of the stack
   * and returns the start of arg array
   */
  address_t _copy_arg(address_t end);

 private:
  typedef std::pair<address_t, address_t> interval_t;
  void _print_elf_type_to_stdout(const ELFIO::elfio& reader);
  void _print_elf_sections_to_stdout(const ELFIO::elfio& reader);
  void _print_elf_segments_to_stdout(const ELFIO::elfio& reader);
  /**
   * wrong arguments cause the program to fail and abort the program
   */
  void _assert_check_program_arguments(int argc, address_t argv);
  std::set<std::pair<address_t, address_t> >::iterator _next_free_area(
      size_t length);
  /**
   * test whether p is a subset of q
   */
  bool _is_subset_of(const std::pair<address_t, address_t>& p,
                     const std::pair<address_t, address_t>& q);
  /**
   *  merge unmapped region to have larger space
   */
  void _merge_intervals();

 public:
  /**
   * useful addresses
   * these addresses will be extracted from the elf file
   */
  address_t syscallargs;  // address for retrieving system call arguments
  address_t curbrk;
  address_t brk_end;
  address_t mmap_start;
  address_t mmap_end;
  address_t to_host;
  address_t from_host;
  address_t entry_point;
  address_t memory_start; // the start of memory in the host address space
  address_t phdr;
  address_t phnum;
  address_t phent;

  /**
   * Dedicated for CARP
   */
  address_t has_mon_addr;
  address_t mon_core_addr;
  unsigned long has_mon_value = 0;
  unsigned long mon_core_value;

  address_t phys_start; // physically start of the CMA address
  size_t elftype;
  size_t memory_size;
  std::string elf_path;

  /**
   *  Useful constants
   */
  size_t brk_size  = 0x01000000ULL; // 16MB
  size_t mmap_size = 0x09000000ULL; // 128MB + (slightly larger than 128)

  /**
   * used for memory map
   */
  std::set<std::pair<address_t, address_t> > unmapped_region;
  std::set<std::pair<address_t, address_t> > mapped_region;
}; // class Runtime
