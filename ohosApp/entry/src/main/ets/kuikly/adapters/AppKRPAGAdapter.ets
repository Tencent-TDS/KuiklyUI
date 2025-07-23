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

import {
  IKRPAGViewAdapter,
  IKRPAGViewController,
  IKRPAGViewListener,
  KRArray,
  KRPAGView,
  KRRecord,
  KRValue,
  KuiklyRenderBaseView,
  KuiklyRenderCallback
} from '@kuikly-open/render';
import { UIContext } from '@ohos.arkui.UIContext';
import { ComponentContent } from '@kit.ArkUI';
import { PAGFile, PAGLayerType, PAGTextLayer, PAGView, PAGViewController, PAGViewListener } from '@tencent/libpag';

@Component
struct MyPAGView {
  @State pagViewController: PAGViewController | null = null;

  build() {
    PAGView({
      controller: this.pagViewController
    });
  }
}

@Builder
function createMyPAGView(view: KuiklyRenderBaseView) {
  MyPAGView({
    pagViewController: ((view as KRPAGView).controller! as AppKRPAGViewController).viewController!,
  });
}

class AppKRPAGViewController implements IKRPAGViewController, PAGViewListener {
  viewController: PAGViewController = new PAGViewController();
  cachedUpdateContent: KRArray = [];
  listeners = new Set<IKRPAGViewListener>();
  myself = new WeakRef(this);
  onAnimationStart = (viewController: PAGViewController) => {
    this.listeners.forEach((listener) => {
      if (listener.onAnimationStart) {
        listener.onAnimationStart(this);
      }
    });
  };
  onAnimationEnd = (viewController: PAGViewController) => {
    this.listeners.forEach((listener) => {
      if (listener.onAnimationEnd) {
        listener.onAnimationEnd(this);
      }
    });
  };
  onAnimationRepeat = (viewController: PAGViewController) => {
    this.listeners.forEach((listener) => {
      if (listener.onAnimationRepeat) {
        listener.onAnimationRepeat(this);
      }
    });
  };
  onAnimationCancel = (viewController: PAGViewController) => {
    this.listeners.forEach((listener) => {
      if (listener.onAnimationCancel) {
        listener.onAnimationCancel(this);
      }
    });
  };
  onAnimationUpdate = (viewController: PAGViewController) => {
    this.listeners.forEach((listener) => {
      if (listener.onAnimationUpdate) {
        listener.onAnimationUpdate(this);
      }
    });
  };

  addListener(listener: IKRPAGViewListener): void {
    this.listeners.add(listener);
    this.viewController.addListener(this.myself);
  }

  removeListener(listener: IKRPAGViewListener): void {
    this.listeners.delete(listener);
  }

  play(): void {
    this.viewController.play();
  }

  stop(): void {
    this.viewController.pause();
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey == 'src') {
      const resdir = getContext().resourceDir;
      const srcValue = propValue.toString();
      if (srcValue.startsWith('assets://')) {
        const trailing = srcValue.substring(8);
        const pathname = resdir + trailing;
        if (this.viewController?.setPath(pathname)) {
          // as a demo, we are updating cached content right after it was loaded
          this.doCachedUpdateContent();

          setTimeout(() => {
            // auto play in next loop
            this.viewController.play();
          });
        }
      } else {
        this.LoadFromPathAsync(srcValue);
      }
    } else if (propKey == 'repeatCount' && typeof (propValue) === 'number') {
      this.viewController.setRepeatCount(propValue as number);
    } else if (propKey == 'replaceLayerContents') {
      this.cachedUpdateContent.push(propValue);
      if (this.viewController.getComposition()) {
        this.doCachedUpdateContent();
      } else {
        setTimeout(() => {
          this.doCachedUpdateContent();
        });
      }
    }

    return false;
  }

  doCachedUpdateContent() {
    if (this.viewController.getComposition() == null) {
      return;
    }

    this.cachedUpdateContent?.forEach((propValue) => {
      const items: KRArray = JSON.parse(propValue.toString());
      items.forEach((it) => {
        const item: KRRecord = it as KRRecord;
        const type = item['type'];
        const layerName = item['layerName'] as string;
        const layerIndex = item['layerIndex'] as number;

        const composition = this.viewController.getComposition();
        if (composition) {
          let layers = this.viewController.getComposition()?.getLayersByName(layerName);
          if (layerIndex != undefined) {
            const targetLayer = this.viewController.getComposition()?.getLayerAt(layerIndex);
            if (targetLayer) {
              layers = [targetLayer];
            }
          }

          if (type == 0) {
            const text = item['text'] as string;
            layers?.forEach((layer) => {
              const layerType = layer.layerType();
              if (layer.layerType() == PAGLayerType.Text) {
                (layer as PAGTextLayer).setText(text);
              } else {
                console.log(`layer type mismatch, actual: ${layer.layerType()}, expected:${PAGLayerType.Text} `);
              }
            });
          } else if (type == 1) {
            const imageFileAsset = item['imageFileAsset'];
            // Image updating is just quite similar to what was done in the text case,
            // so it is not going to be demonstrated here
          }
        } else {
          console.log('no composition');
        }
      });
    });
    this.cachedUpdateContent = [];
  }

  async LoadFromPathAsync(url: string): Promise<void> {
    let file = await PAGFile.LoadFromPathAsync(url);
    this.viewController.setComposition(file);
    this.viewController.play();
  }
}

export class AppKRPagViewAdapter implements IKRPAGViewAdapter {
  createController(): IKRPAGViewController {
    return new AppKRPAGViewController();
  }

  createPAGView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView> {
    return new ComponentContent<KuiklyRenderBaseView>(ctx, wrapBuilder<[KuiklyRenderBaseView]>(createMyPAGView), view);
  }
}
