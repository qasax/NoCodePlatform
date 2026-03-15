package my.nocodeplatform.model.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.nocodeplatform.model.enums.ProgressStatusEnum;
import my.nocodeplatform.model.enums.WorkflowStageEnum;

/**
 * 工作流进度数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowProgressDTO {
    
    /**
     * 进度 ID（用于消息去重）
     */
    private String progressId;
    
    /**
     * 当前阶段
     */
    private WorkflowStageEnum stage;
    
    /**
     * 阶段名称（中文）
     */
    private String stageName;
    
    /**
     * 阶段描述
     */
    private String stageDescription;
    
    /**
     * 状态
     */
    private ProgressStatusEnum status;
    
    /**
     * 完成百分比 (0-100)
     */
    private Integer percentComplete;
    
    /**
     * 预计剩余时间（秒）
     */
    private Integer estimatedRemainingSeconds;
    
    /**
     * 关键操作提示
     */
    private String message;
    
    /**
     * 详细信息（JSON 字符串）
     */
    private String details;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 错误信息（仅在失败时）
     */
    private String errorMessage;
    
    /**
     * 是否为心跳消息
     */
    @Builder.Default
    private Boolean isHeartbeat = false;
    
    /**
     * 应用 ID
     */
    private Long appId;
}
