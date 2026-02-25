//
// Created on 2026/1/6.
//
// Node APIs are not fully supported. To solve the compilation error of the interface cannot be found,
// please include "napi/native_api.h".

#include "libohos_render/expand/modules/log/KRLogTestModule.h"
#include "libohos_render/foundation/KRCommon.h"

namespace kuikly {
namespace module {

const char KRLogTestModule::MODULE_NAME[] = "KRLogTestModule";

KRAnyValue KRLogTestModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) {
    if (method == "test") {
        return test(params);
    }
    return nullptr;
}

KRAnyValue KRLogTestModule::test(const KRAnyValue &params) {
    // 深层嵌套 Object
    KRRenderValue::Map level3;
    level3["level3"] = KRRenderValue::Make("深层嵌套");
    
    KRRenderValue::Map deep;
    deep["key1"] = KRRenderValue::Make("value1");
    deep["key2"] = KRRenderValue::Make("value2");
    deep["deep"] = KRRenderValue::Make(level3);
    
    // 数组
    KRRenderValue::Array intArray;
    intArray.push_back(KRRenderValue::Make(1));
    intArray.push_back(KRRenderValue::Make(2));
    intArray.push_back(KRRenderValue::Make(3));
    
    KRRenderValue::Array strArray;
    strArray.push_back(KRRenderValue::Make("a"));
    strArray.push_back(KRRenderValue::Make("b"));
    strArray.push_back(KRRenderValue::Make("c"));
    
    // 混合数组
    KRRenderValue::Map innerObj;
    innerObj["innerKey"] = KRRenderValue::Make("innerValue");
    
    KRRenderValue::Array mixedArray;
    mixedArray.push_back(KRRenderValue::Make(1));
    mixedArray.push_back(KRRenderValue::Make("str"));
    mixedArray.push_back(KRRenderValue::Make(true));
    mixedArray.push_back(KRRenderValue::Make(innerObj));
    
    // 空对象和空数组
    KRRenderValue::Map emptyObj;
    KRRenderValue::Array emptyArr;
    
    // 主结果
    KRRenderValue::Map result;
    result["nested"] = KRRenderValue::Make(deep);
    result["string"] = KRRenderValue::Make("中文测试🎉");
    result["int"] = KRRenderValue::Make(100);
    result["float"] = KRRenderValue::Make(3.14159);
    result["negative"] = KRRenderValue::Make(-50);
    result["boolTrue"] = KRRenderValue::Make(true);
    result["boolFalse"] = KRRenderValue::Make(false);
    result["intArray"] = KRRenderValue::Make(intArray);
    result["strArray"] = KRRenderValue::Make(strArray);
    result["mixedArray"] = KRRenderValue::Make(mixedArray);
    result["emptyObj"] = KRRenderValue::Make(emptyObj);
    result["emptyArr"] = KRRenderValue::Make(emptyArr);
    result["emptyStr"] = KRRenderValue::Make("");
    result["zero"] = KRRenderValue::Make(0);
    result["largeNum"] = KRRenderValue::Make(static_cast<int64_t>(9999999999LL));
    
    return KRRenderValue::Make(result);
}

}  // namespace module
}  // namespace kuikly
