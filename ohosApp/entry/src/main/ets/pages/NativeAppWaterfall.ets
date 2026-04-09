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

/**
 * 卡片式风格瀑布流 Demo —— 演示如何在鸿蒙 Native 页面中以 View 粒度嵌入 Kuikly() 组件。
 *
 * 架构关系：
 *   NativeAppWaterfall (ArkTS Page)
 *     └─ WaterFlow (两列瀑布流)
 *          └─ FlowItem (明确指定 width/height)
 *               └─ AppKuiklyCard
 *                    └─ Kuikly(pageName='AppCardPage', pageData={...})
 *                         └─ AppCardPage (Kuikly DSL 页面)
 */

import globalNativeManager from '../kuikly/MyNativeManager';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import { AppKRRenderManager } from '../kuikly/adapters/AppKRRenderManager';
import { KRNativeRenderController, KRRenderNativeMode, KRRecord, Kuikly } from '@kuikly-open/render';
import { display } from '@kit.ArkUI';

// region 卡片数据模型

interface AppCardData {
  imageUrl: string;
  imageHeight: number;
  title: string;
  nickname: string;
  avatarUrl: string;
  likeCount: number;
  tag: string;
  colorIndex: number;
  desc: string;
}

// endregion

// region 瀑布流数据源

class WaterfallDataSource implements IDataSource {
  private cardList: AppCardData[] = [];
  private listeners: DataChangeListener[] = [];

  constructor(cards: AppCardData[]) {
    this.cardList = cards;
  }

  totalCount(): number {
    return this.cardList.length;
  }

  getData(index: number): AppCardData {
    return this.cardList[index];
  }

  registerDataChangeListener(listener: DataChangeListener): void {
    if (this.listeners.indexOf(listener) < 0) {
      this.listeners.push(listener);
    }
  }

  unregisterDataChangeListener(listener: DataChangeListener): void {
    const idx = this.listeners.indexOf(listener);
    if (idx >= 0) {
      this.listeners.splice(idx, 1);
    }
  }
}

// endregion

// region 模拟数据

function buildMockCardList(): AppCardData[] {
  // 数据来源：follow_0.json & follow_1.json（三端保持一致）
  const titles: string[] = [
    '清晨的阳光洒在窗台上',
    '我想你',
    '夏日避暑好去处',
    '苹果发布了最新款 iPhone 15',
    '发现一个绝美露营地',
    '埃塞俄比亚耶加雪菲',
    '我家主子今天又双叒叕拆家了',
    'iPhone15预测 全系C口+钛金属边框',
    '苏州街边偶遇的蟹黄面',
    '封神票房破20亿',
    '北方人第一次见到会飞的蟑螂',
    '年轻人要多学习的真实意思',
    '禁止电动车进电梯',
    '美食推荐 超棒的小吃店',
    '拍摄夜景时的摄影技巧',
    '明星动态 反派角色造型颠覆',
    '健身日常 HIIT 高强度间歇训练',
    '好书推荐 百年孤独',
    '今天带狗狗去公园玩飞盘',
    '音乐分享 迷上了爵士乐'
  ];

  const descs: string[] = [
    '一杯咖啡，一本书，一段静谧的时光。生活不需要太多的喧嚣，简单才是最真实的幸福。',
    '我们这代人最擅长的，就是把"我想你"翻译成"你看月亮了吗"。',
    '分享今日打卡的冷门咖啡馆，空调超足人还少！',
    'A17 芯片性能提升显著，摄像头系统也进行了全面升级。',
    '星空太震撼了！周末去哪玩看这里。',
    '柑橘风味明显！咖啡豆评测分享。',
    '云吸猫协会常任理事的日常记录。',
    '果粉冲吗？全系C口+钛金属边框。',
    '一碗浇了8只蟹的膏黄！碳水爱好者天堂。',
    '乌尔善的选角眼光太毒了！强烈推荐。',
    '当北方人第一次见到会飞的蟑螂时的反应...南北差异太大了。',
    '领导说"年轻人要多学习"的真实意思：下班后免费加班三小时。',
    '建议把标语换成"电动车进电梯会爆炸"，恐惧比道德更有效。',
    '他们家的牛肉面汤底浓郁，牛肉软烂入味，简直是人间美味！',
    '使用三脚架和慢快门可以有效减少噪点，拍出清晰明亮的照片。',
    '据悉，某一线明星将在新剧中挑战反派角色，造型颠覆以往形象。',
    '虽然过程很艰难，但完成后的成就感无与伦比！坚持运动！',
    '被马尔克斯的魔幻现实主义深深吸引。推荐给喜欢文学的朋友们！',
    '结果它太兴奋了，直接把飞盘叼到旁边的小池塘里...还好它自己会游泳！',
    '尤其是 Louis Armstrong 的 What a Wonderful World，每次听都觉得心情特别平静。'
  ];

  const nicknames: string[] = [
    '晨间漫步者', '文字失语症', '快乐小番茄', '数码先知', '旅行小青蛙',
    '咖啡研究所', '喵星人日记', '数码先知', '碳水教父', '院线雷达',
    '迷惑行为大赏', '反卷战士', '人间观察员', '美食探店达人', '光影捕手',
    '娱乐小喇叭', '健身狂魔', '书香满屋', '萌宠日记', '音乐爱好者'
  ];

  const avatarUrls: string[] = [
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45ad086d.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3ecf791d.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d77dc0ad.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8a01b17c.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/891fc305.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/ce73f60a.jpg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7d986b3a.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/007634a8.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b2fc4f8d.jpg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cbf96255.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/e8fc74d5.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7ffe7f72.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/53af4d52.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d15833c3.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4321675f.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b6329f72.png'
  ];

  const tags: string[] = [
    '生活', '文字', '探店', '科技', '露营',
    '咖啡', '萌宠', '数码', '美食', '电影',
    '搞笑', '职场', '观点', '美食', '摄影',
    '娱乐', '健身', '读书', '萌宠', '音乐'
  ];

  // 使用 follow_0/follow_1 中的腾讯 CDN 图片
  const imageUrls: string[] = [
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59591ba6.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8ae4eef2.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/bee80ae7.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cadabbca.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/0b393eef.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/610f6fc3.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/126148bf.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3b504031.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/f36214ee.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/5cdb0696.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d510f14.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/6f1a911f.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d502c511.jpeg',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9a547ff4.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4cdea3e6.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c5b97de3.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45acc362.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/20d212d8.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/533c54a0.png',
    'https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/451b99b9.png'
  ];

  const likeNums: number[] = [
    400, 5300, 256, 800, 320,
    180, 800, 1050, 420, 3800,
    15000, 2200, 6800, 550, 600,
    1200, 700, 450, 600, 380
  ];

  // 参差不齐的图片高度（vp），确保瀑布流效果
  const imageHeights: number[] = [
    180, 230, 280, 180, 230,
    280, 180, 180, 230, 280,
    180, 280, 230, 180, 180,
    230, 180, 280, 180, 280
  ];

  const cards: AppCardData[] = [];
  for (let i = 0; i < 20; i++) {
    cards.push({
      imageUrl: imageUrls[i],
      imageHeight: imageHeights[i],
      title: titles[i],
      nickname: nicknames[i],
      avatarUrl: avatarUrls[i],
      likeCount: likeNums[i],
      tag: tags[i],
      colorIndex: i,
      desc: descs[i]
    });
  }
  return cards;
}

