import { jsLeakWatcher, hilog, hidebug } from '@kit.PerformanceAnalysisKit';
import { common } from '@kit.AbilityKit';
import { MMKV } from '@tencent/mmkv';
import BuildProfile from 'BuildProfile';

/**
 * 本仓没有 wg_core/WLog，用 hilog 平替（与 AppKRLogAdapter 一致）。
 * 仅做最小封装，避免改动调用点。
 */
class WLog {
  static i(tag: string, msg: string): void {
    hilog.info(0x30, tag, '%{public}s', msg);
  }

  static e(tag: string, msg: string): void {
    hilog.error(0x30, tag, '%{public}s', msg);
  }
}

const TAG = '[LeakWatcher]';
const MMKV_KEY_MANUAL_MODE = 'leak_watcher_manual_mode';

/**
 * 一次泄漏通知的内容。方式二由系统回调给出文件列表，方式一由调用方 check() 后自行组装。
 * files 的约定与 jsLeakWatcher.dump() 一致：[0]=leaklist(.jsleaklist)，[1]=堆快照(.rawheap/.heapsnapshot)。
 */
export interface LeakReport {
  /** 通知来源：auto=方式二系统自动回调，manual=方式一定点 check/dump */
  source: string;
  /** 疑似泄漏对象个数；无法解析时为 -1 */
  leakCount: number;
  /** 导出的文件绝对路径列表 */
  files: Array<string>;
  /** 发生时间（ms，Date.now()） */
  timestamp: number;
}

/**
 * ArkTS 内存泄漏检测开关（仅 DEBUG 包生效，release 全部为 no-op）。
 *
 * 方式二（默认，开关关闭）：enableLeakWatcher 自动监控组件泄漏，泄漏达阈值自动 dump。
 * 方式一（开关打开）：enable + watch + check + dump，用于定点排查具体可疑对象。
 * 两种方式互斥，切换时先全关再开启目标方式；开关状态持久化到 MMKV，重启后保持。
 *
 * 泄漏通知：两种方式统一走 setLeakListener 注册的回调，同时保留 hilog。
 * 没注册回调时只打日志，所以「看不到通知」通常是没订阅 + 没过滤日志。
 *
 * 注意：ArkTS 静态方法中不允许使用 this，统一通过类名访问静态成员。
 */
export class LeakWatcherManager {
  private static manualMode: boolean = false;
  private static manualEnabled: boolean = false;
  private static autoEnabled: boolean = false;
  private static context: common.Context | null = null;
  private static listener: ((report: LeakReport) => void) | null = null;
  private static lastReport: LeakReport | null = null;

  /** 在 MMKV.initialize() 之后调用（本工程接在 EntryAbilityStage.onCreate 中） */
  static init(context: common.Context): void {
    if (!BuildProfile.DEBUG || !LeakWatcherManager.isSupported()) {
      return;
    }
    LeakWatcherManager.context = context;
    const manual = MMKV.defaultMMKV()?.decodeBool(MMKV_KEY_MANUAL_MODE, false) ?? false;
    LeakWatcherManager.applyMode(manual);
  }

  /** 当前是否为方式一（手动模式） */
  static isManualMode(): boolean {
    return LeakWatcherManager.manualMode;
  }

  /**
   * 订阅泄漏通知。回调发生在「方式二系统检测到泄漏自动 dump 后」或「方式一 reportManualLeak 被调用时」。
   * 传 null 取消订阅。返回值是上一次的泄漏报告（可在订阅后直接补一次状态刷新）。
   */
  static setLeakListener(listener: ((report: LeakReport) => void) | null): LeakReport | null {
    LeakWatcherManager.listener = listener;
    return LeakWatcherManager.lastReport;
  }

  /** 最近一次泄漏报告；没发生过则为 null */
  static getLastReport(): LeakReport | null {
    return LeakWatcherManager.lastReport;
  }

