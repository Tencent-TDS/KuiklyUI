/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
