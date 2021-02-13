#pragma once
#include "encoding.h"

/**
 * the double macros here expands macros properly
 * https://stackoverflow.com/questions/195975/how-to-make-a-char-string-from-a-c-macros-value
 */
#define _STR(name) #name
#define STR(n) _STR(n)

unsigned long __csrr(unsigned int csrid);
unsigned long __csrw(unsigned int csrid, unsigned long value);
unsigned long __getmhartid();
unsigned long __getmepc();
unsigned long __getmtval();
unsigned long __setmepc(unsigned long epc);
unsigned long __setmscratch(unsigned long mscratch);