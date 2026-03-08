package my.nocodeplatform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "登录用户信息视图（脱敏后的当前登录用户信息）")
public class LoginUserVO implements Serializable {

    @Schema(description = "用户 ID", example = "1")
    private Long id;

    @Schema(description = "账号", example = "zhangsan")
    private String userAccount;

    @Schema(description = "用户昵称", example = "张三")
    private String userName;

    @Schema(description = "用户头像 URL")
    private String userAvatar;

    @Schema(description = "用户简介")
    private String userProfile;

    @Schema(description = "用户角色：user / admin", example = "user")
    private String userRole;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}