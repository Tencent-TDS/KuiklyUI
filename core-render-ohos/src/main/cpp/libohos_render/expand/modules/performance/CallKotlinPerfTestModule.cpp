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

#include "libohos_render/expand/modules/performance/CallKotlinPerfTestModule.h"

// Test-only implementation: empty TU when NDEBUG is set (Release / RelWithDebInfo).
#ifndef NDEBUG

#include <chrono>
#include <sstream>
#include <string>
#include <vector>

#include "libohos_render/context/IKRRenderNativeContextHandler.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/type/KRRenderCValue.h"
#include "libohos_render/foundation/type/KRLazyCJsonBridge.h"
#include "libohos_render/utils/KRRenderLoger.h"

extern CallKotlin callKotlin_;

namespace kuikly {
namespace module {

const char CallKotlinPerfTestModule::MODULE_NAME[] = "CallKotlinPerfTestModule";

namespace {

constexpr char kMethodBench[] = "bench";
constexpr char kMethodBenchPhases[] = "benchPhases";
constexpr char kMethodBenchJson[] = "benchJson";
constexpr char kMethodExportNestedOwner[] = "exportNestedOwner";
constexpr int kDefaultIterations = 3000;
constexpr int kWarmup = 100;

int64_t NowNs() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::steady_clock::now().time_since_epoch())
        .count();
}

double Per(int64_t total, int n) {
    return n > 0 ? static_cast<double>(total) / static_cast<double>(n) : 0.0;
}

int ParseIntField(const std::string &s, const char *key, int fallback) {
    auto pos = s.find(key);
    if (pos == std::string::npos) {
        return fallback;
    }
    auto colon = s.find(':', pos);
    if (colon == std::string::npos) {
        return fallback;
    }
    try {
        return std::stoi(s.substr(colon + 1));
    } catch (...) {
        return fallback;
    }
}

int ParseIterations(const KRAnyValue &params, int fallback = kDefaultIterations) {
    if (!params) {
        return fallback;
    }
    auto s = params->toString();
    if (s.empty()) {
        return fallback;
    }
    return std::max(1, ParseIntField(s, "iterations", fallback));
}

void CallKotlinOnce(int methodId, const KRRenderCValue &a0, const KRRenderCValue &a1, const KRRenderCValue &a2,
                    const KRRenderCValue &a3, const KRRenderCValue &a4, const KRRenderCValue &a5) {
    callKotlin_(methodId, a0, a1, a2, a3, a4, a5);
}

std::string MakeJsonPayload(size_t target_bytes) {
    // Produce a valid JSON object whose serialized size is ~target_bytes.
    // {"d":"<pad>"} overhead is 8 chars + pad.
    if (target_bytes <= 8) {
        return "{}";
    }
    size_t pad = target_bytes - 8;
    std::string out;
    out.reserve(target_bytes + 16);
    out.append("{\"d\":\"");
    out.append(pad, 'x');
    out.append("\"}");
    return out;
}

/** Same logical payload as MakeJsonPayload, but as a structured Map (lazy cJSON). */
KRAnyValue MakeMapPayload(size_t target_bytes) {
    KRRenderValue::Map m;
    if (target_bytes <= 8) {
        return KRRenderValue::Make(m);
    }
    size_t pad = target_bytes - 8;
    m["d"] = KRRenderValue::Make(std::string(pad, 'y'));
    return KRRenderValue::Make(m);
}

/** Nested map for optJSONObject lifecycle tests. */
KRAnyValue MakeNestedMapPayload() {
    KRRenderValue::Map grand;
    grand["y"] = KRRenderValue::Make(42);
    KRRenderValue::Map child;
    child["x"] = KRRenderValue::Make(std::string("hello"));
    child["grand"] = KRRenderValue::Make(grand);
    KRRenderValue::Map root;
    root["a"] = KRRenderValue::Make(1);
    root["child"] = KRRenderValue::Make(child);
    root["blob"] = KRRenderValue::Make(std::string(4096, 'z'));
    return KRRenderValue::Make(root);
}

/**
 * Build nested Map → NATIVE_JSON, retain a handle for Kotlin, then drop the
 * KRRenderValue (create handle). Returned owner is a transferred shared_ptr copy.
 */
std::string ExportNestedOwner() {
    auto map = MakeNestedMapPayload();
    const auto &cv = map->toCValue();
    if (cv.type != KRRenderCValue::Type::NATIVE_JSON || cv.value.longValue == 0) {
        return "{\"error\":\"expected NATIVE_JSON\"}";
    }
    int64_t transferred = kuikly_cjson_retain(cv.value.longValue);
    if (transferred == 0) {
        return "{\"error\":\"retain failed\"}";
    }
    // map goes out of scope → releases create handle; [transferred] keeps tree alive.
    std::ostringstream oss;
    oss << "{\"owner\":" << transferred << "}";
    return oss.str();
}

std::string BenchBasic(const std::string &instance_id, int iterations) {
    const auto null_value = KRRenderValue::MakeNull();
    const auto &null_c = null_value->toCValue();

    {
        auto warmup_id = KRRenderValue::Make(instance_id);
        for (int i = 0; i < kWarmup; i++) {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView),
                           warmup_id->toCValue(), null_c, null_c, null_c, null_c, null_c);
        }
    }

    int64_t make_ns = 0, tocvalue_ns = 0, layout_full_ns = 0, layout_cached_ns = 0, fire_ns = 0;

    {
        int64_t t0 = NowNs();
        for (int i = 0; i < iterations; i++) {
            volatile auto v = KRRenderValue::Make(instance_id);
            (void)v;
        }
        make_ns = NowNs() - t0;
    }
    {
        auto arg0 = KRRenderValue::Make(instance_id);
        (void)arg0->toCValue();
        (void)null_value->toCValue();
        int64_t t0 = NowNs();
        for (int i = 0; i < iterations; i++) {
            volatile auto c0 = arg0->toCValue();
            volatile auto c1 = null_value->toCValue();
            (void)c0;
            (void)c1;
        }
        tocvalue_ns = NowNs() - t0;
    }
    {
        int64_t t0 = NowNs();
        for (int i = 0; i < iterations; i++) {
            auto arg0 = KRRenderValue::Make(instance_id);
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView),
                           arg0->toCValue(), null_c, null_c, null_c, null_c, null_c);
        }
        layout_full_ns = NowNs() - t0;
    }
    {
        auto arg0 = KRRenderValue::Make(instance_id);
        const auto &c0 = arg0->toCValue();
        int64_t t0 = NowNs();
        for (int i = 0; i < iterations; i++) {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView), c0, null_c,
                           null_c, null_c, null_c, null_c);
        }
        layout_cached_ns = NowNs() - t0;
    }
    {
        auto arg0 = KRRenderValue::Make(instance_id);
        auto arg1 = KRRenderValue::Make(1);
        auto arg2 = KRRenderValue::Make("click");
        auto arg3 = KRRenderValue::Make("{\"x\":1,\"y\":2}");
        const auto &c0 = arg0->toCValue();
        const auto &c1 = arg1->toCValue();
        const auto &c2 = arg2->toCValue();
        const auto &c3 = arg3->toCValue();
        int64_t t0 = NowNs();
        for (int i = 0; i < iterations; i++) {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), c0, c1,
                           c2, c3, null_c, null_c);
        }
        fire_ns = NowNs() - t0;
    }

    std::ostringstream oss;
    oss.setf(std::ios::fixed);
    oss.precision(2);
    oss << "{"
        << "\"iterations\":" << iterations << ","
        << "\"makeInstanceId_ns_per\":" << Per(make_ns, iterations) << ","
        << "\"toCValue_ns_per\":" << Per(tocvalue_ns, iterations) << ","
        << "\"layout_full_ns_per\":" << Per(layout_full_ns, iterations) << ","
        << "\"layout_cachedArg0_ns_per\":" << Per(layout_cached_ns, iterations) << ","
        << "\"fireEvent_ns_per\":" << Per(fire_ns, iterations)
        << "}";
    return oss.str();
}

