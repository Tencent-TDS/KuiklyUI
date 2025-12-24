import { IKRLogAdapter } from '@kuikly-open/render';
import hilog from '@ohos.hilog';

export class LogAdapter implements IKRLogAdapter {
  d(tag: string, msg: string): void {
    hilog.debug(0x0000, tag, '%{public}s', msg);
  }

  i(tag: string, msg: string): void {
    hilog.info(0x0000, tag, '%{public}s', msg);
  }

  e(tag: string, msg: string): void {
    hilog.error(0x0000, tag, '%{public}s', msg);
  }
}