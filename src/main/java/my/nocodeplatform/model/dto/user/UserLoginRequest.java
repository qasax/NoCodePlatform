package my.nocodeplatform.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    @Schema(description = "账号", required = true, example = "zhangsan")
    private String userAccount;

    @Schema(description = "密码", required = true, example = "12345678")
    private String userPassword;
}