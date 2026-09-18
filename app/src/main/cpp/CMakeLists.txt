cmake_minimum_required(VERSION 3.22)

project(piglauncher_hyos LANGUAGES CXX ASM)

if(NOT ANDROID)
    message(FATAL_ERROR "piglauncher_hyos must use the Android NDK toolchain")
endif()

if(NOT "${ANDROID_ABI}" STREQUAL "arm64-v8a")
    message(FATAL_ERROR "Only arm64-v8a is supported")
endif()

add_library(piglauncher_hyos SHARED piglauncher_native.cpp probe_trampoline.S)
target_compile_features(piglauncher_hyos PRIVATE cxx_std_17)
target_compile_options(piglauncher_hyos PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:-fno-exceptions>
    $<$<COMPILE_LANGUAGE:CXX>:-fno-rtti>
    $<$<COMPILE_LANGUAGE:CXX>:-fvisibility=hidden>
    $<$<COMPILE_LANGUAGE:CXX>:-fvisibility-inlines-hidden>
    $<$<COMPILE_LANGUAGE:CXX>:-ffunction-sections>
    $<$<COMPILE_LANGUAGE:CXX>:-fdata-sections>
    $<$<COMPILE_LANGUAGE:CXX>:-mbranch-protection=standard>
    $<$<COMPILE_LANGUAGE:CXX>:-Wall>
    $<$<COMPILE_LANGUAGE:CXX>:-Wextra>
    $<$<COMPILE_LANGUAGE:CXX>:-Werror>
)
target_link_options(piglauncher_hyos PRIVATE
    "-Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/exports.map"
    -Wl,--gc-sections
    -Wl,--no-undefined
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384
)
target_link_libraries(piglauncher_hyos PRIVATE log dl)
set_target_properties(piglauncher_hyos PROPERTIES CXX_EXTENSIONS OFF OUTPUT_NAME "piglauncher_hyos")
