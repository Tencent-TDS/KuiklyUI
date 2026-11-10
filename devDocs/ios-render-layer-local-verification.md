# 在 macOS 上本地验证 core-render-ios

> 框架开发时阅读。改 `core-render-ios` 不必等 iOS 模拟器、也不必搭 Kotlin/Native 工具链：渲染层可以单独编译成宿主原生二进制，直接跑逻辑断言，甚至把 `KRLabel`、`KRRichTextView` 画出来的结果渲染成位图。

## 为什么可行

渲染层的 Kotlin 依赖只落在与 `core-*` 的桥接处，自身链接后只有系统框架和 `_main` 未定义。`MacSupport/KRUIKit.h` 提供 macOS 兼容层（`UIColor` 映射到 `NSColor`、`UILabel` 映射到 `KRUILabel` 等）。于是每个 `.m`/`.mm` 都能用 `clang` 编到 macOS 目标，配一个自带 `main` 的探针即可运行。

实测：107 个 `.m`/`.mm` 全部编译通过；链接后未定义符号只有 CoreImage、Accelerate 与 libc++ 运行库，补上框架即可。

## 环境

- macOS + Xcode Command Line Tools（用到 `xcrun --sdk macosx`）
- 目标三元组用**宿主机架构**：`$(uname -m)-apple-macos13.0`

> 宿主是 Intel 而目标写成 `arm64-apple-macos13.0` 时，编译和链接都会成功，运行时才报 `Bad CPU type in executable`。同样原因，`simctl spawn` 也会失败。

## 编译整个渲染层

```bash
cd <仓库根>
OUT=$(pwd)/.ios-probe && rm -rf "$OUT" && mkdir -p "$OUT"
SDK=$(xcrun --sdk macosx --show-sdk-path)
TRI=$(uname -m)-apple-macos13.0
INC="-I core-render-ios/include -I core-render-ios -I core-render-ios/Extension \
     -I core-render-ios/Thread -I core-render-ios/Core -I core-render-ios/Handler \
     -I core-render-ios/View -I core-render-ios/Performance -I core-render-ios/Protocol \
     -I core-render-ios/Extension/TextSelection -I core-render-ios/Extension/Vendor \
     -I core-render-ios/MacSupport"

for f in $(find core-render-ios -name '*.m' -o -name '*.mm'); do
  xcrun --sdk macosx clang -c -fobjc-arc -isysroot "$SDK" -target "$TRI" $INC \
    "$f" -o "$OUT/$(echo "$f" | tr '/' '_').o" || echo "FAIL $f"
done
```

## 写探针并链接

探针就是一个普通 `main.m`，`#import` 要验证的头文件，退出码非 0 表示失败。

```bash
xcrun --sdk macosx clang -target "$TRI" -isysroot "$SDK" -o "$OUT/probe" "$OUT"/*.o \
  -framework AppKit -framework Foundation -framework QuartzCore -framework CoreGraphics \
  -framework CoreText -framework CoreImage -framework Accelerate -framework AVFoundation \
  -framework ImageIO -framework CoreMedia -framework UniformTypeIdentifiers -lc++
"$OUT/probe"; echo "exit=$?"
```

> 探针目录里若有多个带 `main` 的对象文件（比如同时编了两个探针），链接会以 exit code 1 失败但默认不打印符号明细，加 `grep -E '[[:space:]]+"|error'` 看真实原因。BSD grep 不支持 `\s`。

## 把绘制结果渲染成位图

绘制类问题可以直接出图。`KRTextRender` 有公开的 `initWithTextStorage:lineHeight:`；`KRLabel` 的 `textRender`、`selectedRange`、`selectionColor` 都是公开属性。

