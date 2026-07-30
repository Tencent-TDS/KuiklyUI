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

#import "KRNavigationController.h"
#import <objc/runtime.h>
#import <QuartzCore/QuartzCore.h>

static const CGFloat kDefaultNavigationBarHeight = 44.0;
static const NSTimeInterval kDefaultAnimationDuration = 0.25;

#pragma mark - KRNavigationBar

@interface KRNavigationBar : NSView

@property (nonatomic, strong) NSButton *backButton;
@property (nonatomic, strong) NSTextField *titleLabel;
@property (nonatomic, copy) void(^backButtonAction)(void);

- (void)setTitle:(NSString *)title;
- (void)setBackButtonVisible:(BOOL)visible;

@end

@implementation KRNavigationBar

- (instancetype)initWithFrame:(NSRect)frameRect {
    self = [super initWithFrame:frameRect];
    if (self) {
        self.wantsLayer = YES;
        self.layer.backgroundColor = [NSColor windowBackgroundColor].CGColor;
        
        // 添加底部边框
        CALayer *bottomBorder = [CALayer layer];
        bottomBorder.backgroundColor = [NSColor separatorColor].CGColor;
        bottomBorder.frame = CGRectMake(0, 0, frameRect.size.width, 1);
        bottomBorder.autoresizingMask = kCALayerWidthSizable;
        [self.layer addSublayer:bottomBorder];
        
        [self setupBackButton];
        [self setupTitleLabel];
    }
    return self;
}

- (void)setupBackButton {
    self.backButton = [[NSButton alloc] initWithFrame:NSMakeRect(8, 8, 80, 28)];
    [self.backButton setTitle:@"< 返回"];
    [self.backButton setBezelStyle:NSBezelStyleRounded];
    [self.backButton setTarget:self];
    [self.backButton setAction:@selector(backButtonClicked:)];
    self.backButton.hidden = YES;
    [self addSubview:self.backButton];
}

- (void)setupTitleLabel {
    self.titleLabel = [[NSTextField alloc] initWithFrame:NSMakeRect(100, 12, 
                                                                     self.bounds.size.width - 200, 20)];
    self.titleLabel.editable = NO;
    self.titleLabel.bordered = NO;
    self.titleLabel.backgroundColor = [NSColor clearColor];
    self.titleLabel.alignment = NSTextAlignmentCenter;
    self.titleLabel.font = [NSFont systemFontOfSize:15 weight:NSFontWeightSemibold];
    self.titleLabel.autoresizingMask = NSViewWidthSizable;
    [self addSubview:self.titleLabel];
}

- (void)backButtonClicked:(id)sender {
    if (self.backButtonAction) {
        self.backButtonAction();
    }
}

- (void)setTitle:(NSString *)title {
    self.titleLabel.stringValue = title ?: @"";
}

- (void)setBackButtonVisible:(BOOL)visible {
    self.backButton.hidden = !visible;
}

@end

#pragma mark - KRNavigationController

@interface KRNavigationController ()

@property (nonatomic, strong) NSMutableArray<NSViewController *> *viewControllersStack;
@property (nonatomic, strong) NSView *containerView;
@property (nonatomic, strong) KRNavigationBar *navigationBar;
@property (nonatomic, assign) BOOL isAnimating;

@end

@implementation KRNavigationController

#pragma mark - Lifecycle

- (instancetype)initWithRootViewController:(NSViewController *)rootViewController {
    NSParameterAssert(rootViewController != nil);
    
    self = [super initWithNibName:nil bundle:nil];
    if (self) {
        _viewControllersStack = [NSMutableArray arrayWithObject:rootViewController];
        _showsNavigationBar = YES;
        _navigationBarHeight = kDefaultNavigationBarHeight;
        _navigationBarBackgroundColor = [NSColor windowBackgroundColor];
        _isAnimating = NO;
    }
    return self;
}

- (void)loadView {
    self.view = [[NSView alloc] initWithFrame:NSMakeRect(0, 0, 800, 600)];
    self.view.wantsLayer = YES;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupNavigationBar];
    [self setupContainerView];
    [self displayViewController:self.viewControllersStack.firstObject animated:NO];
}

#pragma mark - Setup

- (void)setupNavigationBar {
    if (!self.showsNavigationBar) {
        return;
    }
    
    CGFloat width = self.view.bounds.size.width;
    CGFloat height = self.navigationBarHeight;
    self.navigationBar = [[KRNavigationBar alloc] initWithFrame:NSMakeRect(0, 
                                                                           self.view.bounds.size.height - height,
                                                                           width, 
                                                                           height)];
    self.navigationBar.autoresizingMask = NSViewWidthSizable | NSViewMinYMargin;
    
    __weak typeof(self) weakSelf = self;
    self.navigationBar.backButtonAction = ^{
        [weakSelf popViewControllerAnimated:YES];
    };
    
    [self.view addSubview:self.navigationBar];
}

