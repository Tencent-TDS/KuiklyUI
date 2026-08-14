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

#import "KRAPNGViewHandlerLite.h"

#pragma mark - APNG 手工解析

/// 单个动画帧：重建好的标准 PNG 数据 + 展示时长（秒）
@interface KRAPNGLiteFrame : NSObject
@property (nonatomic, strong) NSData *pngData;
@property (nonatomic, assign) NSTimeInterval duration;
@end

@implementation KRAPNGLiteFrame
@end

/// PNG CRC32（自实现，避免引入 libz 链接依赖）
static uint32_t KRAPNGLiteCrc32(const void *bytes, NSUInteger length) {
    static uint32_t table[256];
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        for (uint32_t i = 0; i < 256; i++) {
            uint32_t c = i;
            for (int k = 0; k < 8; k++) {
                c = (c & 1) ? (0xEDB88320u ^ (c >> 1)) : (c >> 1);
            }
            table[i] = c;
        }
    });
    const uint8_t *p = (const uint8_t *)bytes;
    uint32_t c = 0xFFFFFFFFu;
    for (NSUInteger i = 0; i < length; i++) {
        c = table[(c ^ p[i]) & 0xFF] ^ (c >> 8);
    }
    return c ^ 0xFFFFFFFFu;
}

/// 写出一块 PNG chunk（length + type + data + crc32）
static NSData *KRAPNGLiteChunk(NSString *type, NSData *data) {
    NSMutableData *out = [NSMutableData data];
    uint32_t len = CFSwapInt32HostToBig((uint32_t)data.length);
    [out appendBytes:&len length:4];
    NSData *typeData = [type dataUsingEncoding:NSASCIIStringEncoding];
    [out appendData:typeData];
    [out appendData:data];
    NSMutableData *crcIn = [NSMutableData data];
    [crcIn appendData:typeData];
    [crcIn appendData:data];
    uint32_t crcBE = CFSwapInt32HostToBig(KRAPNGLiteCrc32(crcIn.bytes, crcIn.length));
    [out appendBytes:&crcBE length:4];
    return out;
}

/// 解析 APNG 文件。仅支持全帧（offset=0, blend/dispose=0），见头文件说明。
/// 解析失败返回 nil（调用方降级为静态图，不崩溃）。
static NSArray<KRAPNGLiteFrame *> *KRAPNGLiteParse(NSData *data, NSInteger *outNumPlays) {
    static const uint8_t kSig[] = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    if (data.length < 8 || memcmp(data.bytes, kSig, 8) != 0) {
        return nil;
    }
    const uint8_t *p = (const uint8_t *)data.bytes + 8;
    const uint8_t *end = (const uint8_t *)data.bytes + data.length;

    // 注意：ihdr/pendingDelay 在 flushFrame 闭包内读取，必须 __block，
    // 否则闭包按值捕获到的是创建瞬间的初始值（帧永远组装不出来）
    __block NSData *ihdr = nil;
    NSMutableArray<KRAPNGLiteFrame *> *frames = [NSMutableArray array];
    __block NSTimeInterval pendingDelay = 0.1;
    __block BOOL hasPendingFctl = NO;
    NSMutableData *pendingIdat = [NSMutableData data];
    NSInteger numPlays = 0; // acTL num_plays，0 = 无限

    __auto_type flushFrame = ^{
        if (!hasPendingFctl || pendingIdat.length == 0 || ihdr == nil) { return; }
        NSMutableData *png = [NSMutableData data];
        [png appendBytes:kSig length:8];
        [png appendData:KRAPNGLiteChunk(@"IHDR", ihdr)];
        [png appendData:KRAPNGLiteChunk(@"IDAT", pendingIdat)];
        [png appendData:KRAPNGLiteChunk(@"IEND", [NSData data])];
        KRAPNGLiteFrame *f = [[KRAPNGLiteFrame alloc] init];
        f.pngData = png;
        f.duration = pendingDelay;
        [frames addObject:f];
        [pendingIdat setLength:0];
        hasPendingFctl = NO;
    };

    while (p + 12 <= end) {
        uint32_t len = CFSwapInt32BigToHost(*(const uint32_t *)p);
        const uint8_t *type = p + 4;
        const uint8_t *body = p + 8;
        if (body + len + 4 > end) { break; } // 截断文件，止损

        if (memcmp(type, "IHDR", 4) == 0) {
            ihdr = [NSData dataWithBytes:body length:len];
        } else if (memcmp(type, "acTL", 4) == 0 && len >= 8) {
            numPlays = CFSwapInt32BigToHost(*(const uint32_t *)(body + 4));
        } else if (memcmp(type, "fcTL", 4) == 0 && len >= 26) {
            flushFrame();
            uint16_t delayNum = CFSwapInt16BigToHost(*(const uint16_t *)(body + 20));
            uint16_t delayDen = CFSwapInt16BigToHost(*(const uint16_t *)(body + 22));
            if (delayDen == 0) { delayDen = 100; } // 规范：den=0 按 100 计
            pendingDelay = MAX(0.01, (NSTimeInterval)delayNum / delayDen);
            hasPendingFctl = YES;
        } else if (memcmp(type, "IDAT", 4) == 0) {
            if (hasPendingFctl) { [pendingIdat appendBytes:body length:len]; }
        } else if (memcmp(type, "fdAT", 4) == 0 && len >= 4) {
            if (hasPendingFctl) { [pendingIdat appendBytes:(body + 4) length:(len - 4)]; }
        } else if (memcmp(type, "IEND", 4) == 0) {
            break;
        }
        p = body + len + 4; // 跳过 data + crc
    }
    flushFrame();
    if (outNumPlays) { *outNumPlays = numPlays; }
    return frames.count > 0 ? frames : nil;
}