  /**
   * 导出当前线程的 rawheap 快照（方式二的堆快照是系统自动出的，这个用于自己挑时机抓）。
   * dumpJsRawHeapData 是 API 18 起的接口，本工程 compatibleSdkVersion 为 12，
   * 编译能过但低版本设备调用会失败，因此先探测再调，且失败返回 ''。
   * 返回文件路径，失败返回 ''。文件较大且有系统配额限制，用完及时删。
   */
  static dumpRawHeap(): Promise<string> {
    if (!canIUse('SystemCapability.HiviewDFX.HiProfiler.HiDebug')) {
      WLog.i(TAG, 'rawheap 不可用：当前设备不支持 HiDebug');
      return Promise.resolve('');
    }
    return hidebug.dumpJsRawHeapData(true).then((path: string) => {
      WLog.i(TAG, `rawheap 已导出：${path}`);
      return path;
    }).catch((e: Error) => {
      WLog.e(TAG, `rawheap 导出失败：${e?.message ?? ''}`);
      return '';
    });
  }

  /** 调试开关入口：true 切到方式一，false 切到方式二。状态持久化，立即生效。 */
  static setManualMode(manual: boolean): void {
    if (!BuildProfile.DEBUG || !LeakWatcherManager.isSupported()) {
      return;
    }
    MMKV.defaultMMKV()?.encodeBool(MMKV_KEY_MANUAL_MODE, manual);
    LeakWatcherManager.applyMode(manual);
  }

  private static applyMode(manual: boolean): void {
    LeakWatcherManager.disableAll();
    LeakWatcherManager.manualMode = manual;
    if (manual) {
      jsLeakWatcher.enable(true);
      LeakWatcherManager.manualEnabled = true;
      WLog.i(TAG, '方式一已开启：手动 watch/check/dump 定点排查');
    } else {
      // 方式二：自动监控组件类型泄漏
      // 优先尝试 SDK 24+ 的枚举配置（LeakWatcherConfig）。
      // 本工程 compatibleSdkVersion 为 API 12，直接用枚举类型名在部分工具链下无法解析，
      // 因此用 canIUse 先探测，不支持时降级到 Array<string> 旧 API（since 20）。
      const cb = (files: Array<string>) => {
        // 系统只在「泄漏数达到阈值」时回调一次，files 即自动 dump 出来的两个文件
        WLog.i(TAG, `方式二检测到泄漏，files=${files.join(', ')}`);
        LeakWatcherManager.dispatch('auto', -1, files);
      };
      if (LeakWatcherManager.tryEnableWithConfig(cb)) {
        LeakWatcherManager.autoEnabled = true;
        return;
      }
      WLog.i(TAG, '枚举配置不可用，降级字符串数组');
      LeakWatcherManager.enableWithStringConfigs(cb);
    }
  }

  /** 统一分发泄漏通知：打 hilog + 回调给订阅方 */
  private static dispatch(source: string, leakCount: number, files: Array<string>): void {
    const report: LeakReport = {
      source: source,
      leakCount: leakCount,
      files: files,
      timestamp: Date.now(),
    };
    LeakWatcherManager.lastReport = report;
    WLog.i(TAG, `泄漏通知 source=${source} count=${leakCount} files=${files.join(', ')}`);
    const listener = LeakWatcherManager.listener;
    if (listener) {
      listener(report);
    }
  }

  /** 尝试 SDK 24+ 的 LeakWatcherConfig 枚举配置，成功返回 true。isEnabled=false 用于关闭。 */
  private static tryEnableWithConfig(cb: (files: Array<string>) => void, isEnabled: boolean = true): boolean {
    // 枚举配置 API 24 起提供；编译期已可用，但运行时设备可能更低，故保留探测
    if (!canIUse('SystemCapability.HiviewDFX.HiChecker')) {
      return false;
    }
    try {
      const config: jsLeakWatcher.LeakWatcherConfig = {
        monitorObjectTypes: jsLeakWatcher.MonitorObjectType.ALL,
      };
      jsLeakWatcher.enableLeakWatcher(isEnabled, config, cb);
      if (isEnabled) {
        WLog.i(TAG, '方式二已开启：自动监控所有组件类型（枚举 ALL）');
      }
      return true;
    } catch (e) {
      WLog.i(TAG, `枚举配置开启失败：${(e as Error)?.message ?? ''}`);
      return false;
    }
  }