```objc
NSTextStorage *ts = [[NSTextStorage alloc] initWithAttributedString:as];
KRTextRender *tr = [[KRTextRender alloc] initWithTextStorage:ts lineHeight:0];
CGSize sz = [tr textSizeWithRenderWidth:460];
KRLabel *label = [[KRLabel alloc] initWithFrame:NSMakeRect(0, 0, 460, ceil(sz.height))];
label.textRender = tr;
label.selectedRange = NSMakeRange(4, 10);
// …此处触发被测逻辑…
```

绘制交给 AppKit 的视图快照，不要自己造位图上下文：

```objc
// 宿主视图只负责铺容器底色
@interface KRProbeHostView : NSView
@property (nonatomic, strong) NSColor *fillColor;
@end
@implementation KRProbeHostView
- (void)drawRect:(NSRect)dirtyRect { [self.fillColor setFill]; NSRectFill(self.bounds); }
@end

KRProbeHostView *host = [[KRProbeHostView alloc] initWithFrame:NSMakeRect(0, 0, 460, ceil(sz.height))];
host.fillColor = [NSColor colorWithRed:0xFF/255.0 green:0xFD/255.0 blue:0xE7/255.0 alpha:1.0];
[host addSubview:label];
[host layoutSubtreeIfNeeded];            // 必须先布局，否则快照是空的
NSBitmapImageRep *rep = [host bitmapImageRepForCachingDisplayInRect:host.bounds];
[host cacheDisplayInRect:host.bounds toBitmapImageRep:rep];
[[rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}] writeToFile:path atomically:YES];
```

`KRUILabel` 的 `isFlipped` 返回 YES，快照接口会按视图自己的翻转设置准备绘制上下文。产出的位图是 2 倍像素（Retina），断言里的坐标要按 2 折算。

> 不要自己建 `NSBitmapImageRep`、手动 `CGContextTranslateCTM` + `CGContextScaleCTM` 翻转后直接调 `drawTextInRect:`。选区矩形与文字会落在不同空间里，相差约一个行高：实测高亮带占 0..21 行、文字墨迹占 17..35 行，两者几乎不重叠。快照接口没有这个问题。

## 判据

- **逻辑类**：断言 + 退出码，逐条打印 `ok:` / `FAIL:`。
- **颜色按同一条渲染路径下的实测值比对**，不要套理论混合公式：颜色经 AppKit 色彩空间转换会有偏移——背景 `#FFFDE7` 实测渲染成 `#FFFDE1`，`(33,196,94)` 以 0.4 alpha 叠上去是 `#A7E2A5`，不是线性混合的 `#A6E6B0`。改动前/后同条件各渲一次，比对底色是否变化，以及命中像素数与墨迹像素数是否保持不变。
- **像素数不变用来区分「只换了颜色」和「选区范围也变了」**。
- **绘制类先断言几何，再比颜色**：文字墨迹的行范围必须落在高亮带的行范围内，高亮带的列范围要覆盖被选中的字符。只比颜色会把错位的图当成好图——错位的图颜色照样是对的。
- **一定要留阴性对照**：把被测文件换成改动前的版本单独链接，确认探针会失败。改动提交之前用 `git show HEAD:<path>` 取旧实现；**提交之后要写 `HEAD~1`**，否则取到的是改后代码，对照静默失效。

## 覆盖不到的

- `TARGET_OS_IPHONE` 分支与未做 macOS 兼容的 UIKit API：在 macOS 上要么编不过，要么走了另一条实现路径
- 线程、runloop、渲染时序类问题——主线程进入顺序这类缺陷在这里看不出来
- 真机与模拟器上的 ARM64 行为、系统版本差异

以上仍须在 iOS 侧验证。

## 实例

#1793 的 iOS 一侧（选区色下发时序）用此法验证：逻辑断言修复前 4 项 FAIL、修复后全通过；快照渲染的高亮底色从 `KRLabel` 内置默认的 `#B7D7E9` 变为上层配置色 `#A7E2A5`，两版高亮像素数相同（10224）、文字墨迹数也相同（8894）——只换了颜色，选区范围与排版未动。对应 PR 见 #1845。
