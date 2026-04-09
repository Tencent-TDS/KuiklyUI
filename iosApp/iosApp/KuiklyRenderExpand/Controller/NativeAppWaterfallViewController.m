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

#import "NativeAppWaterfallViewController.h"
#import "KuiklyBaseView.h"

static NSInteger const kColumnCount = 2;
static CGFloat const kItemSpacing = 6.0;
static CGFloat const kLineSpacing = 6.0;
static CGFloat const kContentInset = 6.0;
static CGFloat const kSearchBarHeight = 50.0;

#pragma mark - Card Data Model

@interface AppCardModel : NSObject

@property (nonatomic, copy) NSString *imageUrl;
@property (nonatomic, assign) CGFloat imageHeight;
@property (nonatomic, copy) NSString *title;
@property (nonatomic, copy) NSString *nickname;
@property (nonatomic, copy) NSString *avatarUrl;
@property (nonatomic, assign) NSInteger likeCount;
@property (nonatomic, copy) NSString *tag;
@property (nonatomic, assign) NSInteger colorIndex;
@property (nonatomic, copy) NSString *desc;

@end

@implementation AppCardModel
@end

#pragma mark - Waterfall Layout

/// 简单的瀑布流 UICollectionViewLayout（两列，最短列优先）
@interface AppWaterfallLayout : UICollectionViewLayout

@property (nonatomic, assign) NSInteger columnCount;
@property (nonatomic, assign) CGFloat itemSpacing;
@property (nonatomic, assign) CGFloat lineSpacing;
@property (nonatomic, assign) UIEdgeInsets sectionInset;
/// 外部提供每个 item 的高度
@property (nonatomic, copy) CGFloat (^itemHeightBlock)(NSIndexPath *indexPath, CGFloat itemWidth);

@end

@interface AppWaterfallLayout ()
@property (nonatomic, strong) NSMutableArray<UICollectionViewLayoutAttributes *> *attributesArray;
@property (nonatomic, strong) NSMutableArray<NSNumber *> *columnHeights;
@end

@implementation AppWaterfallLayout

- (void)prepareLayout {
    [super prepareLayout];
    
    _attributesArray = [NSMutableArray array];
    _columnHeights = [NSMutableArray array];
    for (NSInteger i = 0; i < _columnCount; i++) {
        [_columnHeights addObject:@(self.sectionInset.top)];
    }
    
    NSInteger itemCount = [self.collectionView numberOfItemsInSection:0];
    CGFloat totalWidth = self.collectionView.bounds.size.width - self.sectionInset.left - self.sectionInset.right;
    CGFloat itemWidth = (totalWidth - (_columnCount - 1) * _itemSpacing) / _columnCount;
    
    for (NSInteger i = 0; i < itemCount; i++) {
        NSIndexPath *indexPath = [NSIndexPath indexPathForItem:i inSection:0];
        
        // 找最短列
        NSInteger shortestColumn = 0;
        CGFloat minHeight = [_columnHeights[0] floatValue];
        for (NSInteger col = 1; col < _columnCount; col++) {
            CGFloat h = [_columnHeights[col] floatValue];
            if (h < minHeight) {
                minHeight = h;
                shortestColumn = col;
            }
        }
        
        CGFloat x = self.sectionInset.left + shortestColumn * (itemWidth + _itemSpacing);
        CGFloat y = [_columnHeights[shortestColumn] floatValue];
        if (y > self.sectionInset.top) {
            y += _lineSpacing;
        }
        
        CGFloat itemHeight = 200; // 默认高度
        if (self.itemHeightBlock) {
            itemHeight = self.itemHeightBlock(indexPath, itemWidth);
        }
        
        UICollectionViewLayoutAttributes *attrs = [UICollectionViewLayoutAttributes layoutAttributesForCellWithIndexPath:indexPath];
        attrs.frame = CGRectMake(x, y, itemWidth, itemHeight);
        [_attributesArray addObject:attrs];
        
        _columnHeights[shortestColumn] = @(CGRectGetMaxY(attrs.frame));
    }
}

