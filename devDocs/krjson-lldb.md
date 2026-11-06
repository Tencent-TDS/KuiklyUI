# LLDB 查看 KRJSON

> 框架开发时阅读。`KRJSON` 在调试器里是 `uint64_t`，变量窗口默认只显示一个整数。`krjson_lldb.py` 按 `Box.h` 的标签把它解成具体值。

脚本路径：

`core-render-ohos/src/main/cpp/libohos_render/utils/json/krjson_lldb.py`

## 加载

在 DevEco 的 LLDB Startup Commands 里，和 `konan_lldb.py` 放在一起：

```shell
command script import <仓库路径>/core-render-ohos/src/main/cpp/libohos_render/utils/json/krjson_lldb.py
```

断住之后，把上面的 import 贴进 LLDB 窗口即可。窗口里已经有 `(lldb)` 提示符。

加载成功会打印 `krjson_lldb: loaded, arrays expand`。调试过程中改过脚本后，要再 import 一次。控制台仍是旧提示 `installed summary for KRJSON` 时，当前会话还在用旧模块，数组和对象都展不开。

## 变量窗口

C 或 C++ 帧里，局部变量的类型名是 `KRJSON` 时，窗口直接显示摘要。例如 `null`、`bool true`、`int32 -1` 和 `string "hi" rc=1`。

数组摘要是 `array len=N rc=1`。点开后每一项仍是 `KRJSON`。对象摘要是 `object len=N rc=1`。点开后子项的名字是键，值仍是 `KRJSON`。键按鸿蒙 libc++ ABI 1 的 `std::u16string` 读取。

摘要只读进程内存，不调用 `KRJSONRetain` 或 `KRJSONRelease`。

Kotlin 帧里，这个值的类型是 `Long` 或 `ULong`。摘要不会自动生效。用命令看：

```shell
(lldb) krjson bits
```

把 `bits` 换成当前变量名。命令还会打印一行 `raw`，后面是寄存器里的 64 位原值。这个原值也可以再传给 `krjson`，十进制或 `0x` 十六进制都可以。以 int32 存的 JSON 整数 `-1`，原值是 `0xffffffffffffff9a`。

C 帧里如果仍然是一长串十进制，执行 `frame variable -T 变量名`。类型名应是 `KRJSON`。调试信息把 typedef 收成 `unsigned long long` 时，继续用 `krjson` 命令。

## 打印整棵树

```shell
(lldb) krjson dump bits
```

`krjson dump` 会在 App 进程里调用 `KRJSONDump`，读完后调用 `KRJSONFreeString`。原值非法时可能把进程打崩。先用 `krjson` 看摘要，确认是合法值再 dump。

若提示找不到 `KRJSONDump`，确认 `libohos_render.so` 已加载，并且当前是带符号的 debug 包。

## 样本

`ohosApp/entry/src/main/cpp/napi_init.cpp` 里的 `KRJSONLldbInspect` 会构造各类型的样本。模块加载时会走进这个函数。在 `g_krjson_lldb_inspect_sink = 1` 这一行下断点。局部变量的类型名都是 `KRJSON`。断点若落在函数入口，先执行到这一行，前面的局部变量才构造完。这批值会留到进程结束。之后也可以再执行：

```shell
(lldb) breakpoint set --name KRJSONLldbInspect
(lldb) expr KRJSONLldbInspect()
```

`array_value` 的第 3 项（下标 2）是 `int32 -1`。它的编码最高位是 1。旧脚本用 Python 整数构造子项时，DevEco LLDB 会在这一项上报错。重新 import 并看到 `arrays expand` 之后，该项应显示 `int32 -1`。
