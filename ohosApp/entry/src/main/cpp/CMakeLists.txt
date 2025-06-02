# the minimum version of CMake.
cmake_minimum_required(VERSION 3.4.1)
project(kuikly_entry)

set(KUIKLY_ENTRY_ROOT_PATH .)
set(CMAKE_CXX_STANDARD 17)
if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

set(SOURCE_SET
        napi_init.cpp
)

add_library(kuikly_entry SHARED ${SOURCE_SET})
include_directories(${KUIKLY_ENTRY_ROOT_PATH} ${KUIKLY_ENTRY_ROOT_PATH}/include)

# include render头文件
target_include_directories(kuikly_entry PRIVATE "${KUIKLY_ENTRY_ROOT_PATH}/../../../../../core-render-ohos/src/main/cpp")
# 链接render库
message(${KUIKLY_ENTRY_ROOT_PATH})
set(OHOS_RENDER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../oh_modules/@kuikly-open/render/build/default/intermediates/libs/default/${OHOS_ARCH})
add_library(kuikly_render SHARED IMPORTED)
set_target_properties(kuikly_render PROPERTIES IMPORTED_LOCATION ${OHOS_RENDER_DIR}/libkuikly.so)
target_link_libraries(kuikly_entry PUBLIC kuikly_render)

# 链接系统库
target_link_libraries(kuikly_entry PUBLIC
    libace_napi.z.so
    libace_ndk.z.so
    hilog_ndk.z.so
    libnative_drawing.so
    libpixelmap.so
    libimage_source.so
    # biz库
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/arm64-v8a/libshared.so
)
