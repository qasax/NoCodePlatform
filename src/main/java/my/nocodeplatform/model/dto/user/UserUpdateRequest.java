package my.nocodeplatform.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新用户请求（管理员专用）")
public class UserUpdateRequest implements Serializable {

    @Schema(description = "用户 ID", required = true, example = "1")
    private Long id;

    @Schema(description = "用户昵称", example = "新昵称")
    private String userName;

    @Schema(description = "用户头像 URL", example = "https://example.com/new-avatar.png")
    private String userAvatar;

    @Schema(description = "用户简介", example = "更新后的简介")
    private String userProfile;

    @Schema(description = "用户角色：user / admin", example = "user")
    private String userRole;

    private static final long serialVersionUID = 1L;
}