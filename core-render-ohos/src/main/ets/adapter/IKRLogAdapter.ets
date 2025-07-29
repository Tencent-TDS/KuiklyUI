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

export interface IKRLogAdapter {
  /**
   * 打印 debug 级别日志
   * @param tag 日志 tag
   * @param msg 日志信息
   */
  d(tag: string, msg: string): void;

  /**
   * 打印 info 级别日志
   * @param tag 日志 tag
   * @param msg 日志信息
   */
  i(tag: string, msg: string): void;

  /**
   * 打印 error 级别日志
   * @param tag 日志 tag
   * @param msg 日志信息
   */
  e(tag: string, msg: string): void;
}