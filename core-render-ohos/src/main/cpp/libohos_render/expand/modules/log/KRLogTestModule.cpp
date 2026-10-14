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
    level3[u"level3"] = KRRenderValue::Make(u"深层嵌套");
    
    KRRenderValue::Map deep;
    deep[u"key1"] = KRRenderValue::Make(u"value1");
    deep[u"key2"] = KRRenderValue::Make(u"value2");
    deep[u"deep"] = KRRenderValue::Make(level3);
    
    // 数组
    KRRenderValue::Array intArray;
    intArray.push_back(KRRenderValue::Make(1));
    intArray.push_back(KRRenderValue::Make(2));
    intArray.push_back(KRRenderValue::Make(3));
    
    KRRenderValue::Array strArray;
    strArray.push_back(KRRenderValue::Make(u"a"));
    strArray.push_back(KRRenderValue::Make(u"b"));
    strArray.push_back(KRRenderValue::Make(u"c"));
    
    // 混合数组
    KRRenderValue::Map innerObj;
    innerObj[u"innerKey"] = KRRenderValue::Make(u"innerValue");
    
    KRRenderValue::Array mixedArray;
    mixedArray.push_back(KRRenderValue::Make(1));
    mixedArray.push_back(KRRenderValue::Make(u"str"));
    mixedArray.push_back(KRRenderValue::Make(true));
    mixedArray.push_back(KRRenderValue::Make(innerObj));
    
    // 空对象和空数组
    KRRenderValue::Map emptyObj;
    KRRenderValue::Array emptyArr;
    
    // 主结果
    KRRenderValue::Map result;
    result[u"nested"] = KRRenderValue::Make(deep);
    result[u"string"] = KRRenderValue::Make(u"中文测试🎉");
    result[u"int"] = KRRenderValue::Make(100);
    result[u"float"] = KRRenderValue::Make(3.14159);
    result[u"negative"] = KRRenderValue::Make(-50);
    result[u"boolTrue"] = KRRenderValue::Make(true);
    result[u"boolFalse"] = KRRenderValue::Make(false);
    result[u"intArray"] = KRRenderValue::Make(intArray);
    result[u"strArray"] = KRRenderValue::Make(strArray);
    result[u"mixedArray"] = KRRenderValue::Make(mixedArray);
    result[u"emptyObj"] = KRRenderValue::Make(emptyObj);
    result[u"emptyArr"] = KRRenderValue::Make(emptyArr);
    result[u"emptyStr"] = KRRenderValue::Make(u"");
    result[u"zero"] = KRRenderValue::Make(0);
    result[u"largeNum"] = KRRenderValue::Make(static_cast<int64_t>(9999999999LL));
    
    return KRRenderValue::Make(result);
}

}  // namespace module
}  // namespace kuikly
