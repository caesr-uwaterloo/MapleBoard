
#include "runtime.h"
#include "common.h"
#include "utility.h"
#include "loguru.hpp"

#include <elfio/elfio.hpp>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <elf.h>


void Runtime::_print_elf_type_to_stdout(const ELFIO::elfio& reader) {
  LOG_F(3, "Dumping ELF Info: ");
  LOG_F(3, "ELF file class: %s", reader.get_class() == ELFCLASS32 ? "ELF32" : "ELF64");
  LOG_F(3, "ELF file ecoding: %s", reader.get_encoding() == ELFDATA2LSB ? "little endian" : "big endian");
  if(reader.get_encoding() == ELFDATA2MSB) {
      LOG_F(ERROR, "The runtime does not support big endian ELF");
  }
}
void Runtime::_print_elf_sections_to_stdout(const ELFIO::elfio& reader) {
  ELFIO::Elf_Half sec_num = reader.sections.size();
  for (int i = 0; i < sec_num; i++) {
    const ELFIO::section* psec = reader.sections[i];
    LOG_F(3, "[%02d] %s 0x%x", i, psec->get_name().c_str(), psec->get_size());
    // Access section's data
    const char* p = reader.sections[i]->get_data();
  }
}
void Runtime::_print_elf_segments_to_stdout(const ELFIO::elfio& reader) {
  LOG_F(3, " ===== ");
  LOG_F(3, "FL VA FS MS");

  ELFIO::Elf_Half seg_num = reader.segments.size();
  for (int i = 0; i < seg_num; ++i) {
    const ELFIO::segment* pseg = reader.segments[i];
    LOG_F(3, "[%02d] 0x%x 0x%x 0x%x 0x%x", i, pseg->get_flags(),
          pseg->get_virtual_address(), pseg->get_file_size(),
          pseg->get_memory_size());
    // Access segments's data
    const char* p = reader.segments[i]->get_data();
  }
}

void Runtime::_load_segment(ELFIO::segment* seg) {
  // no need to handle the address transformation
  address_t dst = seg->get_virtual_address();
  std::memcpy(reinterpret_cast<void *>(dst), (const void *)seg->get_data(), seg->get_file_size());
}

void Runtime::_load_elfheader(ELFIO::elfio& reader) {
    size_t header_size = reader.get_header_size();
    size_t elfspace_size = reader.sections[".elfspace"]->get_size();
    std::string elf_path = this->elf_path;
    LOG_F(3, "ELF Header size: 0x%x bytes", header_size);

    /**
     * using mmap is dirty but more direct
     */
    int fd = open(elf_path.c_str(), O_RDONLY, 0);
    void* elf_file = mmap(0, elfspace_size, PROT_READ, MAP_PRIVATE | MAP_POPULATE, fd, 0);
    memcpy((void*)this->memory_start, elf_file, header_size);

    munmap(elf_file, elfspace_size);
    close(fd);
}
void Runtime::_load_phdr_table(ELFIO::elfio& reader) {
    size_t table_size = reader.get_segment_entry_size() * reader.segments.size();
    size_t elfspace_size = reader.sections[".elfspace"]->get_size();
    std::string elf_path = this->elf_path;
    this->phdr = this->memory_start + reader.get_header_size();
    this->phnum = reader.segments.size();
    this->phent = reader.get_segment_entry_size();
    LOG_F(3, "Program header table size: 0x%x bytes", table_size);
    LOG_F(3, "Program header table entsize: 0x%x bytes", this->phent);
    LOG_F(3, "Program header table location: 0x%x", this->phdr);

    /**
     * using mmap is dirty but more direct
     */
    int fd = open(elf_path.c_str(), O_RDONLY, 0);
    void* elf_file = mmap(0, elfspace_size, PROT_READ, MAP_PRIVATE | MAP_POPULATE, fd, 0);
    memcpy((void*)(this->phdr), (void*)(elf_file + reader.get_header_size()), table_size);


    munmap(elf_file, elfspace_size);
    close(fd);
}

