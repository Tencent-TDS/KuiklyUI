## 1. 框架层：KRFontModule 协议扩展

- [x] 1.1 在 `KRFontModule.h` 的 `KuiklyFontProtocol` 中新增 `@optional` 方法：`hr_fontWithFontFamily:fontSize:fontWeight:contextParams:`
- [x] 1.2 在 `KRFontModule.h/m` 中新增同名类方法，转发到 `gFontHandler`
- [x] 1.3 验证 `@optional` 向后兼容：已有业务方不实现新方法时，旧链路正常运行

## 2. 框架层：KRConvertUtil 优先级调整

- [x] 2.1 在 `+UIFont:` 方法的「静态注册检查」之后、「hr_loadCustomFont 文件注册」之前插入新适配器调用
- [x] 2.2 保证优先级顺序正确：静态注册 > 新适配器 > 文件注册 > componentExpandHandler > 系统默认

## 3. 框架层：KuiklyRenderLayerHandler contextParam 补齐

- [x] 3.1 在 `p_createShadowHandlerWithTag:viewName:` 中调用 `[shadow hrv_setPropWithKey:@"contextParam" propValue:_contextParam]`
- [x] 3.2 验证不走 `setContextParamToShadow:`（有主线程断言），确认 shadow 在 context 线程执行
- [x] 3.3 验证非 TurboDisplay 页面下，shadow 的 `json[@"contextParam"]` 不再为 nil

## 4. Demo 层：KRFontHanlder 示例实现

- [x] 4.1 实现 `hr_fontWithFontFamily:`，完成文件路径拼接、字体加载、PostScript Name 提取、UIFont 返回
- [x] 4.2 将 `registerFontAtLocalURL:` 返回类型从 `BOOL` 改为 `NSString *`（提取的真实 PostScript Name）
- [x] 4.3 在 `registerFontAtLocalURL:` 中加入已注册字体缓存判断，提取 PostScript Name 后先检查是否已注册，再调用 `CTFontManagerRegisterFontsForURL`

## 5. 测试验证（已由业务方验证通过）

- [x] 5.1 测试：fontFamily 为别名（如 "CustomSans"）时，通过适配器成功返回正确 UIFont
- [x] 5.2 测试：fontFamily 恰好为真实 PostScript Name（如 "PingFangSC-Regular"）时，走静态注册路径（优先级 1），不触发适配器
- [x] 5.3 测试：适配器返回 nil 时，降级到旧文件注册路径正常
- [x] 5.4 测试：非 TurboDisplay 页面下，contextParam 正常注入，动态字体可加载
- [x] 5.5 测试：TurboDisplay 页面的字体加载行为不受影响（回归）
- [x] 5.6 测试：字体文件已注册过的场景下，不重复注册（缓存判断生效）
- [x] 5.7 测试：字体文件路径不存在或文件损坏时，不崩溃，正常返回系统默认字体
