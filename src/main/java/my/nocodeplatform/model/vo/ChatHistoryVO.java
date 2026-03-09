package my.nocodeplatform.model.vo;

import lombok.Data;
import my.nocodeplatform.entity.ChatHistory;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图
 */
@Data
public class ChatHistoryVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型 (user/ai/error)
     */
    private String messageType;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;

    /**
     * 包装类转对象
     *
     * @param chatHistoryVO
     * @return
     */
    public static ChatHistory voToObj(ChatHistoryVO chatHistoryVO) {
        if (chatHistoryVO == null) {
            return null;
        }
        ChatHistory chatHistory = new ChatHistory();
        BeanUtils.copyProperties(chatHistoryVO, chatHistory);
        return chatHistory;
    }

    /**
     * 对象转包装类
     *
     * @param chatHistory
     * @return
     */
    public static ChatHistoryVO objToVo(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        BeanUtils.copyProperties(chatHistory, chatHistoryVO);
        return chatHistoryVO;
    }
}
