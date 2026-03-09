package my.nocodeplatform.model.dto.chathistory;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.nocodeplatform.common.PageRequest;

import java.io.Serializable;

/**
 * 对话历史查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 消息类型
     */
    private String messageType;

    private static final long serialVersionUID = 1L;
}