- (NSArray<UICollectionViewLayoutAttributes *> *)layoutAttributesForElementsInRect:(CGRect)rect {
    NSMutableArray *result = [NSMutableArray array];
    for (UICollectionViewLayoutAttributes *attrs in _attributesArray) {
        if (CGRectIntersectsRect(attrs.frame, rect)) {
            [result addObject:attrs];
        }
    }
    return result;
}

- (UICollectionViewLayoutAttributes *)layoutAttributesForItemAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.item < (NSInteger)_attributesArray.count) {
        return _attributesArray[indexPath.item];
    }
    return nil;
}

- (CGSize)collectionViewContentSize {
    CGFloat maxHeight = 0;
    for (NSNumber *h in _columnHeights) {
        if ([h floatValue] > maxHeight) {
            maxHeight = [h floatValue];
        }
    }
    return CGSizeMake(self.collectionView.bounds.size.width, maxHeight + self.sectionInset.bottom);
}

- (BOOL)shouldInvalidateLayoutForBoundsChange:(CGRect)newBounds {
    return NO;
}

@end

#pragma mark - Waterfall Cell

/// 每个 Cell 内嵌一个 KuiklyBaseView，承载独立的 @Page("AppCardPage")
@interface AppWaterfallCell : UICollectionViewCell <KuiklyViewBaseDelegate>

@property (nonatomic, strong) KuiklyBaseView *kuiklyView;
@property (nonatomic, strong) AppCardModel *cardModel;

- (void)configureWithCard:(AppCardModel *)card cellSize:(CGSize)cellSize;

@end

@implementation AppWaterfallCell

- (void)prepareForReuse {
    [super prepareForReuse];
    // 注意：不要在这里销毁 KuiklyBaseView！
    // Cell 复用时应该保留 KuiklyBaseView，只更新数据
    // 销毁操作改为在 dealloc 中进行
}

- (void)configureWithCard:(AppCardModel *)card cellSize:(CGSize)cellSize {
    _cardModel = card;
    
    // 构造传给 @Page 的 pageData
    NSDictionary *pageData = @{
        @"imageUrl": card.imageUrl ?: @"",
        @"imageHeight": @(card.imageHeight),
        @"title": card.title ?: @"",
        @"nickname": card.nickname ?: @"",
        @"avatarUrl": card.avatarUrl ?: @"",
        @"likeCount": @(card.likeCount),
        @"tag": card.tag ?: @"",
        @"colorIndex": @(card.colorIndex),
        @"desc": card.desc ?: @""
    };
    
    if (!_kuiklyView) {
        // 首次创建：初始化 KuiklyBaseView
        _kuiklyView = [[KuiklyBaseView alloc] initWithFrame:CGRectMake(0, 0, cellSize.width, cellSize.height)
                                                   pageName:@"AppCardPage"
                                                   pageData:pageData
                                                   delegate:self
                                              frameworkName:@"shared"];
        _kuiklyView.layer.cornerRadius = 8.0;
        _kuiklyView.clipsToBounds = YES;
        [self.contentView addSubview:_kuiklyView];
        
        // 给 cell 添加阴影效果
        self.contentView.layer.cornerRadius = 8.0;
        self.layer.shadowColor = [UIColor blackColor].CGColor;
        self.layer.shadowOffset = CGSizeMake(0, 1);
        self.layer.shadowRadius = 3.0;
        self.layer.shadowOpacity = 0.1;
        self.layer.masksToBounds = NO;
        
        // 触发生命周期
        [_kuiklyView viewWillAppear];
        [_kuiklyView viewDidAppear];
    } else {
        // Cell 复用：只更新数据，不销毁重建
        // 更新 frame（如果高度变化了）
        _kuiklyView.frame = CGRectMake(0, 0, cellSize.width, cellSize.height);
        // 发送事件通知 KuiklyDSL 层更新数据
        [_kuiklyView sendWithEvent:@"CardDataWillChanged" data:@{@"data": pageData}];
    }
}

#pragma mark - KuiklyViewBaseDelegate

