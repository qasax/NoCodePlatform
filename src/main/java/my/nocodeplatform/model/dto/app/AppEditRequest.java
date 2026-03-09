package my.nocodeplatform.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(description = "编辑应用请求（用户编辑自己的应用）")
public class AppEditRequest implements Serializable {
    
    @Schema(description = "应用 ID", required = true, example = "1")
    private Long id;

    @Schema(description = "应用名称", example = "新的应用名称")
    private String appName;

    private static final long serialVersionUID = 1L;
}
