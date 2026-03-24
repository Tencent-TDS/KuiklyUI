
import globalNativeManager from '../kuikly/MyNativeManager';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import { AppKRRenderManager } from '../kuikly/adapters/AppKRRenderManager';
import { KRNativeRenderController, KRRenderNativeMode, Kuikly } from '@kuikly-open/render';

class ListDataSource implements IDataSource {
  private data: number[] = [];
  private listeners: DataChangeListener[] = [];

  constructor(count: number) {
    for (let i = 0; i < count; i++) {
      this.data.push(i);
    }
  }

  totalCount(): number {
    return this.data.length;
  }

  getData(index: number): number {
    return this.data[index];
  }

  registerDataChangeListener(listener: DataChangeListener): void {
    if (this.listeners.indexOf(listener) < 0) {
      this.listeners.push(listener);
    }
  }

  unregisterDataChangeListener(listener: DataChangeListener): void {
    const index = this.listeners.indexOf(listener);
    if (index >= 0) {
      this.listeners.splice(index, 1);
    }
  }
}

@Entry
@Component
struct KuiklyInList {
  private dataSource: ListDataSource = new ListDataSource(2000);

  aboutToAppear(): void {
    AppKRRenderManager.getInstance().initIfNeed();
  }

  build() {
    Column() {
      Row() {
        Text('Kuikly List')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
      }
      .width('100%')
      .height(48)
      .justifyContent(FlexAlign.Center)

      List({ space: 0 }) {
          LazyForEach(this.dataSource, (item: number) => {
            ListItem() {
              KuiklyListItem({
                index: item,
              })
            }
            .reuseId('kuiklyItem')
            .borderColor(Color.Blue)
            .borderWidth(2)
          }, (item: number) => item.toString())
        }
        .cachedCount(5)
        .divider({ strokeWidth: 10 })
        .width('100%')
        .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
  }
}

@Reusable
@Component
struct KuiklyListItem {
  @State index: number = 0;
  private kuiklyController: KRNativeRenderController | null = null;
  private delegate = new KuiklyViewDelegate()


  build() {
    Column() {
      Kuikly({
        pageName: 'EmbeddedItemPage',
        pageData: {},
        delegate: this.delegate,
        contextCode: '',
        useOhSharedPreferences: true,
        executeMode: KRRenderNativeMode.Native,
        nativeManager: globalNativeManager,
        onControllerReadyCallback: (controller) => {
          this.kuiklyController = controller;
        },
        initialSize: {width:372, height:100}
      })
        .width('100%')
        .height(100)
    }
  }
}