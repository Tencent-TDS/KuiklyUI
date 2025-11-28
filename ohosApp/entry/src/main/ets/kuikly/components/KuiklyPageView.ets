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

import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback,
  KTNativeRenderController, IKuiklyViewDelegate,
  Kuikly} from '@kuikly-open/render';
import { ComponentContent } from '@kit.ArkUI';
import { JSON } from '@kit.ArkTS';
import { KuiklyViewDelegate } from '../KuiklyViewDelegate';
import globalNativeManager from '../MyNativeManager';

const PROP_PAGE_NAME = 'pageName';
const PROP_PAGE_DATA = 'pageData';
const PROP_LOAD_SUCCESS = 'loadSuccess';
const PROP_LOAD_FAILURE = 'loadFailure';
const METHOD_SEND_EVENT = 'sendEvent';

@Builder
function createKuiklyPageView(view: KuiklyRenderBaseView) {
  KuiklyPageViewComponent({ renderView: view as KuiklyPageView })
}

@Observed
export class KuiklyPageView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KuiklyPageView'
  
  pageName: string = ''
  pageData: string = '{}'
  private loadSuccessCallback?: KuiklyRenderCallback
  private loadFailureCallback?: KuiklyRenderCallback
  
  private delegate: IKuiklyViewDelegate = new KuiklyViewDelegate()
  private viewDidAppear = false
  private pageDidAppear = false

  constructor() {
    super()
    console.log('[KuiklyPageView] constructor called')
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    console.log('[KuiklyPageView] createArkUIView called')
    const uiContext = this.getUIContext() as UIContext
    return new ComponentContent<KuiklyRenderBaseView>(
      uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createKuiklyPageView),
      this
    )
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    console.log(`[KuiklyPageView] setProp called: propKey=${propKey}, value=${typeof propValue === 'function' ? 'callback' : propValue}`)
    
    switch (propKey) {
      case PROP_PAGE_NAME:
        this.pageName = propValue as string
        console.log(`[KuiklyPageView] setProp: pageName set to "${this.pageName}"`)
        return true
        
      case PROP_PAGE_DATA:
        this.pageData = propValue as string
        console.log(`[KuiklyPageView] setProp: pageData set to ${this.pageData}`)
        return true
        
      case PROP_LOAD_SUCCESS:
        this.loadSuccessCallback = propValue as KuiklyRenderCallback
        console.log(`[KuiklyPageView] setProp: loadSuccess callback registered`)
        return true
        
      case PROP_LOAD_FAILURE:
        this.loadFailureCallback = propValue as KuiklyRenderCallback
        console.log(`[KuiklyPageView] setProp: loadFailure callback registered`)
        return true
        
      default:
        console.log(`[KuiklyPageView] setProp: delegating to super.setProp`)
        return super.setProp(propKey, propValue)
    }
  }

  call(method: string, param: KRAny, callback: KuiklyRenderCallback): void {
    console.log(`[KuiklyPageView] call invoked: method=${method}, param=${param}`)
    
    switch (method) {
      case METHOD_SEND_EVENT:
        console.log(`[KuiklyPageView] call: handling sendEvent method`)
        this.sendEventWithParams(param as string)
        break
        
      default:
        console.warn(`[KuiklyPageView] call: Unknown method ${method}`)
        break
    }
  }

  onDestroy(): void {
    console.log('[KuiklyPageView] onDestroy')
  }

  /**
   * Called by KTKuiklyComposeView when Kuikly controller is ready
   */
  onKuiklyControllerReady(controller: KTNativeRenderController) {
    console.log('[KuiklyPageView] onKuiklyControllerReady')

    // Register exception callback
    controller.registerExceptionCallback((executeMode, stack) => {
      console.error(`[KuiklyPageView] Exception occurred: ${stack}`)
      this.onPageLoadComplete(false, stack)
    })
    
    // Trigger success callback after controller is ready
    this.onPageLoadComplete(true)
  }

  /**
   * Handle page load completion
   */
  private onPageLoadComplete(isSucceed: boolean, errorReason?: string) {
    console.log(`[KuiklyPageView] onPageLoadComplete: isSucceed=${isSucceed}${errorReason ? ', errorReason=' + errorReason : ''}`)
    
    if (isSucceed && this.loadSuccessCallback) {
      console.log('[KuiklyPageView] onPageLoadComplete: invoking loadSuccess callback')
      this.loadSuccessCallback({})
    }
    
    if (!isSucceed && this.loadFailureCallback) {
      console.log('[KuiklyPageView] onPageLoadComplete: invoking loadFailure callback')
      this.loadFailureCallback({ 'errorReason': errorReason || 'Unknown error' })
    }
  }

  /**
   * Parse and send event with params
   */
  private sendEventWithParams(params: string) {
    try {
      const eventData = JSON.parse(params) as Record<string, Object>
      const event = eventData['event'] as string
      const data = (eventData['data'] as Record<string, Object>) || {}
      console.log(`[KuiklyPageView] sendEventWithParams: parsed event="${event}"`)
      switch (event) {
        case 'didAppear':
          this.viewDidAppear = true;
          if (this.pageDidAppear) {
            this.delegate.pageDidAppear();
          }
          break;
        case 'didDisappear':
          this.viewDidAppear = false;
          if (this.pageDidAppear) {
            this.delegate.pageDidDisappear();
          }
          break;
        case 'viewDidAppear':
          this.pageDidAppear = true;
          if (this.viewDidAppear) {
            this.delegate.pageDidAppear();
          }
          break;
        case 'viewDidDisappear':
          this.pageDidAppear = false;
          if (this.viewDidAppear) {
            this.delegate.pageDidDisappear();
          }
          break;
        case 'windowSizeDidChanged':
        case 'rootViewSizeDidChanged':
        case 'pageFirstFramePaint':
        case 'pageWillDestroy':
        case 'setNeedLayout':
        case 'onBackPressed':
          // do nothing
          break;
        default:
          this.delegate.sendEvent(event, data);
          break;
      }
    } catch (e) {
      console.error(`[KuiklyPageView] sendEventWithParams: parse error - ${e}`)
    }
  }

  /**
   * Get parsed page data as object
   */
  getPageData(): Record<string, Object> {
    console.log(`[KuiklyPageView] getPageData: parsing pageData`)
    try {
      const data = JSON.parse(this.pageData) as Record<string, Object>
      console.log(`[KuiklyPageView] getPageData: successfully parsed`)
      return data
    } catch (e) {
      console.error(`[KuiklyPageView] getPageData: parse error - ${e}`)
      return {}
    }
  }

  /**
   * Get Kuikly view delegate
   */
  getDelegate(): IKuiklyViewDelegate {
    return this.delegate
  }
}

@Component
export struct KuiklyPageViewComponent {
  @ObjectLink renderView: KuiklyPageView

  build() {
    Stack() {
      if (this.renderView.getViewWidth() > 0 && this.renderView.getViewHeight() > 0) {
        Kuikly({
          pageName: this.renderView.pageName,
          pageData: this.renderView.getPageData(),
          delegate: this.renderView.getDelegate(),
          onControllerReadyCallback: (controller) => {
            this.renderView.onKuiklyControllerReady(controller);
          },
          nativeManager: globalNativeManager,
          initialSize: {
            width: this.renderView.getViewWidth(),
            height: this.renderView.getViewHeight()
          }
        })
      }
    }
  }
}