function selectReuseID(height: number) : string {
  if (height == 180) {
    return "small_card"
  } else if (height == 230) {
    return "medium_card"
  }
  return "huge_card"
}

// endregion

@Entry
@Component
struct NativeAppWaterfall {
  private cardList: AppCardData[] = buildMockCardList();
  private dataSource: WaterfallDataSource = new WaterfallDataSource(this.cardList);
  // 瀑布流区域左右 padding 6vp * 2 + 列间距 6vp = 18vp
  private readonly waterfallPaddingGap: number = 18;
  // 卡片底部信息区域高度（标签+标题+描述+底部栏+内间距）
  private readonly cardExtraHeight: number = 132;

  aboutToAppear(): void {
    AppKRRenderManager.getInstance().initIfNeed();
  }

  build() {
    Column() {
      // 顶部搜索栏
      Row() {
        Row() {
          Text('🔍 搜索你感兴趣的内容')
            .fontSize(14)
            .fontColor('#BBBBBB')
        }
        .width('100%')
        .height(34)
        .backgroundColor('#F2F2F2')
        .borderRadius(17)
        .padding({ left: 14, right: 14 })
        .alignItems(VerticalAlign.Center)
      }
      .width('100%')
      .height(50)
      .padding({ left: 12, right: 12 })
      .backgroundColor(Color.White)
      .alignItems(VerticalAlign.Center)

      // 瀑布流
      WaterFlow() {
        LazyForEach(this.dataSource, (card: AppCardData, index: number) => {
          FlowItem() {
            AppKuiklyCard({
              card: card,
              index: index
            })
              .reuseId(selectReuseID(card.imageHeight))
          }
          // 关键：FlowItem 必须明确设置 width 和 height，否则瀑布流布局无法计算
          .width('100%')
          .height(card.imageHeight + this.cardExtraHeight)
          .backgroundColor(Color.White)
          .borderRadius(8)
          .clip(true)
        }, (card: AppCardData, index: number) => index.toString())
      }
      .columnsTemplate('1fr 1fr')
      .columnsGap(6)
      .rowsGap(6)
      .padding(6)
      .width('100%')
      .layoutWeight(1)
      .backgroundColor('#F5F5F5')
      .cachedCount(0)      // 少预加载的范围，增大触发复用的范围
    }
    .width('100%')
    .height('100%')
  }
}

// region 瀑布流卡片组件
@Reusable
@Component
struct AppKuiklyCard {
  @State card: AppCardData = {} as AppCardData;
  @State index: number = 0;
  private delegate = new KuiklyViewDelegate();
  private kuiklyController: KRNativeRenderController | null = null;


  aboutToReuse(params: Record<string, Object>): void {
    this.card = params['card'] as AppCardData;
    this.index = params['index'] as number;
    this.kuiklyController?.sendEvent("CardDataWillChanged", {"data":this.card})
  }

  build() {
    // 卡片总高度 = 封面图高度 + 额外区域（标签行+标题+描述+底部栏+内间距 ≈ 132vp）
    Column() {
      Kuikly({
        pageName: 'AppCardPage',
        pageData: {
          'imageUrl': this.card.imageUrl,
          'imageHeight': `${this.card.imageHeight}`,
          'title': this.card.title,
          'nickname': this.card.nickname,
          'avatarUrl': this.card.avatarUrl,
          'likeCount': `${this.card.likeCount}`,
          'tag': this.card.tag,
          'colorIndex': `${this.card.colorIndex}`,
          'desc': this.card.desc
        } as KRRecord,
        delegate: this.delegate,
        contextCode: '',
        useOhSharedPreferences: true,
        executeMode: KRRenderNativeMode.Native,
        nativeManager: globalNativeManager,
        onControllerReadyCallback: (controller) => {
          this.kuiklyController = controller;
        }
      })
        .width('100%')
        .height('100%')
    }
    .width('100%')
    .height('100%')
  }
}

// endregion
