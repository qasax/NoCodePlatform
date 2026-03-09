package my.nocodeplatform.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(description = "创建应用请求")
public class AppAddRequest implements Serializable {
    
    @Schema(description = "应用名称", example = "我的待办应用")
    private String appName;

    @Schema(description = "应用封面图片 URL", example = "https://example.com/cover.png")
    private String cover;

    @Schema(description = "应用初始化的 Prompt（必填）", example = "帮我生成一个待办事项管理应用")
    private String initPrompt;
    @Schema(description = "应用初始化的 codeGenType（必填）", example = "html")
    private String codeGenType;

    private static final long serialVersionUID = 1L;
}
