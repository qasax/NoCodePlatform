package my.nocodeplatform.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 进度状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProgressStatusEnum {
    
    STARTED("STARTED", "阶段开始"),
    IN_PROGRESS("IN_PROGRESS", "阶段进行中"),
    COMPLETED("COMPLETED", "阶段完成"),
    FAILED("FAILED", "阶段失败"),
    SKIPPED("SKIPPED", "阶段跳过");
    
    /**
     * 状态代码
     */
    private final String code;
    
    /**
     * 状态描述
     */
    private final String description;
    
    /**
     * 根据代码获取枚举
     */
    public static ProgressStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProgressStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