void Runtime::load_elf(std::string& path, size_t size) {

  LOG_F(INFO, "Start loading elf");
  ELFIO::elfio reader;
  if (!reader.load(path.c_str())) {
    LOG_F(ERROR, "Cannot find or process ELF file %s", path.c_str());
    std::exit(-1);
  }
  LOG_F(INFO, "ELFIO Done");

  this->elf_path = path;

  this->_print_elf_type_to_stdout(reader);
  this->_print_elf_sections_to_stdout(reader);
  this->_print_elf_segments_to_stdout(reader);
  
  this->elftype = reader.get_class() == ELFCLASS32 ? 32 : 64;

  std::cout << std::hex;

  
  LOG_F(WARNING, "Extracting .tohost from ELF (fail if not present)");
  this->to_host = reader.sections[".tohost"]->get_address();
  LOG_F(WARNING, ".tohost = %x", this->to_host);

  LOG_F(WARNING, "Extracting .fromhost from ELF (fail if not present)");
  this->from_host = reader.sections[".fromhost"]->get_address();
  LOG_F(WARNING, ".fromhost = %x", this->from_host);

  LOG_F(WARNING, "Extracting .syscallargs from ELF (fail if not present)");
  this->syscallargs = reader.sections[".syscallargs"]->get_address();
  LOG_F(WARNING, ".syscallargs = %x", this->syscallargs);

  if(has_mon_value) {
    LOG_F(WARNING, "Extracting .has_mon from ELF (fail if not present)");
    this->has_mon_addr = reader.sections[".has_mon"]->get_address();
    LOG_F(WARNING, ".has_mon = %x", this->has_mon_addr);

    LOG_F(WARNING, "Extracting .mon_core from ELF (fail if not present)");
    this->mon_core_addr = reader.sections[".mon_core"]->get_address();
    LOG_F(WARNING, ".mon_core = %x", this->mon_core_addr);
  }

  LOG_F(WARNING, "Extracting entry point from ELF (fail if not present)");
  this->entry_point = reader.get_entry();
  LOG_F(WARNING, "entry point = %x", this->entry_point);

  LOG_F(WARNING, "Extracting starting address from ELF (fail if not present)");
  this->memory_start = reader.segments[0]->get_virtual_address();
  LOG_F(WARNING, "memory start = %x", this->memory_start);


  this->_allocate_host_memory(this->memory_start, size - this->memory_start);
  LOG_F(WARNING, "Allocating host memory: ");
  this->memory_start = reader.segments[0]->get_virtual_address();
  LOG_F(WARNING, " 0x%x to 0x%x", this->memory_start, this->memory_start + size);
  this->memory_size = size - this->memory_start;

  LOG_F(WARNING, "Copy ELF into memory");

  for(auto seg : reader.segments) {
    if (seg->get_type() == PT_LOAD) {
      this->_load_segment(seg);
    LOG_F(WARNING, "0x871f30=0x%0x", *(unsigned long *)0x871f30);
    }

  }
  for(int i = 0; i < 0x20 / 4; i++) {
      unsigned int* tdata = (unsigned int*)0x0000000000871ef8;
      LOG_F(WARNING, "tdata: 0x%08x", tdata[i]);
  }

  // LOG_F(WARNING, "0x871f30=0x%0x", *(unsigned long *)0x871f30);
  // reset bss section
  for (auto bss_seg : reader.sections) {
    if (bss_seg->get_name() == ".bss"  ||
        bss_seg->get_name() == ".sbss") {
      std::memset(reinterpret_cast<void*>(bss_seg->get_address()), 0,
                  bss_seg->get_size());
    }
  }
  LOG_F(WARNING, "0x871f30=0x%0x", *(unsigned long *)0x871f30);
  // std::abort();
  /* copies the elf header and program header tables into the memory 
   * these information are needed for setting up tls
   * As it is not shown as default, we reserve 0x2000 space (.elfspace section)
   * anything exceeding this size will result in an error
   * should be enough for now but needs to be fixed
   */
  this->_load_elfheader(reader);
  this->_load_phdr_table(reader);

  LOG_F(WARNING, "0x800040 = %d", *(unsigned int*)0x800040);
  // std::abort();

  LOG_F(3, "Calculating curbrk: ");
  unsigned page_exponent = 12;
  this->curbrk = reader.segments[0]->get_virtual_address() + reader.segments[0]->get_memory_size();
  this->curbrk = Utility::round_up_power(this->curbrk, page_exponent); // align to 4K
  this->brk_end = this->curbrk + this->brk_size;
  this->mmap_start = this->brk_end;
  this->mmap_end = this->mmap_start + this->mmap_size;
  LOG_F(WARNING, "Inserting unmapped region: 0x%lx, 0x%lx, sz: 0x%lx", this->mmap_start, this->mmap_end, this->mmap_size);
  this->unmapped_region.insert(std::make_pair(this->mmap_start, this->mmap_end));
  LOG_F(3, "0x%x", this->curbrk);

}

