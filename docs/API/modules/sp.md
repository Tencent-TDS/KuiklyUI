# SharedPreferencesModule

磁盘键值对缓存模块, 适用于数据量小的键值对缓存

## setString方法

缓存字符串类型键值对

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |
| value <Badge text="必需" type="warn"/> | 缓存值  | String |

## setFloat方法

缓存Float类型键值对

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |
| value <Badge text="必需" type="warn"/> | 缓存值  | Float? |

## setInt方法

缓存Int类型键值对

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |
| value <Badge text="必需" type="warn"/> | 缓存值  | Int? |

## setObject方法

缓存**JSONObject**类型键值对

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |
| value <Badge text="必需" type="warn"/> | 缓存值  | JSONObject? |

## getString方法

获取key对应的String?类型的缓存值

<br/>

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |

## getInt方法

获取key对应的Int?类型的缓存值

<br/>

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |

## getFloat方法

获取key对应的Float?类型缓存值

<br/>

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |

## getObject方法

获取key对应的JSONObject?类型缓存值

<br/>

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |