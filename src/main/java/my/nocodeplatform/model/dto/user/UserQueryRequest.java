package my.nocodeplatform.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.nocodeplatform.common.PageRequest;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户查询请求（支持分页和多条件筛选）")
public class UserQueryRequest extends PageRequest implements Serializable {

    @Schema(description = "用户 ID（精确匹配）", example = "1")
    private Long id;

    @Schema(description = "用户昵称（模糊匹配）", example = "张")
    private String userName;

    @Schema(description = "账号（模糊匹配）", example = "zhang")
    private String userAccount;

    @Schema(description = "用户简介（模糊匹配）", example = "程序员")
    private String userProfile;

    @Schema(description = "用户角色筛选：user / admin / ban", example = "user")
    private String userRole;

    private static final long serialVersionUID = 1L;
}