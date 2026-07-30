#import "HRBridgeModule.h"

#import "KuiklyRenderViewController.h"
#import <OpenKuiklyIOSRender/NSObject+KR.h>

#define REQ_PARAM_KEY @"reqParam"
#define CMD_KEY @"cmd"
#define FROM_HIPPY_RENDER @"from_hippy_render"
// 扩展桥接接口
/*
 * @brief Native暴露接口到kotlin侧，提供kotlin侧调用native能力
 */

@implementation HRBridgeModule

@synthesize hr_rootView;

- (void)copyToPasteboard:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *content = params[@"content"];
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
    pasteboard.string = content;
}

- (void)log:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *content = params[@"content"];
    NSLog(@"KuiklyRender:%@", content);
}

/// 读取 assets 目录下的文件内容（framework 模式从 mainBundle 读取）
- (void)readAssetFile:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *assetPath = params[@"assetPath"];

    if (!assetPath || !callback) {
        return;
    }

    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        // 从 mainBundle 中查找文件
        NSString *fileName = [assetPath lastPathComponent];
        NSString *fileExtension = [fileName pathExtension];
        NSString *fileNameWithoutExt = [fileName stringByDeletingPathExtension];
        NSString *directory = [assetPath stringByDeletingLastPathComponent];

        NSString *filePath = nil;
        if (directory.length > 0) {
            filePath = [[NSBundle mainBundle] pathForResource:fileNameWithoutExt
                                                      ofType:fileExtension
                                                 inDirectory:directory];
        }
        if (!filePath) {
            filePath = [[NSBundle mainBundle] pathForResource:fileNameWithoutExt
                                                      ofType:fileExtension];
        }

        NSError *error = nil;
        NSString *content = nil;
        if (filePath) {
            content = [NSString stringWithContentsOfFile:filePath
                                               encoding:NSUTF8StringEncoding
                                                  error:&error];
        }

        callback(@{
            @"result": content ?: @"",
            @"error": error.description ?: @""
        });
    });
}

@end
