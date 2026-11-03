/*
 * 鸿蒙 Kuikly 内存泄漏复现台。
 *
 * 复现手法：反复「进入 Kuikly 页面 → 返回」，观察 ArkTS 侧对象是否随轮次累积。
 * 每一轮在进入前/返回后各打一次 LeakWatcher.check()，并把轮次与泄漏数打到界面上，
 * 便于直接看出「泄漏量是否随轮次单调增长」。
 *
 * 注意：自动模式（方式二）与定点模式（方式一）共用本页，本页只负责驱动轮次与展示结果。
 */

import router from '@ohos.router';
import { hilog, hidebug } from '@kit.PerformanceAnalysisKit';
import { LeakWatcherManager, LeakReport } from '../kuikly/LeakWatcherManager';

const TAG = '[LeakRepro]';

/** 进入的目标 Kuikly 页面；router 是 demo 的默认入口页 */
const TARGET_PAGE = 'router';
/** Kuikly 页面destroy 到采样之间的等待，给 ArkTS GC 留出调度窗口 */
const SETTLE_DELAY_MS = 500;
/** 自动往返时在 Kuikly 页面停留的时间：要等首帧上屏，否则测的是「没渲染完就退出」 */
const STAY_IN_PAGE_MS = 1500;

@Entry
@Component
struct LeakReproPage {
  @State rounds: number = 0;
  @State running: boolean = false;
  @State lastLeakCount: number = 0;
  @State leakTrend: string = '—';
  @State modeText: string = '';
  @State status: string = '空闲';
  /** 最近一次泄漏通知，用于确认「系统到底有没有通知」 */
  @State notifyText: string = '尚无通知';
  /** 每轮 ArkTS 堆占用（KB），字符串泄漏最终会体现在这条曲线上 */
  @State pssTrend: string = '—';
  @State lastHeapKb: number = 0;
  private pendingRounds: number = 0;
  /** 本页是否已离开过一次；首次 onPageShow 不应被算作「一轮返回」 */
  private hasLeft: boolean = false;
  private baselineHeapKb: number = 0;

  aboutToAppear(): void {
    this.refreshMode();
    // 订阅泄漏通知：方式二由系统回调触发，方式一由 reportManualLeak 触发
    const last = LeakWatcherManager.setLeakListener((report: LeakReport) => {
      this.onLeakNotified(report);
    });
    if (last) {
      this.onLeakNotified(last);
    }
  }

  /**
   * 每次回到本页都刷新一次泄漏快照。
   * 自动跑轮时由 onRoundReturned 统一采样（它会累加轮次），
   * 手动单轮时才在这里采样，避免同一次返回被记两遍。
   */
  onPageShow(): void {
    this.refreshMode();
    if (this.hasLeft && this.running) {
      this.onRoundReturned();
    } else if (this.hasLeft) {
      this.sampleLeak('手动返回');
    } else {
      this.sampleLeak('基线（首次进入）');
    }
  }

  onPageHide(): void {
    this.hasLeft = true;
  }

  aboutToDisappear(): void {
    LeakWatcherManager.setLeakListener(null);
  }

  /** 收到泄漏通知：记下来并展示，同时打日志方便 hilog 侧对齐 */
  private onLeakNotified(report: LeakReport): void {
    const time = new Date(report.timestamp).toTimeString().slice(0, 8);
    this.notifyText =
      `${time} ${report.source} 通知：count=${report.leakCount}${report.files.length > 0 ? ' files=' + report.files.join(', ') : ''}`;
    hilog.info(0x30, TAG, '%{public}s', `收到泄漏通知 ${this.notifyText}`);
  }

  private refreshMode(): void {
    this.modeText = LeakWatcherManager.isManualMode() ? '方式一（定点 watch/check/dump）' : '方式二（自动监控）';
  }

