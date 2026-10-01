cmake_minimum_required(VERSION 3.22.1)
project(lockperm LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions -fvisibility=hidden")

set(DOBBY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/dobby")

set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(BUILD_TEST OFF CACHE BOOL "" FORCE)
set(Plugin.SymbolResolver ON CACHE BOOL "" FORCE)
set(NearBranch ON CACHE BOOL "" FORCE)

add_subdirectory(${DOBBY_DIR} ${CMAKE_CURRENT_BINARY_DIR}/dobby_build)

add_library(lockperm SHARED lockperm_native.cpp)

target_include_directories(lockperm PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/dobby/include
        ${DOBBY_DIR}/include)

target_link_libraries(lockperm
        dobby
        log
        dl)