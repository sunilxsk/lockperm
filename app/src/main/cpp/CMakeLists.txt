cmake_minimum_required(VERSION 3.22.1)
project(lockperm LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 所有目标都带 -fPIC，避免静态库链接进 so 时报重定位错误
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# 关掉 RTTI / 异常，减小体积
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions -fvisibility=hidden")

# ---------------- Dobby 源码（就在 cpp/dobby 下） ----------------
set(DOBBY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/dobby")

set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(BUILD_TEST OFF CACHE BOOL "" FORCE)
set(Plugin.SymbolResolver ON CACHE BOOL "" FORCE)
set(NearBranch ON CACHE BOOL "" FORCE)

add_subdirectory(${DOBBY_DIR} ${CMAKE_CURRENT_BINARY_DIR}/dobby_build)

# ---------------- 本模块的 so ----------------
add_library(lockperm SHARED lockperm_native.cpp)

target_include_directories(lockperm PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/dobby/include
        ${DOBBY_DIR}/include)

target_link_libraries(lockperm
        dobby
        log
        dl)