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

#import "KRInteropPerfTestModule.h"

static const NSInteger KRInteropDefaultCount = 50;
static const NSInteger KRInteropDefaultPayloadChars = 128 * 1024;

@interface KRInteropPerfTestModule ()
@property (nonatomic, strong) NSMutableDictionary<NSString *, id> *plainStringCallbacks;
@property (nonatomic, strong) NSMutableDictionary<NSString *, id> *jsonCallbacks;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSArray<NSString *> *> *plainStringPayloadCaches;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSArray<NSString *> *> *jsonStringPayloadCaches;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSArray<NSString *> *> *krRecordPayloadCaches;
@property (nonatomic, copy) KuiklyRenderCallback statsCallback;
@end

@implementation KRInteropPerfTestModule

- (instancetype)init {
    self = [super init];
    if (self) {
        _plainStringCallbacks = [NSMutableDictionary dictionary];
        _jsonCallbacks = [NSMutableDictionary dictionary];
        _plainStringPayloadCaches = [NSMutableDictionary dictionary];
        _jsonStringPayloadCaches = [NSMutableDictionary dictionary];
        _krRecordPayloadCaches = [NSMutableDictionary dictionary];
    }
    return self;
}

- (id)SetStatsCallback:(NSDictionary *)args {
    self.statsCallback = args[KR_CALLBACK_KEY];
    return nil;
}

- (id)SetCallbackWithPlainString:(NSDictionary *)args {
    NSString *caseName = args[KR_PARAM_KEY] ?: @"";
    self.plainStringCallbacks[caseName] = args[KR_CALLBACK_KEY] ?: [NSNull null];
    return nil;
}

- (id)SetCallbackWithJson:(NSDictionary *)args {
    NSString *caseName = args[KR_PARAM_KEY] ?: @"";
    self.jsonCallbacks[caseName] = args[KR_CALLBACK_KEY] ?: [NSNull null];
    return nil;
}

- (id)RunPlainString:(NSDictionary *)args {
    NSDictionary *config = [self parseConfig:args[KR_PARAM_KEY]];
    NSString *caseName = config[@"caseName"];
    NSArray<NSString *> *payloads = [self preparePlainStringPayloads:caseName count:[config[@"count"] integerValue] payloadChars:[config[@"payloadChars"] integerValue]];
    id callback = [self callbackForKey:caseName in:self.plainStringCallbacks];
    [self runPreparedStrings:caseName payloads:payloads callback:callback];
    return nil;
}

- (id)RunJsonString:(NSDictionary *)args {
    NSDictionary *config = [self parseConfig:args[KR_PARAM_KEY]];
    NSString *caseName = config[@"caseName"];
    NSArray<NSString *> *payloads = [self prepareJsonStringPayloads:caseName count:[config[@"count"] integerValue] payloadChars:[config[@"payloadChars"] integerValue]];
    id callback = [self callbackForKey:caseName in:self.jsonCallbacks];
    [self runPreparedStrings:caseName payloads:payloads callback:callback];
    return nil;
}

- (id)RunKRRecord:(NSDictionary *)args {
    NSDictionary *config = [self parseConfig:args[KR_PARAM_KEY]];
    NSString *caseName = config[@"caseName"];
    NSArray<NSString *> *payloads = [self prepareKRRecordPayloads:caseName count:[config[@"count"] integerValue] payloadChars:[config[@"payloadChars"] integerValue]];
    id callback = [self callbackForKey:caseName in:self.jsonCallbacks];
    [self runPreparedStrings:caseName payloads:payloads callback:callback];
    return nil;
}

- (id)RunKRJsonValue:(NSDictionary *)args {
    NSString *jsonText = args[KR_PARAM_KEY];
    if (![jsonText isKindOfClass:[NSString class]]) {
        return nil;
    }
    NSData *data = [jsonText dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *request = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    NSString *caseName = request[@"case"] ?: @"";
    NSInteger count = [request[@"count"] integerValue];
    NSDictionary *payload = request[@"payload"];
    NSData *payloadData = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];
    NSString *payloadString = [[NSString alloc] initWithData:payloadData encoding:NSUTF8StringEncoding];
    id callback = [self callbackForKey:caseName in:self.jsonCallbacks];
    if (!payloadString || !callback) {
        return nil;
    }
    long long bytes = (long long)payloadString.length * count;
    NSTimeInterval start = [NSDate timeIntervalSinceReferenceDate] * 1000.0;
    for (NSInteger i = 0; i < count; i++) {
        ((KuiklyRenderCallback)callback)(payloadString);
    }
    NSTimeInterval end = [NSDate timeIntervalSinceReferenceDate] * 1000.0;
    [self report:caseName count:(NSInteger)count costMs:(long long)(end - start) bytes:bytes];
    return nil;
}

- (NSDictionary *)parseConfig:(id)params {
    NSString *text = [params isKindOfClass:[NSString class]] ? params : @"";
    NSString *caseName = [self regexCapture:text pattern:@"\"case\"\\s*:\\s*\"([^\"]+)\"" fallback:@""];
    NSString *countText = [self regexCapture:text pattern:@"\"count\"\\s*:\\s*(\\d+)" fallback:@(KRInteropDefaultCount).stringValue];
    NSString *charsText = [self regexCapture:text pattern:@"\"payloadChars\"\\s*:\\s*(\\d+)" fallback:@(KRInteropDefaultPayloadChars).stringValue];
    return @{
        @"caseName": caseName,
        @"count": @(countText.integerValue),
        @"payloadChars": @(charsText.integerValue),
    };
}

