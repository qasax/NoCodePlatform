package my.nocodeplatform.ai.model.core.builder;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {
    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        // 1. 处理 Windows 命令兼容性 (例如将 npm 包装为 cmd /c npm.cmd)
        String[] cmdArray = isWindows() ? new String[]{"cmd", "/c", command} : command.split("\\s+");

        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);

            // 2. 使用 ProcessBuilder 并合并错误流
            ProcessBuilder builder = new ProcessBuilder(cmdArray);
            builder.directory(workingDir);
            builder.redirectErrorStream(true); // 【关键】将 stderr 和 stdout 合并，这样报错信息也能通过 getInputStream 读到

            Process process = builder.start();

            // 3. 【核心修改】在 waitFor 之前开始读取流，防止缓冲区满导致死锁
            // 使用 Hutool 的 IoUtil 一次性读取全部内容
            String allOutput;
            try (InputStream is = process.getInputStream()) {
                // 如果在 Windows 下日志乱码，可以尝试将 UTF_8 换成 Charset.forName("GBK")
                allOutput = IoUtil.read(is, StandardCharsets.UTF_8);
            }

            // 4. 等待进程结束
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程。", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                // 成功时也可以选择性打印 log.debug("输出详情: {}", allOutput);
                return true;
            } else {
                // 5. 【你要的结果】此时 allOutput 包含完整的 RollupError 或其它报错
                log.error("命令执行失败，退出码: {}\n----- 错误详情开始 -----\n{}\n----- 错误详情结束 -----",
                        exitCode, allOutput);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令异常: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }
    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }
}