- (void)setupContainerView {
    CGFloat navBarHeight = self.showsNavigationBar ? self.navigationBarHeight : 0;
    CGRect containerFrame = CGRectMake(0, 0, 
                                      self.view.bounds.size.width,
                                      self.view.bounds.size.height - navBarHeight);
    
    self.containerView = [[NSView alloc] initWithFrame:containerFrame];
    self.containerView.wantsLayer = YES;
    self.containerView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    [self.view addSubview:self.containerView];
}

#pragma mark - Properties

- (NSArray<NSViewController *> *)viewControllers {
    return [self.viewControllersStack copy];
}

- (NSViewController *)topViewController {
    return self.viewControllersStack.lastObject;
}

- (NSViewController *)rootViewController {
    return self.viewControllersStack.firstObject;
}

- (void)setShowsNavigationBar:(BOOL)showsNavigationBar {
    if (_showsNavigationBar != showsNavigationBar) {
        _showsNavigationBar = showsNavigationBar;
        if ([self isViewLoaded]) {
            [self.navigationBar removeFromSuperview];
            self.navigationBar = nil;
            if (showsNavigationBar) {
                [self setupNavigationBar];
                [self updateNavigationBar];
            }
            [self updateContainerViewFrame];
        }
    }
}

#pragma mark - Navigation Methods

- (void)pushViewController:(NSViewController *)viewController animated:(BOOL)animated {
    NSParameterAssert(viewController != nil);
    
    if (self.isAnimating) {
        return;
    }
    
    NSViewController *currentVC = self.topViewController;
    [self.viewControllersStack addObject:viewController];
    
    if (animated) {
        self.isAnimating = YES;
        [self transitionFromViewController:currentVC 
                          toViewController:viewController
                                  isPushing:YES
                                 completion:^{
            self.isAnimating = NO;
        }];
    } else {
        [self displayViewController:viewController animated:NO];
    }
    
    [self updateNavigationBar];
}

- (nullable NSViewController *)popViewControllerAnimated:(BOOL)animated {
    if (self.viewControllersStack.count <= 1 || self.isAnimating) {
        return nil;
    }
    
    NSViewController *poppedVC = self.topViewController;
    [self.viewControllersStack removeLastObject];
    NSViewController *newTopVC = self.topViewController;
    
    if (animated) {
        self.isAnimating = YES;
        [self transitionFromViewController:poppedVC 
                          toViewController:newTopVC
                                  isPushing:NO
                                 completion:^{
            [self removeViewController:poppedVC];
            self.isAnimating = NO;
        }];
    } else {
        [self removeViewController:poppedVC];
        [self displayViewController:newTopVC animated:NO];
    }
    
    [self updateNavigationBar];
    return poppedVC;
}

- (nullable NSArray<NSViewController *> *)popToRootViewControllerAnimated:(BOOL)animated {
    if (self.viewControllersStack.count <= 1) {
        return nil;
    }
    
    return [self popToViewController:self.rootViewController animated:animated];
}

- (nullable NSArray<NSViewController *> *)popToViewController:(NSViewController *)viewController 
                                                     animated:(BOOL)animated {
    if (!viewController || ![self.viewControllersStack containsObject:viewController]) {
        return nil;
    }
    
    NSUInteger targetIndex = [self.viewControllersStack indexOfObject:viewController];
    if (targetIndex == self.viewControllersStack.count - 1) {
        return @[];
    }
    
    NSRange removeRange = NSMakeRange(targetIndex + 1, 
                                     self.viewControllersStack.count - targetIndex - 1);
    NSArray *poppedVCs = [self.viewControllersStack subarrayWithRange:removeRange];
    
    NSViewController *currentVC = self.topViewController;
    [self.viewControllersStack removeObjectsInRange:removeRange];
    
    if (animated && currentVC != viewController) {
        self.isAnimating = YES;
        [self transitionFromViewController:currentVC 
                          toViewController:viewController
                                  isPushing:NO
                                 completion:^{
            for (NSViewController *vc in poppedVCs) {
                [self removeViewController:vc];
            }
            self.isAnimating = NO;
        }];
    } else {
        for (NSViewController *vc in poppedVCs) {
            [self removeViewController:vc];
        }
        [self displayViewController:viewController animated:NO];
    }
    
    [self updateNavigationBar];
    return poppedVCs;
}

