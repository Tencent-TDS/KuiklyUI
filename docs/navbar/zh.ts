import {navbar} from "vuepress-theme-hope";

export const zhNavbar = navbar([
    {text: "简介", link: "/简介/arch.md"},
    {text: "快速开始", link: "/快速开始/env-setup.md"},
    {text: "开发文档", link: "/开发文档/dev-guide-overview.md"},
    {text: "API", link: "/API/组件/override.md"},
    {text: "Compose DSL支持", link: "/ComposeDSL/overview.md"},
    {text: "博客", link: "/博客/roadmap2025.md"},
    {text: "QA", link: "/QA/kuikly-qa.md"},
    {text: "更新日志", link: "/更新日志/changelog.md"},
    {
        text: "源码",
        link: "https://github.com/Tencent-TDS/KuiklyUI",
    },
]);
