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
    level3["level3"] = std::make_shared<KRRenderValue>("深层嵌套");
    
    KRRenderValue::Map deep;
    deep["key1"] = std::make_shared<KRRenderValue>("value1");
    deep["key2"] = std::make_shared<KRRenderValue>("value2");
    deep["deep"] = std::make_shared<KRRenderValue>(level3);
    
    // 数组
    KRRenderValue::Array intArray;
    intArray.push_back(std::make_shared<KRRenderValue>(1));
    intArray.push_back(std::make_shared<KRRenderValue>(2));
    intArray.push_back(std::make_shared<KRRenderValue>(3));
    
    KRRenderValue::Array strArray;
    strArray.push_back(std::make_shared<KRRenderValue>("a"));
    strArray.push_back(std::make_shared<KRRenderValue>("b"));
    strArray.push_back(std::make_shared<KRRenderValue>("c"));
    
    // 混合数组
    KRRenderValue::Map innerObj;
    innerObj["innerKey"] = std::make_shared<KRRenderValue>("innerValue");
    
    KRRenderValue::Array mixedArray;
    mixedArray.push_back(std::make_shared<KRRenderValue>(1));
    mixedArray.push_back(std::make_shared<KRRenderValue>("str"));
    mixedArray.push_back(std::make_shared<KRRenderValue>(true));
    mixedArray.push_back(std::make_shared<KRRenderValue>(innerObj));
    
    // 空对象和空数组
    KRRenderValue::Map emptyObj;
    KRRenderValue::Array emptyArr;
    
    // 主结果
    KRRenderValue::Map result;
    result["nested"] = std::make_shared<KRRenderValue>(deep);
    result["string"] = std::make_shared<KRRenderValue>("中文测试🎉");
    result["int"] = std::make_shared<KRRenderValue>(100);
    result["float"] = std::make_shared<KRRenderValue>(3.14159);
    result["negative"] = std::make_shared<KRRenderValue>(-50);
    result["boolTrue"] = std::make_shared<KRRenderValue>(true);
    result["boolFalse"] = std::make_shared<KRRenderValue>(false);
    result["intArray"] = std::make_shared<KRRenderValue>(intArray);
    result["strArray"] = std::make_shared<KRRenderValue>(strArray);
    result["mixedArray"] = std::make_shared<KRRenderValue>(mixedArray);
    result["emptyObj"] = std::make_shared<KRRenderValue>(emptyObj);
    result["emptyArr"] = std::make_shared<KRRenderValue>(emptyArr);
    result["emptyStr"] = std::make_shared<KRRenderValue>("");
    result["zero"] = std::make_shared<KRRenderValue>(0);
    result["largeNum"] = std::make_shared<KRRenderValue>(static_cast<int64_t>(9999999999LL));
    
    return std::make_shared<KRRenderValue>(result);
}

}  // namespace module
}  // namespace kuikly