/**
 * Note: the load stack will setup the stack for the program, the following is needed:
 * argc, argv, envp, aux these are the pointers to the the strings
 * we also need to provide the data array for the strings
 * Now we set up the stack of the _start
 * with the following fake structures
 * argument structure
 *   argc   argv               envp
 *           |                  |
 *           v                  v
 * | argc | argv poitners, 0 | env pointers array, 0 | aux vec |
 * refer to this website for detailed layout
 * http://articles.manugarg.com/aboutelfauxiliaryvectors.html
 * the information in the  above link is inaccurate since the exact pointers to strings
 * might not reside in the stack.
 * In our case, the array of pointers lies in margs, when calling __libc_main, the arguments
 * are pointers to this section.
 */
void Runtime::load_stack() {
  address_t end_of_mem = this->memory_start + this->memory_size;
  LOG_F(INFO, "Setting up stack from 0x%x to 0x%x", this->memory_start, end_of_mem);

  address_t stack_top = end_of_mem;
  address_t align_end, align_start;
  // set the stack top of the program, loaded into the sp register

  // retrieve host information
  char** riscv_envp = Utility::get_riscv_envp();
  char** riscv_argv = Utility::get_riscv_argv();
  int    riscv_argc = Utility::get_riscv_argc();
  size_t envp_storage_size = Utility::str_arr_storage(riscv_envp);
  size_t argv_storage_size = Utility::str_arr_storage(riscv_argv);

  // the addresses in the riscv image
  std::vector<address_t> new_argv; 
  std::vector<address_t> new_envp; 

  // allocate memory for envp and copy the array into the space
  stack_top -= envp_storage_size;
  auto stack_pointer = stack_top; // save the current stack_top
  for (unsigned i = 0; riscv_envp[i]; i++) {
    new_envp.push_back(stack_pointer);
    stack_pointer = reinterpret_cast<address_t>(
                    stpcpy(reinterpret_cast<char*>(stack_pointer), riscv_envp[i])) +
                1;
  }
  new_envp.push_back(0);
  LOG_F(WARNING, "envp copied (%d env vars).", new_envp.size());

  // allocate memory for argv
  stack_top -= argv_storage_size;
  stack_pointer = stack_top;
  for(unsigned i = 0; riscv_argv[i]; i++) {
    new_argv.push_back(stack_pointer);
    stack_pointer =
        reinterpret_cast<address_t>(
            stpcpy(reinterpret_cast<char*>(stack_pointer), riscv_argv[i])) +
        1;
  }
  new_argv.push_back(0);
  LOG_F(WARNING, "argv copied.");

  // construct the auxiliary data
  // auxv_t
  align_end = stack_top;
  if(this->elftype == 32) {
    auto [aux, aux_size] = Utility::get_aux_array_32(this);
    stack_top -= aux_size;
    std::memcpy(reinterpret_cast<void*>(stack_top), const_cast<void*>(reinterpret_cast<const void*>(aux)), aux_size);
  } else if(this->elftype == 64) {
    auto [aux, aux_size] = Utility::get_aux_array_64(this);
    stack_top -= aux_size;
    std::memcpy(reinterpret_cast<void*>(stack_top), const_cast<void*>(reinterpret_cast<const void*>(aux)), aux_size);
  }
  LOG_F(3, "auxv constructed at %x.", stack_top);
  // construct the envp and argv
  stack_top -= new_envp.size() * sizeof(address_t);
  std::copy(new_envp.begin(), new_envp.end(), reinterpret_cast<address_t*>(stack_top));
  LOG_F(WARNING, "envp pointer copied to %x.", stack_top);
  stack_top -= new_argv.size() * sizeof(address_t);
  std::copy(new_argv.begin(), new_argv.end(), reinterpret_cast<address_t*>(stack_top));
  LOG_F(WARNING, "argv pointer copied to %x.", stack_top);
  stack_top -= XLEN / 8;
  *(reinterpret_cast<long*>(stack_top)) = riscv_argc;
  LOG_F(WARNING, "argc copied.");
  align_start = stack_top;


  // we need to align the stack top to XLEN boundary otherwise there might be mis-aligned exception
  size_t total_size = align_end - align_start;
  align_start = align_start & ~((XLEN / 8) - 1);
  LOG_F(WARNING, "stacktop realigned to %x", align_start);
  off_t stack_offset = align_start - stack_top;
  LOG_F(WARNING, "stacktop alignment offset %x", stack_offset);
  stack_top = reinterpret_cast<address_t>(
      std::memmove(reinterpret_cast<void*>(align_start),
                   reinterpret_cast<void*>(stack_top), total_size));

  // used for bootstraping the riscv image
  *(reinterpret_cast<address_t*>(this->from_host)) = stack_top;
  LOG_F(WARNING, "stack top copied from_host: %x, stack_top: %x.", this->from_host, stack_top);
  LOG_F(WARNING, "reloading from stack top: %lx", *(reinterpret_cast<int32_t*>(stack_top)));

  if(has_mon_value) {
    *(reinterpret_cast<address_t*>(this->has_mon_addr)) = this->has_mon_value;
    *(reinterpret_cast<address_t*>(this->mon_core_addr)) = this->mon_core_value;
    LOG_F(WARNING, "Setting has_mon = %d", this->has_mon_value);
    LOG_F(WARNING, "Setting mon_core = %d", this->mon_core_value);
  }

  this->_assert_check_program_arguments(riscv_argc, stack_top + XLEN / 8);

  // the stack cannot overwrite the content of programs
  CHECK_F(this->curbrk < stack_top);
}