- (void)fetchContextCodeWithPageName:(NSString *)pageName resultCallback:(KuiklyContextCodeCallback)callback {
    if (callback) {
        callback(@"shared", nil);
    }
}

- (UIView *)createLoadingView {
    UIView *v = [[UIView alloc] init];
    v.backgroundColor = [UIColor colorWithWhite:0.95 alpha:1.0];
    return v;
}

- (UIView *)createErrorView {
    UIView *v = [[UIView alloc] init];
    v.backgroundColor = [UIColor clearColor];
    return v;
}

- (void)contentViewDidLoad {
    // 卡片首屏上屏
}

- (void)onPageLoadComplete:(BOOL)isSucceed error:(NSError *)error mode:(KuiklyContextMode)mode {
    if (!isSucceed) {
        NSLog(@"[App] Card page load failed: %@", error.localizedDescription);
    }
}

@end

#pragma mark - ViewController

@interface NativeAppWaterfallViewController () <UICollectionViewDataSource, UICollectionViewDelegate>

@property (nonatomic, strong) UICollectionView *collectionView;
@property (nonatomic, strong) AppWaterfallLayout *waterfallLayout;
@property (nonatomic, strong) NSMutableArray<AppCardModel *> *cardList;

@end

@implementation NativeAppWaterfallViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor colorWithRed:0.96 green:0.96 blue:0.96 alpha:1.0];
    self.title = @"卡片式 Native 瀑布流";
    
    // 准备模拟数据
    [self setupData];
    // 搜索栏
    [self setupSearchBar];
    // 瀑布流
    [self setupCollectionView];
}

#pragma mark - Data

