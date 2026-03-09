package my.nocodeplatform.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.nocodeplatform.common.PageRequest;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "应用查询请求（支持分页和多条件筛选）")
public class AppQueryRequest extends PageRequest implements Serializable {

    @Schema(description = "应用 ID（精确匹配）", example = "1")
    private Long id;

    @Schema(description = "应用名称（模糊匹配）", example = "待办")
    private String appName;

    @Schema(description = "代码生成类型", example = "vue")
    private String codeGenType;

    @Schema(description = "创建用户 ID（精确匹配）", example = "1")
    private Long userId;

    private static final long serialVersionUID = 1L;
}
