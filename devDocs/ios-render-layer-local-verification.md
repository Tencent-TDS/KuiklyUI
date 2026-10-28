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
KRLabel *label = [[KRLabel alloc] initWithFrame:CGRectMake(0, 0, 460, ceil(sz.height) + 8)];
label.textRender = tr;
label.selectedRange = NSMakeRange(4, 10);
// …此处触发被测逻辑…
```

AppKit 位图上下文的原点在左下，渲染前要翻转，才和 UIKit 语义下的 `drawTextInRect:` 一致：

```objc
NSBitmapImageRep *rep = [[NSBitmapImageRep alloc] initWithBitmapDataPlanes:NULL pixelsWide:W pixelsHigh:H
    bitsPerSample:8 samplesPerPixel:4 hasAlpha:YES isPlanar:NO
    colorSpaceName:NSDeviceRGBColorSpace bytesPerRow:0 bitsPerPixel:0];
NSGraphicsContext *ctx = [NSGraphicsContext graphicsContextWithBitmapImageRep:rep];
[NSGraphicsContext saveGraphicsState];
[NSGraphicsContext setCurrentContext:ctx];
CGContextRef cg = [ctx CGContext];
CGContextTranslateCTM(cg, 0, H);
CGContextScaleCTM(cg, 1, -1);
// 先铺容器底色，再让视图绘制
[label drawTextInRect:label.bounds];
[NSGraphicsContext restoreGraphicsState];
[[rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}] writeToFile:path atomically:YES];
```

## 判据

- **逻辑类**：断言 + 退出码，逐条打印 `ok:` / `FAIL:`。
- **绘制类**：按合成公式算期望色。半透明层叠在已知底色上的结果可以直接预测——例如选区色 `(33,196,94)` 以 0.4 alpha 叠在 `#FFFDE7` 上应为 `#A6E6B0`。同时比对命中像素数是否与改动前一致，用来区分「只换了颜色」和「选区范围也变了」。
- **一定要留阴性对照**：把被测文件换成改动前的版本单独链接，确认探针会失败。改动提交之前用 `git show HEAD:<path>` 取旧实现；**提交之后要写 `HEAD~1`**，否则取到的是改后代码，对照静默失效。

## 覆盖不到的

- `TARGET_OS_IPHONE` 分支与未做 macOS 兼容的 UIKit API：在 macOS 上要么编不过，要么走了另一条实现路径
- 线程、runloop、渲染时序类问题——主线程进入顺序这类缺陷在这里看不出来
- 真机与模拟器上的 ARM64 行为、系统版本差异

以上仍须在 iOS 侧验证。

## 实例

#1793 的 iOS 一侧（选区色下发时序）用此法验证：逻辑断言修复前 4 项 FAIL、修复后全通过；渲染位图的选区底色从 `KRLabel` 内置默认的 `#B2DEEE` 变为上层配置色 `#A6E6B0`，两侧选区像素数 3922 / 3983、文字笔画 991 / 991。对应 PR 见 #1845。
