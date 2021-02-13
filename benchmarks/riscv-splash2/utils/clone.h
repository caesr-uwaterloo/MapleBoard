#pragma once
unsigned long __clone_rt(
    int   flags,
    void* child_stack,
    void* parent_tidptr,
    void* tls,
    void* child_tidptr
);