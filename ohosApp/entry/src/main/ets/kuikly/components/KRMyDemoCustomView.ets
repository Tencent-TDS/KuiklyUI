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
import { ComponentContent, NodeContent } from '@kit.ArkUI';

const kMessage = 'message';

@Observed
export class KRMyDemoCustomView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KRMyDemoCustomView';
  width  = 1.0
  height = 1.0
  x = 0
  y = 0
  onMyViewTapped : null | KuiklyRenderCallback = null

  cssMessage: string | null = null;
  slot: NodeContent | null = new NodeContent()

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    throw new Error('Method not implemented.');
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createMyView),
      this);
  }

  getContentSlot(): NodeContent | null{
    return this.slot
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case kMessage:
        this.cssMessage = propValue as string;
        return true;
      case "frame":
        const components = (propValue as string).split(" ")
        this.x = Number(components[0])
        this.y = Number(components[1])
        this.width =  Number(components[2])
        this.height = Number(components[3])
        return true
      case "onMyViewTapped":
        if(propValue){
          this.onMyViewTapped = (propValue as KuiklyRenderCallback)
        }
        return true
      default:
        return super.setProp(propKey, propValue);
    }
  }

  getMessageCenterImage(): ResourceStr | null {
    return null;
  }
}

@Component
export struct KRMyDemoCustomViewComponent {
  @ObjectLink renderView: KRMyDemoCustomView;
  nodeContent: NodeContent | null = null

  build() {
    Stack(){
      Column() {
        Text(this.renderView.cssMessage)
          .fontSize(48)
          .width('100%')
          .height('50%')
          .textAlign(TextAlign.Center).align(Alignment.End)

        Button("Tap Me").onClick(()=>{
          if(this.renderView.onMyViewTapped){
            this.renderView.onMyViewTapped("{}")
          }
        })
          .width('100%')
          .height('50%')
      }
      .justifyContent(FlexAlign.Center)

      ContentSlot(this.renderView.slot)
    }.alignContent(Alignment.TopStart)
    .backgroundColor(this.renderView.cssBackgroundColor)
    .position({x:this.renderView.x, y:this.renderView.y})
    .size({width:this.renderView.width, height: this.renderView.height})
    .alignSelf(ItemAlign.Start)
    .border({color: Color.Black, width: 2})
    .id("KRMyDemoCustomView")
  }
}

@Builder
function createMyView(view: KuiklyRenderBaseView) {
  KRMyDemoCustomViewComponent({ renderView: view as KRMyDemoCustomView });
}
