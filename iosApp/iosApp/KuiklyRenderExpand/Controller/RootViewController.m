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

#import "RootViewController.h"
#import "KuiklyRenderViewController.h"
#import <os/signpost.h>
#import <sys/kdebug_signpost.h>

// Instruments 性能分析标识
os_log_t textPerfLog;
os_signpost_id_t textPerfSignpostId;

@interface RootViewController ()

@end

@implementation RootViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    // 初始化 Instruments 性能分析日志
    textPerfLog = os_log_create("com.kuikly.textperf", "TextPerformance");
    textPerfSignpostId = os_signpost_id_generate(textPerfLog);
    
    // 监听 Text 性能测试完成通知
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onTextPerfAllLayoutComplete:)
                                                 name:@"TextPerfAllLayoutComplete"
                                               object:nil];
    
    UIButton *button = [[UIButton alloc] initWithFrame:CGRectMake(100, 100, 100, 50)];
    [button setTitle:@"normal" forState:UIControlStateNormal];
    [button setTitle:@"highlight" forState:UIControlStateHighlighted];
    button.backgroundColor = [UIColor redColor];
    [button addTarget:self action:@selector(gotoxx) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:button];
    
    // Compose DSL Text 性能测试按钮
    UIButton *composeDslBtn = [[UIButton alloc] initWithFrame:CGRectMake(100, 200, 180, 50)];
    [composeDslBtn setTitle:@"Compose DSL Text" forState:UIControlStateNormal];
    composeDslBtn.backgroundColor = [UIColor systemBlueColor];
    composeDslBtn.layer.cornerRadius = 8;
    [composeDslBtn addTarget:self action:@selector(gotoComposeTextPerf) forControlEvents:UIControlEventTouchDown];
    [self.view addSubview:composeDslBtn];
    
    // Kuikly DSL Text 性能测试按钮
    UIButton *kuiklyDslBtn = [[UIButton alloc] initWithFrame:CGRectMake(100, 270, 180, 50)];
    [kuiklyDslBtn setTitle:@"Kuikly DSL Text" forState:UIControlStateNormal];
    kuiklyDslBtn.backgroundColor = [UIColor systemGreenColor];
    kuiklyDslBtn.layer.cornerRadius = 8;
    [kuiklyDslBtn addTarget:self action:@selector(gotoKuiklyTextPerf) forControlEvents:UIControlEventTouchDown];
    [self.view addSubview:kuiklyDslBtn];
}

- (void)gotoxx {
//    FlutterViewController *flutterViewController =
//        [[FlutterViewController alloc] initWithEngine:flutterEngine nibName:nil bundle:nil];
//    flutterViewController.modalPresentationStyle = UIModalPresentationFullScreen;
//    [[TFlutterLaunchMonitor sharedInstance] onEnterFlutter];
//    id rootVC = [[[UIApplication sharedApplication] keyWindow] rootViewController];
//    [rootVC presentViewController:flutterViewController animated:YES completion:nil];

    KuiklyRenderViewController *kuiklyVc = [[KuiklyRenderViewController alloc] initWithPageName:@"WBTabPage" pageData:@{}];
    kuiklyVc.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:kuiklyVc animated:YES completion:nil];
    
    //        let hrVC = KuiklyRenderViewController(pageName: pageName, pageData: data)
    UIButton *dismissbtn = [[UIButton alloc] initWithFrame:CGRectMake(50, 300, 50, 50)];
    dismissbtn.backgroundColor = [UIColor redColor];
    [kuiklyVc.view addSubview:dismissbtn];
    [dismissbtn addTarget:self action:@selector(dismiss) forControlEvents:UIControlEventTouchUpInside];
}

