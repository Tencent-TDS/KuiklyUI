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
export class KRRect {
  /** 矩形左上角的 x 坐标 */
  x: number;
  /** 矩形左上角的 y 坐标 */
  y: number;
  /** 矩形的宽度 */
  width: number;
  /** 矩形的高度 */
  height: number;
  static readonly zero: Readonly<KRRect> = new KRRect(0, 0, 0, 0);

  /**
   * 创建一个新的 KRRect 实例。
   * @param x 矩形左上角的 x 坐标
   * @param y 矩形左上角的 y 坐标
   * @param width 矩形的宽度
   * @param height 矩形的高度
   */
  constructor(x: number, y: number, width: number, height: number) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  equals(other: KRRect) {
    return this.x === other.x && this.y === other.y && this.width === other.width && this.height === other.height;
  }
}