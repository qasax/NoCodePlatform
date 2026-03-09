package my.nocodeplatform.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.entity.ChatHistory;
import my.nocodeplatform.entity.User;
import my.nocodeplatform.mapper.ChatHistoryMapper;
import my.nocodeplatform.model.dto.chathistory.ChatHistoryQueryRequest;
import my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum;
import my.nocodeplatform.model.vo.ChatHistoryVO;
import my.nocodeplatform.model.vo.UserVO;
import my.nocodeplatform.service.ChatHistoryService;
import my.nocodeplatform.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author zhangfajin
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private UserService userService;

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        if (chatHistoryQueryRequest == null) {
            return QueryWrapper.create();
        }

        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        String messageType = chatHistoryQueryRequest.getMessageType();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("messageType", messageType);

        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory, HttpServletRequest request) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = ChatHistoryVO.objToVo(chatHistory);
        // 关联查询用户信息
        Long userId = chatHistory.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        chatHistoryVO.setUser(userVO);

        return chatHistoryVO;
    }

    @Override
    public Page<ChatHistoryVO> getChatHistoryVOPage(Page<ChatHistory> chatHistoryPage, HttpServletRequest request) {
        List<ChatHistory> chatHistoryList = chatHistoryPage.getRecords();
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(chatHistoryPage.getPageNumber(), chatHistoryPage.getPageSize(), chatHistoryPage.getTotalRow());
        if (CollUtil.isEmpty(chatHistoryList)) {
            return chatHistoryVOPage;
        }
        // 关联查询用户信息
        Set<Long> userIdSet = chatHistoryList.stream().map(ChatHistory::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryList.stream().map(chatHistory -> {
            ChatHistoryVO chatHistoryVO = ChatHistoryVO.objToVo(chatHistory);
            Long userId = chatHistory.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            chatHistoryVO.setUser(userService.getUserVO(user));
            return chatHistoryVO;
        }).collect(Collectors.toList());

        chatHistoryVOPage.setRecords(chatHistoryVOList);
        return chatHistoryVOPage;
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }
}
