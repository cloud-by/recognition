package com.oj.tools;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 文件操作工具类
 * 所有操作基于项目根目录下的 files 文件夹
 */
@Component
public class FileUtil {
    // 基础路径：项目根目录下的 files 文件夹
    private static final String BASE_PATH = System.getProperty("user.dir") + File.separator + "files";

    //获取基础路径
    public static String getBasePath() {
        return BASE_PATH;
    }

    //确保基础路径存在，不存在则创建
    private static void ensureBasePathExists() {
        File baseDir = new File(BASE_PATH);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    // ==================== 文件夹操作 ====================

    /**
     * 创建文件夹（支持多级目录）
     * @param relativePath 相对路径，如 "user1/code/java"
     * @return 是否创建成功
     */
    public static boolean createDirectory(String relativePath) {
        ensureBasePathExists();
        File dir = new File(BASE_PATH + File.separator + relativePath);
        if (dir.exists()) {
            return true;
        }
        return dir.mkdirs();
    }

    /**
     * 删除文件夹（递归删除，包含所有子文件）
     * @param relativePath 相对路径
     * @return 是否删除成功
     */
    public static boolean deleteDirectory(String relativePath) {
        ensureBasePathExists();
        Path dirPath = Paths.get(BASE_PATH, relativePath);
        if (!Files.exists(dirPath)) {
            return true;
        }
        try {
            // 递归删除目录及其所有内容
            try (Stream<Path> walk = Files.walk(dirPath)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断文件夹是否存在
     * @param relativePath 相对路径
     */
    public static boolean directoryExists(String relativePath) {
        File dir = new File(BASE_PATH + File.separator + relativePath);
        return dir.exists() && dir.isDirectory();
    }

    // ==================== 文件操作 ====================

    /**
     * 创建空文件
     * @param relativePath 相对路径（包含文件名），如 "user1/code.java"
     * @return 是否创建成功
     */
    public static boolean createFile(String relativePath) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (file.exists()) {
            return true;
        }
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建文件并写入内容
     * @param relativePath 相对路径
     * @param content 文件内容
     * @return 是否成功
     */
    public static boolean createFileWithContent(String relativePath, String content) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除文件
     * @param relativePath 相对路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String relativePath) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (!file.exists()) {
            return true;
        }
        return file.delete();
    }

    /**
     * 判断文件是否存在
     * @param relativePath 相对路径
     */
    public static boolean fileExists(String relativePath) {
        File file = new File(BASE_PATH + File.separator + relativePath);
        return file.exists() && file.isFile();
    }

    /**
     * 读取文件内容（字符串）
     * @param relativePath 相对路径
     * @return 文件内容，文件不存在返回 null
     */
    public static String readFileContent(String relativePath) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取文件内容（字节数组）
     * @param relativePath 相对路径
     * @return 文件字节数组
     */
    public static byte[] readFileBytes(String relativePath) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 修改文件内容（覆盖写入）
     * @param relativePath 相对路径
     * @param newContent 新内容
     * @return 是否成功
     */
    public static boolean modifyFileContent(String relativePath, String newContent) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(newContent);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 追加内容到文件末尾
     * @param relativePath 相对路径
     * @param appendContent 追加的内容
     * @return 是否成功
     */
    public static boolean appendToFile(String relativePath, String appendContent) {
        ensureBasePathExists();
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(appendContent);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== 重命名操作 ====================

    /**
     * 重命名文件或文件夹
     * @param oldRelativePath 原相对路径
     * @param newRelativePath 新相对路径
     * @return 是否成功
     */
    public static boolean rename(String oldRelativePath, String newRelativePath) {
        ensureBasePathExists();
        File oldFile = new File(BASE_PATH + File.separator + oldRelativePath);
        File newFile = new File(BASE_PATH + File.separator + newRelativePath);
        if (!oldFile.exists()) {
            return false;
        }
        // 确保新文件的父目录存在
        File parentDir = newFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        return oldFile.renameTo(newFile);
    }

    /**
     * 仅修改文件名（不改变路径）
     * @param relativePath 原相对路径
     * @param newFileName 新文件名（不含路径）
     * @return 是否成功
     */
    public static boolean renameFileOnly(String relativePath, String newFileName) {
        File oldFile = new File(BASE_PATH + File.separator + relativePath);
        if (!oldFile.exists()) {
            return false;
        }
        String parentPath = oldFile.getParent();
        String newPath = parentPath + File.separator + newFileName;
        return oldFile.renameTo(new File(newPath));
    }

    // ==================== 上传下载操作 ====================

    /**
     * 上传文件（从 MultipartFile）
     * @param relativePath 保存的相对路径
     * @param file 上传的文件
     * @return 是否成功
     */
    public static boolean uploadFile(String relativePath, MultipartFile file) {
        ensureBasePathExists();
        File targetFile = new File(BASE_PATH + File.separator + relativePath);
        try {
            // 确保父目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            file.transferTo(targetFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 列出文件夹下的所有文件和子文件夹
     * @param relativePath 相对路径
     * @return 文件/文件夹名数组，路径不存在返回 null
     */
    public static String[] listDirectory(String relativePath) {
        ensureBasePathExists();
        File dir = new File(BASE_PATH + File.separator + relativePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        String[] list = dir.list();
        return list != null ? list : new String[0];
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取文件大小（字节）
     * @param relativePath 相对路径
     * @return 文件大小，文件不存在返回 -1
     */
    public static long getFileSize(String relativePath) {
        File file = new File(BASE_PATH + File.separator + relativePath);
        if (!file.exists() || !file.isFile()) {
            return -1;
        }
        return file.length();
    }

    /**
     * 复制文件
     * @param sourceRelativePath 源文件相对路径
     * @param targetRelativePath 目标文件相对路径
     * @return 是否成功
     */
    public static boolean copyFile(String sourceRelativePath, String targetRelativePath) {
        ensureBasePathExists();
        Path source = Paths.get(BASE_PATH, sourceRelativePath);
        Path target = Paths.get(BASE_PATH, targetRelativePath);
        try {
            // 确保目标父目录存在
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移动文件
     * @param sourceRelativePath 源文件相对路径
     * @param targetRelativePath 目标文件相对路径
     * @return 是否成功
     */
    public static boolean moveFile(String sourceRelativePath, String targetRelativePath) {
        ensureBasePathExists();
        Path source = Paths.get(BASE_PATH, sourceRelativePath);
        Path target = Paths.get(BASE_PATH, targetRelativePath);
        try {
            // 确保目标父目录存在
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== 测试数据文件读取 ====================

    /**
     * 从指定文件夹读取所有 .in 和 .out 文件，返回对应的输入输出数组
     * 文件命名规则：1.in, 1.out, 2.in, 2.out, 3.in, 3.out...
     *
     * @param folderRelativePath 文件夹相对路径（相对于 files 目录）
     * @return TestData 对象，包含 inputs 和 outputs 两个字符串数组，失败返回 null
     */
    public static TestData readInOutFiles(String folderRelativePath) {
        ensureBasePathExists();
        File folder = new File(BASE_PATH + File.separator + folderRelativePath);

        // 检查文件夹是否存在
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }

        // 获取所有 .in 和 .out 文件
        File[] inFiles = folder.listFiles((dir, name) -> name.endsWith(".in"));
        File[] outFiles = folder.listFiles((dir, name) -> name.endsWith(".out"));

        if (inFiles == null || outFiles == null) {
            return null;
        }

        // 提取文件编号并排序
        java.util.Map<Integer, String> inMap = new java.util.HashMap<>();
        java.util.Map<Integer, String> outMap = new java.util.HashMap<>();

        for (File f : inFiles) {
            int num = extractNumber(f.getName());
            if (num > 0) {
                inMap.put(num, readFileContent(folderRelativePath + File.separator + f.getName()));
            }
        }

        for (File f : outFiles) {
            int num = extractNumber(f.getName());
            if (num > 0) {
                outMap.put(num, readFileContent(folderRelativePath + File.separator + f.getName()));
            }
        }

        // 获取所有编号并排序
        java.util.Set<Integer> allNumbers = new java.util.TreeSet<>();
        allNumbers.addAll(inMap.keySet());
        allNumbers.addAll(outMap.keySet());

        if (allNumbers.isEmpty()) {
            return null;
        }

        // 构建结果数组（按编号顺序）
        String[] inputs = new String[allNumbers.size()];
        String[] outputs = new String[allNumbers.size()];

        int idx = 0;
        for (int num : allNumbers) {
            inputs[idx] = inMap.getOrDefault(num, null);
            outputs[idx] = outMap.getOrDefault(num, null);
            idx++;
        }

        // 验证是否每个编号都有对应的 .in 和 .out
        boolean hasMissing = false;
        for (int num : allNumbers) {
            if (!inMap.containsKey(num)) {
                hasMissing = true;
            }
            if (!outMap.containsKey(num)) {
                hasMissing = true;
            }
        }

        if (hasMissing) {
        }

        return new TestData(inputs, outputs);
    }

    /**
     * 从文件名中提取数字编号
     * 例如: "1.in" -> 1, "10.out" -> 10, "test1.in" -> 1, "case_2.out" -> 2
     *
     * @param filename 文件名
     * @return 提取的数字，无法提取返回 -1
     */
    private static int extractNumber(String filename) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(filename);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * 从指定文件夹读取所有 .in 和 .out 文件，返回对应的输入输出数组
     * 此方法要求 .in 和 .out 文件必须成对出现（1.in 必须对应 1.out）
     *
     * @param folderRelativePath 文件夹相对路径（相对于 files 目录）
     * @return 二维字符串数组，第一维是测试用例索引，第二维是 [input, output]，失败返回 null
     */
    public static String[][] readInOutPairs(String folderRelativePath) {
        TestData data = readInOutFiles(folderRelativePath);
        if (data == null) {
            return null;
        }

        String[][] result = new String[data.inputs.length][2];
        for (int i = 0; i < data.inputs.length; i++) {
            result[i][0] = data.inputs[i];
            result[i][1] = data.outputs[i];
        }
        return result;
    }

    /**
     * 获取指定文件夹下的测试用例数量
     *
     * @param folderRelativePath 文件夹相对路径
     * @return 测试用例数量（基于 .in 文件数量），失败返回 -1
     */
    public static int getTestCaseCount(String folderRelativePath) {
        ensureBasePathExists();
        File folder = new File(BASE_PATH + File.separator + folderRelativePath);

        if (!folder.exists() || !folder.isDirectory()) {
            return -1;
        }

        File[] inFiles = folder.listFiles((dir, name) -> name.endsWith(".in"));
        if (inFiles == null) {
            return -1;
        }

        // 提取所有编号，返回最大编号（假设编号连续）
        java.util.Set<Integer> numbers = new java.util.HashSet<>();
        for (File f : inFiles) {
            int num = extractNumber(f.getName());
            if (num > 0) {
                numbers.add(num);
            }
        }

        return numbers.size();
    }

    /**
     * 测试数据容器类
     */
    public static class TestData {
        private final String[] inputs;
        private final String[] outputs;

        public TestData(String[] inputs, String[] outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }

        public String[] getInputs() {
            return inputs;
        }

        public String[] getOutputs() {
            return outputs;
        }

        public int getSize() {
            return inputs.length;
        }

        /**
         * 获取第 i 个测试用例的输入
         */
        public String getInput(int i) {
            if (i >= 0 && i < inputs.length) {
                return inputs[i];
            }
            return null;
        }

        /**
         * 获取第 i 个测试用例的输出
         */
        public String getOutput(int i) {
            if (i >= 0 && i < outputs.length) {
                return outputs[i];
            }
            return null;
        }
    }
}