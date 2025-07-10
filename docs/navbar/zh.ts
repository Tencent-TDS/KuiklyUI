import {navbar} from "vuepress-theme-hope";

export const zhNavbar = navbar([
    {text: "简介", link: "/Introduction/arch.md"},
    {text: "快速开始", link: "/QuickStart/env-setup.md"},
    {text: "开发文档", link: "/DevGuide/dev-guide-overview.md"},
    {text: "API", link: "/API/components/override.md"},
    {text: "Compose DSL支持", link: "/ComposeDSL/overview.md"},
    {text: "博客", link: "/Blog/roadmap2025.md"},
    {text: "QA", link: "/QA/kuikly-qa.md"},
    {text: "更新日志", link: "/ChangeLog/changelog.md"},
    {
        text: "源码",
        link: "https://github.com/Tencent-TDS/KuiklyUI",
    },
]);
