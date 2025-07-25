/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRANYDATA_H
#define CORE_RENDER_OHOS_KRANYDATA_H

#include "stdint.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 向C暴露的数据对象handle。
 */
typedef void *KRAnyData;

/**
 * 检测是否是一个字符串
 * @param data 输入的对象
 */
bool KRAnyDataIsString(KRAnyData data);

/**
 * 检测是否是一个Int
 * @param data 输入的对象
 * @return
 */
bool KRAnyDataIsInt(KRAnyData data);

/**
 * 检测是否是一个Long
 * @param data 输入的对象
 */
bool KRAnyDataIsLong(KRAnyData data);

/**
 * 检测是否是一个Float
 * @param data 输入的对象
 */
bool KRAnyDataIsFloat(KRAnyData data);

/**
 * 检测是否是一个Bool
 * @param data 输入的对象
 */
bool KRAnyDataIsBool(KRAnyData data);

/**
 * 检测是否是一个Bytes
 * @param data 输入的对象
 */
bool KRAnyDataIsBytes(KRAnyData data);

/**
 * 检测是否是一个Array
 * @param data 输入的对象
 */
bool KRAnyDataIsArray(KRAnyData data);

/**
 * 返回字符串内容
 * @param data
 * @return 字符串指针，仅当前scope有效，请勿转移指针，如有需要请拷贝字符串内容。
 */
const char *KRAnyDataGetString(KRAnyData data);

/**
 * @brief KRAnData错误码
 */
typedef enum {
    KRANYDATA_SUCCESS = 0,        // 成功
    KRANYDATA_NULL_INPUT = 1,     // 输入参数为 nullptr
    KRANYDATA_NULL_OUTPUT = 2,    // 接收参数为 nullptr
    KRANYDATA_OUT_OF_INDEX = 3,   // 索引越界
    KRANYDATA_TYPE_MISMATCH = 4,  // 类型不匹配
} KRAnDataErrorCode;

/**
 * @brief 从 KRAnyData 获取 int32_t 值
 * @param data 输入数据
 * @param value 输出参数，存储获取的整数值
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetInt(KRAnyData data, int32_t* value);

/**
 * @brief 从 KRAnyData 中提取 int64_t 值
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 输出参数指针，用于存储提取的长整数值（int64_t 类型）
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetLong(KRAnyData data, int64_t* value);

/**
 * @brief 从 KRAnyData 中提取 float 值
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 输出参数指针，用于存储提取的浮点值（float 类型）
 * @return KRAnDataErrorCode
 * @note 若数据为双精度浮点型（double）会自动进行精度转换
 */
int KRAnyDataGetFloat(KRAnyData data, float* value);

/**
 * @brief 从 KRAnyData 中提取 bool 值
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 输出参数指针，用于存储提取的布尔值（bool 类型）
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetBool(KRAnyData data, bool* value);

/**
 * @brief 从 KRAnyData 中提取 二进制 值
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 用于接收二进制数据的地址
 * @param size 用于接收二进制数据的长度
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetBytes(KRAnyData data, const char** value, int *size);

/**
 * @brief 从 KRAnyData 中提取 二进制 值
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 用于接收字符串数据地址
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetStr(KRAnyData data, const char** value);

/**
 * @brief 从 KRAnyData 中提取其中数组指定的元素
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 输出参数指针，存储提取的元素句柄，仅当前scope有效，如有需要请提取里面的内容。
 * @param index 元素索引（从0开始）
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetArrayElement(KRAnyData data, KRAnyData* value, int index);

/**
 * @brief 从 KRAnyData 中提取其中数组长度
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param size 输出参数，存储数组的长度
 * @return KRAnDataErrorCode
 */
int KRAnyDataGetArraySize(KRAnyData data, int* size);

/**
 * @brief 创建一个新的 KRAnyData 值为 null 类型
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreate();

/**
 * @brief 创建一个新的 KRAnyData 值为 int32_t 类型
 * @param value 设置的 int32_t 值
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateInt(int32_t value);

/**
 * @brief 创建一个新的 KRAnyData 值为 int64_t 类型
 * @param value 设置的 int64_t 值
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateLong(int64_t value);

/**
 * @brief 创建一个新的 KRAnyData 值为 float 类型
 * @param value 设置的 float 值
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateFloat(float value);

/**
 * @brief 创建一个新的 KRAnyData 值为 bool 类型
 * @param value 设置的 bool 值
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateBool(bool value);

/**
 * @brief 创建一个新的 KRAnyData 值为 char* 类型
 * @param value 设置的 字符串 值
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateString(const char* value);

/**
 * @brief 创建一个新的 KRAnyData 值为 char* 类型
 * @param value 设置的 二进制 值
 * @param size 二进制数据的长度
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateBytes(const char* value, int size);

/**
 * @brief 创建一个新的 KRAnyData 值为 Array 类型
 * @param size 设置的数组长度
 * @return KRAnyData
 */
KRAnyData KRAnyDataCreateArray(int size);

/**
 * @brief 设置 KRAnyData 数组中指定位置的元素值
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 要设置的 KRAnyData 元素
 * @param index 要设置元素的索引位置
 * @return KRAnDataErrorCode
 */
int KRAnyDataSetArrayElement(KRAnyData data, KRAnyData value, int index);

/**
 * @brief 给 KRAnyData 数组中添加元素
 * @param data 输入数据句柄，类型为 KRAnyData
 * @param value 要添加的 KRAnyData 元素
 * @return KRAnDataErrorCode
 */
int KRAnyDataAddArrayElement(KRAnyData data, KRAnyData value);

/**
 * @brief 销毁 KRAnyData 对象
 */
void KRAnyDataDestroy(KRAnyData data);

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRANYDATA_H
