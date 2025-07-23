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

package com.tencent.tdf.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class TDFParamsUtils {

   /**
    * 参数列表数据解析，从参数列表中获取指定位置、类型的值，如果获取不到则抛出异常
    * @param params 参数列表
    * @param paramType 参数类型
    * @param index 参数位置
    * @return 参数值
    */
   public static Object parseParams(List<Object> params, Type paramType, int index) {
      if (paramType == String.class) {
         return TDFListUtils.getString(params, index);
      }

      if (paramType == int.class || paramType == Integer.class) {
         return TDFListUtils.getInt(params, index);
      }

      if (paramType == long.class || paramType == Long.class) {
         return TDFListUtils.getLong(params, index);
      }

      if (paramType == double.class || paramType == Double.class) {
         return TDFListUtils.getDouble(params, index);
      }

      if (paramType == boolean.class || paramType == Boolean.class) {
         return TDFListUtils.getBoolean(params, index);
      }

      if (paramType == float.class || paramType == Float.class) {
         return TDFListUtils.getFloat(params, index);
      }

      if (isListType(paramType)) {
         return TDFListUtils.getArray(params, index);
      }

      if (isMapType(paramType)) {
         return TDFListUtils.getMap(params, index);
      }

      throw new IllegalArgumentException("parseArgument exception, index: " + index + " paramType: " + paramType);
   }

   private static boolean isListType(Type type) {
      if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) type;
         return parameterizedType.getRawType() == List.class;
      }
      return false;
   }

   private static boolean isMapType(Type type) {
      if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) type;
         return parameterizedType.getRawType() == Map.class;
      }
      return false;
   }

}