- (NSString *)regexCapture:(NSString *)text pattern:(NSString *)pattern fallback:(NSString *)fallback {
    NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:pattern options:0 error:nil];
    NSTextCheckingResult *match = [regex firstMatchInString:text options:0 range:NSMakeRange(0, text.length)];
    if (match && match.numberOfRanges > 1) {
        return [text substringWithRange:[match rangeAtIndex:1]];
    }
    return fallback;
}

- (NSString *)makePad:(NSInteger)targetLen {
    NSString *token = @"0123456789abcdef测Abc";
    NSMutableString *out = [NSMutableString stringWithCapacity:targetLen];
    while (out.length < targetLen) {
        NSInteger remaining = targetLen - out.length;
        if (remaining >= token.length) {
            [out appendString:token];
        } else {
            [out appendString:[token substringToIndex:remaining]];
        }
    }
    return out;
}

- (NSString *)makeJsonString:(NSInteger)i targetLen:(NSInteger)targetLen {
    NSMutableDictionary *json = [NSMutableDictionary dictionaryWithDictionary:@{@"kind": @"json_string", @"i": @(i), @"v": @""}];
    NSData *baseData = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
    NSString *base = [[NSString alloc] initWithData:baseData encoding:NSUTF8StringEncoding];
    NSInteger padLen = MAX(0, targetLen - (NSInteger)base.length);
    json[@"v"] = [self makePad:padLen];
    NSData *data = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

- (NSString *)makeKRRecord:(NSInteger)i targetLen:(NSInteger)targetLen {
    NSMutableDictionary *json = [NSMutableDictionary dictionaryWithDictionary:@{@"kind": @"kr_record", @"i": @(i), @"v": @""}];
    NSData *baseData = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
    NSString *base = [[NSString alloc] initWithData:baseData encoding:NSUTF8StringEncoding];
    NSInteger padLen = MAX(0, targetLen - (NSInteger)base.length);
    json[@"v"] = [self makePad:padLen];
    NSData *data = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

- (NSArray<NSString *> *)preparePlainStringPayloads:(NSString *)caseName count:(NSInteger)count payloadChars:(NSInteger)payloadChars {
    NSArray *cached = self.plainStringPayloadCaches[caseName];
    if (cached && cached.count == count) {
        return cached;
    }
    NSMutableArray<NSString *> *out = [NSMutableArray arrayWithCapacity:count];
    for (NSInteger i = 0; i < count; i++) {
        [out addObject:[self makePad:payloadChars]];
    }
    self.plainStringPayloadCaches[caseName] = out;
    return out;
}

- (NSArray<NSString *> *)prepareJsonStringPayloads:(NSString *)caseName count:(NSInteger)count payloadChars:(NSInteger)payloadChars {
    NSArray *cached = self.jsonStringPayloadCaches[caseName];
    if (cached && cached.count == count) {
        return cached;
    }
    NSMutableArray<NSString *> *out = [NSMutableArray arrayWithCapacity:count];
    for (NSInteger i = 0; i < count; i++) {
        [out addObject:[self makeJsonString:i targetLen:payloadChars]];
    }
    self.jsonStringPayloadCaches[caseName] = out;
    return out;
}

- (NSArray<NSString *> *)prepareKRRecordPayloads:(NSString *)caseName count:(NSInteger)count payloadChars:(NSInteger)payloadChars {
    NSArray *cached = self.krRecordPayloadCaches[caseName];
    if (cached && cached.count == count) {
        return cached;
    }
    NSMutableArray<NSString *> *out = [NSMutableArray arrayWithCapacity:count];
    for (NSInteger i = 0; i < count; i++) {
        [out addObject:[self makeKRRecord:i targetLen:payloadChars]];
    }
    self.krRecordPayloadCaches[caseName] = out;
    return out;
}

- (id)callbackForKey:(NSString *)caseName in:(NSDictionary *)callbacks {
    id callback = callbacks[caseName];
    return callback == [NSNull null] ? nil : callback;
}

- (void)runPreparedStrings:(NSString *)caseName payloads:(NSArray<NSString *> *)payloads callback:(id)callback {
    if (!callback) {
        return;
    }
    long long bytes = 0;
    for (NSString *payload in payloads) {
        bytes += payload.length;
    }
    NSTimeInterval start = [NSDate timeIntervalSinceReferenceDate] * 1000.0;
    for (NSString *payload in payloads) {
        ((KuiklyRenderCallback)callback)(payload);
    }
    NSTimeInterval end = [NSDate timeIntervalSinceReferenceDate] * 1000.0;
    [self report:caseName count:(NSInteger)payloads.count costMs:(long long)(end - start) bytes:bytes];
}

- (void)report:(NSString *)caseName count:(NSInteger)count costMs:(long long)costMs bytes:(long long)bytes {
    if (!self.statsCallback) {
        return;
    }
    NSDictionary *json = @{
        @"case": caseName ?: @"",
        @"count": @(count),
        @"cost_ms": @(costMs),
        @"bytes": @(bytes),
    };
    NSData *data = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
    NSString *line = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    self.statsCallback(line);
}

@end
