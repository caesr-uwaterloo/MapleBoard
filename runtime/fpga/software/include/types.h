#pragma once

// #include "coreparam.h"

#include <cstdint>

// 64-bit address
typedef unsigned long long address_t;

/**
 * word_t is of size XLEN, the struct might affect the function of 
 * system calls, for example, system arguments are of size XLEN
 */
template <int word_size>
class _word_t {
 public:

  static _word_t<word_size> from(void*);
  static _word_t<word_size> from(int);
};

template <>
class _word_t<64> {
 public:
  uint64_t v;
  static _word_t<64> from(int i) { return _word_t<64> { .v = (uint64_t)i };};
  static _word_t<64> from(void* addr) {
    uint64_t v = *reinterpret_cast<volatile uint64_t*>(addr);
    return _word_t<64> { .v = v };
  };
};
template <>
class _word_t<32> {
 public:
  uint32_t v;
  static _word_t<32> from(int i) { return _word_t<32> { .v = (uint32_t)i };};
  static _word_t<32> from(void* addr) {
    uint32_t v = *reinterpret_cast<volatile uint32_t*>(addr);
    return _word_t<32> { .v = v };
  }
};
template<int word_size>
struct _word_comparator_t {
  bool operator()(const _word_t<word_size>& l,
                  const _word_t<word_size>& r) const {
    return l.v < r.v;
  }
};  // comparator_t

typedef _word_t<XLEN> word_t;
typedef _word_comparator_t<XLEN> word_comparator_t;
