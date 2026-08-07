#import <Foundation/Foundation.h>
#import <stdint.h>

__attribute__((weak)) int __llvm_profile_write_file(void) {
    return 0;
}

__attribute__((weak)) void __llvm_profile_set_filename(const char *Name) {
    (void)Name;
}

__attribute__((weak)) void __llvm_profile_initialize_file(void) {
}

__attribute__((weak)) uint64_t __llvm_profile_get_size_for_buffer(void) {
    return 0;
}

__attribute__((weak)) int __llvm_profile_write_buffer(char *Buffer) {
    (void)Buffer;
    return -1;
}
