package my.nocodeplatform.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "应用部署请求")
public class AppDeployRequest implements Serializable {

    @Schema(description = "要部署的应用 ID", required = true, example = "1")
    long appId;
}