- (void)setupData {
    _cardList = [NSMutableArray array];
    
    // 数据来源：follow_0.json & follow_1.json（三端保持一致）
    NSArray *titles = @[
        @"清晨的阳光洒在窗台上",
        @"我想你",
        @"夏日避暑好去处",
        @"苹果发布了最新款 iPhone 15",
        @"发现一个绝美露营地",
        @"埃塞俄比亚耶加雪菲",
        @"我家主子今天又双叒叕拆家了",
        @"iPhone15预测 全系C口+钛金属边框",
        @"苏州街边偶遇的蟹黄面",
        @"封神票房破20亿",
        @"北方人第一次见到会飞的蟑螂",
        @"年轻人要多学习的真实意思",
        @"禁止电动车进电梯",
        @"美食推荐 超棒的小吃店",
        @"拍摄夜景时的摄影技巧",
        @"明星动态 反派角色造型颠覆",
        @"健身日常 HIIT 高强度间歇训练",
        @"好书推荐 百年孤独",
        @"今天带狗狗去公园玩飞盘",
        @"音乐分享 迷上了爵士乐"
    ];
    
    NSArray *descs = @[
        @"一杯咖啡，一本书，一段静谧的时光。生活不需要太多的喧嚣，简单才是最真实的幸福。",
        @"我们这代人最擅长的，就是把\"我想你\"翻译成\"你看月亮了吗\"。",
        @"分享今日打卡的冷门咖啡馆，空调超足人还少！",
        @"A17 芯片性能提升显著，摄像头系统也进行了全面升级。",
        @"星空太震撼了！周末去哪玩看这里。",
        @"柑橘风味明显！咖啡豆评测分享。",
        @"云吸猫协会常任理事的日常记录。",
        @"果粉冲吗？全系C口+钛金属边框。",
        @"一碗浇了8只蟹的膏黄！碳水爱好者天堂。",
        @"乌尔善的选角眼光太毒了！强烈推荐。",
        @"当北方人第一次见到会飞的蟑螂时的反应...南北差异太大了。",
        @"领导说\"年轻人要多学习\"的真实意思：下班后免费加班三小时。",
        @"建议把标语换成\"电动车进电梯会爆炸\"，恐惧比道德更有效。",
        @"他们家的牛肉面汤底浓郁，牛肉软烂入味，简直是人间美味！",
        @"使用三脚架和慢快门可以有效减少噪点，拍出清晰明亮的照片。",
        @"据悉，某一线明星将在新剧中挑战反派角色，造型颠覆以往形象。",
        @"虽然过程很艰难，但完成后的成就感无与伦比！坚持运动！",
        @"被马尔克斯的魔幻现实主义深深吸引。推荐给喜欢文学的朋友们！",
        @"结果它太兴奋了，直接把飞盘叼到旁边的小池塘里...还好它自己会游泳！",
        @"尤其是 Louis Armstrong 的 What a Wonderful World，每次听都觉得心情特别平静。"
    ];
    
    NSArray *nicknames = @[
        @"晨间漫步者", @"文字失语症", @"快乐小番茄", @"数码先知", @"旅行小青蛙",
        @"咖啡研究所", @"喵星人日记", @"数码先知", @"碳水教父", @"院线雷达",
        @"迷惑行为大赏", @"反卷战士", @"人间观察员", @"美食探店达人", @"光影捕手",
        @"娱乐小喇叭", @"健身狂魔", @"书香满屋", @"萌宠日记", @"音乐爱好者"
    ];
    
    NSArray *avatarUrls = @[
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45ad086d.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3ecf791d.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d77dc0ad.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8a01b17c.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/891fc305.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/ce73f60a.jpg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7d986b3a.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/007634a8.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b2fc4f8d.jpg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cbf96255.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/e8fc74d5.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7ffe7f72.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/53af4d52.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d15833c3.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4321675f.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b6329f72.png"
    ];
    
    NSArray *tags = @[
        @"生活", @"文字", @"探店", @"科技", @"露营",
        @"咖啡", @"萌宠", @"数码", @"美食", @"电影",
        @"搞笑", @"职场", @"观点", @"美食", @"摄影",
        @"娱乐", @"健身", @"读书", @"萌宠", @"音乐"
    ];
    
    // 使用 follow_0/follow_1 中的腾讯 CDN 图片
    NSArray *imageUrls = @[
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59591ba6.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8ae4eef2.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/bee80ae7.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cadabbca.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/0b393eef.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/610f6fc3.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/126148bf.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3b504031.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/f36214ee.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/5cdb0696.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d510f14.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/6f1a911f.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d502c511.jpeg",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9a547ff4.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4cdea3e6.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c5b97de3.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45acc362.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/20d212d8.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/533c54a0.png",
        @"https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/451b99b9.png"
    ];
    
    NSInteger likeNums[] = {
        400, 5300, 256, 800, 320,
        180, 800, 1050, 420, 3800,
        15000, 2200, 6800, 550, 600,
        1200, 700, 450, 600, 380
    };
    
    // 参差不齐的图片高度（确保瀑布流效果）
    CGFloat heights[] = {
        180, 220, 160, 240, 200,
        250, 170, 210, 190, 230,
        260, 150, 200, 220, 180,
        240, 170, 230, 190, 210
    };
    
    for (NSInteger i = 0; i < 20; i++) {
        AppCardModel *card = [[AppCardModel alloc] init];
        card.imageUrl = imageUrls[i];
        card.imageHeight = heights[i];
        card.title = titles[i];
        card.nickname = nicknames[i];
        card.avatarUrl = avatarUrls[i];
        card.likeCount = likeNums[i];
        card.tag = tags[i];
        card.colorIndex = i;
        card.desc = descs[i];
        [_cardList addObject:card];
    }
}

#pragma mark - UI Setup

