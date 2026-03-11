package my.nocodeplatform.controller;

import annotation.AuthCheck;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import my.nocodeplatform.common.BaseResponse;
import my.nocodeplatform.common.DeleteRequest;
import my.nocodeplatform.common.ResultUtils;
import my.nocodeplatform.constant.UserConstant;
import my.nocodeplatform.entity.App;
import my.nocodeplatform.entity.User;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.exception.ThrowUtils;
import my.nocodeplatform.model.dto.app.*;
import my.nocodeplatform.model.vo.AppVO;
import my.nocodeplatform.service.AppService;
import my.nocodeplatform.service.ChatHistoryService;
import my.nocodeplatform.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author zhangfajin
 */
@RestController
@RequestMapping("/app")
@Tag(name = "应用管理", description = "应用的创建、编辑、删除、查询、AI 代码生成、预览与部署等接口")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;


    // region 增删改查

    @Operation(summary = "创建应用", description = "用户提交应用名称、封面和初始 Prompt 来创建一个新应用，返回应用 ID")
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        if (appAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StrUtil.isBlank(appAddRequest.getInitPrompt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须填写初始化 prompt");
        }
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        User loginUser = userService.getLoginUser(request);
        app.setUserId(loginUser.getId());
        app.setPriority(0); // 默认非精选
        app.setCodeGenType(null);
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(app.getInitPrompt().substring(0, Math.min(app.getInitPrompt().length(), 12)));
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    @Operation(summary = "删除应用", description = "删除指定应用，仅应用创建者或管理员可操作")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(user.getId()) && !UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = appService.removeById(id);
        if (b) {
            com.mybatisflex.core.query.QueryWrapper queryWrapper = com.mybatisflex.core.query.QueryWrapper.create().eq("appId", id);
            chatHistoryService.remove(queryWrapper);
        }
        return ResultUtils.success(b);
    }

    @Operation(summary = "更新应用（管理员）", description = "管理员可更新任意应用的名称、封面和优先级等信息")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = new App();
        BeanUtil.copyProperties(appUpdateRequest, app);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @Operation(summary = "编辑应用", description = "用户编辑自己创建的应用名称，仅本人可操作")
    @PostMapping("/edit")
    public BaseResponse<Boolean> editApp(@RequestBody AppEditRequest appEditRequest, HttpServletRequest request) {
        if (appEditRequest == null || appEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        App oldApp = appService.getById(appEditRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可编辑
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(appEditRequest.getId());
        app.setAppName(appEditRequest.getAppName());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @Operation(summary = "获取应用详情（脱敏）", description = "根据应用 ID 获取应用的包装视图信息，包含创建者信息")
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(
            @Parameter(description = "应用 ID", required = true) @RequestParam long id,
            HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = appService.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(appService.getAppVO(app, request));
    }

    @Operation(summary = "获取应用详情（管理员）", description = "管理员根据 ID 获取未脱敏的应用完整数据库信息")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppById(
            @Parameter(description = "应用 ID", required = true) @RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

    // endregion

    // region 分页查询

    @Operation(summary = "分页查询应用列表（管理员）", description = "管理员可不受限制地查询全量应用分页列表，支持按任意字段检索")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        long current = appQueryRequest.getPageNum();
        long size = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(Page.of(current, size), appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    @Operation(summary = "分页查询精选应用列表", description = "查询当前平台已发布的精选应用分页列表，每页最多 20 条")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                     HttpServletRequest request) {
        long current = appQueryRequest.getPageNum();
        long size = appQueryRequest.getPageSize();
        appQueryRequest.setPriority(99);
        // 限制最多20条
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<App> appPage = appService.page(Page.of(current, size), appService.getQueryWrapper(appQueryRequest).eq("priority", appQueryRequest.getPriority()));
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    @Operation(summary = "分页查询我的应用列表", description = "查询当前登录用户自己创建的应用分页列表，每页最多 20 条")
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        long current = appQueryRequest.getPageNum();
        long size = appQueryRequest.getPageSize();
        // 限制最多20条
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<App> appPage = appService.page(Page.of(current, size), appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    // endregion

    // region AI 代码生成与部署

    @Operation(summary = "AI 聊天生成代码（SSE 流式）", description = "用户通过聊天消息驱动 AI 为指定应用生成代码，以 SSE 流式返回结果。仅应用创建者可调用")
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       @RequestParam(required = false, defaultValue = "false") Boolean agent,
                                                       @RequestParam(required = false, defaultValue = "false") String selectedElement,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if(selectedElement!=null && !selectedElement.equals("false")){
            message+=selectedElement;
        }
        // 调用服务生成代码（流式）
        Flux<String> contentFlux;
        if (Boolean.TRUE.equals(agent)) {
            contentFlux = appService.chatToGenCodeWithAgent(appId, message, loginUser);
        } else {
            contentFlux = appService.chatToGenCode(appId, message, loginUser);
        }
        // 转换为 ServerSentEvent 格式
        return contentFlux
                .map(chunk -> {
                    // 将内容包装成JSON对象
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    @Operation(summary = "部署应用", description = "将已生成的应用代码部署到公共目录，生成唯一的访问 URL。仅应用创建者可部署")
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        appService.saveCover(appId,deployUrl);
        return ResultUtils.success(deployUrl);
    }

    @Operation(summary = "预览应用", description = "获取已生成应用的预览静态资源地址，仅应用创建者可预览")
    @GetMapping("/preview/{appId}")
    public BaseResponse<String> previewApp(
            @Parameter(description = "应用 ID", required = true) @PathVariable Long appId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务获取预览 URL
        String previewUrl = appService.getPreviewUrl(appId, loginUser);
        return ResultUtils.success(previewUrl);
    }

    // endregion
}
