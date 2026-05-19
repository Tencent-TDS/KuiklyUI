#!/usr/bin/env node

/**
 * Bundle Manifest Generator
 *
 * 自动扫描 src/bundles 目录并生成 manifest.json 文件。
 *
 * 输出字段（新版，index.html 强依赖）：
 * - files: 公共依赖 bundle 列表（用于公共依赖加载/排序）
 * - fileCount: files 数量
 * - pages: pageName -> 页面 bundle 文件名（支持带 contenthash）
 * - pageCount: pages 数量
 * - totalCount: bundles 目录下总 bundle 数量
 */

const fs = require('fs');
const path = require('path');

const BUNDLES_DIR = path.join(__dirname, '../src/bundles');
const MANIFEST_PATH = path.join(BUNDLES_DIR, 'manifest.json');

const COMMON_PREFIXES = [
  'kotlin-stdlib.',
  'runtime.',
  'common.',
  'vendors.'
];

function isBundleJsFile(file) {
  return file.endsWith('.bundle.js');
}

function isCommonBundle(file) {
  return COMMON_PREFIXES.some(prefix => file.startsWith(prefix));
}

/**
 * 从页面 bundle 文件名中提取 pageName
 * - 开发环境: {page}.bundle.js
 * - 生产环境: {page}.{contenthash:8}.bundle.js
 *
 * 注意：pageName 可能包含 '.'，所以不能用 indexOf('.') 取第一段。
 */
function extractPageNameFromPageBundle(file) {
  const hashed = file.match(/^(.+)\.[0-9a-f]{8}\.bundle\.js$/i);
  if (hashed) return hashed[1];

  const plain = file.match(/^(.+)\.bundle\.js$/i);
  if (plain) return plain[1];

  return null;
}

function listAllBundleFiles() {
  console.log('[Manifest Generator] 扫描目录:', BUNDLES_DIR);

  if (!fs.existsSync(BUNDLES_DIR)) {
    throw new Error(`bundles directory not found: ${BUNDLES_DIR}`);
  }

  return fs.readdirSync(BUNDLES_DIR)
    .filter(file => {
      if (file === 'manifest.json') return false;
      const full = path.join(BUNDLES_DIR, file);
      if (!fs.statSync(full).isFile()) return false;
      return isBundleJsFile(file);
    });
}

/**
 * 为每个 pageName 选出最合适的页面 bundle
 * 如果同一页面存在多个文件（例如残留旧 hash），选择 mtime 最新的。
 */
function buildPagesMap(pageFiles) {
  const grouped = new Map();

  for (const file of pageFiles) {
    const pageName = extractPageNameFromPageBundle(file);
    if (!pageName) continue;

    const full = path.join(BUNDLES_DIR, file);
    const mtimeMs = fs.statSync(full).mtimeMs;

    const arr = grouped.get(pageName) || [];
    arr.push({ file, mtimeMs });
    grouped.set(pageName, arr);
  }

  const pages = {};
  for (const [pageName, list] of grouped.entries()) {
    list.sort((a, b) => b.mtimeMs - a.mtimeMs);
    pages[pageName] = list[0].file;
  }

  return pages;
}

function generateManifest() {
  const allFiles = listAllBundleFiles();

  if (allFiles.length === 0) {
    console.warn('[Manifest Generator] ⚠️  没有找到任何 .bundle.js 文件');
    return;
  }

  const commonFiles = allFiles.filter(isCommonBundle).sort();
  const pageFiles = allFiles.filter(f => !isCommonBundle(f)).sort();
  const pages = buildPagesMap(pageFiles);

  const manifest = {
    version: '2.0.0',
    generatedAt: new Date().toISOString(),

    files: commonFiles,
    fileCount: commonFiles.length,

    pages,
    pageCount: Object.keys(pages).length,

    totalCount: allFiles.length
  };

  fs.writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 2), 'utf8');

  console.log('[Manifest Generator] ✅ Manifest 生成成功!');
  console.log('[Manifest Generator] 📁 文件路径:', MANIFEST_PATH);
  console.log('[Manifest Generator] 📊 公共依赖:', manifest.fileCount, '个');
  console.log('[Manifest Generator] 📊 页面:', manifest.pageCount, '个');
  console.log('[Manifest Generator] 📊 总计:', manifest.totalCount, '个');
}

try {
  generateManifest();
} catch (error) {
  console.error('[Manifest Generator] ❌ 生成失败:', error.message);
  process.exit(1);
}
