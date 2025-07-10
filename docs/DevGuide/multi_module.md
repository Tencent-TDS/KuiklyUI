# Kuikly工程多模块

当项目中存在多个Kuikly工程模块时，直接引入多个独立模块可能会导致入口类冲突的问题。

为解决这一问题，需要采用Kuikly工程的多模块，通过合理的模块化设计来配置Kuikly工程。


## 工程配置

1. 工程引入其他的 `Kuikly` 模块

   工程根目录：`settings.gradle.kts`
    ```settings.gradle.kts
    include(":子模块1")
    include(":子模块2")
    ```

2. 主模块添加子模块依赖，并在 `ksp` 配置 `Kuikly` 多模块信息

    主模块 `build.gradle.kts` 配置
    ```build.gradle.kts
    kotlin {
        ...
        sourceSets {
            val commonMain by getting {
                dependencies {
                    // 添加子模块工程依赖
                    implementation(project(":子模块1"))    
                    implementation(project(":子模块2"))
                }
            }
        }
        ...
    }
    
    ksp {
        arg("moduleId", "shared")                // 模块Id
        arg("isMainModule", "true")              // 是否是主模块
        arg("subModules", "子模块1&子模块2")       // 子模块，用&间隔
        arg("enableMultiModule","true")          // 启用多模块
        ...
    }
   
    ```

3. 子模块在 `ksp` 配置 `Kuikly` 多模块信息
    
    子模块 `build.gradle.kts` 配置
    ```build.gradle.kts
    ksp {
        arg("moduleId", "shared1")        // 标识模块Id
        arg("isMainModule", "false")      // 是否是主模块
        arg("enableMultiModule","true")   // 启用多模块
        ...
    }
   ```    
   其余配置项与主模块相同

    :::tip 注意
    子模块需要依赖主模块编译，不能单独存在
    :::