#pragma mark - KRAPNGViewHandlerLite

@interface KRAPNGViewHandlerLite ()
@property (nonatomic, strong) UIImageView *imageView;
@property (nonatomic, copy) NSArray<KRAPNGLiteFrame *> *frames;
@property (nonatomic, assign) NSInteger fileNumPlays; // 文件自带的播放次数
@property (nonatomic, assign) NSUInteger frameIndex;
@property (nonatomic, assign) NSUInteger playedLoops;
@property (nonatomic, assign) BOOL animating;
@property (nonatomic, assign) BOOL didSetPlayCount;   // 区分「未设置」与「显式 0」
@end

@implementation KRAPNGViewHandlerLite

@synthesize delegate = _delegate;
@synthesize playCount = _playCount;

+ (void)registerToKuikly {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        [KRAPNGView registerAPNGViewCreator:^id<APNGImageViewProtocol> _Nonnull(CGRect frame) {
            return [[KRAPNGViewHandlerLite alloc] initWithFrame:frame];
        }];
    });
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _imageView = [[UIImageView alloc] initWithFrame:self.bounds];
        _imageView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        _imageView.contentMode = UIViewContentModeScaleAspectFit;
        [self addSubview:_imageView];
    }
    return self;
}

- (void)setPlayCount:(NSInteger)playCount {
    _playCount = playCount;
    _didSetPlayCount = YES;
}

#pragma mark - APNGImageViewProtocol

- (void)setFilePath:(NSString *)filePath {
    [self setFilePath:filePath withCompletion:nil];
}

- (void)setFilePath:(NSString *)filePath withCompletion:(void (^)(UIImage * _Nullable))completion {
    [self p_stopTimer];
    self.animating = NO;
    self.frames = nil;
    self.imageView.image = nil;

    NSData *data = filePath.length > 0 ? [NSData dataWithContentsOfFile:filePath] : nil;
    NSInteger numPlays = 0;
    NSArray<KRAPNGLiteFrame *> *frames = data ? KRAPNGLiteParse(data, &numPlays) : nil;
    UIImage *firstImage = nil;
    if (frames.count > 0) {
        self.frames = frames;
        self.fileNumPlays = numPlays;
        firstImage = [UIImage imageWithData:frames.firstObject.pngData];
        self.imageView.image = firstImage; // 未播放时定格首帧
    } else if (data) {
        // 解析失败/普通 PNG：降级为静态图兜底，不崩溃
        firstImage = [UIImage imageWithData:data];
        self.imageView.image = firstImage;
    }
    if (completion) { completion(firstImage); }
}

- (void)startAPNGAnimating {
    if (self.frames.count == 0) { return; }
    [self p_stopTimer];
    self.animating = YES;
    self.frameIndex = 0;
    self.playedLoops = 0;
    [self p_showFrameAtIndex:0];
}

- (void)stopAPNGAnimating {
    [self p_stopTimer];
    self.animating = NO;
}

#pragma mark - private

- (NSInteger)p_effectivePlayCount {
    if (self.didSetPlayCount) { return self.playCount; } // 宿主显式设置（含 0=无限）
    return self.fileNumPlays;                            // 否则用文件里的 num_plays
}

- (void)p_showFrameAtIndex:(NSUInteger)index {
    if (index >= self.frames.count) { return; }
    self.frameIndex = index;
    KRAPNGLiteFrame *frame = self.frames[index];
    self.imageView.image = [UIImage imageWithData:frame.pngData];
    NSTimeInterval delay = frame.duration;
    __weak typeof(self) weakSelf = self;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), ^{
        [weakSelf p_advanceFromIndex:index];
    });
}

- (void)p_advanceFromIndex:(NSUInteger)index {
    if (!self.animating || index != self.frameIndex) { return; } // 已停止或已被新播放覆盖
    NSUInteger next = index + 1;
    if (next < self.frames.count) {
        [self p_showFrameAtIndex:next];
        return;
    }
    // 一轮播完
    self.playedLoops += 1;
    if ([self.delegate respondsToSelector:@selector(apngImageView:playEndLoop:)]) {
        [self.delegate apngImageView:self playEndLoop:self.playedLoops];
    }
    NSInteger maxLoops = [self p_effectivePlayCount];
    if (maxLoops <= 0 || self.playedLoops < (NSUInteger)maxLoops) {
        [self p_showFrameAtIndex:0]; // 继续下一轮
    } else {
        self.animating = NO; // 定格末帧（imageView 已停在最后一帧）
    }
}

- (void)p_stopTimer {
    // 播放基于 dispatch_after + frameIndex 守卫，无需显式取消：把 index 推大即可让旧回调失效
    self.frameIndex = NSUIntegerMax / 2;
}

- (void)dealloc {
    self.animating = NO;
}

@end
