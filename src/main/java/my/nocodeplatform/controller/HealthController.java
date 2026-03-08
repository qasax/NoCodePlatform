package my.nocodeplatform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import my.nocodeplatform.common.BaseResponse;
import my.nocodeplatform.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查 控制层。
 *
 * @author zhangfajin
 */
@RestController("/health")
@Tag(name = "健康检查", description = "服务健康状态检测接口")
public class HealthController {

    @Operation(summary = "健康检查", description = "检测服务是否正常运行，返回 ok 表示服务正常")
    @GetMapping("/")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