- (void)setupSearchBar {
    CGFloat statusBarHeight = 0;
    if (@available(iOS 13.0, *)) {
        UIWindowScene *scene = (UIWindowScene *)UIApplication.sharedApplication.connectedScenes.allObjects.firstObject;
        statusBarHeight = scene.statusBarManager.statusBarFrame.size.height;
    } else {
        statusBarHeight = UIApplication.sharedApplication.statusBarFrame.size.height;
    }
    CGFloat navHeight = self.navigationController.navigationBar.frame.size.height;
    CGFloat topOffset = statusBarHeight + navHeight;
    
    UIView *searchBar = [[UIView alloc] initWithFrame:CGRectMake(0, topOffset, CGRectGetWidth(self.view.bounds), kSearchBarHeight)];
    searchBar.backgroundColor = [UIColor whiteColor];
    
    UIView *searchBox = [[UIView alloc] initWithFrame:CGRectMake(12, 8, CGRectGetWidth(self.view.bounds) - 24, 34)];
    searchBox.backgroundColor = [UIColor colorWithRed:0.95 green:0.95 blue:0.95 alpha:1.0];
    searchBox.layer.cornerRadius = 17;
    
    UILabel *placeholder = [[UILabel alloc] initWithFrame:CGRectMake(14, 0, CGRectGetWidth(searchBox.frame) - 28, 34)];
    placeholder.text = @"🔍 搜索你感兴趣的内容";
    placeholder.font = [UIFont systemFontOfSize:14];
    placeholder.textColor = [UIColor colorWithWhite:0.73 alpha:1.0];
    [searchBox addSubview:placeholder];
    
    [searchBar addSubview:searchBox];
    [self.view addSubview:searchBar];
}

- (void)setupCollectionView {
    CGFloat statusBarHeight = 0;
    if (@available(iOS 13.0, *)) {
        UIWindowScene *scene = (UIWindowScene *)UIApplication.sharedApplication.connectedScenes.allObjects.firstObject;
        statusBarHeight = scene.statusBarManager.statusBarFrame.size.height;
    } else {
        statusBarHeight = UIApplication.sharedApplication.statusBarFrame.size.height;
    }
    CGFloat navHeight = self.navigationController.navigationBar.frame.size.height;
    CGFloat topOffset = statusBarHeight + navHeight + kSearchBarHeight;
    
    // 瀑布流 Layout
    __weak typeof(self) weakSelf = self;
    _waterfallLayout = [[AppWaterfallLayout alloc] init];
    _waterfallLayout.columnCount = kColumnCount;
    _waterfallLayout.itemSpacing = kItemSpacing;
    _waterfallLayout.lineSpacing = kLineSpacing;
    _waterfallLayout.sectionInset = UIEdgeInsetsMake(kContentInset, kContentInset, kContentInset, kContentInset);
    _waterfallLayout.itemHeightBlock = ^CGFloat(NSIndexPath *indexPath, CGFloat itemWidth) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf || indexPath.item >= (NSInteger)strongSelf.cardList.count) return 200;
        AppCardModel *card = strongSelf.cardList[indexPath.item];
        // 卡片总高度 = 封面图高度 + 标签行(~20) + 标题(~36) + 描述(~30) + 底部栏(~30) + 内间距(~16)
        CGFloat extraHeight = 132.0;
        return card.imageHeight + extraHeight;
    };
    
    CGRect frame = CGRectMake(0, topOffset, CGRectGetWidth(self.view.bounds), CGRectGetHeight(self.view.bounds) - topOffset);
    _collectionView = [[UICollectionView alloc] initWithFrame:frame collectionViewLayout:_waterfallLayout];
    _collectionView.backgroundColor = [UIColor clearColor];
    _collectionView.dataSource = self;
    _collectionView.delegate = self;
    _collectionView.showsVerticalScrollIndicator = NO;
    [_collectionView registerClass:[AppWaterfallCell class] forCellWithReuseIdentifier:@"AppWaterfallCell"];
    
    [self.view addSubview:_collectionView];
}

#pragma mark - UICollectionViewDataSource

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section {
    return _cardList.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath {
    AppWaterfallCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"AppWaterfallCell" forIndexPath:indexPath];
    
    AppCardModel *card = _cardList[indexPath.item];
    // 计算 cell 尺寸（需要与 layout 一致）
    CGFloat totalWidth = collectionView.bounds.size.width - kContentInset * 2;
    CGFloat itemWidth = (totalWidth - (kColumnCount - 1) * kItemSpacing) / kColumnCount;
    CGFloat itemHeight = card.imageHeight + 132.0;
    
    [cell configureWithCard:card cellSize:CGSizeMake(itemWidth, itemHeight)];
    
    return cell;
}

@end
