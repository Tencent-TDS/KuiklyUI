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

/**
 * KRSize 类表示一个二维尺寸，包括宽度和高度。
 */
export class KRSize {
  /** 尺寸的宽度 */
  width: number;
  /** 尺寸的高度 */
  height: number;

  /**
   * 创建一个新的 KRSize 实例。
   * @param width 尺寸的宽度
   * @param height 尺寸的高度
   */
  constructor(width: number, height: number) {
    this.width = width;
    this.height = height;
  }
}