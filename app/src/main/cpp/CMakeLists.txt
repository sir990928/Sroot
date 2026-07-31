cmake_minimum_required(VERSION 3.22.1)
project(sroot_native LANGUAGES C)

add_library(sroot_native SHARED native_probe.c)
target_compile_options(sroot_native PRIVATE -O2 -Wall -Wextra -Werror)
target_link_libraries(sroot_native PRIVATE log)
