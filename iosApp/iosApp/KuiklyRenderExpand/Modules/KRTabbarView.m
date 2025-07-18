/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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


#import "KRTabbarView.h"
#import "KRComponentDefine.h"

@interface KRTabbarView ()
@property (nonatomic, strong) UITabBar *tabBar;
@property (nonatomic, copy) NSArray *items;
@property (nonatomic, assign) NSInteger selectedIndex;
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onTabSelected;
@end

@implementation KRTabbarView

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _tabBar = [[UITabBar alloc] initWithFrame:self.bounds];
        _tabBar.delegate = (id<UITabBarDelegate>)self;
        [self addSubview:_tabBar];
    }
    return self;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    _tabBar.frame = self.bounds;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (UIImage *)resizeImage:(UIImage *)image toSize:(CGSize)size {
    UIGraphicsBeginImageContextWithOptions(size, NO, 0.0);
    [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return scaledImage;
}

- (void)setCss_items:(NSArray *)css_items {
    // css_items: [{title, icon, selectedIcon}]
    NSMutableArray *tabBarItems = [NSMutableArray array];
    for (NSDictionary *item in css_items) {
        NSString *title = item[@"title"] ?: @"";
        NSString *icon = item[@"icon"] ?: @"";
        NSString *selectedIcon = item[@"selectedIcon"] ?: @"";
        
        
        UIImage *image = [UIImage imageNamed:icon];
        UIImage *selectedImage = [UIImage imageNamed:selectedIcon];
        UIImage *scaledImg = [self resizeImage:image toSize:CGSizeMake(25.0, 25.0)];
        UIImage *scaledselectedImg = [self resizeImage:selectedImage toSize:CGSizeMake(25.0, 25.0)];
        UITabBarItem *tabBarItem = [[UITabBarItem alloc] initWithTitle:title
                                                                 image:scaledImg
                                                         selectedImage:scaledselectedImg];
        [tabBarItems addObject:tabBarItem];
    }
    self.tabBar.items = tabBarItems;
    self.items = css_items;
}

- (void)setCss_selectedIndex:(NSNumber *)css_selectedIndex {
    NSInteger idx = [css_selectedIndex integerValue];
    self.tabBar.selectedItem = self.tabBar.items[idx];
    self.selectedIndex = idx;
}

- (void)setCss_onTabSelected:(KuiklyRenderCallback)css_onTabSelected {
    _css_onTabSelected = css_onTabSelected;
}

#pragma mark - UITabBarDelegate

- (void)tabBar:(UITabBar *)tabBar didSelectItem:(UITabBarItem *)item {
    NSInteger idx = [tabBar.items indexOfObject:item];
    self.selectedIndex = idx;
    if (self.css_onTabSelected) {
        self.css_onTabSelected(@{@"index": @(idx)});
    }
}

#pragma mark - Kuikly 方法调用

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

- (void)css_setBadge:(NSString *)params {
    // params: {"index": 1, "badge": "99+"}
    NSData *data = [params dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    NSInteger idx = [dict[@"index"] integerValue];
    NSString *badge = dict[@"badge"];
    if (idx < self.tabBar.items.count) {
        UITabBarItem *item = self.tabBar.items[idx];
        item.badgeValue = badge;
    }
}

@end
