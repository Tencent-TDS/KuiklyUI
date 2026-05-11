
import { BuglyBuilder, Bugly, ICrashListener, TDLog, ModuleName, AppVersionMode } from "bugly"
import BuildProfile from "BuildProfile";
import { getKuiklyDemoBuglyService } from "../../ts-api/provider";

const TAG = "BuglyHM";

export class CrashReport {
  static initialize = false
  private static _instance: CrashReport | null = null
  private context: Context | null = null

  private constructor() {}

  static getInstance(): CrashReport {
    if (!CrashReport._instance) {
      CrashReport._instance = new CrashReport()
    }
    return CrashReport._instance
  }

  initCrashReport(c: Context): void {
    if (CrashReport.initialize) {
      return;
    }
    this.context = c

    let builder = new BuglyBuilder();

    builder.appId = 'xxxxxxx';   // 必填，Bugly产品信息中的APP ID
    builder.appKey = 'xxx-xxxx-xxxx-xxxx-xxxx';    // 必填，Bugly产品信息中的APP KEY    // builder.appId = 'xxxxxxx';   // 必填，Bugly产品信息中的APP ID
    builder.deviceId = "12345";     // 必填，设备ID，应保证设备ID对不同设备唯一
    builder.platform = BuglyBuilder.PLATFORM_PRO;    // 必填，设置上报平台，OA版本需设置为[BuglyBuilder.PLATFORM_OA]

    builder.appVersion = '1.0.0';   // 选填，业务的App版本
    builder.appVersionMode = BuildProfile.DEBUG ?  AppVersionMode.DEBUG : AppVersionMode.RELEASE;//"DEBUG";  // 选填，当前App的版本类型，支持根据不同的版本类型下发配置
    builder.buildNum = '0';         // 选填，业务App版本的构建号
    builder.appChannel = 'website'; // 选填，业务App渠道
    builder.userId = "12345";       // 选填，用户ID，如不设置则为空
    builder.deviceModel = "huawei"; // 选填，机型，如不设置则为空
    builder.debugMode = BuildProfile.DEBUG;       // 选填，默认开启，开启后Bugly SDK会打印更多调试日志，线上版本可关闭
    builder.sdkLogMode = true;      // 选填，设置debugMode或sdkLogMode均可开启Bugly sdk日志，适用于线上关闭debug模式但又希望打印bugly日志的场景
    builder.initDelay = 0;          // 选填，延迟初始化时间，单位ms
    builder.enableJsCrashProtect = false;   // 选填，是否开启Js异常崩溃保护，设置为true后发生未捕获Js异常，进程不会退出
    builder.enablePerfModules = ModuleName.AllModules;  // 选填，开启性能监控项，可传入单个性能模块名称或一组性能模块名称，此处初始化后，还需配置开启对应模块采样率，模块才会真正开启

    Bugly.init(c, builder);   // 如果需等待Bugly完全初始化完成，使用await Bugly.init(context, builder);

    // 通过knoi注册上报回调
    getKuiklyDemoBuglyService().registerUnhandledExceptionHook(this.postCrashRelatedError);
    // 注册crash相关的异常上报
    CrashReport.initialize = true
  }

  /**
   * 上报自定义错误
   */
  reportError(name: string, errorMsg: string, stack: string): void {
    Bugly.postCustomError(name, errorMsg, stack, false, this.context)
  }

  private postCrashRelatedError(name: string, message: string, stack: string, storeFirst: boolean) {
    Bugly.postCrashRelatedError(name, message, stack)
  }
}
