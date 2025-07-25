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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TDFListUtils {

   public static int getInt(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         return value instanceof Number ? ((Number) value).intValue() : 0;
      }
      return 0;
   }

   public static long getLong(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         return value instanceof Number ? ((Number) value).longValue() : 0;
      }
      return 0;
   }

   public static float getFloat(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         return value instanceof Number ? ((Number) value).floatValue() : 0;
      }
      return 0;
   }

   public static double getDouble(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         return value instanceof Number ? ((Number) value).doubleValue() : 0;
      }
      return 0;
   }

   public static String getString(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         if (value != null) {
            return String.valueOf(value);
         }
      }
      return null;
   }

   public static boolean getBoolean(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         return (value instanceof Boolean) && (boolean) value;
      }
      return false;
   }

   public static List<Object> getArray(List<Object> list, int index) {
      if (list.size() > index) {
         Object value = list.get(index);
         return value instanceof List ? (List) value : null;
      }
      return null;
   }

   public static Map<String, Object> getMap(List<Object> list, int index) {
      try {
         if (list.size() > index) {
            Object value = list.get(index);
            return value instanceof Map ? (Map) value : null;
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
   }

   public static Object getObject(List<Object> list, int index) {
      if (list.size() > index) {
         return list.get(index);
      }
      return null;
   }

   public static List<Object> fromJsonStr(String jStr) {
      List<Object> list = new ArrayList<>();
      try {
         if (jStr != null) {
            JSONArray jArray = new JSONArray(jStr);
            list = fromJsonArray(jArray);
         }
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return list;
   }

   /**
    * JSONArray 转 List
    */
   public static List<Object> fromJsonArray(JSONArray jArray) {
      List<Object> list = new ArrayList<>();
      if (jArray == null || jArray.length() == 0) {
         return list;
      }
      try {
         for (int i = 0; i < jArray.length(); i++) {
            Object obj = jArray.opt(i);
            if (obj == null) {
               list.add(null);
            } else if (obj instanceof JSONObject) {
               list.add(TDFMapUtils.fromJsonObject((JSONObject) obj));
            } else if (obj instanceof JSONArray) {
               list.add(TDFListUtils.fromJsonArray((JSONArray) obj));
            } else {
               list.add(obj);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return list;
   }

   /**
    * List 转 JSONArray
    */
   public static JSONArray toJSONArray(List<Object> list) {
      JSONArray jArray = new JSONArray();
      if (list.size() == 0) {
         return jArray;
      }

      try {
         Iterator<Object> it = list.iterator();
         Object value;
         while (it.hasNext()) {
            value = it.next();
            if (value instanceof Map) {
               JSONObject jObjectMap = TDFMapUtils.toJSONObject(((Map) value));
               jArray.put(jObjectMap);
            } else if (value instanceof List) {
               JSONArray jObjectArray = TDFListUtils.toJSONArray((List) value);
               jArray.put(jObjectArray);
            } else {
               jArray.put(value);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return jArray;
   }
   
}
