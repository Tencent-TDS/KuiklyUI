# 能力全览

本页介绍 Kuikly Compose 的当前开发状态、已支持的 API 情况。

## 当前可用性
- **内置模式**：已在腾讯新闻、地图、IMA 等多个业务，超过 100 个页面线上验证，成熟可直接接入使用。

## 定位与原则

- **与官方 Compose 对齐 API**：保持 API 形态和行为一致（当前约 95%，持续演进），便于直接迁移和使用官方生态。
- **AI 辅助编码友好**：因 API 高度对齐，可直接使用 Cursor / Copilot 等 AI 生成 Compose 代码。
- **跨端一致性与性能**：确保 Android / iOS / HarmonyOS / Web / 小程序一致的交互和性能体验。
- **差异化与扩展能力**：在对齐的基础上，扩展动态化、跨端特性及与 Kuikly Core 的深度协同，提供超出官方 Compose 的能力。

## 标准Compose API支持概览

<table>
  <colgroup>
    <col style="width:15%" />
    <col style="width:17%" />
    <col style="width:68%" />
  </colgroup>
  <thead>
    <tr>
      <th>模块</th>
      <th>支持度</th>
      <th>覆盖API</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>状态管理</td>
      <td>完全支持</td>
      <td>
        官方 <code>androidx.compose.runtime.*</code><br>
        <code>remember</code> / <code>mutableStateOf</code> / <code>derivedStateOf</code><br>
        <code>LaunchedEffect</code> / <code>DisposableEffect</code> / <code>SideEffect</code><br>
        <code>rememberCoroutineScope</code> / <code>rememberUpdatedState</code> 等
      </td>
    </tr>
    <tr>
      <td>布局系统</td>
      <td>完全支持</td>
      <td>
        <strong>基础布局</strong>：<code>Column</code> / <code>Row</code> / <code>Box</code> / <code>BoxWithConstraints</code><br>
        <strong>流式</strong>：<code>FlowRow</code> / <code>FlowColumn</code><br>
        <strong>自定义</strong>：<code>Layout</code><br>
        <strong>修饰符</strong>：<code>padding</code>、<code>size</code>、<code>fillMaxWidth/Height/Size</code>、<code>weight</code> 等
      </td>
    </tr>
    <tr>
      <td>列表与滚动</td>
      <td>完全支持</td>
      <td>
        <strong>列表</strong>：<code>LazyColumn</code> / <code>LazyRow</code><br>
        <strong>网格</strong>：<code>LazyVerticalGrid</code> / <code>LazyHorizontalGrid</code><br>
        <strong>瀑布流</strong>：<code>LazyVerticalStaggeredGrid</code> / <code>LazyHorizontalStaggeredGrid</code><br>
        <strong>轮播</strong>：<code>HorizontalPager</code> / <code>VerticalPager</code>
      </td>
    </tr>
    <tr>
      <td>动画系统</td>
      <td>完全支持</td>
      <td>
        <code>AnimatedVisibility</code>、<code>AnimatedContent</code>、<code>Crossfade</code><br>
        <code>animateContentSize</code>、<code>Transition</code> / <code>updateTransition</code><br>
        <code>animate*AsState</code> 系列
      </td>
    </tr>
    <tr>
      <td>手势系统</td>
      <td>大部分支持</td>
      <td>
        <code>clickable</code> / <code>combinedClickable</code><br>
        <code>draggable</code>、<code>transformable</code><br>
        <code>pointerInput</code> 等常用手势修饰符
      </td>
    </tr>
    <tr>
      <td>Material3 组件</td>
      <td>大部分支持</td>
      <td>
        <strong>基础</strong>：<code>Text</code> / <code>Button</code> / <code>Card</code> / <code>Surface</code><br>
        <strong>结构</strong>：<code>Scaffold</code>、<code>TopAppBar</code> / <code>CenterAlignedTopAppBar</code>、<code>TabRow</code> / <code>ScrollableTabRow</code>、<code>Tab</code><br>
        <strong>表单</strong>：<code>TextField</code>、<code>Checkbox</code>、<code>Switch</code>、<code>Slider</code> / <code>RangeSlider</code><br>
        <strong>反馈</strong>：<code>Snackbar</code> / <code>SnackbarHost</code>、<code>ModalBottomSheet</code>、<code>CircularProgressIndicator</code> / <code>LinearProgressIndicator</code><br>
        <strong>其他</strong>：<code>HorizontalDivider</code> / <code>VerticalDivider</code>、<code>PullToRefresh</code>
      </td>
    </tr>
    <tr>
      <td>其他</td>
      <td>大部分支持</td>
      <td>
        <strong>绘制</strong>：<code>Canvas</code> 自定义绘制<br>
        <strong>修饰符</strong>：常用 <code>Modifier</code><br>
        <strong>架构</strong>：<code>viewModel()</code> 与生命周期感知副作用/状态管理（与官方 Runtime 对齐）
      </td>
    </tr>
  </tbody>
</table>

## 工具与调试

<table>
  <colgroup>
    <col style="width:20%" />
    <col style="width:20%" />
    <col style="width:60%" />
  </colgroup>
  <thead>
    <tr>
      <th>项目</th>
      <th>状态</th>
      <th>说明</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>资源管理</td>
      <td>可用</td>
      <td>与官方 Compose 对齐，支持常规资源加载与管理</td>
    </tr>
    <tr>
      <td>预览</td>
      <td>建设中</td>
      <td>规划提供跨端实时预览与快速迭代能力</td>
    </tr>
    <tr>
      <td>Inspector</td>
      <td>可用</td>
      <td>可直接使用各端原生 Inspector（Android / iOS / HarmonyOS）</td>
    </tr>
    <tr>
      <td>性能工具</td>
      <td>建设中</td>
      <td>规划聚焦重组/Recompose 性能瓶颈的跟踪与定位</td>
    </tr>
  </tbody>
</table>

## 示例代码

我们提供了丰富的 Demo 示例，涵盖组件使用、手势交互、动画效果及列表滚动等核心场景。

- **代码路径**：[demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/](https://github.com/Tencent-TDS/KuiklyUI/tree/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose)
- **特别说明**：所有示例代码均由 AI 直接生成，无需手工调整即可运行。这验证了 Kuikly Compose 的 AI 辅助生成代码目前已处于高可用状态。

## 反馈与贡献
如果你在使用过程中发现：

- API 支持问题
- 行为与官方 Compose 不一致
- 性能问题
- 其他问题或建议

欢迎通过以下方式反馈：

- [GitHub Issues](https://github.com/Tencent-TDS/KuiklyUI/issues)
- 内部反馈渠道（端框架小助手）

你的反馈将帮助我们持续改进 Kuikly Compose。

