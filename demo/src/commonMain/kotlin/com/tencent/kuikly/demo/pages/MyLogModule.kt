package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class MyLogModule : Module() {
    override fun moduleName(): String {
        return "KRMyLogModule"
    }

    fun test() : String {
        // 鸿蒙侧 CallModuleMethod 返回值传输规则：
        // 基本数据类型 -> 基本数据类型(Bool -> 0 or 1) ; Array、Map、Record -> JSON字符串 ; ByteArray -> ByteArray
        // 当前测试Module返回值为 Map ( Module 在Arkts层）

        val jsonData = syncToNativeMethod(
            methodName = "test",
            arrayOf(1, 2, 3),
            null
        ).toString()
        val receivedData = JSONObject(jsonData).toMap()

        return buildString {
            appendLine("=== MyLogModule 数据验证 ===")
            if (receivedData == null) {
                appendLine("【错误】: 返回数据为null")
                return@buildString
            }
            
            // 基础类型验证
            val stringMatch = receivedData["string"] == "中文测试🎉"
            val intMatch = (receivedData["int"] as? Number)?.toInt() == 100
            val floatMatch = (receivedData["float"] as? Number)?.toDouble() == 3.14159
            val negativeMatch = (receivedData["negative"] as? Number)?.toInt() == -50
            val boolTrueMatch = receivedData["boolTrue"] == true
            val boolFalseMatch = receivedData["boolFalse"] == false
            val zeroMatch = (receivedData["zero"] as? Number)?.toInt() == 0
            val largeNumMatch = (receivedData["largeNum"] as? Number)?.toLong() == 9999999999L
            val emptyStrMatch = receivedData["emptyStr"] == ""
            
            appendLine("【string】: $stringMatch (${receivedData["string"]})")
            appendLine("【int】: $intMatch (${receivedData["int"]})")
            appendLine("【float】: $floatMatch (${receivedData["float"]})")
            appendLine("【negative】: $negativeMatch (${receivedData["negative"]})")
            appendLine("【boolTrue】: $boolTrueMatch (${receivedData["boolTrue"]})")
            appendLine("【boolFalse】: $boolFalseMatch (${receivedData["boolFalse"]})")
            appendLine("【zero】: $zeroMatch (${receivedData["zero"]})")
            appendLine("【largeNum】: $largeNumMatch (${receivedData["largeNum"]})")
            appendLine("【emptyStr】: $emptyStrMatch (${receivedData["emptyStr"]})")
            
            // 嵌套对象验证（2层嵌套）
            val nested = receivedData["nested"] as? Map<String, Any?>
            val nestedKey1Match = nested?.get("key1") == "value1"
            val nestedKey2Match = nested?.get("key2") == "value2"
            
            appendLine("【nested.key1】: $nestedKey1Match (${nested?.get("key1")})")
            appendLine("【nested.key2】: $nestedKey2Match (${nested?.get("key2")})")
            
            // 数组验证
            val intArray = receivedData["intArray"] as? List<*>
            val intArrayMatch = intArray?.size == 3 && 
                (intArray[0] as? Number)?.toInt() == 1 &&
                (intArray[1] as? Number)?.toInt() == 2 &&
                (intArray[2] as? Number)?.toInt() == 3
            
            val strArray = receivedData["strArray"] as? List<*>
            val strArrayMatch = strArray?.size == 3 && 
                strArray[0] == "a" && strArray[1] == "b" && strArray[2] == "c"
            
            val mixedArray = receivedData["mixedArray"] as? List<*>
            val mixedArrayMatch = mixedArray?.size == 4 &&
                (mixedArray[0] as? Number)?.toInt() == 1 &&
                mixedArray[1] == "str" &&
                mixedArray[2] == true &&
                (mixedArray[3] as? Map<String, Any?>)?.get("innerKey") == "innerValue"
            
            val emptyArr = receivedData["emptyArr"] as? List<*>
            val emptyArrMatch = emptyArr?.isEmpty() == true
            
            appendLine("【intArray】: $intArrayMatch ($intArray)")
            appendLine("【strArray】: $strArrayMatch ($strArray)")
            appendLine("【mixedArray】: $mixedArrayMatch ($mixedArray)")
            appendLine("【emptyArr】: $emptyArrMatch ($emptyArr)")
            
            // 空对象验证
            val emptyObj = receivedData["emptyObj"] as? Map<String, Any?>
            val emptyObjMatch = emptyObj?.isEmpty() == true
            appendLine("【emptyObj】: $emptyObjMatch ($emptyObj)")
            
            // 总结
            val allMatch = stringMatch && intMatch && floatMatch && negativeMatch &&
                boolTrueMatch && boolFalseMatch && zeroMatch && largeNumMatch && emptyStrMatch &&
                nestedKey1Match && nestedKey2Match &&
                intArrayMatch && strArrayMatch && mixedArrayMatch && emptyArrMatch && emptyObjMatch
            appendLine("=== 全部验证通过: $allMatch ===")
        }
    }
}
