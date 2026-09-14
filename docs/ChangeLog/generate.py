#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Kuikly 版本三分文档生成脚本

用法:
    python3 docs/ChangeLog/generate.py <version> [--repo <path>] [--out-dir <path>] [--no-fetch]

示例:
    python3 docs/ChangeLog/generate.py 2.27.0

行为:
    1. 校验版本 tag（本地缺失时自动从 origin 拉取），定位上一版本 tag
    2. 收集区间内 commit（subject + 改动路径），按规则分桶（多端 / 单端 / 其他）
    3. 生成 changelog.md（完整）与 announcement.md / breaking.md 骨架
    4. 写入 docs/ChangeLog/<version>/（目录不存在则创建，文件已存在则覆盖）

说明:
    changelog.md 由脚本完整生成；announcement.md 与 breaking.md 的语义内容
    （能力点描述、破坏性判定）需按 docs/ChangeLog/TEMPLATE.md 补全，
    推荐通过 kuikly-changelog-gen skill 调用本脚本完成。
"""
import argparse
import os
import re
import subprocess
import sys

GITHUB = "https://github.com/Tencent-TDS/KuiklyUI"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_REPO = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))

MULTI_MODULES = {"core", "compose", "core-ksp", "core-annotations"}
END_MODULES = [
    ("Android", {"core-render-android", "androidApp"}),
    ("iOS", {"core-render-ios", "iosApp"}),
    ("鸿蒙", {"core-render-ohos", "ohosApp"}),
    ("H5", {"core-render-web", "h5App"}),
    ("小程序", {"miniApp"}),
]
END_ORDER = [name for name, _ in END_MODULES]
END_KEYWORDS = [
    ("Android", ["android"]),
    ("iOS", ["ios", "macos"]),
    ("鸿蒙", ["ohos", "harmony"]),
    ("H5", ["h5"]),
    ("小程序", ["miniapp", "mini app", "miniprogram"]),
]
ALL_END_MODULES = set().union(*(mods for _, mods in END_MODULES))
RENDER_MODULES = {"core-render-android", "core-render-ios", "core-render-ohos", "core-render-web"}


def sh(cmd, repo):
    result = subprocess.run(cmd, cwd=repo, capture_output=True, text=True)
    if result.returncode != 0:
        sys.stderr.write(result.stderr)
    return result.stdout


def version_key(v):
    return tuple(int(x) for x in v.split("."))


def all_tags(repo):
    out = sh(["git", "tag"], repo)
    tags = [t.strip() for t in out.splitlines() if re.fullmatch(r"\d+\.\d+\.\d+", t.strip())]
    return sorted(tags, key=version_key)


def remote_tags(repo):
    """从远端获取正式版本 tag 列表（upstream → origin → 官方 GitHub）。"""
    for remote in ("upstream", "origin", GITHUB):
        out = sh(["git", "ls-remote", "--tags", remote], repo)
        tags = []
        for line in out.splitlines():
            m = re.search(r"refs/tags/(\d+\.\d+\.\d+)$", line.strip())
            if m:
                tags.append(m.group(1))
        if tags:
            return sorted(set(tags), key=version_key)
    return []


def fetch_tag(repo, tag):
    """依次尝试从 upstream / origin / 官方 GitHub 拉取单个 tag。"""
    for remote in ("upstream", "origin", GITHUB):
        sh(["git", "fetch", remote, f"refs/tags/{tag}:refs/tags/{tag}", "--no-tags"], repo)
        if tag in all_tags(repo):
            return True
    return False


def resolve_range(repo, version, allow_fetch):
    """确定 (prev, version) 区间；本地 tag 不全时用远端完整列表纠正并补齐。

    返回 (prev, version, full_list)：version 不在列表中时为 (None, None, full)。
    """
    local = all_tags(repo)
    remote = remote_tags(repo) if allow_fetch else []
    full = sorted(set(local) | set(remote), key=version_key)
    if version not in full:
        return None, None, full
    idx = full.index(version)
    if idx == 0:
        return None, version, full
    prev = full[idx - 1]
    if allow_fetch:
        for t in (prev, version):
            if t not in all_tags(repo):
                fetch_tag(repo, t)
    return prev, version, full


def collect_commits(repo, prev, version):
    out = sh(
        ["git", "log", "--no-merges", "--pretty=format:@@%H|%s", "--name-only", f"{prev}..{version}"],
        repo,
    )
    commits, cur = [], None
    for line in out.splitlines():
        if line.startswith("@@"):
            if cur:
                commits.append(cur)
            h, subject = line[2:].split("|", 1)
            cur = {"hash": h, "subject": subject, "paths": []}
        elif line.strip() and cur is not None:
            cur["paths"].append(line.strip())
    if cur:
        commits.append(cur)
    return commits


def split_subject(subject):
    m = re.search(r"\(#(\d+)\)\s*$", subject)
    if not m:
        return subject, None
    return subject[: m.start()].strip(), m.group(1)


def classify(commit):
    """按改动路径与标题分桶，返回 (bucket, end_name)：bucket ∈ multi / single / other"""
    tops = {p.split("/")[0] for p in commit["paths"]}
    title = split_subject(commit["subject"])[0].lower()

    # 1. 示例类（含 demo 且不涉及框架核心模块）→ 其他
    if "demo" in tops and not (tops & (MULTI_MODULES | RENDER_MODULES)):
        return "other", None

    # 2. 标题点名单端且该端模块有改动 → 该端；点名多端 → 多端
    title_ends = [name for name, kws in END_KEYWORDS if any(k in title for k in kws)]
    if len(title_ends) == 1:
        mods = dict(END_MODULES)[title_ends[0]]
        if tops & mods:
            return "single", title_ends[0]
    if len(title_ends) >= 2:
        return "multi", title_ends

    # 3. 跨端通用模块 → 多端
    if tops & MULTI_MODULES:
        return "multi", None

    # 4. 端模块命中：2 个及以上 → 多端；1 个 → 单端
    hits = [name for name, mods in END_MODULES if tops & mods]
    if len(hits) >= 2:
        return "multi", hits
    if len(hits) == 1:
        return "single", hits[0]

    return "other", None


def drop_reverted(commits):
    """被同区间 Revert 的原条目不再列出（只保留 Revert 条目）。"""
    reverted = []
    for c in commits:
        m = re.match(r'^Revert "(.+?)"\s*\(#\d+\)\s*$', c["subject"])
        if m:
            reverted.append(m.group(1).strip())
    if not reverted:
        return commits
    out = []
    for c in commits:
        title, pr = split_subject(c["subject"])
        if not title.startswith("Revert"):
            original = f"{title} (#{pr})" if pr else title
            if any(r == original or r.startswith(title) for r in reverted):
                continue
        out.append(c)
    return out


def entry(subject):
    title, pr = split_subject(subject)
    if pr:
        return f"- {title} · [#{pr}]({GITHUB}/pull/{pr})"
    return f"- {title}"


def is_feat(subject):
    return re.match(r"^(feat|feature)\b", subject, re.I) is not None


def is_docs_like(commit):
    title = split_subject(commit["subject"])[0]
    if re.match(r"^docs\b", title, re.I):
        return True
    tops = {p.split("/")[0] for p in commit["paths"]}
    return bool(tops) and tops <= {"docs", "img"}


def touches_dsl(commit):
    tops = {p.split("/")[0] for p in commit["paths"]}
    return bool(tops & {"core", "compose", "core-ksp", "core-annotations"})


def path_brief(commit):
    return ", ".join(sorted({p.split("/")[0] for p in commit["paths"]}))


def render_changelog(version, buckets):
    lines = [
        f"# {version} 变更汇总",
        "",
        "> 本版导航：[版本说明](./announcement.md) · **变更汇总** · [破坏性与迁移](./breaking.md)",
        "",
    ]
    if buckets["multi"]:
        lines.append("## 多端")
        lines.append("")
        for c in buckets["multi"]:
            lines.append(entry(c["subject"]))
        lines.append("")
    if buckets["single"]:
        lines.append("## 单端")
        lines.append("")
        for name in END_ORDER:
            items = buckets["single"].get(name)
            if not items:
                continue
            lines.append(f"### {name}")
            lines.append("")
            for c in items:
                lines.append(entry(c["subject"]))
            lines.append("")
    if buckets["other"]:
        lines.append("## 其他")
        lines.append("")
        docs_items = [c for c in buckets["other"] if is_docs_like(c)]
        eng_items = [c for c in buckets["other"] if not is_docs_like(c)]
        if docs_items:
            lines.append("### 文档")
            lines.append("")
            for c in docs_items:
                lines.append(entry(c["subject"]))
            lines.append("")
        if eng_items:
            lines.append("### 工程与示例")
            lines.append("")
            for c in eng_items:
                lines.append(entry(c["subject"]))
            lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def render_announcement(version, commits):
    feats = [c for c in commits if is_feat(c["subject"])]
    k_feats = [c for c in feats if not touches_dsl(c)]
    c_feats = [c for c in feats if touches_dsl(c)]

    def feat_block(items):
        if not items:
            return ["<!-- 待补全：本模块本版无 feat 类条目 -->"]
        out = []
        for c in items:
            title, pr = split_subject(c["subject"])
            pr_txt = f"（#{pr}）" if pr else ""
            out.append(f"<!-- 改动路径：{path_brief(c)} -->")
            out.append(f"- **{title}**：<1~2 句说明，业务视角>{pr_txt}")
        return out

    lines = [
        f"# {version} 版本说明",
        "",
        "> 本版导航：**版本说明** · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)",
        "",
        "<总述：1 句，本版核心方向；纯修复版写「本版以稳定性修复为主」>",
        "",
        "## Kuikly DSL",
        "",
        "### 能力增加",
        "",
        "<!-- 待 AI 补全：按能力点聚类，端专属的标端点名；不逐条罗列 PR -->",
    ]
    lines.extend(feat_block(k_feats))
    lines += [
        "",
        "### 性能优化",
        "",
        "<!-- 待 AI 补全：无内容则整节省略 -->",
        "",
        "### Bug 修复",
        "",
        "<!-- 待 AI 补全：1~2 句概括修复范围，指向 changelog -->",
        "",
        "## Compose DSL",
        "",
        "### 能力增加",
        "",
        "<!-- 待 AI 补全：按能力点聚类 -->",
    ]
    lines.extend(feat_block(c_feats))
    lines += [
        "",
        "### 性能优化",
        "",
        "<!-- 待 AI 补全：无内容则整节省略 -->",
        "",
        "### Bug 修复",
        "",
        "<!-- 待 AI 补全：1~2 句概括修复范围，指向 changelog -->",
        "",
        "## 升级建议",
        "",
        "<!-- 待 AI 补全：由 breaking 结论回填；无则写「本版无破坏性变更，可直接升级」 -->",
    ]
    return "\n".join(lines).rstrip() + "\n"


def render_breaking(version, prev, commits):
    risky = [
        c
        for c in commits
        if touches_dsl(c)
        and re.match(r"^(feat|fix|refactor)\b", split_subject(c["subject"])[0], re.I)
    ]
    lines = [
        f"# {version} 破坏性与迁移",
        "",
        "> 本版导航：[版本说明](./announcement.md) · [变更汇总](./changelog.md) · **破坏性与迁移**",
        "",
        "<!-- 待 AI 判定：对下列高风险 commit 做业务视角扫描（属性 / 事件 / 内置组件 / 内置 Module / 页面注解）；有命中写「条件性破坏性」条目，无命中写「本版无破坏性变更」 -->",
        "",
        "## 扫描依据",
        "",
        f"[{prev}...{version}]({GITHUB}/compare/{prev}...{version}) 共 {len(commits)} 个 commit。"
        "涉及对外接口的改动（需逐一核对）：",
        "",
    ]
    if risky:
        for c in risky:
            title, pr = split_subject(c["subject"])
            pr_txt = f"#{pr} " if pr else ""
            lines.append(f"- {pr_txt}{title}（{path_brief(c)}）")
    else:
        lines.append("- 无")
    lines.append("")
    lines.append("按业务能力面（属性 / 事件 / 内置组件 / 内置 Module / 页面注解）核对：<待补全结论>。")
    return "\n".join(lines).rstrip() + "\n"


def main():
    ap = argparse.ArgumentParser(description="生成指定版本的三分变更文档")
    ap.add_argument("version", help="版本号，与 git tag 一致，如 2.27.0")
    ap.add_argument("--repo", default=DEFAULT_REPO, help="仓库路径（默认脚本上两级）")
    ap.add_argument("--out-dir", default=None, help="输出目录（默认 docs/ChangeLog/<version>/）")
    ap.add_argument("--no-fetch", action="store_true", help="本地缺 tag 时不尝试从 origin 拉取")
    args = ap.parse_args()

    repo = os.path.abspath(args.repo)
    prev, version, full = resolve_range(repo, args.version, allow_fetch=not args.no_fetch)
    if version is None:
        sys.stderr.write(
            f"错误：tag {args.version} 不存在（已尝试从远端拉取）。近期可用版本：{', '.join(full[-8:])}\n"
        )
        sys.exit(1)
    if prev is None:
        sys.stderr.write(f"错误：{args.version} 是最早的版本，没有可比对的上一版本。\n")
        sys.exit(1)

    commits = drop_reverted(collect_commits(repo, prev, args.version))
    if not commits:
        sys.stderr.write(f"错误：{prev}..{args.version} 区间没有 commit。\n")
        sys.exit(1)

    buckets = {"multi": [], "single": {}, "other": []}
    for c in commits:
        bucket, end = classify(c)
        if bucket == "single":
            buckets["single"].setdefault(end, []).append(c)
        else:
            buckets[bucket].append(c)

    out_dir = args.out_dir or os.path.join(SCRIPT_DIR, args.version)
    os.makedirs(out_dir, exist_ok=True)

    files = {
        "changelog.md": render_changelog(args.version, buckets),
        "announcement.md": render_announcement(args.version, commits),
        "breaking.md": render_breaking(args.version, prev, commits),
    }
    for name, content in files.items():
        with open(os.path.join(out_dir, name), "w", encoding="utf-8") as f:
            f.write(content)

    print(f"区间：{prev}..{args.version}，共 {len(commits)} 个 commit")
    print(f"输出目录：{out_dir}")
    print("  changelog.md     完整生成")
    print("  announcement.md  骨架（待 AI 补全能力点描述）")
    print("  breaking.md      骨架（待 AI 判定破坏性）")
    print("下一步：按 docs/ChangeLog/TEMPLATE.md 补全 announcement / breaking，并更新 README 版本列表。")


if __name__ == "__main__":
    main()
