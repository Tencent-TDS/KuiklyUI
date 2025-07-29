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


- (NSCalendar *)localCalendar {
    return [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierChinese];
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

- (NSDate *)calcDate:(NSDate *)originDate operation:(NSString *)operationString {
    NSDictionary *operation = [operationString kr_stringToDictionary];
    NSString *opt = operation[@"opt"];
    NSString *value = operation[@"value"];
    
    if ([opt isEqualToString:@"set"]) {
        NSInteger year = [self stringFromDate:originDate format:@"yyyy"].integerValue;
        NSInteger month = [self stringFromDate:originDate format:@"MM"].integerValue;
        NSInteger day = [self stringFromDate:originDate format:@"dd"].integerValue;
        NSInteger hour = [self stringFromDate:originDate format:@"HH"].integerValue;
        NSInteger minute = [self stringFromDate:originDate format:@"mm"].integerValue;
        NSInteger second = [self stringFromDate:originDate format:@"ss"].integerValue;
        NSInteger millisecond = [self stringFromDate:originDate format:@"SSS"].integerValue;
        switch([operation[@"field"] integerValue]) {
            case 1: {
                year = [value integerValue];
                break;
            }
            case 2: {
                month = [value integerValue] + 1;
                break;
            }
            case 5: {
                day = [value integerValue];
                break;
            }
            case 6: {
                NSInteger originDay = [self stringFromDate:originDate format:@"D"].integerValue;
                NSInteger newDay = [value integerValue];
                NSDictionary *operation = @{
                    @"opt": @"add",
                    @"field": @(5),
                    @"value": @(newDay - originDay)
                };
                return [self calcDate:originDate operation:[operation kr_dictionaryToString]];
            }
            case 11: {
                hour = [value integerValue];
                break;
            }
            case 12: {
                minute = [value integerValue];
                break;
            }
            case 13: {
                second = [value integerValue];
                break;
            }
            case 14: {
                millisecond = [value integerValue];
                break;
            }
        }
        NSString *dateString = [NSString stringWithFormat:@"%04ld-%02ld-%02ld %02ld:%02ld:%02ld.%03ld",
                                year, month, day, hour, minute, second, millisecond];
        return [self dateFromString:dateString format:@"YYYY-MM-dd HH:mm:ss.SSS"];
    }
    
    if (![opt isEqualToString:@"add"]) {
        NSAssert(NO, @"操作不存在，仅支持add和set");
        return [NSDate date];
    }
    
    NSDateComponents *dateComponents = [[NSDateComponents alloc] init];
    dateComponents.year = 0;
    dateComponents.month = 0;
    switch([operation[@"field"] integerValue]) {
        case 1: {
            dateComponents.year = [value integerValue];
            break;
        }
        case 2: {
            dateComponents.month = [value integerValue];
            break;
        }
        case 5: {
            dateComponents.day = [value integerValue];
            break;
        }
        case 6: {
            dateComponents.day = [value integerValue];
            break;
        }
        case 11: {
            dateComponents.hour = [value integerValue];
            break;
        }
        case 12: {
            dateComponents.minute = [value integerValue];
            break;
        }
        case 13: {
            dateComponents.second = [value integerValue];
            break;
        }
        case 14: {
            dateComponents.nanosecond = [value integerValue] * 1000000;
            break;
        }
    }
    
    NSDate *date;
    if (dateComponents.year == 0 && dateComponents.month == 0) {
        date = [self.localCalendar dateByAddingComponents:dateComponents toDate:originDate options:0];
    } else {
        NSInteger year = [self stringFromDate:originDate format:@"yyyy"].integerValue + dateComponents.year;
        NSInteger month = [self stringFromDate:originDate format:@"MM"].integerValue;
        while (dateComponents.month > 0) {
            month ++;
            year += month > 12 ? 1 : 0;
            month = (month - 1) % 12 + 1;
            dateComponents.month --;
        }
        while (dateComponents.month < 0) {
            month --;
            year -= month == 0 ? 1 : 0;
            month = month == 0 ? 12 : month;
            dateComponents.month ++;
        }
        NSInteger day = [self stringFromDate:originDate format:@"dd"].integerValue;
        NSInteger maxDayCount = [self.localCalendar rangeOfUnit:NSCalendarUnitDay inUnit:NSCalendarUnitMonth forDate:[self dateFromString:[NSString stringWithFormat:@"%04ld-%02ld", year, month] format:@"yyyy-MM"]].length;
        day = MIN(day, maxDayCount);
        NSString *dateString = [NSString stringWithFormat:@"%04ld-%02ld-%02ld %@", year, month, day, [self stringFromDate:originDate format:@"HH:mm:ss.SSS"]];
        date = [self dateFromString:dateString format:@"YYYY-MM-dd HH:mm:ss.SSS"];
    }
    
    return date;
}

- (NSString *)method_cur_timestamp:(NSDictionary *)args {
    return [NSString stringWithFormat:@"%ld", (NSInteger)([[NSDate date] timeIntervalSince1970] * 1000)];
}

- (NSString *)method_get_field:(NSDictionary *)args {
    
    NSDictionary *params = [args[KR_PARAM_KEY] kr_stringToDictionary];
    NSArray<NSString *> *operations = (NSArray *)[params[@"operations"] kr_stringToArray];
    NSDate *date = [NSDate dateWithTimeIntervalSince1970:[params[@"timeMillis"] integerValue] / 1000.0];
    
    for (NSString *operationString in operations) {
        date = [self calcDate:date operation:operationString];
    }
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
            NSCalendar *calendar = [NSCalendar currentCalendar];
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
    NSDate *date = [NSDate dateWithTimeIntervalSince1970:[params[@"timeMillis"] integerValue] / 1000.0];
    
    for (NSString *operationString in operations) {
        date = [self calcDate:date operation:operationString];
    }
    return [NSString stringWithFormat:@"%ld", (NSInteger)([date timeIntervalSince1970] * 1000)];
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
    return [NSString stringWithFormat:@"%ld", (NSInteger)([date timeIntervalSince1970] * 1000L)];
}


@end
