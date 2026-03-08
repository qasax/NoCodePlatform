package my.nocodeplatform.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    @Schema(description = "账号（至少 4 位）", required = true, example = "zhangsan")
    private String userAccount;

    @Schema(description = "密码（至少 8 位）", required = true, example = "12345678")
    private String userPassword;

    @Schema(description = "确认密码（需与密码一致）", required = true, example = "12345678")
    private String checkPassword;
}