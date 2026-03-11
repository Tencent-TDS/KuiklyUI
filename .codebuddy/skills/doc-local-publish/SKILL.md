---
name: doc-local-publish
description: 用于在本地发布 KuiklyUI 文档的 Skill。当用户需要预览或调试 KuiklyUI 文档网站时使用。该 Skill 会自动拉取 website-shell 仓库，将 KuiklyUI 的 docs 目录覆盖到 website-shell，并启动本地开发服务器。
---

# Doc Local Publish

## Overview

This skill enables local publishing and previewing of KuiklyUI documentation. It automates the workflow of:

1. Cloning the website-shell repository from `https://git.woa.com/Kuikly/website-shell.git`
2. Switching to the `website-Internal` branch
3. Copying KuiklyUI's `docs` directory to overwrite website-shell's docs
4. Starting the local dev server with `npm run docs:dev`

## When to Use

Use this skill when:
- Previewing KuiklyUI documentation changes locally
- Debugging documentation website rendering issues
- Testing documentation updates before deployment
- Working on documentation improvements

## Workflow

### Step 1: Clone website-shell Repository

Clone the website-shell repository to a temporary directory:

```bash
git clone https://git.woa.com/Kuikly/website-shell.git /tmp/website-shell
```

### Step 2: Switch to website-Internal Branch

Navigate to the cloned repository and switch to the correct branch:

```bash
cd /tmp/website-shell
git checkout website-Internal
```

### Step 3: Copy KuiklyUI docs

Copy the KuiklyUI docs directory to overwrite website-shell's docs:

```bash
# Assuming current working directory is KuiklyUI root
rm -rf /tmp/website-shell/docs
cp -r docs /tmp/website-shell/docs
```

### Step 4: Start Dev Server

Navigate to the docs directory and start the development server:

```bash
cd /tmp/website-shell/docs
npm install  # If node_modules doesn't exist
npm run docs:dev
```

The dev server will start and provide a local URL (typically `http://localhost:5173` or similar).

## Cleanup

After finishing work, the temporary directory can be removed:

```bash
rm -rf /tmp/website-shell
```

## Notes

- The website-shell repository is cloned to `/tmp/website-shell` by default
- Ensure you have proper Git credentials for `git.woa.com`
- Ensure Node.js and npm are installed for running the dev server
- The docs directory in website-shell will be completely replaced by KuiklyUI's docs
