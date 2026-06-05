# the minimum version of CMake.
cmake_minimum_required(VERSION 3.4.1)
project(kuikly_entry)

set(KUIKLY_ENTRY_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_CXX_STANDARD 17)

if(EXISTS ${PACKAGE_FIND_FILE})
    include(${PACKAGE_FIND_FILE})
    find_package(render REQUIRED CONFIG)
else()
    message(FATAL_ERROR "PACKAGE_FIND_FILE not found. 请检查CMake构建环境")
endif()

set(SOURCE_SET
        napi_init.cpp
)

add_library(kuikly_entry SHARED ${SOURCE_SET})
include_directories(${KUIKLY_ENTRY_ROOT_PATH} ${KUIKLY_ENTRY_ROOT_PATH}/include)

# Kuikly SDK
add_library(kuikly_render ALIAS render::kuikly)

# 业务 so
add_library(kuikly_shared SHARED IMPORTED)
set_target_properties(kuikly_shared
    PROPERTIES
    IMPORTED_LOCATION ${KUIKLY_ENTRY_ROOT_PATH}/../../../libs/${OHOS_ARCH}/libshared.so)

# 链接系统库、Kuikly SDK 和业务 so
target_link_libraries(kuikly_entry
    PUBLIC
    libace_napi.z.so
    libace_ndk.z.so
    hilog_ndk.z.so
    libnative_drawing.so
    libpixelmap.so
    libimage_source.so
    librawfile.z.so
    kuikly_render
    kuikly_shared
)
