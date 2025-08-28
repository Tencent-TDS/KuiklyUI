import { KRAny, KuiklyRenderCallback } from '../utils/KRDataTypes'
import { KuiklyRenderBaseModule } from './export'

export class KRPerformanceModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = "KRPerformanceModule"
  private static readonly METHOD_NOTIFY_INIT_STATE: string = "notifyInitState"

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRPerformanceModule.METHOD_NOTIFY_INIT_STATE: {
        this.controller?.dispatchLifecycleStateChanged(params as number)
        break;
      }
      default:
        break;
    }
    return null;
  }

}