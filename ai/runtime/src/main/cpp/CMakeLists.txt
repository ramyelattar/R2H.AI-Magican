cmake_minimum_required(VERSION 3.22.1)
project(ai_runtime_jni)

option(ENABLE_STUB_RUNTIME "Enable JNI stub runtime when llama.cpp is unavailable" ON)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/llama.cpp)
set(HAS_LLAMA_CPP OFF)
if(EXISTS ${LLAMA_CPP_DIR}/CMakeLists.txt)
    set(HAS_LLAMA_CPP ON)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    add_subdirectory(${LLAMA_CPP_DIR} ${CMAKE_CURRENT_BINARY_DIR}/llama_cpp_build)
else()
    if(ENABLE_STUB_RUNTIME)
        message(WARNING "llama.cpp not found at ${LLAMA_CPP_DIR}. Building JNI stub fallback.")
    else()
        message(FATAL_ERROR "llama.cpp not found and ENABLE_STUB_RUNTIME=OFF. Release build cannot proceed.")
    endif()
endif()

add_library(
    ai_runtime_jni
    SHARED
    llama_jni.cpp
)

if(HAS_LLAMA_CPP)
    target_include_directories(
        ai_runtime_jni
        PRIVATE
        ${LLAMA_CPP_DIR}
        ${LLAMA_CPP_DIR}/include
    )
    target_compile_definitions(ai_runtime_jni PRIVATE HAVE_LLAMA_CPP=1)
else()
    if(ENABLE_STUB_RUNTIME)
        target_compile_definitions(ai_runtime_jni PRIVATE USE_LLAMA_STUB=1)
    endif()
endif()

target_link_libraries(
    ai_runtime_jni
    PRIVATE
    log
    android
)

if(HAS_LLAMA_CPP)
    target_link_libraries(ai_runtime_jni PRIVATE llama)
endif()
