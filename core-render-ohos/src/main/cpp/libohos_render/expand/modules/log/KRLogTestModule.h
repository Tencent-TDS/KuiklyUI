//
// Created on 2026/1/6.
//
// Node APIs are not fully supported. To solve the compilation error of the interface cannot be found,
// please include "napi/native_api.h".

#ifndef OHOSAPP_KRLOGTESTMODULE_H
#define OHOSAPP_KRLOGTESTMODULE_H

#include "libohos_render/export/IKRRenderModuleExport.h"

namespace kuikly {
namespace module {

class KRLogTestModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];
    
    KRLogTestModule() = default;

    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;
    
 private:
    KRAnyValue test(const KRAnyValue &params);
};

}  // namespace module
}  // namespace kuikly

#endif  // OHOSAPP_KRLOGTESTMODULE_H
