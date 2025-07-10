# Module概述

``Kuikly``作为一个跨端的UI框架, 他本身不具备调用平台API的能力, 但是``Kuikly``提供了一套``Module``机制，你可以通过``Module``机制将平台的API暴露给``Kuikly``侧调用。
``Kuikly``内置了一些通用的``Module``, 如果这些``Module``不满足你业务诉求时, 你可以通过[扩展原生API](../../DevGuide/expand-native-api.md)来自定义``Module``, 将更多的宿主平台API暴露给``Kuikly``侧使用。
下面是``Kuikly``内置的``Module``

* [MemoryCacheModule](memory-cache.md)
* [SharedPreferencesModule](sp.md)
* [RouterModule](router.md)
* [NetworkModule](network.md)
* [NotifyModule](notify.md)
* [SnapshotModule](snapshot.md)
* [CodecModule](codec.md)
* [CalendarModule](calendar.md)