std::string BenchPhases(const std::string &instance_id, int iterations, size_t json_bytes, const std::string &mode) {
    const auto null_value = KRRenderValue::MakeNull();
    const auto &null_c = null_value->toCValue();
    const bool use_map =
        (mode == "map" || mode == "update_map" || mode == "callback_map");
    const bool is_update = (mode == "update" || mode == "update_map");
    const bool is_callback = (mode == "callback" || mode == "callback_map");
    const bool is_fire = (mode == "fire" || mode == "map" || mode == "both");
    auto payload_str = MakeJsonPayload(json_bytes);

    auto arg0 = KRRenderValue::Make(instance_id);
    auto arg1_tag = KRRenderValue::Make(1);
    auto arg2_event = KRRenderValue::Make(is_update ? "perf_update" : "click");
    auto arg1_cb = KRRenderValue::Make(std::string("0"));
    auto arg3 = use_map ? MakeMapPayload(json_bytes) : KRRenderValue::Make(payload_str);
    const auto &c0 = arg0->toCValue();
    const auto &c1_tag = arg1_tag->toCValue();
    const auto &c2_event = arg2_event->toCValue();
    const auto &c1_cb = arg1_cb->toCValue();
    const auto &c3 = arg3->toCValue();

    auto call_payload_once = [&]() {
        if (is_update) {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodUpdateInstance), c0,
                           c2_event, c3, null_c, null_c, null_c);
        } else if (is_callback) {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireCallback), c0,
                           c1_cb, c3, null_c, null_c, null_c);
        } else {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), c0,
                           c1_tag, c2_event, c3, null_c, null_c);
        }
    };

    for (int i = 0; i < 10; i++) {
        if (mode == "layout" || mode == "both") {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView), c0, null_c,
                           null_c, null_c, null_c, null_c);
        }
        if (is_fire || is_update || is_callback) {
            call_payload_once();
        }
    }

    int64_t tocvalue_ns = 0, call_ns = 0;
    int64_t cold_make_ns = 0, cold_tocvalue_ns = 0;

    if (mode == "layout" || mode == "both") {
        for (int i = 0; i < iterations; i++) {
            int64_t t0 = NowNs();
            const auto &a0 = arg0->toCValue();
            const auto &a1 = null_value->toCValue();
            const auto &a2 = null_value->toCValue();
            const auto &a3 = null_value->toCValue();
            const auto &a4 = null_value->toCValue();
            const auto &a5 = null_value->toCValue();
            int64_t t1 = NowNs();
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView), a0, a1, a2,
                           a3, a4, a5);
            int64_t t2 = NowNs();
            tocvalue_ns += t1 - t0;
            call_ns += t2 - t1;
        }
    }

    if (is_fire || is_update || is_callback) {
        for (int i = 0; i < iterations; i++) {
            int64_t t0 = NowNs();
            const auto &a0 = arg0->toCValue();
            const auto &a1 = is_callback ? arg1_cb->toCValue() : (is_update ? arg2_event->toCValue() : arg1_tag->toCValue());
            const auto &a2 = is_callback ? arg3->toCValue() : (is_update ? arg3->toCValue() : arg2_event->toCValue());
            const auto &a3 = is_fire ? arg3->toCValue() : null_c;
            const auto &a4 = null_value->toCValue();
            const auto &a5 = null_value->toCValue();
            int64_t t1 = NowNs();
            if (is_update) {
                CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodUpdateInstance), a0,
                               a1, a2, null_c, null_c, null_c);
            } else if (is_callback) {
                CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireCallback), a0, a1,
                               a2, null_c, null_c, null_c);
            } else {
                CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), a0,
                               a1, a2, a3, a4, a5);
            }
            int64_t t2 = NowNs();
            tocvalue_ns += t1 - t0;
            call_ns += t2 - t1;
        }
    }

    {
        int64_t t0 = NowNs();
        auto fresh = use_map ? MakeMapPayload(json_bytes) : KRRenderValue::Make(payload_str);
        cold_make_ns = NowNs() - t0;
        t0 = NowNs();
        (void)fresh->toCValue();
        cold_tocvalue_ns = NowNs() - t0;
    }

    int calls = iterations;
    if (mode == "both") {
        calls = iterations * 2;
    }

    const char *method_name = "LayoutView";
    if (is_update) {
        method_name = "UpdateInstance";
    } else if (is_callback) {
        method_name = "FireCallback";
    } else if (is_fire) {
        method_name = "FireViewEvent";
    }

    std::ostringstream oss;
    oss.setf(std::ios::fixed);
    oss.precision(2);
    oss << "{"
        << "\"mode\":\"" << mode << "\","
        << "\"method\":\"" << method_name << "\","
        << "\"iterations\":" << iterations << ","
        << "\"calls\":" << calls << ","
        << "\"json_bytes\":" << payload_str.size() << ","
        << "\"payload\":\"" << (use_map ? "lazy_cjson" : "string") << "\","
        << "\"cpp_toCValue_ns_per\":" << Per(tocvalue_ns, calls) << ","
        << "\"cpp_callKotlin_ns_per\":" << Per(call_ns, calls) << ","
        << "\"cpp_total_ns_per\":" << Per(tocvalue_ns + call_ns, calls) << ","
        << "\"cold_make_json_ns\":" << cold_make_ns << ","
        << "\"cold_toCValue_json_ns\":" << cold_tocvalue_ns
        << "}";
    return oss.str();
}