void Runtime::_assert_check_program_arguments(int argc, address_t argv) {
  LOG_SCOPE_F(INFO, "checking arguments");
  char** riscv_envp = Utility::get_riscv_envp();
  char** riscv_argv = Utility::get_riscv_argv();
  int    riscv_argc = Utility::get_riscv_argc();
  CHECK_F(riscv_argc == argc, "HI");
  char** new_argv = reinterpret_cast<char**>(argv);
  unsigned i = 0;
  for(i = 0; new_argv[i]; i++) {
      CHECK_F(strcmp(new_argv[i], riscv_argv[i]) == 0, "arguments should be the same for host and riscv memory image");
  }
  char** new_envp = &new_argv[i] + 1;
  for(i = 0; new_envp[i]; i++) {
      CHECK_F(strcmp(new_envp[i], riscv_envp[i]) == 0, "environment should be the same for host and riscv memory image");
  }
  LOG_F(INFO, "passed. ");
}

address_t Runtime::create_mmap2(address_t addr, size_t length, int prot, int flags, int fd, off_t pgoffset) {
  // we only support private mappings that is for memory allocation
  if(( (flags & MAP_PRIVATE) != 0) && addr == 0) {
      auto it = this->_next_free_area(length);
      // unable to locate a free memory chunk
      if(it == this->unmapped_region.end()) {
        return -ENOMEM;
      }
      auto chunk = *it;
      this->unmapped_region.erase(it);

      auto ret = chunk.first;
      chunk.first += length;
      if(chunk.first != chunk.second) {
        this->unmapped_region.insert(chunk);
      }

      this->mapped_region.insert(std::make_pair(ret, ret + length));
      for(auto it : this->mapped_region) {
          LOG_F(WARNING, "0x%016lx - 0x%016lx", it.first, it.second);
      }
      return ret;
  }
  LOG_F(ERROR, "Failed preliminary mmap2.\n");
  return -ENOMEM;
}

address_t Runtime::remove_mmap(address_t addr, size_t length) {
    std::pair<address_t, address_t> chunk_to_unmap = std::make_pair(addr, addr + length);
    auto it = this->mapped_region.begin();
    bool found = false;
    for(;it != this->mapped_region.end(); it++) {
        if(this->_is_subset_of(chunk_to_unmap, *it)) {
          found = true;
            break;
        }
    }
    if(!found) return ~0ull;
    if(it == this->mapped_region.end()) return reinterpret_cast<address_t>(~0uLL);
    // the chunk that is enclosing the chunk_to_unmap
    std::pair<address_t, address_t> enclosing_chunk = *it;
    this->mapped_region.erase(it);
    if(enclosing_chunk.first != chunk_to_unmap.first) {
      this->mapped_region.insert(
          std::make_pair(enclosing_chunk.first, chunk_to_unmap.first));
    }
    if(chunk_to_unmap.second != enclosing_chunk.second) {
      this->mapped_region.insert(
          std::make_pair(chunk_to_unmap.second, enclosing_chunk.second));
    }
    this->unmapped_region.insert(chunk_to_unmap);
    return 0;
}


std::set<std::pair<address_t, address_t> >::iterator
Runtime::_next_free_area(size_t length) {
    for(auto it = this->unmapped_region.begin(); it != this->unmapped_region.end(); it++) {
        LOG_F(WARNING, "next free 0x%lx (0x%lx to 0x%lx, size 0x%lx)", it->first, it->first, it->second, it->second - it->first);
        if(it->second - it->first >= length) {
            return it;
        }
    }
    return this->unmapped_region.end();
}


bool Runtime::_is_subset_of(const std::pair<address_t, address_t>& p, const std::pair<address_t, address_t>& q) {
    return p.first >= q.first && p.second <= q.second;
}