  /**
   * 取一次泄漏快照。check() 会触发 GC，返回疑似泄漏对象的 JSON。
   * 仅在手动模式下有返回值，自动模式下靠系统回调自行 dump。
   */
  private sampleLeak(reason: string): void {
    // 手动模式：check() 有输出；自动模式：无输出，靠系统回调
    const count = LeakWatcherManager.reportManualLeak(this.lastLeakCount);
    if (count >= 0) {
      this.lastLeakCount = count;
      this.leakTrend = `${this.leakTrend === '—' ? '' : this.leakTrend + ' → '}${count}`;
    }
    const heapKb = this.sampleHeap();
    hilog.info(0x30, TAG, '%{public}s', `${reason}：泄漏对象 ${count} 个，ArkTS 堆 ${heapKb} KB`);
    if (this.running && this.pendingRounds > 0) {
      this.status = `第 ${this.rounds}/${this.pendingRounds} 轮完成`;
    }
  }

  /** 采一次 ArkTS 堆占用（KB）。字符串对象泄漏会直接反映在这条曲线上。 */
  private sampleHeap(): number {
    let heapKb = 0;
    try {
      const info = hidebug.getAppVMMemoryInfo();
      // heapUsed 单位本身就是 KB；bigint 转 number 量级远小于 MAX_SAFE_INTEGER
      heapKb = Number(info.heapUsed);
    } catch (e) {
      hilog.error(0x30, TAG, '%{public}s', `getAppVMMemoryInfo 失败：${(e as Error)?.message ?? ''}`);
      return 0;
    }
    if (this.baselineHeapKb === 0) {
      this.baselineHeapKb = heapKb;
    }
    this.lastHeapKb = heapKb;
    const delta = heapKb - this.baselineHeapKb;
    this.pssTrend = `${this.pssTrend === '—' ? '' : this.pssTrend + ' → '}${heapKb}(${delta >= 0 ? '+' : ''}${delta})`;
    return heapKb;
  }

  /** 抓一份当前线程的 rawheap，用于本地 mem-analyzer 分析 */
  private dumpRawHeap(): void {
    this.status = '正在抓 rawheap（可能较慢）…';
    LeakWatcherManager.dumpRawHeap().then((path: string) => {
      if (path.length === 0) {
        this.status = 'rawheap 抓取失败：可能超配额，稍后再试';
        return;
      }
      this.status = `rawheap: ${path}`;
      hilog.info(0x30, TAG, '%{public}s', `rawheap path=${path}`);
    });
  }

  /** 手动模式下导出泄漏列表 + 堆快照到应用 files 目录 */
  private dumpNow(): void {
    const files: Array<string> = LeakWatcherManager.dump();
    if (files.length === 0) {
      this.status = 'dump 无输出：需切到方式一';
      return;
    }
    this.status = `已导出 ${files.length} 个文件`;
    hilog.info(0x30, TAG, '%{public}s', `dump files=${files.join(', ')}`);
  }

  /**
   * 连续跑多轮。真正的「进入/返回」由用户或本页触发，
   * 这里用 onPageShow 作为「本轮返回」的信号来推进下一轮，
   * 避免无脑 pushUrl 把页面栈堆爆（那测出来的不是泄漏，是栈增长）。
   */
  private startAutoRounds(total: number): void {
    this.pendingRounds = total;
    this.rounds = 0;
    this.leakTrend = '—';
    this.pssTrend = '—';
    this.baselineHeapKb = 0;
    this.running = true;
    this.status = `自动往返 ${total} 轮中…`;
    this.pushKuiklyPage(true);
  }

  /** 由 onPageShow 调用：本页重新可见即视为上一轮结束 */
  private onRoundReturned(): void {
    if (!this.running) {
      return;
    }
    this.rounds += 1;
    // 刚返回时 Kuikly 页面的对象还没被 GC 回收，等一会儿再采样，否则趋势是假的
    setTimeout(() => {
      this.sampleLeak(`第 ${this.rounds} 轮返回`);
      this.afterRoundSampled();
    }, SETTLE_DELAY_MS);
  }

  private afterRoundSampled(): void {
    if (this.rounds >= this.pendingRounds) {
      this.running = false;
      this.status = `已完成 ${this.rounds} 轮，看趋势是否单调增长`;
      return;
    }
    this.status = `自动往返中 ${this.rounds}/${this.pendingRounds}`;
    this.pushKuiklyPage(true);
  }

