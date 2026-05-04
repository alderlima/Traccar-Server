cmake_minimum_required(VERSION 3.18.1)
project("jvm-bridge")

# Incluir headers JNI do Android
find_library(log-lib log)

add_library(jvm-bridge SHARED jvm-bridge.cpp)

target_link_libraries(jvm-bridge ${log-lib})