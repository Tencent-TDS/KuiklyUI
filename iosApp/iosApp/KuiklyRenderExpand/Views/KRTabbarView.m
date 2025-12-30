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


#import "KRTabbarView.h"
#import "KRComponentDefine.h"
#import "NSObject+KR.h"

extern NSString *const KRImageAssetsPrefix;

/**
 * KRTabbarView Implementation
 * 
 * A Kuikly extension component that wraps iOS native UITabBar.
 * Provides declarative interface for TabBar functionality including:
 * - Dynamic tab items configuration
 * - Icon and selected icon support
 * - Badge value management
 * - Tab selection callbacks
 */
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

- (void)setCss_items:(NSString *)css_items {
    // css_items: [{title, icon, selectedIcon}]
    NSMutableArray *tabBarItems = [NSMutableArray array];
    // Parse the JSON string parameter into an array
    NSArray *itemsArray = [css_items hr_stringToArray];
    for (NSDictionary *item in itemsArray) {
        NSString *title = item[@"title"] ?: @"";
        NSString *iconCssPath = item[@"icon"] ?: @"";
        NSString *selectedIconCssPath = item[@"selectedIcon"] ?: @"";
        
        // 创建TabBarItem
        UITabBarItem *tabBarItem = [self createTabBarItemWithTitle:title
                                                              icon:iconCssPath
                                                      selectedIcon:selectedIconCssPath];
        [tabBarItems addObject:tabBarItem];
    }
    self.tabBar.items = tabBarItems;
    self.items = itemsArray;
}

- (void)setCss_selectedIndex:(NSNumber *)css_selectedIndex {
    NSInteger idx = [css_selectedIndex integerValue];
    if (idx >= 0 && idx < self.tabBar.items.count) {
        self.tabBar.selectedItem = self.tabBar.items[idx];
        self.selectedIndex = idx;
    }
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

#pragma mark - Private Helper Methods

- (UIImage *)resizeImage:(UIImage *)image toSize:(CGSize)size {
    if (!image) return nil;
    
    UIGraphicsBeginImageContextWithOptions(size, NO, 0.0);
    [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return scaledImage;
}

- (UIImage *)loadImageFromPath:(NSString *)cssPath {
    if (!cssPath || cssPath.length == 0) return nil;
    
    // 检查路径是否以KRImageAssetsPrefix开头
    if (![cssPath hasPrefix:KRImageAssetsPrefix]) {
        NSLog(@"Warning: Image path does not start with expected prefix: %@", cssPath);
        return nil;
    }
    
    // 提取资源路径
    NSRange subRange = NSMakeRange(KRImageAssetsPrefix.length, cssPath.length - KRImageAssetsPrefix.length);
    if (subRange.length <= 0) return nil;
    
    NSString *path = [cssPath substringWithRange:subRange];
    NSString *fileName = [path stringByDeletingPathExtension];
    NSString *extension = [cssPath pathExtension];
    
    // 从Bundle加载图片
    NSURL *imageUrl = [[NSBundle mainBundle] URLForResource:fileName withExtension:extension];
    if (!imageUrl) {
        NSLog(@"Warning: Could not find image resource: %@.%@", fileName, extension);
        return nil;
    }
    
    UIImage *image = [UIImage imageWithContentsOfFile:imageUrl.path];
    return image;
}

- (UITabBarItem *)createTabBarItemWithTitle:(NSString *)title
                                       icon:(NSString *)iconPath
                               selectedIcon:(NSString *)selectedIconPath {
    // 加载图片
    UIImage *iconImage = [self loadImageFromPath:iconPath];
    UIImage *selectedIconImage = [self loadImageFromPath:selectedIconPath];
    
    // 缩放图片到标准尺寸
    static const CGFloat kTabBarIconSize = 25.0;
    CGSize iconSize = CGSizeMake(kTabBarIconSize, kTabBarIconSize);
    
    UIImage *scaledIcon = [self resizeImage:iconImage toSize:iconSize];
    UIImage *scaledSelectedIcon = [self resizeImage:selectedIconImage toSize:iconSize];
    
    // 创建TabBarItem
    UITabBarItem *tabBarItem = [[UITabBarItem alloc] initWithTitle:title
                                                             image:scaledIcon
                                                     selectedImage:scaledSelectedIcon];
    return tabBarItem;
}

@end