  /** autoBack=true 时进入后停留一会再 router.back()，用来无人值守地跑满 N 轮 */
  private pushKuiklyPage(autoBack: boolean = false): void {
    // router.pushUrl 在 ArkTS 静态检查里被标记为可能抛异常，这里显式兜住
    try {
      router.pushUrl({
        url: 'pages/Index',
        params: {
          pageName: TARGET_PAGE,
          pageData: {}
        }
      }).then(() => {
        hilog.info(0x30, TAG, '%{public}s', `第 ${this.rounds + 1} 轮：已进入 ${TARGET_PAGE}`);
        if (autoBack && this.running) {
          setTimeout(() => {
            if (this.running) {
              this.goBack();
            }
          }, STAY_IN_PAGE_MS);
        }
      }).catch((err: Error) => {
        this.status = `进入失败：${(err as Error)?.message ?? ''}`;
        this.running = false;
      });
    } catch (e) {
      this.status = `进入异常：${(e as Error)?.message ?? ''}`;
      this.running = false;
    }
  }

  private goBack(): void {
    try {
      router.back();
    } catch (e) {
      this.status = `返回异常：${(e as Error)?.message ?? ''}`;
      this.running = false;
    }
  }

  build() {
    Column({ space: 12 }) {
      Text('Kuikly 内存泄漏复现台')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
        .margin({ top: 24, bottom: 4 })

      Text(`目标页：${TARGET_PAGE}   轮次：${this.rounds}`)
        .fontSize(14)
        .fontColor('#666666')

      Text(`LeakWatcher：${this.modeText}`)
        .fontSize(13)
        .fontColor('#888888')

      Text(`疑似泄漏对象：${this.lastLeakCount}`)
        .fontSize(14)

      Text(`趋势：${this.leakTrend}`)
        .fontSize(13)
        .fontColor('#888888')

      Text(`ArkTS 堆：${this.lastHeapKb} KB`)
        .fontSize(13)

      Text(`堆趋势：${this.pssTrend}`)
        .fontSize(13)
        .fontColor('#888888')

      Text(`通知：${this.notifyText}`)
        .fontSize(12)
        .fontColor('#999999')

      Text(this.status)
        .fontSize(13)
        .fontColor('#999999')

      Button('进入 Kuikly 页面（单轮）')
        .width('80%')
        .onClick(() => {
          this.status = '已进入，返回后查看趋势';
          this.pushKuiklyPage();
        })

      Button('自动往返 10 轮')
        .width('80%')
        .enabled(!this.running)
        .onClick(() => {
          this.startAutoRounds(10);
        })

      Button('自动往返 30 轮')
        .width('80%')
        .enabled(!this.running)
        .onClick(() => {
          this.startAutoRounds(30);
        })

      Button('切到方式一（定点排查）')
        .width('80%')
        .onClick(() => {
          LeakWatcherManager.setManualMode(true);
          this.refreshMode();
          this.status = '已切方式一：可用 dump 导出快照';
        })

      Button('切回方式二（自动监控）')
        .width('80%')
        .onClick(() => {
          LeakWatcherManager.setManualMode(false);
          this.refreshMode();
          this.status = '已切方式二：泄漏达阈值自动 dump';
        })

      Button('导出堆快照 (dump)')
        .width('80%')
        .onClick(() => {
          this.dumpNow();
        })

      Button('抓 rawheap（mem-analyzer 用）')
        .width('80%')
        .onClick(() => {
          this.dumpRawHeap();
        })

      Button('清空趋势')
        .width('80%')
        .onClick(() => {
          this.rounds = 0;
          this.lastLeakCount = 0;
          this.leakTrend = '—';
          this.pssTrend = '—';
          this.baselineHeapKb = this.lastHeapKb;
          this.status = '已重置';
        })
    }
    .width('100%')
    .height('100%')
    .alignItems(HorizontalAlign.Center)
    .padding({ bottom: 24 })
  }
}
