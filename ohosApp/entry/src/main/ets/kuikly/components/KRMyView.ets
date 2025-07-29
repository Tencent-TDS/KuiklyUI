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

import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import { fileUri } from '@kit.CoreFileKit';
import { ComponentContent } from '@kit.ArkUI';

const kMessage = 'message';

@Observed
export class KRMyView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KRMyView';
  cssMessage: string | null = null;

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    throw new Error('Method not implemented.');
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createMyView),
      this);
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case kMessage:
        this.cssMessage = propValue as string;
        return true;
      default:
        return super.setProp(propKey, propValue);
    }
  }

  getMessageCenterImage(): ResourceStr | null {
    return null;
  }
}

@Component
export struct KRMy {
  @ObjectLink renderView: KRMyView;

  public getImageResourceStr(str: string): ResourceStr | null {

    let resdir = getContext().resourceDir;
    //let path = resdir + '/' + str.substring(KuiklyRenderBaseView.ASSETS_IMAGE_PREFIX.length)
    let path = resdir + '/' + str.substring(10);
    let uri = fileUri.getUriFromPath(path);
    return uri;

  }

  build() {
    Column() {
      Image(this.renderView.getMessageCenterImage())
        .width('100%')
        .height('50%')
        .objectFit(ImageFit.Contain);
      Text(this.renderView.cssMessage)
        .width('100%')
        .height('50%')
        .textAlign(TextAlign.Center);
    }
    .justifyContent(FlexAlign.Center);

    //.applyRenderViewBaseAttr(this.renderView)
  }
}

// 应用基础属性
// TODO: 用attributeModifier替换
@Extend(Column)
function applyRenderViewBaseAttr(renderView: KuiklyRenderBaseView) {

} // end of applyRenderViewBaseAttr()


function getImageResourceStr(str: string): ResourceStr | null {
  let resdir = getContext().resourceDir;
  let path = resdir + '/' + str.substring(10);
  let uri = fileUri.getUriFromPath(path);
  return uri;
}

@Builder
function createMyView(view: KuiklyRenderBaseView) {
  KRMy({ renderView: view as KRMyView });
}
