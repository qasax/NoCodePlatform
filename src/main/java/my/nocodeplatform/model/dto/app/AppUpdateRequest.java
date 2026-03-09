package my.nocodeplatform.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(description = "更新应用请求（管理员专用）")
public class AppUpdateRequest implements Serializable {

    @Schema(description = "应用 ID", required = true, example = "1")
    private Long id;

    @Schema(description = "应用名称", example = "更新后的应用名称")
    private String appName;

    @Schema(description = "应用封面图片 URL", example = "https://example.com/new-cover.png")
    private String cover;

    @Schema(description = "优先级（用于精选推荐排序，0 为非精选）", example = "10")
    private Integer priority;

    private static final long serialVersionUID = 1L;
}