- (void)dismiss {
    id rootVC = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    [rootVC dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - Text Performance Test

- (void)gotoComposeTextPerf {
    // Instruments 性能分析起点标识 - 使用 kdebug_signpost 在时间线上标记
    kdebug_signpost_start(1, 0, 0, 0, 1); // code=1 表示 Compose DSL
    os_signpost_interval_begin(textPerfLog, textPerfSignpostId, "ComposeTextPerf", "Start Compose DSL Text Performance Test");
    // 同时发送一个 Point of Interest 事件
    os_signpost_event_emit(textPerfLog, textPerfSignpostId, "ComposeTextStart", ">>> Compose DSL Text Start <<<");
    NSLog(@"[TextPerf] Begin Compose DSL Text Performance Test");
    
    KuiklyRenderViewController *kuiklyVc = [[KuiklyRenderViewController alloc] initWithPageName:@"TextPerfComposeDemo" pageData:@{}];
    kuiklyVc.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:kuiklyVc animated:NO completion:nil];
    
    [self addDismissButtonToVC:kuiklyVc];
}

- (void)gotoKuiklyTextPerf {
    // Instruments 性能分析起点标识 - 使用 kdebug_signpost 在时间线上标记
    kdebug_signpost_start(2, 0, 0, 0, 2); // code=2 表示 Kuikly DSL
    os_signpost_interval_begin(textPerfLog, textPerfSignpostId, "KuiklyTextPerf", "Start Kuikly DSL Text Performance Test");
    // 同时发送一个 Point of Interest 事件
    os_signpost_event_emit(textPerfLog, textPerfSignpostId, "KuiklyTextStart", ">>> Kuikly DSL Text Start <<<");
    NSLog(@"[TextPerf] Begin Kuikly DSL Text Performance Test");
    
    KuiklyRenderViewController *kuiklyVc = [[KuiklyRenderViewController alloc] initWithPageName:@"TextPerfKuiklyDemo" pageData:@{}];
    kuiklyVc.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:kuiklyVc animated:NO completion:nil];
    
    [self addDismissButtonToVC:kuiklyVc];
}

- (void)addDismissButtonToVC:(UIViewController *)vc {
    UIButton *dismissbtn = [[UIButton alloc] initWithFrame:CGRectMake(50, 300, 50, 50)];
    dismissbtn.backgroundColor = [UIColor redColor];
    dismissbtn.layer.cornerRadius = 25;
    [vc.view addSubview:dismissbtn];
    [dismissbtn addTarget:self action:@selector(dismiss) forControlEvents:UIControlEventTouchUpInside];
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

#pragma mark - Text Performance Notification

- (void)onTextPerfAllLayoutComplete:(NSNotification *)notification {
    NSDictionary *userInfo = notification.userInfo;
    NSString *type = userInfo[@"type"] ?: @"Unknown";
    NSNumber *total = userInfo[@"total"] ?: @0;
    
    if ([type isEqualToString:@"Compose"]) {
        // Compose DSL 性能测试结束标识
        kdebug_signpost_end(1, 0, 0, 0, 1);
        os_signpost_interval_end(textPerfLog, textPerfSignpostId, "ComposeTextPerf", "End Compose DSL - All %d Text Layout Complete", total.intValue);
        os_signpost_event_emit(textPerfLog, textPerfSignpostId, "ComposeTextEnd", "<<< Compose DSL Text End <<<");
        NSLog(@"[TextPerf] End Compose DSL - All %@ Text Layout Complete", total);
    } else if ([type isEqualToString:@"Kuikly"]) {
        // Kuikly DSL 性能测试结束标识
        kdebug_signpost_end(2, 0, 0, 0, 2);
        os_signpost_interval_end(textPerfLog, textPerfSignpostId, "KuiklyTextPerf", "End Kuikly DSL - All %d Text Layout Complete", total.intValue);
        os_signpost_event_emit(textPerfLog, textPerfSignpostId, "KuiklyTextEnd", "<<< Kuikly DSL Text End <<<");
        NSLog(@"[TextPerf] End Kuikly DSL - All %@ Text Layout Complete", total);
    }
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
