package my.nocodeplatform.controller;


import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import my.nocodeplatform.annotation.AuthCheck;
import my.nocodeplatform.common.BaseResponse;
import my.nocodeplatform.common.ResultUtils;
import my.nocodeplatform.constant.UserConstant;
import my.nocodeplatform.entity.App;
import my.nocodeplatform.entity.ChatHistory;
import my.nocodeplatform.entity.User;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.exception.ThrowUtils;
import my.nocodeplatform.model.dto.chathistory.ChatHistoryQueryRequest;
import my.nocodeplatform.model.vo.ChatHistoryVO;
import my.nocodeplatform.service.AppService;
import my.nocodeplatform.service.ChatHistoryService;
import my.nocodeplatform.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author zhangfajin
 */
@RestController
@RequestMapping("/chat_history")
@Tag(name = "对话历史管理", description = "对话历史的查询与管理")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Operation(summary = "分页查询对话历史列表（管理员）", description = "管理员可查询全量对话历史分页列表，倒序展示")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listChatHistoryByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        long current = chatHistoryQueryRequest.getPageNum();
        long size = chatHistoryQueryRequest.getPageSize();
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(current, size), chatHistoryService.getQueryWrapper(chatHistoryQueryRequest));
        return ResultUtils.success(chatHistoryPage);
    }

    @Operation(summary = "分页查询应用的对话历史视图", description = "根据 appId 获取应用对应的聊天历史，仅应用创建者可见。每次请求10条。")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryVOByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest,
                                                                     HttpServletRequest request) {
        if (chatHistoryQueryRequest == null || chatHistoryQueryRequest.getAppId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少 appId 参数");
        }
        Long appId = chatHistoryQueryRequest.getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        User loginUser = userService.getLoginUser(request);
        // 仅包含本人或管理员的校验逻辑
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能查看自己创建的应用历史对话");
        }

        long current = chatHistoryQueryRequest.getPageNum();
        long size = chatHistoryQueryRequest.getPageSize();
        // 强制倒序获取最新消息，通过时间降序
        chatHistoryQueryRequest.setSortField("createTime");
        chatHistoryQueryRequest.setSortOrder("descend");

        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(current, size), chatHistoryService.getQueryWrapper(chatHistoryQueryRequest));
        return ResultUtils.success(chatHistoryService.getChatHistoryVOPage(chatHistoryPage, request));
    }

}
