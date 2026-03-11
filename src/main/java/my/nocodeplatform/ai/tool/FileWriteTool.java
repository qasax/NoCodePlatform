package my.nocodeplatform.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.constant.AppConstant;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool  extends BaseTool{


    public String writeFiles(@P("文件列表 JSON，每个文件包含 path 和 content") String filesJson,
                                   @ToolMemoryId Long appId) {
        List<String> results = new ArrayList<>();
        try {
            if (StrUtil.isEmpty(filesJson)) {
                results.add("文件列表为空");
                return results.stream().collect(Collectors.joining("\n"));
            }

            // 如果 JSON 被外层引号包裹，例如 "\"[...]\""，去掉首尾引号
            if ((filesJson.startsWith("\"") && filesJson.endsWith("\""))
                    || (filesJson.startsWith("'") && filesJson.endsWith("'"))) {
                filesJson = filesJson.substring(1, filesJson.length() - 1)
                        .replace("\\\"", "\"")  // 反转义双引号
                        .replace("\\n", "");    // 去掉换行符
            }

            // 解析为原始 List<Map>
            List<Map> rawList = JSONUtil.toList(JSONUtil.parseArray(filesJson), Map.class);

            // 转为 List<Map<String,String>>
            List<Map<String, String>> files = rawList.stream().map(m -> {
                Map<String, String> map = new HashMap<>();
                m.forEach((k, v) -> map.put(String.valueOf(k), String.valueOf(v)));
                return map;
            }).toList();

            for (Map<String, String> file : files) {
                String relativeFilePath = file.get("path");
                String content = file.get("content");
                if (relativeFilePath == null || content == null) {
                    results.add("跳过无效文件条目: " + file);
                    continue;
                }
                results.add(writeFile(relativeFilePath, content, appId));
            }

        } catch (Exception e) {
            String errorMessage = "解析文件列表失败: " + e.getMessage();
            log.error(errorMessage, e);
            results.add(errorMessage);
        }
        return results.stream().collect(Collectors.joining("\n"));
    }

    @Tool("写入文件到指定目录。")
    public String writeFile(@P("文件的相对路径") String relativeFilePath, @P("要写入文件的内容") String content, @ToolMemoryId Long appId) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                // 相对路径处理，创建基于 appId 的项目目录
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}