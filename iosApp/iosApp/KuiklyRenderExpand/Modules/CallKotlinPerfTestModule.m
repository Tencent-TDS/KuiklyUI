/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#import "CallKotlinPerfTestModule.h"

#if DEBUG

#import <QuartzCore/QuartzCore.h>
#import "KuiklyRenderView.h"
#import "KuiklyRenderCore.h"
#import "KRConvertUtil.h"
#import "NSObject+KR.h"

// Private declarations — keep test-only APIs out of public headers.
@interface KuiklyRenderView (CallKotlinPerfPrivate)
- (void)perf_callWithMethod:(KuiklyRenderContextMethod)method args:(NSArray *)args;
- (NSString *)perf_instanceId;
@end

@interface KuiklyRenderCore (CallKotlinPerfPrivate)
- (void)perf_callWithMethod:(KuiklyRenderContextMethod)method args:(NSArray *)args;
- (NSString *)perf_instanceId;
@end

@implementation CallKotlinPerfTestModule

static int64_t KRPerfNowNs(void) {
    return (int64_t)(CACurrentMediaTime() * 1e9);
}

static double KRPerfPer(int64_t total, int n) {
    return n > 0 ? (double)total / (double)n : 0.0;
}

static NSDictionary *KRPerfParams(NSDictionary *args) {
    id param = args[KR_PARAM_KEY];
    if ([param isKindOfClass:[NSDictionary class]]) {
        return (NSDictionary *)param;
    }
    if ([param isKindOfClass:[NSString class]]) {
        return [(NSString *)param hr_stringToDictionary] ?: @{};
    }
    return @{};
}

static NSDictionary *KRPerfMakePayload(NSUInteger targetBytes) {
    if (targetBytes <= 8) {
        return @{};
    }
    NSUInteger pad = targetBytes - 8;
    char *buf = (char *)malloc(pad + 1);
    if (!buf) {
        return @{};
    }
    memset(buf, 'x', pad);
    buf[pad] = '\0';
    NSString *s = [[NSString alloc] initWithBytesNoCopy:buf length:pad encoding:NSUTF8StringEncoding freeWhenDone:YES];
    return @{@"d": s ?: @""};
}

/** Modes that force legacy JSON stringify before call. */
static BOOL KRPerfModeForceString(NSString *mode) {
    return [mode isEqualToString:@"fire"] ||
           [mode isEqualToString:@"update"] ||
           [mode isEqualToString:@"callback"];
}

/** Modes that pass NSDictionary through (lazy bridge). */
static BOOL KRPerfModeLazyDict(NSString *mode) {
    return [mode isEqualToString:@"map"] ||
           [mode isEqualToString:@"update_map"] ||
           [mode isEqualToString:@"callback_map"];
}

static BOOL KRPerfModeHasJsonPayload(NSString *mode) {
    return KRPerfModeForceString(mode) || KRPerfModeLazyDict(mode) || [mode isEqualToString:@"both"];
}

- (KuiklyRenderView *)perfRoot {
    if ([self.hr_rootView isKindOfClass:[KuiklyRenderView class]]) {
        return (KuiklyRenderView *)self.hr_rootView;
    }
    return nil;
}

- (void)perfInvoke:(KuiklyRenderView *)root
              mode:(NSString *)mode
        instanceId:(NSString *)instanceId
             arg:(id)arg {
    if ([mode isEqualToString:@"fire"] || [mode isEqualToString:@"map"] || [mode isEqualToString:@"both"]) {
        [root perf_callWithMethod:KuiklyRenderContextMethodFireViewEvent
                             args:@[instanceId, @1, @"perf", arg]];
    } else if ([mode isEqualToString:@"update"] || [mode isEqualToString:@"update_map"]) {
        // Same toBridgeJSONObject path as CreateInstance pageData (arg2).
        [root perf_callWithMethod:KuiklyRenderContextMethodUpdateInstance
                             args:@[instanceId, @"perf_update", arg]];
    } else if ([mode isEqualToString:@"callback"] || [mode isEqualToString:@"callback_map"]) {
        [root perf_callWithMethod:KuiklyRenderContextMethodFireCallback
                             args:@[instanceId, @"0", arg]];
    }
}

- (NSString *)bench:(NSDictionary *)args {
    KuiklyRenderView *root = [self perfRoot];
    if (!root) {
        return @"{\"error\":\"no root\"}";
    }
    NSDictionary *params = KRPerfParams(args);
    int iterations = MAX(1, [params[@"iterations"] intValue] ?: 5000);
    NSString *instanceId = [root perf_instanceId];
    const int warmup = 100;
    for (int i = 0; i < warmup; i++) {
        [root perf_callWithMethod:KuiklyRenderContextMethodLayoutView args:@[instanceId]];
    }
    int64_t layoutNs = 0;
    {
        int64_t t0 = KRPerfNowNs();
        for (int i = 0; i < iterations; i++) {
            [root perf_callWithMethod:KuiklyRenderContextMethodLayoutView args:@[instanceId]];
        }
        layoutNs = KRPerfNowNs() - t0;
    }
    int64_t fireNs = 0;
    {
        NSDictionary *payload = @{@"k": @1};
        int64_t t0 = KRPerfNowNs();
        for (int i = 0; i < iterations; i++) {
            [root perf_callWithMethod:KuiklyRenderContextMethodFireViewEvent
                                 args:@[instanceId, @1, @"click", payload]];
        }
        fireNs = KRPerfNowNs() - t0;
    }
    return [NSString stringWithFormat:
            @"{\"iterations\":%d,\"layout_ns_per\":%.2f,\"fireEvent_ns_per\":%.2f}",
            iterations, KRPerfPer(layoutNs, iterations), KRPerfPer(fireNs, iterations)];
}

