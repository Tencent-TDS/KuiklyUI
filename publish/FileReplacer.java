import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 文件内容替换工具
 *
 * 使用方法：
 *   java FileReplacer.java replace [config.yaml]
 *   java FileReplacer.java restore [config.yaml]
 */
public class FileReplacer {
    private static final String BACKUP_SUFFIX = ".bak";
    private static final String DEFAULT_CONFIG_FILE = "replacement.yaml";

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String action = args[0];
        String configFile = args.length > 1 ? args[1] : DEFAULT_CONFIG_FILE;

        try {
            if ("replace".equals(action)) {
                applyReplacements(configFile);
            } else if ("restore".equals(action)) {
                restoreBackups(configFile);
            } else {
                System.err.println("Unknown action: " + action);
                printUsage();
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java FileReplacer.java replace [config.yaml]   - Apply replacements and backup files");
        System.out.println("  java FileReplacer.java restore [config.yaml]   - Restore files from backup");
        System.out.println();
        System.out.println("Default config file: " + DEFAULT_CONFIG_FILE);
        System.out.println();
        System.out.println("Config format:");
        System.out.println("  files:");
        System.out.println("    - path: path/to/file.kt");
        System.out.println("      prepend: 'text to add at beginning'  # optional");
        System.out.println("      append: 'text to add at end'         # optional");
        System.out.println("      replacements:                        # optional");
        System.out.println("        - from: old text");
        System.out.println("          to: new text");
    }

    /**
     * 应用替换
     */
    private static void applyReplacements(String configFile) throws IOException {
        List<FileOperation> fileOperations = parseYamlConfig(configFile);

        if (fileOperations.isEmpty()) {
            System.out.println("No replacements to apply.");
            return;
        }

        System.out.println("=== Applying Replacements ===");
        System.out.println();

        int totalFiles = 0;
        int totalChanges = 0;

        for (FileOperation operation : fileOperations) {
            // 安全校验
            if (!isPathSafe(operation.path)) {
                System.out.println();
                continue;
            }
            Path path = Paths.get(operation.path);

            if (!Files.exists(path)) {
                System.err.println("⚠️  File not found: " + operation.path);
                System.out.println();
                continue;
            }

            // 备份原文件
            Path backupPath = Paths.get(operation.path + BACKUP_SUFFIX);
            Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("📁 " + operation.path);
            System.out.println("   Backup: " + backupPath.getFileName());

            // 读取并替换内容
            String content = new String(Files.readAllBytes(path), "UTF-8");
            String modifiedContent = content;
            int changes = 0;

            // 1. Prepend（在开头添加）
            if (operation.prepend != null && !operation.prepend.isEmpty()) {
                // 如果 prepend 不是以换行结尾，添加一个换行
                // 如果是以单个换行结尾，再添加一个换行（变成两个）
                String prependContent = operation.prepend;
                if (!prependContent.endsWith("\n")) {
                    prependContent += "\n";  // 没有换行，添加一个
                }
                prependContent += "\n";  // 额外添加一个换行（用于空行）

                modifiedContent = prependContent + modifiedContent;
                System.out.println("    Prepended: " + operation.prepend);
                changes++;
            }

            // 2. Replacements（替换）
            for (int i = 0; i < operation.replacements.size(); i++) {
                ReplacementPair pair = operation.replacements.get(i);
                String beforeReplace = modifiedContent;
                modifiedContent = modifiedContent.replace(pair.from, pair.to);

                if (!beforeReplace.equals(modifiedContent)) {
                    changes++;
                    System.out.println("    [" + (i + 1) + "] " + pair.from + " → " + pair.to);
                } else {
                    System.out.println("    [" + (i + 1) + "] Not found: " + pair.from);
                }
            }

            // 3. Append（在末尾添加）
            if (operation.append != null && !operation.append.isEmpty()) {
                String appendContent = operation.append;
                // 确保 append 前面有空行
                if (!appendContent.startsWith("\n\n")) {
                    appendContent = (appendContent.startsWith("\n") ? "\n" : "\n\n") + appendContent;
                }
                modifiedContent = modifiedContent + appendContent;
                System.out.println("    Appended: " + operation.append);
                changes++;
            }

            // 写回文件
            if (!content.equals(modifiedContent)) {
                Files.write(path, modifiedContent.getBytes("UTF-8"));
                System.out.println("    Updated (" + changes + " change(s))");
                totalFiles++;
                totalChanges += changes;
            } else {
                System.out.println("    No changes applied");
            }

            System.out.println();
        }

        System.out.println("=== Complete ===");
        System.out.println("Files updated: " + totalFiles + " / " + fileOperations.size());
        System.out.println("Total changes: " + totalChanges);
    }

    /**
     * 恢复备份
     */
    private static void restoreBackups(String configFile) throws IOException {
        List<FileOperation> fileOperations = parseYamlConfig(configFile);

        if (fileOperations.isEmpty()) {
            System.out.println("No files to restore.");
            return;
        }

        System.out.println("=== Restoring Backups ===");
        System.out.println();

        int restoredCount = 0;

        for (FileOperation operation : fileOperations) {
            // 安全校验
            if (!isPathSafe(operation.path)) {
                System.out.println();
                continue;
            }

            Path backupPath = Paths.get(operation.path + BACKUP_SUFFIX);

            if (!Files.exists(backupPath)) {
                System.out.println("    Backup not found: " + operation.path);
                continue;
            }

            Path path = Paths.get(operation.path);
            Files.copy(backupPath, path, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(backupPath);
            System.out.println("    Restored: " + operation.path);
            restoredCount++;
        }

        System.out.println();
        System.out.println("=== Restore Complete ===");
        System.out.println("Files restored: " + restoredCount);
    }

    /**
     * 解析 YAML 配置文件
     *
     * 支持格式：
     * files:
     *   - path: xxx
     *     prepend: xxx  (optional)
     *     append: xxx   (optional)
     *     replacements: (optional)
     *       - from: xxx
     *         to: xxx
     */
    private static List<FileOperation> parseYamlConfig(String configFile) throws IOException {
        Path configPath = Paths.get(configFile);

        if (!Files.exists(configPath)) {
            throw new IOException("Config file not found: " + configFile);
        }

        List<String> lines = Files.readAllLines(configPath);
        List<FileOperation> fileOperations = new ArrayList<>();

        FileOperation currentOp = null;
        String pendingFrom = null;

        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
            String line = lines.get(lineNum);
            String trimmed = line.trim();

            // 跳过空行和注释
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            try {
                // 匹配 "  - path: xxx"
                if (trimmed.startsWith("- path:")) {
                    if (currentOp != null) {
                        fileOperations.add(currentOp);
                    }
                    currentOp = new FileOperation(extractYamlValue(trimmed.substring(7)));
                    pendingFrom = null;
                }
                // 匹配 "    prepend: xxx"
                else if (trimmed.startsWith("prepend:")) {
                    if (currentOp != null) {
                        currentOp.prepend = unescapeString(extractYamlValue(trimmed.substring(8)));
                    }
                }
                // 匹配 "    append: xxx"
                else if (trimmed.startsWith("append:")) {
                    if (currentOp != null) {
                        currentOp.append = unescapeString(extractYamlValue(trimmed.substring(7)));
                    }
                }
                // 匹配 "      - from: xxx"
                else if (trimmed.startsWith("- from:")) {
                    pendingFrom = unescapeString(extractYamlValue(trimmed.substring(7)));
                }
                // 匹配 "        to: xxx"
                else if (trimmed.startsWith("to:")) {
                    String to = unescapeString(extractYamlValue(trimmed.substring(3)));
                    if (pendingFrom != null && currentOp != null) {
                        currentOp.replacements.add(new ReplacementPair(pendingFrom, to));
                    }
                    pendingFrom = null;
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to parse line " + (lineNum + 1) + ": " + line);
            }
        }

        // 添加最后一个
        if (currentOp != null) {
            fileOperations.add(currentOp);
        }

        if (fileOperations.isEmpty()) {
            System.out.println("No operations found in: " + configFile + " (empty config)");
            return fileOperations;
        }
        int totalOps = fileOperations.stream()
                .mapToInt(op -> op.replacements.size() +
                        (op.prepend != null ? 1 : 0) +
                        (op.append != null ? 1 : 0))
                .sum();
        System.out.println("Loaded " + fileOperations.size() + " file(s) with " + totalOps + " operation(s) from " + configFile);
        System.out.println();

        return fileOperations;
    }

    /**
     * 验证文件路径
     */
    private static boolean isPathSafe(String path) {
        if (path == null || path.isEmpty()) {
            System.err.println("Security: Empty path not allowed");
            return false;
        }

        // 1. 绝对路径
        if (path.startsWith("/") || path.startsWith("\\")) {
            System.err.println("Security: Absolute path not allowed: " + path);
            return false;
        }

        // 2. 包含..
        if (path.contains("..")) {
            System.err.println("Security: Path traversal (..) not allowed: " + path);
            return false;
        }

        // 3. 用户目录
        if (path.startsWith("~")) {
            System.err.println("Security: Home directory (~) not allowed: " + path);
            return false;
        }

        // 4. 规范化并验证路径
        try {
            Path normalized = Paths.get(path).normalize();

            if (normalized.isAbsolute()) {
                System.err.println("Security: Path normalizes to absolute: " + path);
                return false;
            }

            String normalizedStr = normalized.toString();
            if (normalizedStr.startsWith("..")) {
                System.err.println("Security: Path escapes working directory: " + path);
                return false;
            }

        } catch (InvalidPathException e) {
            System.err.println("Security: Invalid path syntax: " + path);
            return false;
        }

        return true;
    }

    /**
     * 提取值(去除前后空格)
     */
    private static String extractYamlValue(String str) {
        return str.trim();
    }

    /**
     * 反转义字符串中的 \n, \t 等
     */
    private static String unescapeString(String str) {
        return str.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }

    /**
     * 文件操作
     */
    static class FileOperation {
        final String path;
        String prepend = null;
        String append = null;
        List<ReplacementPair> replacements = new ArrayList<>();

        FileOperation(String path) {
            this.path = path;
        }
    }

    /**
     * 替换对
     */
    static class ReplacementPair {
        final String from;
        final String to;

        ReplacementPair(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return "ReplacementPair{from='" + from + "', to='" + to + "'}";
        }
    }
}