  /** 降级路径：Array<string> 旧 API（since 20） */
  private static enableWithStringConfigs(cb: (files: Array<string>) => void): void {
    try {
      // 覆盖 CustomComponent / Window / NodeContainer
      const configs: Array<string> = ['CustomComponent', 'Window', 'NodeContainer', 'XComponent', 'Ability'];
      jsLeakWatcher.enableLeakWatcher(true, configs, cb);
      LeakWatcherManager.autoEnabled = true;
      WLog.i(TAG, `方式二已开启：自动监控 ${configs.join('/')}（字符串数组降级）`);
    } catch (e2) {
      WLog.e(TAG, `方式二开启失败（双重降级仍异常）: ${(e2 as Error)?.message ?? ''}`);
    }
  }

  private static disableAll(): void {
    if (LeakWatcherManager.manualEnabled) {
      jsLeakWatcher.enable(false);
      LeakWatcherManager.manualEnabled = false;
    }
    if (LeakWatcherManager.autoEnabled) {
      const noop = (): void => {};
      // 与开启对称：优先枚举配置，失败降级空数组；异常忽略（重复关闭不应抛错）
      if (!LeakWatcherManager.tryEnableWithConfig(noop, false)) {
        try {
          jsLeakWatcher.enableLeakWatcher(false, [], noop);
        } catch {
          // 忽略重复关闭异常
        }
      }
      LeakWatcherManager.autoEnabled = false;
    }
  }

  // ---- 以下为方式一 API，仅手动模式下生效，其余情况 no-op ----

  /** 注册要定点监控的对象（创建后尽早调用） */
  static watch(obj: object, msg: string): void {
    if (!LeakWatcherManager.canManual()) {
      return;
    }
    jsLeakWatcher.watch(obj, msg);
  }

  /** 触发 GC 并返回泄漏对象列表（JSON 字符串），建议在关键操作前后各调一次对比 */
  static check(): string {
    if (!LeakWatcherManager.canManual()) {
      return '';
    }
    return jsLeakWatcher.check();
  }

  /**
   * 方式一：check() 后把「个数 > since」的结果当成一次泄漏通知发出去。
   * 用于复现场景：每轮返回后调用，配合界面就能看到泄漏量是否随轮次增长。
   * 返回本次解析出的泄漏个数（-1 表示解析不了）。
   */
  static reportManualLeak(since: number = 0, files: Array<string> = []): number {
    const result: string = LeakWatcherManager.check();
    if (result.length === 0) {
      return -1;
    }
    const count = LeakWatcherManager.parseLeakCount(result);
    if (count > since) {
      LeakWatcherManager.dispatch('manual', count, files);
    }
    return count;
  }

  /**
   * 解析 check() 返回的 JSON，得到疑似泄漏对象个数。
   * 返回结构在不同版本上可能是数组、或包裹在对象字段里，这里做兼容；
   * 解析失败返回 -1，交由调用方区分「解析不了」和「真的是 0」。
   */
  static parseLeakCount(json: string): number {
    if (json.length === 0) {
      return -1;
    }
    try {
      const parsed: Object = JSON.parse(json);
      if (Array.isArray(parsed)) {
        return parsed.length;
      }
      const record = parsed as Record<string, Object>;
      // 常见字段名兜底
      const candidates: Array<string> = ['objects', 'leakList', 'list', 'data'];
      for (const key of candidates) {
        const value = record[key];
        if (Array.isArray(value)) {
          return value.length;
        }
      }
      return -1;
    } catch (e) {
      WLog.e(TAG, `parseLeakCount 失败：${(e as Error)?.message ?? ''}`);
      return -1;
    }
  }

  /** 导出泄漏列表(.jsleaklist) + 堆快照(.heapsnapshot)到应用 files 目录，返回文件路径 */
  static dump(): Array<string> {
    const context = LeakWatcherManager.context;
    if (!LeakWatcherManager.canManual() || !context) {
      return [];
    }
    const files = jsLeakWatcher.dump(context.filesDir);
    WLog.i(TAG, `方式一 dump: ${files.join(', ')}`);
    return files;
  }

  private static canManual(): boolean {
    return BuildProfile.DEBUG && LeakWatcherManager.manualMode && LeakWatcherManager.manualEnabled
      && LeakWatcherManager.isSupported();
  }

  private static isSupported(): boolean {
    return canIUse('SystemCapability.HiviewDFX.HiChecker');
  }
}
