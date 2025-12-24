import {  KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import { fileUri } from '@kit.CoreFileKit';
import { ComponentContent } from '@kit.ArkUI';

const kMessage = 'message';

@Observed
export class KRMyView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KRMyView';
  cssMessage: string | null = null;

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createMyView), this)
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
  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    return null
  }

  getMessageCenterImage(): ResourceStr | null{
    return null
  }
}

@Component
export struct KRMy {
  @ObjectLink renderView: KRMyView

  public getImageResourceStr(str: string): ResourceStr | null {

      let resdir = getContext().resourceDir
      let path = resdir + "/" + str.substring(0)
      let uri = fileUri.getUriFromPath(path)
      return uri

  }
  build() {
    Column() {
      Image(this.renderView.getMessageCenterImage())
        .backgroundColor(this.renderView.cssBackgroundColor)
        .width('100%')
        .height('50%')
        .objectFit(ImageFit.Contain)
      Text(this.renderView.cssMessage)
        .width('100%')
        .height('50%')
        .textAlign(TextAlign.Center)
    }
    .justifyContent(FlexAlign.Center)

  }
}

function getImageResourceStr(str: string): ResourceStr | null {
  let resdir = getContext().resourceDir
  let path = resdir + "/" + str.substring(0)
  let uri = fileUri.getUriFromPath(path)
  return uri
}

@Builder
function createMyView(view: KuiklyRenderBaseView) {
  KRMy({ renderView: view as KRMyView })
}