include_guard(GLOBAL)
include(FetchContent)

# simdutf v9.3.0. Keep dependency configuration shared with the host tests.
set(SIMDUTF_TESTS OFF CACHE BOOL "" FORCE)
set(SIMDUTF_TOOLS OFF CACHE BOOL "" FORCE)
set(SIMDUTF_BENCHMARKS OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
        simdutf
        GIT_REPOSITORY https://github.com/simdutf/simdutf.git
        GIT_TAG bb77e7bb98fe0c2296e7cf9f324c56fcd704885a
)
function(kuikly_fetch_simdutf)
    set(BUILD_SHARED_LIBS OFF)
    FetchContent_MakeAvailable(simdutf)
endfunction()
kuikly_fetch_simdutf()
set_target_properties(simdutf PROPERTIES
        POSITION_INDEPENDENT_CODE ON
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
)
target_compile_definitions(simdutf PUBLIC
        SIMDUTF_FEATURE_BASE64=0
        SIMDUTF_FEATURE_LATIN1=0
        SIMDUTF_FEATURE_DETECT_ENCODING=0
)
