// poke the address then flush
#define POKE_ADDR(addr_label, val, r1, r2, r3) \
  csrr r1, mhartid;    \
  slli r1, r1, 6;      \
  la   r2, addr_label; \
  add  r2, r2, r1;     \
  li   r3, val;        \
  sw   r3, 0(r2);      \
  lw   x0, 1024(r2);

// r1 is the target, r2 is an auxiliary structure
#define PEEK_ADDR(addr_label, r1, r2) \
  csrr r1, mhartid;          \
  slli r1, r1, 6;            \
  la   r2, addr_label;       \
  add  r2, r2, r1;           \
  lw   x0, 1024(r2);         \
  lw   r1, 0(r2);

#define STORE_SYSCALL_ARGS(addr_reg) \
  sw a0, 0(addr_reg); \
  sw a1, 4(addr_reg); \
  sw a2, 8(addr_reg); \
  sw a3,12(addr_reg); \
  sw a4,16(addr_reg); \
  sw a5,20(addr_reg); \
  sw a6,24(addr_reg); \
  sw a7,28(addr_reg);


