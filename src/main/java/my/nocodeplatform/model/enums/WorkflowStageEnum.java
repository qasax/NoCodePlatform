package my.nocodeplatform.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流阶段枚举
 */
@Getter
@AllArgsConstructor
public enum WorkflowStageEnum {
    
    WORKFLOW_START("WORKFLOW_START", "工作流启动", "开始执行代码生成工作流", 0),
    IMAGE_COLLECTION("IMAGE_COLLECTION", "图片收集", "正在智能收集和匹配相关图片资源", 10),
    PROMPT_ENHANCEMENT("PROMPT_ENHANCEMENT", "提示词增强", "正在优化和增强需求描述", 20),
    INTELLIGENT_ROUTING("INTELLIGENT_ROUTING", "智能路由", "正在分析项目类型并选择最佳生成方案", 30),
    CODE_GENERATION("CODE_GENERATION", "代码生成", "AI 正在生成项目代码", 50),
    QUALITY_CHECK("QUALITY_CHECK", "质量检查", "正在进行代码质量和规范性检查", 70),
    PROJECT_BUILD("PROJECT_BUILD", "项目构建", "正在编译和打包项目代码", 85),
    WORKFLOW_COMPLETE("WORKFLOW_COMPLETE", "工作流完成", "代码生成工作流执行完成", 100);
    
    /**
     * 阶段代码
     */
    private final String code;
    
    /**
     * 阶段名称
     */
    private final String name;
    
    /**
     * 阶段描述
     */
    private final String description;
    
    /**
     * 阶段权重（完成时的累计进度百分比）
     */
    private final Integer weight;
    
    /**
     * 根据代码获取枚举
     */
    public static WorkflowStageEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkflowStageEnum stage : values()) {
            if (stage.code.equals(code)) {
                return stage;
            }
        }
        return null;
    }
}
