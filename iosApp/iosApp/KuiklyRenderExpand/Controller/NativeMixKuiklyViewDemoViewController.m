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

#import "NativeMixKuiklyViewDemoViewController.h"
#import "KuiklyBaseView.h"

@interface NativeMixKuiklyViewDemoViewController ()<KuiklyViewBaseDelegate>

@property (nonatomic, strong) KuiklyBaseView *kuiklyBaseView;
@property (nonatomic, strong) UIView *bgMaskView;

@end

@implementation NativeMixKuiklyViewDemoViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor whiteColor];
    
    UIButton *button = [UIButton buttonWithType:(UIButtonTypeSystem)];
    [button setTitle:@"点我显示一个KuiklyView" forState:(UIControlStateNormal)];
    [button.titleLabel sizeToFit];
    [button addTarget:self action:@selector(onClickButtonWithSender:) forControlEvents:(UIControlEventTouchUpInside)];
    
    button.frame = CGRectMake(CGRectGetMidX(self.view.frame) - 100, 100, 200, 40);
    
    [self.view addSubview:button];
    // Do any additional setup after loading the view.
}



- (void)onClickButtonWithSender:(id)sender {
    // 添加一个背景蒙层
    _bgMaskView = [[UIView alloc] initWithFrame:self.view.bounds];
    _bgMaskView.backgroundColor = [UIColor blackColor];
    _bgMaskView.alpha = 0;
    UITapGestureRecognizer *tapGR = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(onClickBgMaskViewWithSender:)];
    [_bgMaskView addGestureRecognizer:tapGR];
    [self.view addSubview:_bgMaskView];
    
    CGFloat height = 400;
    CGRect frame = CGRectMake(0, CGRectGetHeight(self.view.bounds), CGRectGetWidth(self.view.bounds), height);
    // 创建一个kuiklyView放在当前原生页面底部，可以完全当做系统的UIView派生类去使用
    _kuiklyBaseView = [[KuiklyBaseView alloc] initWithFrame:frame
                                           pageName:@"ViewExamplePage" // 随便找一个kuikly kmm工程侧写好的@page页面
                                           pageData:@{}
                                           delegate:self
                                      frameworkName:@"shared"];
    [self.view addSubview:_kuiklyBaseView];
    [_kuiklyBaseView viewWillAppear];
    [UIView animateWithDuration:0.25 animations:^{
        self.kuiklyBaseView.frame = CGRectMake(0, CGRectGetHeight(self.view.bounds) - height, CGRectGetWidth(self.view.bounds), height);
        self.bgMaskView.alpha = 0.5f;
        [self.kuiklyBaseView viewDidAppear];
    }];
}

// 点击背景蒙层
- (void)onClickBgMaskViewWithSender:(id)sender {
    [_kuiklyBaseView viewWillDisappear];
    [UIView animateWithDuration:0.25 animations:^{
        self.kuiklyBaseView.frame = CGRectMake(0, CGRectGetHeight(self.view.bounds), CGRectGetWidth(self.view.bounds), CGRectGetHeight(self.kuiklyBaseView.frame));
        self.bgMaskView.alpha = 0;
    } completion:^(BOOL finished) {
        [self.kuiklyBaseView removeFromSuperview];
        [self.bgMaskView removeFromSuperview];
        [self.kuiklyBaseView viewDidDisappear];
        self.kuiklyBaseView = nil;
        self.bgMaskView = nil;
    }];
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
