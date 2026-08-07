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
#import <errno.h>
#import <stdio.h>
#import <stdlib.h>
#import <string.h>
#import "KRLLVMProfileDump.h"

extern int __llvm_profile_write_file(void);
extern void __llvm_profile_set_filename(const char *Name);
extern void __llvm_profile_initialize_file(void);
extern uint64_t __llvm_profile_get_size_for_buffer(void);
extern int __llvm_profile_write_buffer(char *Buffer);

void KRLLVMProfileSetFilename(const char *path) {
    if (path == NULL) {
        return;
    }
    NSString *nsPath = [NSString stringWithUTF8String:path];
    NSString *dir = [nsPath stringByDeletingLastPathComponent];
    [[NSFileManager defaultManager] createDirectoryAtPath:dir
                              withIntermediateDirectories:YES
                                               attributes:nil
                                                    error:nil];
    setenv("LLVM_PROFILE_FILE", path, 1);
    __llvm_profile_set_filename(path);
    __llvm_profile_initialize_file();
    NSLog(@"[LLVM_PGO] set_filename => %s", path);
}

void KRLLVMProfileDump(void) {
    // 先尝试 runtime 原生 write_file（与插装同版本的 compiler-rt 时更可靠）
    errno = 0;
    int rc = __llvm_profile_write_file();
    NSLog(@"[LLVM_PGO] __llvm_profile_write_file => %d errno=%d (%s)", rc, errno, strerror(errno));
    if (rc == 0) {
        return;
    }

    // 回退：buffer API
    uint64_t size = __llvm_profile_get_size_for_buffer();
    NSLog(@"[LLVM_PGO] profile buffer size=%llu", (unsigned long long)size);
    if (size > 0 && size < 512ull * 1024ull * 1024ull) {
        char *buf = (char *)malloc((size_t)size);
        if (buf != NULL) {
            int wrc = __llvm_profile_write_buffer(buf);
            NSLog(@"[LLVM_PGO] write_buffer => %d", wrc);
            if (wrc == 0) {
                const char *path = getenv("LLVM_PROFILE_FILE");
                if (path != NULL) {
                    FILE *fp = fopen(path, "wb");
                    if (fp != NULL) {
                        size_t n = fwrite(buf, 1, (size_t)size, fp);
                        fclose(fp);
                        NSLog(@"[LLVM_PGO] fwrite => %zu/%llu path=%s", n, (unsigned long long)size, path);
                        free(buf);
                        return;
                    }
                    NSLog(@"[LLVM_PGO] fopen failed errno=%d (%s) path=%s", errno, strerror(errno), path);
                }
            }
            free(buf);
        }
    }
}
