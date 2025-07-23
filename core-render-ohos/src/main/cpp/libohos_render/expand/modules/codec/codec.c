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

#include "codec.h"

#include <stddef.h>
#include <malloc.h>
#include <strings.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <stdio.h>

#include "md5.h"
#include "sha256.h"

static void binary_to_hex(const unsigned char *buffer, size_t buffer_size, char *hex_string) {
    static const char hex_chars[] = "0123456789ABCDEF";

    for (size_t i = 0; i < buffer_size; i++) {
        unsigned char byte = buffer[i];
        hex_string[i * 2] = hex_chars[byte >> 4];
        hex_string[i * 2 + 1] = hex_chars[byte & 0x0F];
    }
}

static void read_str(napi_env env, napi_value value, char **out_buf, size_t *out_len) {
    napi_valuetype value_type;
    napi_typeof(env, value, &value_type);

    if (value_type == napi_string) {
        napi_status status;
        size_t capacity = 2;
        static size_t max_capacity = 10 * 1024 * 1024; // 10 M
        size_t len = 0;
        char *buf = NULL;

        do {
            if (buf) {
                free(buf);
            }
            capacity = capacity * 2;
            buf = (char *)malloc(capacity);
            status = napi_get_value_string_utf8(env, value, buf, capacity, &len);
        } while (len + 1 >= capacity && // napi_get_value_string_utf8 use only `capacity - 1` bytes
                 capacity < max_capacity);
        *out_buf = buf;
        *out_len = len;
    }
}

napi_value kuikly_sha256(napi_env env, napi_callback_info info) {
    size_t requireArgc = 1;
    size_t argc = 1;
    napi_value args[1] = {NULL};
    napi_value sha256hash;

    napi_get_cb_info(env, info, &argc, args, NULL, NULL);

    char *buf = NULL;
    size_t len = 0;
    read_str(env, args[0], &buf, &len);

    if (buf) {
        SHA256_CTX ctx;
        char sha256str[SHA256_DIGEST_SIZE * 2];
        SHA256_init(&ctx);
        SHA256_update(&ctx, buf, len);
        const unsigned char *p = SHA256_final(&ctx);

        binary_to_hex(p, SHA256_DIGEST_SIZE, sha256str);
        napi_create_string_latin1(env, sha256str, sizeof(sha256str), &sha256hash);
        if (buf) {
            free(buf);
        }
        return sha256hash;
    }
    
    napi_create_string_latin1(env, "", 0, &sha256hash);
    return sha256hash;
}

napi_value kuikly_md5(napi_env env, napi_callback_info info) {
    size_t requireArgc = 1;
    size_t argc = 1;
    napi_value args[1] = {NULL};
    napi_value md5hash;

    napi_get_cb_info(env, info, &argc, args, NULL, NULL);

    char *buf = NULL;
    size_t len = 0;
    read_str(env, args[0], &buf, &len);
    if (buf) {
        unsigned char md5buf[16];
        char md5str[32];
        MD5_CTX ctx;
        MD5_Init(&ctx);
        MD5_Update(&ctx, buf, len);
        MD5_Final(&md5buf[0], &ctx);
        binary_to_hex(md5buf, sizeof(md5buf), md5str);

        napi_create_string_latin1(env, md5str, sizeof(md5str), &md5hash);
        if (buf) {
            free(buf);
        }
        return md5hash;
    }
    
    napi_create_string_latin1(env, "", 0, &md5hash);
    return md5hash;
}

