import {sidebar} from "vuepress-theme-hope";

export const zhSidebar = sidebar({
    "/Introduction": [
            {
                text: "",
                prefix: "/Introduction",
                collapsible: false,
                children: [
                    {
                        text: "架构介绍",
                        collapsible: false,
                        children: ["arch.md"]
                    },
                    {
                        text: "跨端工程模式",
                        collapsible: false,
                        children: ["paradigm.md"]
                    },
                    {
                        text: "应用场景案例",
                        collapsible: false,
                        children: ["application_cases.md"]
                    },
                    {
                        text: "Demo体验",
                        collapsible: false,
                        children: ["demo_experience.md"]
                    },
                ],
            },
        ],
    "/QuickStart": [
        {
            text: "",
            prefix: "/QuickStart",
            collapsible: false,
            children: [
                {
                    text: "环境搭建",
                    collapsible: false,
                    children: ["env-setup.md"]
                },
                {
                    text: "编写第一个Kuikly页面",
                    collapsible: false,
                    children: ["hello-world.md"]
                },
                {
                    text: "Kuikly接入",
                    collapsible: false,
                    children: ["overview.md", "privacy-policy.md", "common.md", "android.md", "iOS.md", "harmony.md", "H5.md", "Miniapp.md", "Mac.md"]
                },

                
            ],
        },
    ],
    "/DevGuide": [
        {
            text: "",
            prefix: "/DevGuide",
            children: [
                {
                    text: "入门指南",
                    collapsible: false,
                    children: ["dev-guide-overview.md", "pager.md", "page-data.md", "pager-lifecycle.md", "pager-event.md", "attr.md", "event.md", "reactive-update.md",
                            "directive.md", "view-ref.md", "compose-view.md", "compose-view-lifecycle.md", "set-timeout.md", "module.md", "open-and-close-page.md",
                            "notify.md", "network.md", "assets-resource.md","back-press-handler.md", "platform-utils.md"
                    ]
                },
                {
                    text: "布局教程",
                    collapsible: false,
                    children: ["flexbox-basic.md", "flexbox-in-action.md"]
                },

                {
                    text: "进阶教程",
                    collapsible: false,
                    children : [
                        {
                            text: "",
                            children : [
                                {
                                    text: "动画",
                                    collapsible: true,
                                    children: ["animation-basic", "animation-declarative.md", "animation-imperative.md"]
                                },
                                {
                                    text: "扩展Native能力",
                                    collapsible: true,
                                    children: ["expand-native-api.md", "expand-native-ui.md"]
                                },
                                {
                                    text: "动态化",
                                    collapsible: true,
                                    children: ["dynamic-guide.md"]
                                },
                                {
                                    text: "性能优化",
                                    collapsible: true,
                                    children: ["kuikly-perf-guidelines.md", "android-start-guide.md", "turboDisplay.md"]
                                }
                            ]
                        },
                        "view-external-prop.md",
                        "text-measure.md",
                        "text-post-processor-guide.md",
                        "font-size-and-display-scale.md",
                        "get-component-size-and-position.md",
                        "protobuf.md",
                        "thread-and-coroutines.md",
                        "multi-page.md",
                        "override-builtin-view.md"]
                },
                {
                    text: "工程与集成",
                    children : [
                        {
                            text: "",
                            children : [
                                {
                                    text: "开发方式与集成",
                                    collapsible: true,
                                    children : ["dev-overview.md", "android-dev.md", "ios-dev.md", "harmony-dev.md", "h5-dev.md", "miniapp-dev.md", "miniapp-wx-components.md"]
                                },
                                {
                                    text: "调试与工具",
                                    collapsible: true,
                                    children: ["android-debug.md", "iOS-debug.md", "ohos-debug.md", "h5-debug.md", "miniapp-debug.md"]
                                },
                            ]
                        },
                        "multi_module.md",
                        "version_skills.md",
                        "compile_skills.md"
                    ]
                },

                {
                    text: "KuiklyBase基建",
                    collapsible: false,
                    children: [
                        {
                            text: "",
                            children : [
                                {
                                    text: "堆栈捕获和还原",
                                    collapsible: true,
                                    children: ["kuiklybase-feat-stack-symbolication.md", "symbol-iOS.md", "ohos-kn-stack-symbolication.md", "ohos-kn-stack-symbolication-report.md", ]
                                },
                                {
                                    text: "脚手架和插件",
                                    collapsible: true,
                                    children: ["kuiklybase-feat-scaffolding.md", "as-plugin.md"]
                                },
                            ]
                        },
                        "kuiklybase-ohos-kn.md", "kuiklybase-feat-remaining.md"
                    ]
                },
                {
                    text: "Web教程",
                    collapsible: false,
                    children: [
                        "web-import-jssdk.md", "h5-image-path.md", "h5-spa-demo.md", "h5-custom-font.md", "h5-css-class.md"
                    ]
                }
            ],
        },
    ],

    "/API": [
        {
            text: "组件",
            prefix: "/API/components",
            children: [
                "override.md", "basic-attr-event.md", "pager.md", "ios26-liquid-glass.md", "view.md", "safe-area.md", "transition-view.md", "text.md", "rich-text.md", "text-selection.md", "image.md", "input.md", "text-area.md",  "canvas.md",
                "button.md", "scroller.md", "list.md", "waterfall-list.md", "slider-page.md", "page-list.md", "modal.md", "refresh.md",
                "footer-refresh.md", "date-picker.md", "scroll-picker.md", "slider.md", "switch.md", "blur.md",
                "activity-indicator.md", "hover.md", "mask.md", "checkbox.md", "pag.md","apng.md", "tabs.md","alert-dialog.md","action-sheet.md", "video.md",
                "liquid-glass.md", "glass-effect-container.md", "ios-segmented-control.md"
            ],
        },
        {
            text: "Module",
            prefix: "/API/modules",
            children: [
                "overview.md", 'memory-cache.md', "sp.md", "router.md", "network.md", "notify.md", "snapshot.md","codec.md","calendar.md", "performance.md", "turbo-display.md", "vsync.md", "back-press.md", "font.md", "file.md", "log.md"
            ]
        }
    ],
    "/Compose": [
        {
            text: "入门",
            prefix: "/Compose",
            collapsible: false,
            children: [
                "overview.md",
                "getting-started.md",
                "how-to-read.md",
                "status.md"
            ]
        },
        {
            text: "开发指南",
            prefix: "/Compose",
            collapsible: false,
            children: [
                "status-management.md",
                "layout.md",
                "theming.md",
                "canvas-drawing.md",
                "list-and-scroll.md",
                "core-components.md",
                "extended-components.md",
                "text-input.md",
                "accessibility.md",
                "kuikly-modifiers.md",
                "text-selection.md",
                "modifier.md",
                "animation-system.md",
                "native-animation.md",
                "gesture-system.md",
                "thread-and-coroutines.md",
                "view-model.md"
            ]
        },
        {
            text: "集成与扩展",
            prefix: "/Compose",
            collapsible: false,
            children: [
                "extend-kuikly-dsl-ui.md",
                "extend-native-ui.md",
                "extend-native-api.md"
            ]
        },
        {
            text: "工具链",
            prefix: "/Compose",
            collapsible: false,
            children: [
                "resource-management.md",
                "preview.md",
                "ui-inspector.md",
                "recomposition-performance.md"
            ]
        },
        {
            text: "FAQ",
            prefix: "/Compose",
            collapsible: false,
            children: [
                "faq.md"
            ]
        }
    ],  
    "/Community": [
        "contribute-guide.md",
        "honor_wall.md",
        "contributor_role.md",
        "contributor_role_details.md",
        {
            text: "社区生态",
            collapsible: false,
            children: [
                "component_market.md",
                "tech_sharing.md",
                "kmp_ohos_adaptation_guide.md",
                "kuikly_extension_lib_guide.md",
            ]
        },
    ],
    "/AI": [
        {
            text: "AI 编程",
            prefix: "/AI",
            collapsible: false,
            children: [
                "index.md",
                "kuikly-ai.md",
                "a2ui.md",
                "chat-markdown.md",
                "transcode.md",
                "d2c.md",
                "workshop-agent.md",
            ],
        },
    ],
    "/Blog": [
        "roadmap2026.md",
        "roadmap2025.md",
        {
            text: "架构原理",
            prefix: "/Blog/architecture",
            children: [
                "kuikly-rendering.md", "architecture_and_advantages.md"
                ]
        },
        {
            text: "公众号文章",
            collapsible: false,
            children: [
                "长小牛 × 腾讯Kuikly：金融类AI APP三端跨平台架构落地实践.md",
                "Kotlin 15 周年 × Kuikly 生态日｜一场关于跨端与 AI 的技术盛宴.md",
                "iPhone Duo来了，腾讯 Kuikly 助 App 从容适配.md",
                "我用腾讯 Kuikly，把 DeepSeek Harness装进了口袋.md",
                "7.5 小时搞定安卓／iOS／鸿蒙三端！AI + Kuikly 「多模态聊天 App」 实战.md",
                "跨端页面秒开，腾讯开源框架Kuikly是怎么做到的？.md",
                "Kuikly Compose DSL 正式发布：六端原生渲染的标准 Compose 实践.md",
                "AI智能转码 + 原生高性能：QQ音乐存量代码迁移Kuikly实践.md",
                "Kuikly Roadmap（2026）.md",
                "Kuikly AI：为跨端开发构建 Harness 工程基座，开源首批 Rules 与 Skills.md",
                "OpenClaw给我发来了它写的第一个Kuikly apk.md",
                "搜狗输入法 Kuikly AI 工程化：Spec coding 的探索与落地.md",
                "腾讯开源Kuikly框架能力、生态再升级，诚邀你携手，共筑跨端技术新生态.md",
                "多端开发背景下，腾讯的应对方式和效果如何？.md",
                "一套代码搞定多端！HelloTalk使用Kuikly的历程和心得.md",
                "腾讯开源框架 Kuikly 再升级！率先适配 “液态玻璃”，原生体验更极致.md",
                "腾讯Kuikly框架进一步开源，新增支持Web，开启一码五端新体验！.md",
                "适配鸿蒙操作系统！！腾讯游戏直播SDK基于Kuikly三端一码开发实践.md",
                "重磅！支持纯血鸿蒙！腾讯视频ovCompose跨平台框架发布.md",
                "腾讯Kuikly框架鸿蒙版正式开源 —— 揭秘卓越性能适配之旅.md",
                "腾讯开源跨平台开发框架 Kuikly，能显著提升多端开发效率.md",
            ]
        },
    ],
    "/QA": [
        "kuikly-qa.md", "kuikly-example.md"
    ],
    "/ChangeLog": [
        {
            text: "",
            prefix: "/ChangeLog",
            collapsible: false,
            children: [
                "README.md",
                { text: "2.28.0", link: "/ChangeLog/2.28.0/changelog.md" },
                { text: "2.27.0", link: "/ChangeLog/2.27.0/changelog.md" },
                { text: "2.26.0", link: "/ChangeLog/2.26.0/changelog.md" },
                { text: "2.25.0", link: "/ChangeLog/2.25.0/changelog.md" },
                { text: "2.24.0", link: "/ChangeLog/2.24.0/changelog.md" },
                { text: "2.23.3", link: "/ChangeLog/2.23.3/changelog.md" },
                { text: "2.23.2", link: "/ChangeLog/2.23.2/changelog.md" },
                { text: "2.23.1", link: "/ChangeLog/2.23.1/changelog.md" },
                { text: "2.23.0", link: "/ChangeLog/2.23.0/changelog.md" },
                { text: "2.22.0", link: "/ChangeLog/2.22.0/changelog.md" },
                { text: "2.21.0", link: "/ChangeLog/2.21.0/changelog.md" },
                { text: "2.20.1", link: "/ChangeLog/2.20.1/changelog.md" },
                { text: "2.20.0", link: "/ChangeLog/2.20.0/changelog.md" },
                { text: "2.19.1", link: "/ChangeLog/2.19.1/changelog.md" },
                { text: "2.19.0", link: "/ChangeLog/2.19.0/changelog.md" },
                { text: "2.18.0", link: "/ChangeLog/2.18.0/changelog.md" },
                { text: "2.17.0", link: "/ChangeLog/2.17.0/changelog.md" },
                { text: "2.16.0", link: "/ChangeLog/2.16.0/changelog.md" },
                { text: "2.15.3", link: "/ChangeLog/2.15.3/changelog.md" },
                { text: "2.15.2", link: "/ChangeLog/2.15.2/changelog.md" },
                { text: "2.15.1", link: "/ChangeLog/2.15.1/changelog.md" },
                { text: "2.15.0", link: "/ChangeLog/2.15.0/changelog.md" },
                { text: "2.14.0", link: "/ChangeLog/2.14.0/changelog.md" },
                { text: "2.13.0", link: "/ChangeLog/2.13.0/changelog.md" },
                { text: "2.12.1", link: "/ChangeLog/2.12.1/changelog.md" },
                { text: "2.12.0", link: "/ChangeLog/2.12.0/changelog.md" },
                { text: "2.11.0", link: "/ChangeLog/2.11.0/changelog.md" },
                { text: "2.10.0", link: "/ChangeLog/2.10.0/changelog.md" },
            ],
        },
    ]
});
