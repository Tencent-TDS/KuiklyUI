# Kuikly 社区

欢迎来到``Kuikly``开源社区！这里是开发者交流、学习和协作的平台。无论您是初次了解``Kuikly``，还是已经在项目中使用，都可以在这里找到组件资源、技术文章、问答交流等内容。

## 快速入口

| | 入口 | 说明 |
| --- | --- | --- |
| 🧩 | **[组件市场](https://kuikly.tds.qq.com/third-party?tab=component)** | 浏览和发现社区共享组件 |
| 📝 | **[社区分享](https://kuikly.tds.qq.com/third-party?tab=artical)** | 技术文章、教程与实践经验 |
| 💬 | **[Q&A 问答](https://github.com/Tencent-TDS/KuiklyUI/discussions/categories/q-a)** | 使用疑问交流与解答 |
| 🌐 | **[GitHub 仓库](https://github.com/Tencent-TDS/KuiklyUI)** | 源码、Issue 与 PR |

## 参与社区

``Kuikly``社区欢迎所有形式的参与。您可以：

* **使用组件**：在[组件市场](./component_market.md)找到现成的社区组件，快速集成到您的项目中。
* **阅读与分享**：浏览[社区分享](./tech_sharing.md)中的技术文章，也欢迎分享您的实践经验。
* **提问与讨论**：在 [Q&A 问答](https://github.com/Tencent-TDS/KuiklyUI/discussions/categories/q-a)板块交流使用疑问，也可以帮助解答其他开发者的问题。
* **报告问题**：在 [GitHub Issues](https://github.com/Tencent-TDS/KuiklyUI/issues) 提交 Bug 报告或功能建议。

## 贡献指南

如果您希望更深入地参与``Kuikly``的发展，以下是主要的贡献方式：

> 🏅 ``Kuikly``社区设有[贡献者荣誉激励机制](./contributor_role.md)，对持续贡献者授予专属头衔、物质激励等权益，详情请查阅。

### 贡献代码

**寻找合适的任务**：新手可以从带有"good first issue"标签的问题开始，这些问题通常专门为新手贡献者设计，难度较低。也可以查看带有"Help Wanted"标签的issue参与贡献。

**设置开发环境**：在开始编码前，请先阅读[README 源码编译](https://github.com/Tencent-TDS/KuiklyUI)指引，确保您的本地环境能够正确构建和测试Kuikly。

**Fork仓库**：在正式修改代码前，请先Fork仓库，在Fork的仓库进行编码、测试。

**代码规范与质量**：``Kuikly``遵循Kotlin官方代码规范，请在提交前确保代码风格一致。新增功能应包含适当的测试用例，确保功能稳定性和长期维护性。

**提交Pull Request**：PR应包含清晰的目的描述、相关Issue链接、测试计划和任何可能影响的范围。请确保您的提交信息遵循[Contribution Guide](../../CONTRIBUTING.md)约定格式，如"feat: 添加新组件"、"fix: 修复XX问题"等。

### 贡献组件

开发基于``Kuikly``的通用组件或业务组件，贡献到社区生态仓库。优质组件可能被纳入官方推荐生态，获得更多曝光和使用。详见[社区组件](./component_market.md)页面。

### 知识分享

技术分享不限于篇幅，大到技术方案、小到解决某个具体问题的心得，都欢迎随时共享到社区。详见[社区分享](./tech_sharing.md)页面。

### 文档贡献

**文档位置**：``Kuikly``文档位于项目``docs``目录，以``markdown``格式编写。如需增加图片，请存放在对应目录下的``img``文件夹中。

**增删导航栏**：导航栏配置在``docs/navbar/zh.ts``文件，新增条目需同步更新。

**增删文档**：在某个导航栏条目下增删``markdown``文件时，请同步更新``docs/sidebar/zh.ts``配置。