- (void)setViewControllers:(NSArray<NSViewController *> *)viewControllers animated:(BOOL)animated {
    NSParameterAssert(viewControllers.count > 0);
    
    NSArray *oldVCs = [self.viewControllersStack copy];
    NSViewController *oldTopVC = self.topViewController;
    
    [self.viewControllersStack removeAllObjects];
    [self.viewControllersStack addObjectsFromArray:viewControllers];
    
    NSViewController *newTopVC = self.topViewController;
    
    if (animated && oldTopVC != newTopVC) {
        self.isAnimating = YES;
        [self transitionFromViewController:oldTopVC 
                          toViewController:newTopVC
                                  isPushing:viewControllers.count > oldVCs.count
                                 completion:^{
            for (NSViewController *vc in oldVCs) {
                if (![self.viewControllersStack containsObject:vc]) {
                    [self removeViewController:vc];
                }
            }
            self.isAnimating = NO;
        }];
    } else {
        for (NSViewController *vc in oldVCs) {
            if (![self.viewControllersStack containsObject:vc]) {
                [self removeViewController:vc];
            }
        }
        [self displayViewController:newTopVC animated:NO];
    }
    
    [self updateNavigationBar];
}

#pragma mark - Private Methods

- (void)displayViewController:(NSViewController *)viewController animated:(BOOL)animated {
    if (!viewController) {
        return;
    }
    
    // 移除当前显示的视图控制器的视图
    for (NSView *subview in self.containerView.subviews) {
        [subview removeFromSuperview];
    }
    
    // 添加新的视图控制器
    [self addChildViewController:viewController];
    viewController.view.frame = self.containerView.bounds;
    viewController.view.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    [self.containerView addSubview:viewController.view];
}

- (void)removeViewController:(NSViewController *)viewController {
    [viewController.view removeFromSuperview];
    [viewController removeFromParentViewController];
}

- (void)transitionFromViewController:(NSViewController *)fromVC
                    toViewController:(NSViewController *)toVC
                            isPushing:(BOOL)isPushing
                          completion:(void(^)(void))completion {
    
    if (!fromVC || !toVC) {
        if (completion) completion();
        return;
    }
    
    // 准备新视图
    [self addChildViewController:toVC];
    CGRect containerBounds = self.containerView.bounds;
    toVC.view.frame = containerBounds;
    toVC.view.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    
    // 设置初始位置
    CGFloat offsetX = isPushing ? containerBounds.size.width : -containerBounds.size.width;
    toVC.view.frame = CGRectOffset(containerBounds, offsetX, 0);
    
    [self.containerView addSubview:toVC.view];
    
    // 执行动画
    [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
        context.duration = kDefaultAnimationDuration;
        context.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut];
        
        // 移动新视图到位
        toVC.view.animator.frame = containerBounds;
        
        // 移动旧视图出去
        CGFloat oldOffsetX = isPushing ? -containerBounds.size.width : containerBounds.size.width;
        fromVC.view.animator.frame = CGRectOffset(containerBounds, oldOffsetX, 0);
        
    } completionHandler:^{
        [fromVC.view removeFromSuperview];
        if (completion) {
            completion();
        }
    }];
}

- (void)updateNavigationBar {
    if (!self.showsNavigationBar || !self.navigationBar) {
        return;
    }
    
    NSViewController *topVC = self.topViewController;
    [self.navigationBar setTitle:topVC.title ?: @""];
    [self.navigationBar setBackButtonVisible:self.viewControllersStack.count > 1];
}

- (void)updateContainerViewFrame {
    CGFloat navBarHeight = self.showsNavigationBar ? self.navigationBarHeight : 0;
    CGRect newFrame = CGRectMake(0, 0, 
                                self.view.bounds.size.width,
                                self.view.bounds.size.height - navBarHeight);
    self.containerView.frame = newFrame;
}

@end

#pragma mark - NSViewController Category Implementation

static char kAssociatedNavigationControllerKey;

@implementation NSViewController (KRNavigation)

- (KRNavigationController *)kr_navigationController {
    // 先检查是否直接关联
    KRNavigationController *navController = objc_getAssociatedObject(self, &kAssociatedNavigationControllerKey);
    if (navController) {
        return navController;
    }
    
    // 遍历父视图控制器查找NavigationController
    NSViewController *parent = self.parentViewController;
    while (parent) {
        if ([parent isKindOfClass:[KRNavigationController class]]) {
            // 缓存结果
            objc_setAssociatedObject(self, &kAssociatedNavigationControllerKey, 
                                    parent, OBJC_ASSOCIATION_ASSIGN);
            return (KRNavigationController *)parent;
        }
        parent = parent.parentViewController;
    }
    
    return nil;
}

@end