- (NSString *)benchPhases:(NSDictionary *)args {
    KuiklyRenderView *root = [self perfRoot];
    if (!root) {
        return @"{\"error\":\"no root\"}";
    }
    NSDictionary *params = KRPerfParams(args);
    int iterations = MAX(1, [params[@"iterations"] intValue] ?: 2000);
    int jsonBytes = MAX(0, [params[@"jsonBytes"] intValue] ?: 1024);
    NSString *mode = params[@"mode"] ?: @"fire";
    NSArray *allowed = @[
        @"layout", @"fire", @"map", @"both",
        @"update", @"update_map", @"callback", @"callback_map"
    ];
    if (![allowed containsObject:mode]) {
        mode = @"fire";
    }
    NSString *instanceId = [root perf_instanceId];
    NSMutableDictionary *result = [NSMutableDictionary dictionary];
    result[@"mode"] = mode;
    result[@"iterations"] = @(iterations);
    result[@"json_bytes"] = @(jsonBytes);

    if ([mode isEqualToString:@"layout"] || [mode isEqualToString:@"both"]) {
        for (int i = 0; i < 50; i++) {
            [root perf_callWithMethod:KuiklyRenderContextMethodLayoutView args:@[instanceId]];
        }
        int64_t t0 = KRPerfNowNs();
        for (int i = 0; i < iterations; i++) {
            [root perf_callWithMethod:KuiklyRenderContextMethodLayoutView args:@[instanceId]];
        }
        int64_t callNs = KRPerfNowNs() - t0;
        result[@"calls"] = @(iterations);
        result[@"payload"] = @"none";
        result[@"method"] = @"LayoutView";
        result[@"cpp_toCValue_ns_per"] = @(0);
        result[@"cpp_callKotlin_ns_per"] = @(KRPerfPer(callNs, iterations));
        result[@"cpp_total_ns_per"] = @(KRPerfPer(callNs, iterations));
    }

    if (KRPerfModeHasJsonPayload(mode)) {
        NSDictionary *payload = KRPerfMakePayload((NSUInteger)jsonBytes);
        BOOL forceString = KRPerfModeForceString(mode) || [mode isEqualToString:@"both"];
        // For mode=both keep fire semantics (string); map modes never stringify.
        if (KRPerfModeLazyDict(mode)) {
            forceString = NO;
        }
        int64_t coldConvertNs = 0;
        if (forceString) {
            int64_t c0 = KRPerfNowNs();
            (void)[KRConvertUtil hr_dictionaryToJSON:payload];
            coldConvertNs = KRPerfNowNs() - c0;
        }

        NSString *invokeMode = mode;
        if ([mode isEqualToString:@"both"]) {
            invokeMode = @"fire";
        }

        for (int i = 0; i < 20; i++) {
            id arg = forceString ? [KRConvertUtil hr_dictionaryToJSON:payload] : payload;
            [self perfInvoke:root mode:invokeMode instanceId:instanceId arg:arg];
        }
        int64_t convertTotal = 0;
        int64_t callTotal = 0;
        for (int i = 0; i < iterations; i++) {
            id arg = payload;
            if (forceString) {
                int64_t c0 = KRPerfNowNs();
                arg = [KRConvertUtil hr_dictionaryToJSON:payload];
                convertTotal += KRPerfNowNs() - c0;
            }
            int64_t t0 = KRPerfNowNs();
            [self perfInvoke:root mode:invokeMode instanceId:instanceId arg:arg];
            callTotal += KRPerfNowNs() - t0;
        }

        NSString *methodName = @"FireViewEvent";
        if ([mode hasPrefix:@"update"]) {
            methodName = @"UpdateInstance";
        } else if ([mode hasPrefix:@"callback"]) {
            methodName = @"FireCallback";
        }

        result[@"calls"] = @(iterations);
        result[@"method"] = methodName;
        result[@"payload"] = forceString ? @"string" : @"lazy_nsdict";
        result[@"cpp_toCValue_ns_per"] = @(forceString ? KRPerfPer(convertTotal, iterations) : 0);
        result[@"cpp_callKotlin_ns_per"] = @(KRPerfPer(callTotal, iterations));
        result[@"cpp_total_ns_per"] = @(KRPerfPer(convertTotal + callTotal, iterations));
        result[@"cold_toCValue_json_ns"] = @(coldConvertNs);
    }

    NSData *data = [NSJSONSerialization dataWithJSONObject:result options:0 error:nil];
    return data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : @"{}";
}

- (NSString *)benchJson:(NSDictionary *)args {
    return [self benchPhases:args];
}

- (NSString *)exportNestedOwner:(NSDictionary *)args {
    (void)args;
    return @"{\"owner\":1}";
}

@end

#endif  // DEBUG
