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
                    children: ["overview.md", "common.md", "android.md", "iOS.md", "harmony.md"]
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
                            "notify.md", "network.md", "assets-resource.md"
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
                            ]
                        },
                        "view-external-prop.md",
                        "protobuf.md",
                        "kuikly-perf-guidelines.md",
                        "thread-and-coroutines.md",
                        "multi-page.md"]
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
                                    children : ["dev-overview.md", "android-dev.md", "ios-dev.md", "harmony-dev.md"]
                                },
                                {
                                    text: "调试与工具",
                                    collapsible: true,
                                    children: ["android-debug.md", "iOS-debug.md", "ohos-debug.md"]
                                },
                            ]
                        },
                        "multi_module.md"
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
                                    children: ["kuiklybase-feat-stack-symbolication.md", "symbol-iOS.md", "ohos-kn-stack-symbolication.md"]
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
            ],
        },
    ],

    "/API": [
        {
            text: "组件",
            prefix: "/API/components",
            children: [
                "override.md", "basic-attr-event.md", "view.md", "text.md", "rich-text.md", "image.md", "input.md", "text-area.md",  "canvas.md",
                "button.md", "scroller.md", "list.md", "waterfall-list.md", "slider-page.md", "page-list.md", "modal.md", "refresh.md",
                "footer-refresh.md", "date-picker.md", "scroll-picker.md", "slider.md", "switch.md", "blur.md",
                "activity-indicator.md", "hover.md", "mask.md", "checkbox.md", "pag.md","apng.md", "tabs.md","alert-dialog.md","action-sheet.md", "video.md"
            ],
        },
        {
            text: "Module",
            prefix: "/API/modules",
            children: [
                "overview.md", 'memory-cache.md', "sp.md", "router.md", "network.md", "notify.md", "snapshot.md","codec.md","calendar.md", "performance.md"
            ]
        }
    ],
    "/ComposeDSL": [
        "overview.md",
        "quickStart.md",
        "allApi.md"
    ],
    "/Blog": [
        "roadmap2025.md",
        {
            text: "架构原理",
            prefix: "/Blog/architecture",
            children: [
                "kuikly-rendering.md", "architecture_and_advantages.md"
                ]
        },
        {
            text: "实践分享",
            prefix: "/Blog/sharing",
            children: [
                "tech_practice_sharing_template.md"
                ]
        }
    ],
    "/QA": [
        "kuikly-qa.md"
    ]
});