/**
 * Size sweep: legacy JSON string vs structured Map bridge for FIRE_VIEW_EVENT.
 */
std::string BenchJson(const std::string &instance_id, int iterations_per_size) {
    // Includes large sizes requested for Map-bridge vs string comparison:
    // 128KB, 256KB, 496KB, 3MB
    static const size_t kSizes[] = {0, 64, 256, 1024, 4096, 16384, 65536, 131072, 262144, 507904, 3145728};
    const auto null_value = KRRenderValue::MakeNull();
    const auto &null_c = null_value->toCValue();
    auto arg0 = KRRenderValue::Make(instance_id);
    auto arg1 = KRRenderValue::Make(1);
    auto arg2 = KRRenderValue::Make("click");
    const auto &c0 = arg0->toCValue();
    const auto &c1 = arg1->toCValue();
    const auto &c2 = arg2->toCValue();

    std::ostringstream oss;
    oss.setf(std::ios::fixed);
    oss.precision(2);
    oss << "{\"iterations_per_size\":" << iterations_per_size << ",\"sizes\":[";

    bool first = true;
    for (size_t target : kSizes) {
        auto payload = MakeJsonPayload(target);
        const size_t actual = payload.size();

        // Cold: new KRRenderValue + first toCValue (string copy into cache)
        int64_t cold_make = 0, cold_toc = 0;
        {
            int64_t t0 = NowNs();
            auto v = KRRenderValue::Make(payload);
            cold_make = NowNs() - t0;
            t0 = NowNs();
            (void)v->toCValue();
            cold_toc = NowNs() - t0;
        }

        // Hot toCValue only
        auto hot = KRRenderValue::Make(payload);
        (void)hot->toCValue();
        int64_t hot_toc = 0;
        {
            int64_t t0 = NowNs();
            for (int i = 0; i < iterations_per_size; i++) {
                volatile auto c = hot->toCValue();
                (void)c;
            }
            hot_toc = NowNs() - t0;
        }

        // Full FIRE_VIEW_EVENT with JSON string (legacy: toKString + JSONObject parse)
        const auto &c3 = hot->toCValue();
        for (int i = 0; i < std::min(20, kWarmup); i++) {
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), c0, c1,
                           c2, c3, null_c, null_c);
        }
        // Scale down iterations for huge payloads to keep wall time sane
        int iters = iterations_per_size;
        if (actual >= 3145728) {  // 3MB
            iters = std::max(5, iterations_per_size / 80);
        } else if (actual >= 507904) {  // ~496KB
            iters = std::max(10, iterations_per_size / 40);
        } else if (actual >= 131072) {  // 128KB
            iters = std::max(20, iterations_per_size / 20);
        } else if (actual >= 65536) {
            iters = std::max(50, iterations_per_size / 10);
        } else if (actual >= 16384) {
            iters = std::max(100, iterations_per_size / 3);
        }

        int64_t call_ns = 0, toc_ns = 0;
        for (int i = 0; i < iters; i++) {
            int64_t t0 = NowNs();
            const auto &a0 = arg0->toCValue();
            const auto &a1 = arg1->toCValue();
            const auto &a2 = arg2->toCValue();
            const auto &a3 = hot->toCValue();
            int64_t t1 = NowNs();
            CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), a0, a1,
                           a2, a3, null_c, null_c);
            int64_t t2 = NowNs();
            toc_ns += t1 - t0;
            call_ns += t2 - t1;
        }

        // Map bridge path: lazy NATIVE_JSON (cJSON*), Kotlin CJsonJSONObject.fromOwner
        int64_t map_cold_toc = 0, map_call_ns = 0, map_toc_ns = 0;
        {
            auto map_v = MakeMapPayload(target);
            int64_t t0 = NowNs();
            (void)map_v->toCValue();  // ToNativeJsonLocked → NATIVE_JSON
            map_cold_toc = NowNs() - t0;
            const auto &mc3 = map_v->toCValue();
            for (int i = 0; i < std::min(20, kWarmup); i++) {
                CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), c0,
                               c1, c2, mc3, null_c, null_c);
            }
            for (int i = 0; i < iters; i++) {
                t0 = NowNs();
                const auto &a0 = arg0->toCValue();
                const auto &a1 = arg1->toCValue();
                const auto &a2 = arg2->toCValue();
                const auto &a3 = map_v->toCValue();
                int64_t t1 = NowNs();
                CallKotlinOnce(static_cast<int>(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent), a0,
                               a1, a2, a3, null_c, null_c);
                int64_t t2 = NowNs();
                map_toc_ns += t1 - t0;
                map_call_ns += t2 - t1;
            }
        }

        if (!first) {
            oss << ',';
        }
        first = false;
        oss << "{"
            << "\"target_bytes\":" << target << ","
            << "\"actual_bytes\":" << actual << ","
            << "\"cold_make_ns\":" << cold_make << ","
            << "\"cold_toCValue_ns\":" << cold_toc << ","
            << "\"hot_toCValue_ns_per\":" << Per(hot_toc, iterations_per_size) << ","
            << "\"fire_cpp_toCValue_ns_per\":" << Per(toc_ns, iters) << ","
            << "\"fire_cpp_callKotlin_ns_per\":" << Per(call_ns, iters) << ","
            << "\"fire_total_ns_per\":" << Per(toc_ns + call_ns, iters) << ","
            << "\"map_cold_toCValue_ns\":" << map_cold_toc << ","
            << "\"map_fire_cpp_toCValue_ns_per\":" << Per(map_toc_ns, iters) << ","
            << "\"map_fire_cpp_callKotlin_ns_per\":" << Per(map_call_ns, iters) << ","
            << "\"map_fire_total_ns_per\":" << Per(map_toc_ns + map_call_ns, iters) << ","
            << "\"fire_iters\":" << iters
            << "}";
    }
    oss << "]}";
    return oss.str();
}

}  // namespace

