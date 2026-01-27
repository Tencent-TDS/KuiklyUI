# iOS Compose 对比 kuikly 多的耗时大头分析： 

Weight	Symbol Names
36.00 ms  100.0%	                                                                      kfun:kotlin.Function2#invoke(1:0;1:1){}1:2-trampoline
21.00 ms  58.3%	                                                                       kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$5.$<bridge-DNNNN>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;com.tencent.kuikly.compose.ui.text.TextStyle){}#internal
21.00 ms  58.3%	                                                                        kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$5.invoke#internal
21.00 ms  58.3%	                                                                         <inlined-out:withTextView>
21.00 ms  58.3%	                                                                          kfun:kotlin#run__at__0:0(kotlin.Function1<0:0,0:1>){0§<kotlin.Any?>;1§<kotlin.Any?>}0:1
21.00 ms  58.3%	                                                                           <inlined-lambda>
21.00 ms  58.3%	                                                                            <inlined-out:run>
21.00 ms  58.3%	                                                                             <inlined-lambda>
21.00 ms  58.3%	                                                                              <inlined-out:run>
21.00 ms  58.3%	                                                                               <inlined-lambda>
21.00 ms  58.3%	                                                                                kfun:com.tencent.kuikly.compose.foundation.text.applyTextStyle#internal
4.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.compose.foundation.text.applyTextIndent#internal
4.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#firstLineHeadIndent(kotlin.Float){}com.tencent.kuikly.core.views.TextAttr
4.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
4.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
4.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
4.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
4.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
2.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
2.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
2.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                                  (anonymous namespace)::slowPath(kotlin::mm::ThreadData&)
2.00 ms   0.0%	                                                                                                   (anonymous namespace)::safePointActionImpl(kotlin::mm::ThreadData&)
2.00 ms   0.0%	                                                                                                    kotlin::mm::ThreadSuspensionData::suspendIfRequested()
2.00 ms   0.0%	                                                                                                     kotlin::gc::mark::ParallelMark::parallelMark(kotlin::ParallelProcessor<kotlin::intrusive_forward_list<kotlin::gc::GC::ObjectData, kotlin::DefaultIntrusiveForwardListTraits<kotlin::gc::GC::ObjectData>>, 512ul, 4096ul>::Worker&)
1.00 ms   0.0%	                                                                                                      Kotlin_processObjectInMark
1.00 ms   0.0%	                                                                                                      Kotlin_processEmptyObjectInMark
2.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
2.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
2.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
2.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
2.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
1.00 ms   0.0%	                                                                                                      -[__NSDictionaryM objectForKeyedSubscript:]
1.00 ms   0.0%	                                                                                                       __CFNumberHash
1.00 ms   0.0%	                                                                                                        __CFNumberGetValue
4.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#color(com.tencent.kuikly.core.base.Color){}com.tencent.kuikly.core.views.TextAttr-trampoline
4.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#color(com.tencent.kuikly.core.base.Color){}com.tencent.kuikly.core.views.TextAttr
4.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
4.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
4.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
3.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
2.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
2.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
2.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    -[__NSArrayI dealloc]
1.00 ms   0.0%	                                                                                                     objc_msgSend
1.00 ms   0.0%	                                                                                                  Kotlin_ObjCExport_CreateRetainedNSStringFromKString
1.00 ms   0.0%	                                                                                                   __CFStringCreateImmutableFunnel3
1.00 ms   0.0%	                                                                                                    _CFRuntimeCreateInstance
1.00 ms   0.0%	                                                                                                     _xzm_xzone_malloc
1.00 ms   0.0%	                                                                                      kfun:kotlin.collections#set__at__kotlin.collections.MutableMap<0:0,0:1>(0:0;0:1){0§<kotlin.Any?>;1§<kotlin.Any?>}
1.00 ms   0.0%	                                                                                       kfun:kotlin.collections.MutableMap#put(1:0;1:1){}1:1?-trampoline
1.00 ms   0.0%	                                                                                        kfun:kotlin.collections.HashMap#put(1:0;1:1){}1:1?
1.00 ms   0.0%	                                                                                         kfun:kotlin.collections.HashMap.allocateValuesArray#internal
1.00 ms   0.0%	                                                                                          (anonymous namespace)::slowPath()
1.00 ms   0.0%	                                                                                           (anonymous namespace)::safePointActionImpl(kotlin::mm::ThreadData&)
1.00 ms   0.0%	                                                                                            kotlin::mm::ThreadSuspensionData::suspendIfRequested()
1.00 ms   0.0%	                                                                                             kotlin::gc::mark::ParallelMark::parallelMark(kotlin::ParallelProcessor<kotlin::intrusive_forward_list<kotlin::gc::GC::ObjectData, kotlin::DefaultIntrusiveForwardListTraits<kotlin::gc::GC::ObjectData>>, 512ul, 4096ul>::Worker&)
1.00 ms   0.0%	                                                                                              Kotlin_processArrayInMark
3.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.compose.foundation.text.applyFontStyle#internal
3.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#fontStyleNormal(){}com.tencent.kuikly.core.views.TextAttr
3.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
3.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
3.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
3.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
3.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
3.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
3.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
3.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
3.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
3.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
3.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
3.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
3.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
3.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
2.00 ms   0.0%	                                                                                                      __47-[KuiklyRenderCore p_initShadowMethodRegisters]_block_invoke_3
1.00 ms   0.0%	                                                                                                       objc_msgSend
1.00 ms   0.0%	                                                                                                       -[KuiklyRenderLayerHandler setShadowPropWithTag:propKey:propValue:]
1.00 ms   0.0%	                                                                                                        -[KRRichTextShadow hrv_setPropWithKey:propValue:]
1.00 ms   0.0%	                                                                                                         -[__NSDictionaryM setObject:forKeyedSubscript:]
1.00 ms   0.0%	                                                                                                          -[NSTaggedPointerString isEqual:]
1.00 ms   0.0%	                                                                                                           _NSIsNSString
1.00 ms   0.0%	                                                                                                      -[KuiklyRenderCore p_shouldSyncCallWithWithMethod:args:]
1.00 ms   0.0%	                                                                                                       objc_retain
3.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.compose.foundation.text.applyTextAlign#internal
3.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#textAlignLeft(){}com.tencent.kuikly.core.views.TextAttr
3.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
3.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
3.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
3.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
3.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
3.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
3.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
3.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
3.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
3.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
2.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
1.00 ms   0.0%	                                                                                                      -[__NSDictionaryM objectForKeyedSubscript:]
1.00 ms   0.0%	                                                                                                       objc_msgSend
1.00 ms   0.0%	                                                                                                    _CFRelease
1.00 ms   0.0%	                                                                                                  _CFRelease
1.00 ms   0.0%	                                                                                                   malloc_zone_free
2.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.compose.foundation.text.applyShadow#internal
1.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.compose.ui.graphics.Color#toKuiklyColor(){}com.tencent.kuikly.core.base.Color
1.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.compose.ui.graphics#toArgb__at__com.tencent.kuikly.compose.ui.graphics.Color(){}kotlin.Int
1.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.compose.ui.graphics.Color#convert(com.tencent.kuikly.compose.ui.graphics.colorspace.ColorSpace){}com.tencent.kuikly.compose.ui.graphics.Color
1.00 ms   0.0%	                                                                                     kfun:kotlin.Any#equals(kotlin.Any?){}kotlin.Boolean-trampoline
1.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.compose.ui.graphics.colorspace.Rgb#equals(kotlin.Any?){}kotlin.Boolean
1.00 ms   0.0%	                                                                                       _tlv_get_addr
1.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#textShadow(kotlin.Float;kotlin.Float;kotlin.Float;com.tencent.kuikly.core.base.Color){}com.tencent.kuikly.core.views.TextAttr
1.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
1.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
1.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
1.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x10664d86c (iosApp +0x1aa98b9) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   +[NSNumber allocWithZone:]
2.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#letterSpacing(kotlin.Float){}com.tencent.kuikly.core.views.TextAttr-trampoline
2.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#letterSpacing(kotlin.Float){}com.tencent.kuikly.core.views.TextAttr
2.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
2.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
2.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
2.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
1.00 ms   0.0%	                                                                                                      objc_msgSend
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
1.00 ms   0.0%	                                                                                                      __47-[KuiklyRenderCore p_initShadowMethodRegisters]_block_invoke_3
1.00 ms   0.0%	                                                                                                       -[KuiklyRenderLayerHandler setShadowPropWithTag:propKey:propValue:]
1.00 ms   0.0%	                                                                                                        -[KRRichTextShadow hrv_setPropWithKey:propValue:]
1.00 ms   0.0%	                                                                                                         -[__NSDictionaryM setObject:forKeyedSubscript:]
1.00 ms   0.0%	                                                                                                          __CFStringCreateImmutableFunnel3
2.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.compose.foundation.text.applyFontWeight#internal
2.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#fontWeightNormal(){}com.tencent.kuikly.core.views.TextAttr
2.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
2.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
2.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
2.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
1.00 ms   0.0%	                                                                                                      __47-[KuiklyRenderCore p_initShadowMethodRegisters]_block_invoke_3
1.00 ms   0.0%	                                                                                                       objc_loadWeakRetained
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  __CFStringCreateImmutableFunnel3
1.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#fontSize$default(kotlin.Float;kotlin.Boolean?;kotlin.Int){}com.tencent.kuikly.core.views.TextAttr
1.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#fontSize(kotlin.Float;kotlin.Boolean?){}com.tencent.kuikly.core.views.TextAttr-trampoline
1.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.views.TextAttr#fontSize(kotlin.Float;kotlin.Boolean?){}com.tencent.kuikly.core.views.TextAttr
1.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
1.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
1.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                               kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                 kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                  DYLD-STUB$$objc_release
7.00 ms  19.4%	                                                                       kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$10.$<bridge-DNNNU>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;kotlin.Int){}#internal
7.00 ms  19.4%	                                                                        kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$10.invoke#internal
7.00 ms  19.4%	                                                                         <inlined-out:withTextView>
7.00 ms  19.4%	                                                                          kfun:kotlin#run__at__0:0(kotlin.Function1<0:0,0:1>){0§<kotlin.Any?>;1§<kotlin.Any?>}0:1
7.00 ms  19.4%	                                                                           <inlined-lambda>
7.00 ms  19.4%	                                                                            <inlined-out:run>
7.00 ms  19.4%	                                                                             <inlined-lambda>
7.00 ms  19.4%	                                                                              <inlined-out:run>
7.00 ms  19.4%	                                                                               <inlined-lambda>
7.00 ms  19.4%	                                                                                kfun:com.tencent.kuikly.compose.foundation.text.applyMaxLines#internal
7.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#lines(kotlin.Int){}com.tencent.kuikly.core.views.TextAttr-trampoline
7.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#lines(kotlin.Int){}com.tencent.kuikly.core.views.TextAttr
7.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
7.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
7.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
7.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
7.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
5.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
5.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
5.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
5.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
5.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
5.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
5.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
5.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
5.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
5.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
4.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
4.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
3.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
3.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
3.00 ms   0.0%	                                                                                                      __47-[KuiklyRenderCore p_initShadowMethodRegisters]_block_invoke_3
3.00 ms   0.0%	                                                                                                       -[KuiklyRenderLayerHandler setShadowPropWithTag:propKey:propValue:]
2.00 ms   0.0%	                                                                                                        -[KuiklyRenderLayerHandler p_shadowHandlerWithTag:]
2.00 ms   0.0%	                                                                                                         -[__NSDictionaryM objectForKeyedSubscript:]
1.00 ms   0.0%	                                                                                                          objc_msgSend
1.00 ms   0.0%	                                                                                                          -[__NSCFNumber isEqualToNumber:]
1.00 ms   0.0%	                                                                                                           objc_msgSend$compare:
1.00 ms   0.0%	                                                                                                        -[KRRichTextShadow hrv_setPropWithKey:propValue:]
1.00 ms   0.0%	                                                                                                         -[__NSDictionaryM setObject:forKeyedSubscript:]
1.00 ms   0.0%	                                                                                                          __CFStringCreateImmutableFunnel3
1.00 ms   0.0%	                                                                                                    +[KRConvertUtil nativeObjectToKotlinObject:]
1.00 ms   0.0%	                                                                                                     +[KRConvertUtil hr_isJsonArray:]
1.00 ms   0.0%	                                                                                                  __CFStringCreateImmutableFunnel3
1.00 ms   0.0%	                                                                                                   _CFRuntimeCreateInstance
2.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
2.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
2.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                             kfun:kotlin.collections.MutableMap#get(1:0){}1:1?-trampoline
1.00 ms   0.0%	                                                                                              kfun:kotlin.collections.HashMap#get(1:0){}1:1?
1.00 ms   0.0%	                                                                                               kfun:kotlin.collections.HashMap.findKey#internal
1.00 ms   0.0%	                                                                                                kfun:kotlin.collections.HashMap.hash#internal
1.00 ms   0.0%	                                                                                                 kfun:kotlin.Any#hashCode(){}kotlin.Int-trampoline
3.00 ms   8.3%	                                                                       kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$2.$<bridge-DNNNN>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;kotlin.String?){}#internal
3.00 ms   8.3%	                                                                        kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$2.invoke#internal
3.00 ms   8.3%	                                                                         <inlined-out:withTextView>
3.00 ms   8.3%	                                                                          kfun:kotlin#run__at__0:0(kotlin.Function1<0:0,0:1>){0§<kotlin.Any?>;1§<kotlin.Any?>}0:1
3.00 ms   8.3%	                                                                           <inlined-lambda>
3.00 ms   8.3%	                                                                            <inlined-out:run>
3.00 ms   8.3%	                                                                             <inlined-lambda>
3.00 ms   8.3%	                                                                              <inlined-out:run>
3.00 ms   8.3%	                                                                               <inlined-lambda>
3.00 ms   8.3%	                                                                                kfun:com.tencent.kuikly.core.views.RichTextAttr#text(kotlin.String){}com.tencent.kuikly.core.views.TextAttr-trampoline
3.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#text(kotlin.String){}com.tencent.kuikly.core.views.TextAttr
3.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
3.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
3.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
3.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
2.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
2.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                              kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
2.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
2.00 ms   0.0%	                                                                                                kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                                 0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
2.00 ms   0.0%	                                                                                                  -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
2.00 ms   0.0%	                                                                                                   __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
2.00 ms   0.0%	                                                                                                    -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
2.00 ms   0.0%	                                                                                                     __47-[KuiklyRenderCore p_initShadowMethodRegisters]_block_invoke_3
2.00 ms   0.0%	                                                                                                      -[KuiklyRenderLayerHandler setShadowPropWithTag:propKey:propValue:]
1.00 ms   0.0%	                                                                                                       -[KRRichTextShadow hrv_setPropWithKey:propValue:]
1.00 ms   0.0%	                                                                                                        -[__NSDictionaryM setObject:forKeyedSubscript:]
1.00 ms   0.0%	                                                                                                         __CFStringCreateImmutableFunnel3
1.00 ms   0.0%	                                                                                                       -[KuiklyRenderLayerHandler p_shadowHandlerWithTag:]
1.00 ms   0.0%	                                                                                                        -[__NSDictionaryM objectForKeyedSubscript:]
1.00 ms   0.0%	                                                                                                         -[__NSCFNumber isEqualToNumber:]
1.00 ms   0.0%	                                                                                                          -[__NSCFNumber compare:]
1.00 ms   0.0%	                                                                                                           -[__NSCFNumber longLongValue]
1.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.utils#checkThread(kotlin.String;kotlin.String){}
3.00 ms   8.3%	                                                                       kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$9.$<bridge-DNNNU>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;com.tencent.kuikly.compose.ui.text.style.TextOverflow){}#internal
3.00 ms   8.3%	                                                                        kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$9.invoke#internal
3.00 ms   8.3%	                                                                         <inlined-out:withTextView>
3.00 ms   8.3%	                                                                          kfun:kotlin#run__at__0:0(kotlin.Function1<0:0,0:1>){0§<kotlin.Any?>;1§<kotlin.Any?>}0:1
3.00 ms   8.3%	                                                                           <inlined-lambda>
3.00 ms   8.3%	                                                                            <inlined-out:run>
3.00 ms   8.3%	                                                                             <inlined-lambda>
3.00 ms   8.3%	                                                                              <inlined-out:run>
3.00 ms   8.3%	                                                                               <inlined-lambda>
3.00 ms   8.3%	                                                                                kfun:com.tencent.kuikly.compose.foundation.text.applyOverflow#internal
3.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#textOverFlowClip(){}com.tencent.kuikly.core.views.TextAttr
3.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
3.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
3.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
3.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
3.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
3.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
3.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
3.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
3.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
3.00 ms   0.0%	                                                                                              kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
3.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
3.00 ms   0.0%	                                                                                                kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
2.00 ms   0.0%	                                                                                                 0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
2.00 ms   0.0%	                                                                                                  -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                   __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                    objc_release
1.00 ms   0.0%	                                                                                                   +[NSArray arrayWithObjects:count:]
1.00 ms   0.0%	                                                                                                    __NSArrayI_new
1.00 ms   0.0%	                                                                                                 0x10664d86c (iosApp +0x1aa98b9) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
2.00 ms   5.6%	                                                                       kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$8.$<bridge-DNNNU>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;kotlin.Boolean){}#internal
2.00 ms   5.6%	                                                                        kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$8.invoke#internal
2.00 ms   5.6%	                                                                         <inlined-out:withTextView>
2.00 ms   5.6%	                                                                          kfun:kotlin#run__at__0:0(kotlin.Function1<0:0,0:1>){0§<kotlin.Any?>;1§<kotlin.Any?>}0:1
2.00 ms   5.6%	                                                                           <inlined-lambda>
2.00 ms   5.6%	                                                                            <inlined-out:run>
2.00 ms   5.6%	                                                                             <inlined-lambda>
2.00 ms   5.6%	                                                                              <inlined-out:run>
2.00 ms   5.6%	                                                                               <inlined-lambda>
2.00 ms   5.6%	                                                                                kfun:com.tencent.kuikly.compose.foundation.text.applySoftWrap#internal
2.00 ms   0.0%	                                                                                 kfun:com.tencent.kuikly.core.views.TextAttr#textOverFlowWordWrapping(){}com.tencent.kuikly.core.views.TextAttr-trampoline
2.00 ms   0.0%	                                                                                  kfun:com.tencent.kuikly.core.views.TextAttr#textOverFlowWordWrapping(){}com.tencent.kuikly.core.views.TextAttr
2.00 ms   0.0%	                                                                                   kfun:com.tencent.kuikly.core.base.Props#with__at__kotlin.String(kotlin.Any){}
2.00 ms   0.0%	                                                                                    kfun:com.tencent.kuikly.core.base.Props.bindProp#internal
2.00 ms   0.0%	                                                                                     kfun:com.tencent.kuikly.core.base.Props#setProp(kotlin.String;kotlin.Any){}
2.00 ms   0.0%	                                                                                      kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}-trampoline
2.00 ms   0.0%	                                                                                       kfun:com.tencent.kuikly.core.views.RichTextView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.views.shadow.TextShadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.Shadow#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setShadowProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#callNativeMethod$default(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Int){}kotlin.Any?
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:kotlin.collections.MutableMap#get(1:0){}1:1?-trampoline
1.00 ms   0.0%	                                                                                              kfun:kotlin.collections.HashMap#get(1:0){}1:1?
1.00 ms   0.0%	                                                                                               _tlv_get_addr
1.00 ms   0.0%	                                                                                        kfun:com.tencent.kuikly.core.base.AbstractBaseView#didSetProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                         kfun:com.tencent.kuikly.core.base.RenderView#setProp(kotlin.String;kotlin.Any){}
1.00 ms   0.0%	                                                                                          kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                           kfun:com.tencent.kuikly.core.manager.BridgeManager#setViewProp(kotlin.String;kotlin.Int;kotlin.String;kotlin.Any;kotlin.Int;kotlin.Int){}
1.00 ms   0.0%	                                                                                            kfun:com.tencent.kuikly.core.manager.BridgeManager.callNativeMethod#internal
1.00 ms   0.0%	                                                                                             kfun:com.tencent.kuikly.core.nvi.NativeBridge#toNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                              kfun:com.tencent.kuikly.core.nvi.NativeBridge.IOSNativeBridgeDelegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                               kfun:KuiklyCoreEntry.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1.KuiklyCoreEntry$callKotlinMethod$callKotlinClosure$1$invoke$1.callNative#internal
1.00 ms   0.0%	                                                                                                kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?-trampoline
1.00 ms   0.0%	                                                                                                 kotlin2objc_kfun:KuiklyCoreEntry.Delegate#callNative(kotlin.Int;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?;kotlin.Any?){}kotlin.Any?
1.00 ms   0.0%	                                                                                                  0x1065224a8 (iosApp +0x197e4bf) <A46C6E4A-E49C-35E0-8F8E-05E65428BB4A>
1.00 ms   0.0%	                                                                                                   -[KuiklyRenderFrameworkContextHandler callNativeMethodId:arg0:arg1:arg2:arg3:arg4:arg5:]
1.00 ms   0.0%	                                                                                                    __72-[KuiklyRenderCore p_initContextHandlerWithContextCode:pageName:params:]_block_invoke
1.00 ms   0.0%	                                                                                                     -[KuiklyRenderCore p_performNativeMethodWithMethod:args:]
1.00 ms   0.0%	                                                                                                      -[KuiklyRenderUIScheduler addTaskToMainQueueWithTask:]
1.00 ms   0.0%	                                                                                                       _Block_copy
1.00 ms   0.0%	                                                                                                        _xzm_xzone_malloc_freelist_outlined