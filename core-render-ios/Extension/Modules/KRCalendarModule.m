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

#import "KRCalendarModule.h"

@implementation KRCalendarModule


/** 公历日历，时区需与 dateFromString / stringFromDate 保持一致 */
- (NSCalendar *)gregorianCalendar {
    NSCalendar *calendar = [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
    calendar.timeZone = [NSTimeZone timeZoneWithName:@"Asia/Shanghai"];
    return calendar;
}

/** set 只累积到 components 不落盘，add 先落盘再加减，最后一次性求值：避免一串 set 的中间态（如 8/31 时 set 到 9 月得到 9-31）被当作非法日期解析成 nil */
- (NSDate *)dateByApplyingOperations:(NSArray<NSString *> *)operations toDate:(NSDate *)originDate {
    NSUInteger units = NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay |
                       NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitSecond |
                       NSCalendarUnitNanosecond;
    NSCalendar *calendar = [self gregorianCalendar];
    NSDateComponents *components = [calendar components:units fromDate:originDate];

    for (NSString *operationString in operations) {
        NSDictionary *operation = [operationString kr_stringToDictionary];
        NSString *opt = operation[@"opt"];
        NSInteger field = [operation[@"field"] integerValue];
        NSInteger value = [operation[@"value"] integerValue];

        if ([opt isEqualToString:@"set"]) {
            if (field == 6) { // DAY_OF_YEAR：等价 add(DAY_OF_MONTH, newDay - currentDay)
                NSDate *current = [calendar dateFromComponents:components];
                NSInteger currentDay = [calendar ordinalityOfUnit:NSCalendarUnitDay
                                                           inUnit:NSCalendarUnitYear
                                                          forDate:current];
                components.day += (value - currentDay);
                continue;
            }
            switch (field) {
                case 1:  components.year = value; break;
                case 2:  components.month = value + 1; break; // MONTH 从 0 开始
                case 5:  components.day = value; break;
                case 11: components.hour = value; break;
                case 12: components.minute = value; break;
                case 13: components.second = value; break;
                case 14: components.nanosecond = (long long)value * 1000000; break;
                default: break;
            }
        } else if ([opt isEqualToString:@"add"]) {
            NSDate *current = [calendar dateFromComponents:components];
            NSDateComponents *delta = [[NSDateComponents alloc] init];
            switch (field) {
                case 1:  delta.year = value; break;
                case 2:  delta.month = value; break;
                case 5:  delta.day = value; break;
                case 6:  delta.day = value; break;
                case 11: delta.hour = value; break;
                case 12: delta.minute = value; break;
                case 13: delta.second = value; break;
                case 14: delta.nanosecond = (long long)value * 1000000; break;
                default: break;
            }
            NSDate *next = [calendar dateByAddingComponents:delta toDate:current options:0];
            components = [calendar components:units fromDate:next];
        }
    }

    return [calendar dateFromComponents:components];
}

- (NSDate *)dateFromString:(NSString *)dateString format:(NSString *)format {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    formatter.timeZone = [NSTimeZone timeZoneWithName:@"Asia/Shanghai"];
    formatter.dateFormat = format;
    return [formatter dateFromString:dateString];
}

- (NSString *)stringFromDate:(NSDate *)date format:(NSString *)format {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    formatter.timeZone = [NSTimeZone timeZoneWithName:@"Asia/Shanghai"];
    formatter.dateFormat = format;
    return [formatter stringFromDate:date];
}

- (NSString *)method_cur_timestamp:(NSDictionary *)args {
    return [NSString stringWithFormat:@"%lld", (long long)([[NSDate date] timeIntervalSince1970] * 1000)];
}

- (NSString *)method_get_field:(NSDictionary *)args {
    
    NSDictionary *params = [args[KR_PARAM_KEY] kr_stringToDictionary];
    NSArray<NSString *> *operations = (NSArray *)[params[@"operations"] kr_stringToArray];
    NSDate *originDate = [NSDate dateWithTimeIntervalSince1970:[params[@"timeMillis"] integerValue] / 1000.0];
    NSDate *date = [self dateByApplyingOperations:operations toDate:originDate];

    switch([params[@"field"] integerValue]) {
        case 1: {
            return [self stringFromDate:date format:@"yyyy"];
        }
        case 2: {
            return [NSString stringWithFormat:@"%ld", [self stringFromDate:date format:@"MM"].integerValue - 1];
        }
        case 5: {
            return [self stringFromDate:date format:@"dd"];
        }
        case 6: { // day of year
            return [self stringFromDate:date format:@"D"];
        }
        case 7: { // dayOfWeek
            // 用系统时区而非 gregorianCalendar(Asia/Shanghai)：与 Android Calendar.getInstance() 口径一致，星期受时区跨日影响
            NSCalendar *calendar = [NSCalendar calendarWithIdentifier:NSCalendarIdentifierGregorian];
            NSDateComponents *components = [calendar components:NSCalendarUnitWeekday fromDate:date];
            NSInteger dayOfWeek = [components weekday];
            return [NSString stringWithFormat:@"%ld", (long)dayOfWeek];
        }
        case 11: {
            return [self stringFromDate:date format:@"HH"];
        }
        case 12: {
            return [self stringFromDate:date format:@"mm"];
        }
        case 13: {
            return [self stringFromDate:date format:@"ss"];
        }
        case 14: {
            return [self stringFromDate:date format:@"SSS"];
        }
    }
    return @"";
}

- (NSString *)method_get_time_in_millis:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] kr_stringToDictionary];
    NSArray<NSString *> *operations = (NSArray *)[params[@"operations"] kr_stringToArray];
    NSDate *originDate = [NSDate dateWithTimeIntervalSince1970:[params[@"timeMillis"] integerValue] / 1000.0];
    NSDate *date = [self dateByApplyingOperations:operations toDate:originDate];

    return [NSString stringWithFormat:@"%lld", (long long)([date timeIntervalSince1970] * 1000)];
}

- (NSString *)method_format:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] kr_stringToDictionary];
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    formatter.timeZone = [NSTimeZone timeZoneWithName:@"Asia/Shanghai"];
    formatter.dateFormat = params[@"format"];
    NSTimeInterval mills = [params[@"timeMillis"] integerValue] / 1000.0;
    NSDate *date = mills == 0 ? [NSDate date] :  [NSDate dateWithTimeIntervalSince1970:mills];
    return [formatter stringFromDate:date];
    
}

- (NSString *)method_parse_format:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] kr_stringToDictionary];
    NSDate *date = [self dateFromString:params[@"formattedTime"] format:params[@"format"]];
    return [NSString stringWithFormat:@"%lld", (long long)([date timeIntervalSince1970] * 1000)];
}


@end
