import { KRMonitorType } from '../KRNativeRenderController'
import { KRAny, KRRecord, KuiklyRenderCallback } from '../utils/KRDataTypes'
import { KuiklyRenderBaseModule } from './export'

export class KRPerformanceModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = "KRPerformanceModule"
  private static readonly METHOD_NOTIFY_INIT_STATE: string = "notifyInitState"
  private static readonly METHOD_ON_GET_LAUNCH_DATA: string = "onGetLaunchData"
  private static readonly METHOD_ON_GET_PERFORMANCE_DATA: string = "onGetPerformanceData"

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRPerformanceModule.METHOD_NOTIFY_INIT_STATE: {
        this.controller?.dispatchLifecycleStateChanged(params as number);
        break;
      }
      case KRPerformanceModule.METHOD_ON_GET_LAUNCH_DATA: {
        const data: Record<string, number> = JSON.parse(params as string);
        if (this.controller?.performanceMonitorTypes().includes(KRMonitorType.LAUNCH)) {
          this.controller?.onGetLaunchData(data);
        }
        break;
      }
      case KRPerformanceModule.METHOD_ON_GET_PERFORMANCE_DATA: {
        const data: Record<string, number> = JSON.parse(params as string);
        if (this.controller?.performanceMonitorTypes().length != 0) {
          this.controller?.onGetPerformanceData(data);
        }
        break;
      }
      default:
        break;
    }
    return null;
  }

}