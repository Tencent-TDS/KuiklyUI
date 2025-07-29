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

@interface RootViewController ()

@end

@implementation RootViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    UIButton *button = [[UIButton alloc] initWithFrame:CGRectMake(100, 100, 100, 50)];
    [button setTitle:@"normal" forState:UIControlStateNormal];
    [button setTitle:@"highlight" forState:UIControlStateHighlighted];
    button.backgroundColor = [UIColor redColor];
    [button addTarget:self action:@selector(gotoxx) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:button];
    
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


/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