KRAnyValue CallKotlinPerfTestModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                              const KRRenderCallback &callback) {
    if (callKotlin_ == nullptr) {
        KR_LOG_ERROR << "[CallKotlinPerf] callKotlin_ is null";
        return KRRenderValue::Make("{\"error\":\"callKotlin_ null\"}");
    }
    const auto &instance_id = GetInstanceId();
    std::string json;
    if (method == kMethodBench) {
        json = BenchBasic(instance_id, ParseIterations(params, 5000));
    } else if (method == kMethodBenchPhases) {
        int iterations = ParseIterations(params, 2000);
        int json_bytes = 1024;
        std::string mode = "fire";
        if (params) {
            auto s = params->toString();
            json_bytes = ParseIntField(s, "jsonBytes", 1024);
            auto mpos = s.find("mode");
            if (mpos != std::string::npos) {
                auto q1 = s.find('"', s.find(':', mpos));
                auto q2 = s.find('"', q1 + 1);
                if (q1 != std::string::npos && q2 != std::string::npos && q2 > q1) {
                    mode = s.substr(q1 + 1, q2 - q1 - 1);
                }
            }
        }
        if (mode != "layout" && mode != "fire" && mode != "both" && mode != "map" && mode != "update" &&
            mode != "update_map" && mode != "callback" && mode != "callback_map") {
            mode = "fire";
        }
        json = BenchPhases(instance_id, iterations, static_cast<size_t>(std::max(0, json_bytes)), mode);
    } else if (method == kMethodBenchJson) {
        json = BenchJson(instance_id, ParseIterations(params, 500));
    } else if (method == kMethodExportNestedOwner) {
        json = ExportNestedOwner();
    } else {
        return KREmptyValue();
    }
    KR_LOG_INFO << "[CallKotlinPerf] " << method << " " << json;
    return KRRenderValue::Make(json);
}

}  // namespace module
}  // namespace kuikly

#endif  // !NDEBUG — CallKotlinPerfTestModule (debug/test only)
