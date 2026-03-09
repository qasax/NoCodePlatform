package my.nocodeplatform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import my.nocodeplatform.entity.App;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "应用信息视图（脱敏后的应用信息，包含创建者信息）")
public class AppVO implements Serializable {

    @Schema(description = "应用 ID", example = "1")
    private Long id;

    @Schema(description = "应用名称", example = "我的待办应用")
    private String appName;

    @Schema(description = "应用封面图片 URL")
    private String cover;

    @Schema(description = "代码生成类型", example = "vue")
    private String codeGenType;

    @Schema(description = "部署标识 Key")
    private String deployKey;

    @Schema(description = "部署时间")
    private LocalDateTime deployedTime;

    @Schema(description = "优先级（用于精选排序）", example = "0")
    private Integer priority;

    @Schema(description = "创建用户 ID", example = "1")
    private Long userId;

    @Schema(description = "编辑时间")
    private LocalDateTime editTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人信息")
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 包装类转对象
     *
     * @param appVO
     * @return
     */
    public static App voToObj(AppVO appVO) {
        if (appVO == null) {
            return null;
        }
        App app = new App();
        BeanUtils.copyProperties(appVO, app);
        return app;
    }

    /**
     * 对象转包装类
     *
     * @param app
     * @return
     */
    public static AppVO objToVo(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);
        return appVO;
    }
}
