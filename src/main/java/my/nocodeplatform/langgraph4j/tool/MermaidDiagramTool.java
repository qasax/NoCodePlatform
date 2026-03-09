package my.nocodeplatform.langgraph4j.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.utils.WebScreenshotUtils;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.langgraph4j.state.ImageCategoryEnum;
import my.nocodeplatform.langgraph4j.state.ImageResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MermaidDiagramTool {


    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            String IMAGE_SUFFIX = ".svg";
            String path = System.getProperty("user.dir") + File.separator + "tmp"
                    + File.separator +"MermaidDiagram"
                    +File.separator+ UUID.randomUUID().toString().substring(0, 8)
                    +File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            try {
                FileUtil.writeBytes(FileUtil.readBytes(diagramFile), path);
            } catch (Exception e) {
                log.error("保存图片失败: {}", path, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
            }
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(path)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(path)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        // 根据操作系统选择命令
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        // 构建命令
        String cmdLine = String.format("%s -i %s -o %s -b transparent",
                command,
                tempInputFile.getAbsolutePath(),
                tempOutputFile.getAbsolutePath()
        );
        // 执行命令
        RuntimeUtil.execForStr(cmdLine);
        // 检查输出文件
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败");
        }
        // 清理输入文件，保留输出文件供上传使用
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }
}