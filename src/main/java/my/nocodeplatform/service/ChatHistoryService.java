package my.nocodeplatform.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.servlet.http.HttpServletRequest;
import my.nocodeplatform.entity.ChatHistory;
import my.nocodeplatform.model.dto.chathistory.ChatHistoryQueryRequest;
import my.nocodeplatform.model.vo.ChatHistoryVO;

/**
 * 对话历史 服务层。
 *
 * @author zhangfajin
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 获取对话历史封装
     *
     * @param chatHistory
     * @param request
     * @return
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory, HttpServletRequest request);

    /**
     * 分页获取对话历史封装
     *
     * @param chatHistoryPage
     * @param request
     * @return
     */
    Page<ChatHistoryVO> getChatHistoryVOPage(Page<ChatHistory> chatHistoryPage, HttpServletRequest request);

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
