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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TDFMapUtils {

    public static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static float getFloat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : 0;
    }

    public static double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    public static int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null && (boolean) value;
    }

    public static long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value instanceof Map) {
                return (Map) value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Object> getArray(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List) value;
        }
        return null;
    }

    public static Map<String, Object> fromJsonStr(String jStr) {
        Map<String, Object> map = new HashMap<>();
        try {
            if (jStr != null) {
                JSONObject jObj = new JSONObject(jStr);
                map = fromJsonObject(jObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * JSONObject 转 Map
     */
    public static Map<String, Object> fromJsonObject(JSONObject jObject) {
        Map<String, Object> map = new HashMap<>();
        if (jObject == null) {
            return map;
        }

        try {
            Iterator<?> it = jObject.keys();
            while (it.hasNext()) {
                String key = it.next().toString();
                Object obj = jObject.opt(key);
                if (jObject.isNull(key)) {
                    map.put(key, null);
                } else if (obj instanceof JSONObject) {
                    map.put(key, TDFMapUtils.fromJsonObject((JSONObject) obj));
                } else if (obj instanceof JSONArray) {
                    map.put(key, TDFListUtils.fromJsonArray((JSONArray)obj));
                } else {
                    map.put(key, obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Map 转 JSONObject
     */
    public static JSONObject toJSONObject(Map<String, Object> map) {
        JSONObject jObject = new JSONObject();
        if (map.size() == 0) {
            return jObject;
        }

        Iterator<?> iterator = map.entrySet().iterator();
        try {
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry) iterator.next();
                String key = entry.getKey();
                if (entry.getValue() instanceof Map) {
                    JSONObject jObjectMap = TDFMapUtils.toJSONObject((Map) entry.getValue());
                    jObject.put(key, jObjectMap);
                } else if (entry.getValue() instanceof List) {
                    JSONArray jObjectArray = TDFListUtils.toJSONArray((List) entry.getValue());
                    jObject.put(key, jObjectArray);
                } else {
                    jObject.put(key, entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jObject;
    }


}
