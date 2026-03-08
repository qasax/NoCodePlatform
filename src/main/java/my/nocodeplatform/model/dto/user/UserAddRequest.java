package my.nocodeplatform.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建用户请求（管理员专用）")
public class UserAddRequest implements Serializable {

    @Schema(description = "用户昵称", example = "张三")
    private String userName;

    @Schema(description = "账号", required = true, example = "zhangsan")
    private String userAccount;

    @Schema(description = "用户头像 URL", example = "https://example.com/avatar.png")
    private String userAvatar;

    @Schema(description = "用户简介", example = "这是一个程序员")
    private String userProfile;

    @Schema(description = "用户角色：user / admin", example = "user")
    private String userRole;

    private static final long serialVersionUID = 1L;
}