# Debug 版本数据

## TextPerfComposeDemo profile 分析
Kuikly线程：
Weight	Self Weight	Symbol Names
40.00 ms  23.1%	0 s	                                     kfun:com.tencent.kuikly.core.views.shadow.TextShadow#calculateRenderViewSize(kotlin.Float;kotlin.Float){}com.tencent.kuikly.core.base.Size
17.00 ms   9.8%	                                     kfun:androidx.compose.runtime.Recomposer.composing#internal
52.00 ms  30.1%	                                     kfun:androidx.compose.runtime.ControlledComposition#applyChanges(){}-trampoline
    13.00 ms   7.5%	                                                                            kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$5.$<bridge-DNNNN>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;com.tencent.kuikly.compose.ui.text.TextStyle){}#internal
    24.00 ms  13.9%	                                                                           kfun:androidx.compose.runtime.Applier#insertTopDown(kotlin.Int;1:0){}-trampoline


## TextPerfKuiklyDemo profile 分析
Kuikly线程
74.00 ms  83.1%	                   kfun:com.tencent.kuikly.core.manager.PagerManager#createPager(kotlin.String;kotlin.String;kotlin.String){}
    13.00 ms  14.6%	                                  kfun:com.tencent.kuikly.core.base.ViewContainer#addChild(0:0;kotlin.Function1<0:0,kotlin.Unit>;kotlin.Int){0§<com.tencent.kuikly.core.base.DeclarativeBaseView<*,*>>}
    5.00 ms   5.6%	                       kfun:com.tencent.kuikly.core.pager.Pager#createRenderView(){}-trampoline
    48.00 ms  53.9%	                                  kfun:com.tencent.kuikly.core.views.shadow.TextShadow#calculateRenderViewSize(kotlin.Float;kotlin.Float){}com.tencent.kuikly.core.base.Size-trampoline


# Release 版本数据
## TextPerfComposeDemo profile 分析
98.00 ms  45.8%	                                             -[KRRichTextShadow hrv_calculateRenderViewSizeWithConstraintSize:]
74.00 ms  34.6%	                                        kfun:androidx.compose.runtime.CompositionImpl#setContent(kotlin.Function2<androidx.compose.runtime.Composer,kotlin.Int,kotlin.Unit>){}
    60.00 ms  28.0%	                                       kfun:androidx.compose.runtime.CompositionImpl#applyChanges(){}
        20.00 ms   9.3%	0 s	                                kfun:com.tencent.kuikly.compose.KuiklyApplier#insertTopDown(kotlin.Int;com.tencent.kuikly.compose.ui.node.KNode<com.tencent.kuikly.core.base.DeclarativeBaseView<*,*>>){}
        7.00 ms   3.3%	                                     kfun:com.tencent.kuikly.compose.foundation.text.BasicTextWithNoInlinContent$$inlined$ReusableComposeNode$10.$<bridge-DNNNU>invoke(com.tencent.kuikly.compose.ui.node.ComposeUiNode;kotlin.Int){}#internal
        21.00 ms   9.8%	0 s	                          kfun:com.tencent.kuikly.compose.foundation.text.applyTextStyle#internal    10.00 ms   4.7%	0 s	                                 kfun:com.tencent.kuikly.compose.foundation.text._BasicText#internal
14.00 ms   6.5%	                                   kfun:androidx.compose.runtime.ControlledComposition#composeContent(kotlin.Function2<androidx.compose.runtime.Composer,kotlin.Int,kotlin.Unit>){}-trampoline
    10.00 ms   4.7%                                     kfun:com.tencent.kuikly.compose.foundation.text._BasicText#internal
总计185 ms

## TextPerfKuiklyDemo profile 分析
95.00 ms  73.1%	                                               -[KRRichTextShadow hrv_calculateRenderViewSizeWithConstraintSize:]
12.00 ms   9.2%	                           kfun:com.tencent.kuikly.core.base.ViewContainer#addChild(0:0;kotlin.Function1<0:0,kotlin.Unit>;kotlin.Int){0§<com.tencent.kuikly.core.base.DeclarativeBaseView<*,*>>}
总计107ms 