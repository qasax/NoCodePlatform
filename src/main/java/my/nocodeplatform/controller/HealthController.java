package my.nocodeplatform.controller;

import my.nocodeplatform.common.BaseResponse;
import my.nocodeplatform.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/health")
public class HealthController {
    @GetMapping("/")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
