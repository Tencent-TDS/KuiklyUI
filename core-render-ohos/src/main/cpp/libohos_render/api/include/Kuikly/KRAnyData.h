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

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 向C暴露的数据对象handle。暂时支持String和Int，后续业务有需求后再提供其他类型数据的获取。
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
 * 返回字符串内容
 * @param data
 * @return 字符串指针，仅当前scope有效，请勿转移指针，如有需要请拷贝字符串内容。
 */
const char *KRAnyDataGetString(KRAnyData data);

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRANYDATA_H
