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

import emitter from '@ohos.events.emitter';
import { KRAny, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KRJSON } from '../utils/KRJSON';
import { KuiklyRenderBaseModule } from './export';

class KRNotifyCallbackWrapper {
  constructor(callback: KuiklyRenderCallback, callbackId: string) {
    this.callback = callback;
    this.callbackId = callbackId;
  }

  callback: KuiklyRenderCallback;
  callbackId: string;
}

interface KRAddNotifyParams {
  id: string;
  eventName: string;
}

interface KRRemoveNotifyParams {
  id: string;
  eventName: string;
}

interface KRPostNotifyParams {
  eventName: string;
  data: Object;
}

export class KRNotifyModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRNotifyModule';
  private static readonly METHOD_ADD_NOTIFY: string = 'addNotify';
  private static readonly METHOD_REMOVE_NOTIFY: string = 'removeNotify';
  private static readonly METHOD_POST_NOTIFY: string = 'postNotify';
  private subscribersMap: Record<string, Array<KRNotifyCallbackWrapper> | null> = {};
  private globalCallback: Callback<emitter.EventData> | null = null;

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRNotifyModule.METHOD_ADD_NOTIFY: {
        const data = KRJSON.asRecord(params);
        this.addNotify(data['id'] as string, data['eventName'] as string, callback);
        break;
      }
      case KRNotifyModule.METHOD_REMOVE_NOTIFY: {
        const data = KRJSON.asRecord(params);
        this.removeNotify(data['id'] as string, data['eventName'] as string);
        break;
      }
      case KRNotifyModule.METHOD_POST_NOTIFY: {
        const data = KRJSON.asRecord(params);
        this.postNotify(data['eventName'] as string, data['data'] as Object);
        break;
      }
    }

    return null;
  }

  onDestroy(): void {
    Object.keys(this.subscribersMap).forEach((key) => {
      emitter.off(key, this.globalCallback!);
    });
    this.subscribersMap = {};
    this.globalCallback = null;
  }

  addNotify(id: string, eventName: string, callback: KuiklyRenderCallback | null) {
    if (callback == null) {
      return;
    }

    const callbackWrappers = this.subscribersMap[eventName];
    if (callbackWrappers) {
      // if subscribed before, simple add it the list
      console.info(`NotifyModule: already subscribe, appending to the list, old count:${callbackWrappers.length}`);
      callbackWrappers.push(new KRNotifyCallbackWrapper(callback, id));
      console.info(`NotifyModule: already subscribe, appended to the list, new count:${callbackWrappers.length}`);
      return;
    }

    this.subscribersMap[eventName] = [new KRNotifyCallbackWrapper(callback, id)];
    if (this.globalCallback === null) {
      this.globalCallback = (event: emitter.EventData) => {
        const data = event.data as Record<string, string>;
        const eventName = data.eventName;
        const stringify = data.stringify ?? '{}';
        // The event subscribe was triggered
        console.info(`NotifyModule: subscribe event received, dispatching`);
        let callbackWrappers = this.subscribersMap[eventName];
        callbackWrappers?.forEach((value) => {
          value.callback(stringify);
        });
      };
    }
    emitter.on(eventName, this.globalCallback);
    return;
  }

  removeNotify(id: string, eventName: string) {
    let callbackWrappers = this.subscribersMap[eventName];
    if (callbackWrappers) {
      const index = callbackWrappers.findIndex((value) => {
        return value.callbackId == id;
      });
      if (index > -1) {
        callbackWrappers.splice(index, 1);
        if (callbackWrappers.length === 0) {
          emitter.off(eventName, this.globalCallback!);
          this.subscribersMap[eventName] = null;
        }
      }
    }
  }

  postNotify(eventName: string, data: Object) {
    emitter.emit(eventName, {
      data: {
        'eventName': eventName,
        'stringify': data ? JSON.stringify(data) : null
      }
    });
  }
}
