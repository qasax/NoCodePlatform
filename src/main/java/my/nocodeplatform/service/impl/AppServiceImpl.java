package my.nocodeplatform.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.service.AiCodeGenTypeRoutingService;
import my.nocodeplatform.ai.service.AiCodeGenTypeRoutingServiceFactory;
import my.nocodeplatform.ai.service.AiCodeGeneratorFacade;
import my.nocodeplatform.ai.model.core.builder.VueProjectBuilder;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.ai.utils.WebScreenshotUtils;
import my.nocodeplatform.langgraph4j.workflow.CodeGenConcurrentWorkflow;
import my.nocodeplatform.constant.AppConstant;
import my.nocodeplatform.entity.App;
import my.nocodeplatform.entity.User;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.exception.ThrowUtils;
import my.nocodeplatform.mapper.AppMapper;
import my.nocodeplatform.model.dto.app.AppQueryRequest;
import my.nocodeplatform.model.vo.AppVO;
import my.nocodeplatform.model.vo.UserVO;
import my.nocodeplatform.service.AppService;
import my.nocodeplatform.service.UserService;
import my.nocodeplatform.utils.MinioFileUploadUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author zhangfajin
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private my.nocodeplatform.service.ChatHistoryService chatHistoryService;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private AiCodeGenTypeRoutingServiceFactory  aiCodeGenTypeRoutingServiceFactory;
    @Resource
    private MinioFileUploadUtil minioFileUploadUtil;

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            return QueryWrapper.create();
        }

        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String codeGenType = appQueryRequest.getCodeGenType();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .eq("codeGenType", codeGenType)
                .eq("userId", userId)
                .like("appName", appName);

        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    @Override
    public AppVO getAppVO(App app, HttpServletRequest request) {
        if (app == null) {
            return null;
        }
        AppVO appVO = AppVO.objToVo(app);
        // 关联查询用户信息
        Long userId = app.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        appVO.setUser(userVO);

        return appVO;
    }

    @Override
    public Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request) {
        List<App> appList = appPage.getRecords();
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        if (CollUtil.isEmpty(appList)) {
            return appVOPage;
        }
        // 关联查询用户信息
        Set<Long> userIdSet = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<AppVO> appVOList = appList.stream().map(app -> {
            AppVO appVO = AppVO.objToVo(app);
            Long userId = app.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            appVO.setUser(userService.getUserVO(user));
            return appVO;
        }).collect(Collectors.toList());

        appVOPage.setRecords(appVOList);
        return appVOPage;
    }
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 记录用户发送的消息
        my.nocodeplatform.entity.ChatHistory userMessage = new my.nocodeplatform.entity.ChatHistory();
        userMessage.setAppId(appId);
        userMessage.setUserId(loginUser.getId());
        userMessage.setMessageType(my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum.USER.getValue());
        userMessage.setMessage(message);
        chatHistoryService.save(userMessage);
        // 使用 AI 智能选择代码生成类型
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.getAiCodeGenTypeRoutingService();

        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(app.getInitPrompt());
        app.setCodeGenType(selectedCodeGenType.getValue());
        this.updateById(app);
        // 6. 调用 AI 生成代码，并监控返回流进行数据记录
        StringBuilder aiMessageBuilder = new StringBuilder();
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId)
                .doOnNext(chunk -> aiMessageBuilder.append(chunk))
                .doOnComplete(() -> {
                    if(app.getCodeGenType().equals(CodeGenTypeEnum.VUE_PROJECT.getValue())) {
                        // 异步构造 Vue 项目
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProjectAsync(projectPath);
                    }
                    my.nocodeplatform.entity.ChatHistory aiMessage = new my.nocodeplatform.entity.ChatHistory();
                    aiMessage.setAppId(appId);
                    aiMessage.setUserId(loginUser.getId());
                    aiMessage.setMessageType(my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum.AI.getValue());
                    aiMessage.setMessage(aiMessageBuilder.toString());
                    chatHistoryService.save(aiMessage);
                })
                .doOnError(e -> {
                    my.nocodeplatform.entity.ChatHistory errorMessage = new my.nocodeplatform.entity.ChatHistory();
                    errorMessage.setAppId(appId);
                    errorMessage.setUserId(loginUser.getId());
                    errorMessage.setMessageType(my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum.ERROR.getValue());
                    errorMessage.setMessage(e.getMessage());
                    chatHistoryService.save(errorMessage);
                });
    }
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }else{
            String[] parts = deployKey.split("/");
            deployKey = parts[parts.length - 1].isEmpty() ? parts[parts.length - 2] : parts[parts.length - 1];
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;

        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey));
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    @Override
    public String getPreviewUrl(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限预览该应用，目前仅本人可以预览（也可以放开）
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限预览该应用");
        }
        // 4. 构建源工作空间名称
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isBlank(codeGenType)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未知的代码生成类型");
        }
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        
        // 5. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        
        // 6. 返回本地 StaticResourceController 处理的预览 URL
        // 例如：/api/static/{sourceDirName}/ 或者根据前端自行拼接 baseUrl
        return String.format("/api/static/%s/", sourceDirName);
    }

    @Override
    public void saveCover(Long appId, String deployUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(3000);
                // 调用截图服务生成截图
                String screenshotUrl = WebScreenshotUtils.saveWebPageScreenshot(deployUrl);
                if (StrUtil.isBlank(screenshotUrl)) {
                    log.warn("截图生成失败，appId={}", appId);
                    return;
                }
                File screenshotFile = new File(AppConstant.PIC_ROOT_DIR + screenshotUrl);
                if (!screenshotFile.exists() || screenshotFile.isDirectory()) {
                    log.warn("截图文件不存在，appId={}, path={}", appId, screenshotFile.getAbsolutePath());
                    return;
                }
                String ext = FileUtil.extName(screenshotFile);
                if (StrUtil.isBlank(ext)) {
                    ext = "jpg";
                }
                String objectName = String.format("app/cover/%d/%s.%s",
                        appId,
                        UUID.randomUUID().toString().replace("-", ""),
                        ext);
                String coverUrl = minioFileUploadUtil.uploadFile(screenshotFile, objectName);
                // 清理本地截图
                File parentDir = screenshotFile.getParentFile();
                if (parentDir != null) {
                    FileUtil.del(parentDir);
                }
                // 更新应用封面字段
                App updateApp = new App();
                updateApp.setId(appId);
                updateApp.setCover(coverUrl);
                boolean updated = this.updateById(updateApp);
                ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Flux<String> chatToGenCodeWithAgent(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证权限
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 记录用户消息
        my.nocodeplatform.entity.ChatHistory userMessage = new my.nocodeplatform.entity.ChatHistory();
        userMessage.setAppId(appId);
        userMessage.setUserId(loginUser.getId());
        userMessage.setMessageType(my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum.USER.getValue());
        userMessage.setMessage(message);
        chatHistoryService.save(userMessage);
        String codeGenType = app.getCodeGenType();
        // 5. 执行工作流
        CodeGenConcurrentWorkflow workflow = new CodeGenConcurrentWorkflow();
        StringBuilder aiMessageBuilder = new StringBuilder();
        return workflow.executeWorkflowWithFlux(appId, codeGenType, message)
                .doOnNext(chunk -> aiMessageBuilder.append(chunk).append("\n"))
                .doOnComplete(() -> {
                    my.nocodeplatform.entity.ChatHistory aiMessage = new my.nocodeplatform.entity.ChatHistory();
                    aiMessage.setAppId(appId);
                    aiMessage.setUserId(loginUser.getId());
                    aiMessage.setMessageType(my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum.AI.getValue());
                    aiMessage.setMessage(aiMessageBuilder.toString());
                    chatHistoryService.save(aiMessage);
                })
                .doOnError(e -> {
                    my.nocodeplatform.entity.ChatHistory errorMessage = new my.nocodeplatform.entity.ChatHistory();
                    errorMessage.setAppId(appId);
                    errorMessage.setUserId(loginUser.getId());
                    errorMessage.setMessageType(my.nocodeplatform.model.enums.ChatHistoryMessageTypeEnum.ERROR.getValue());
                    errorMessage.setMessage(e.getMessage());
                    chatHistoryService.save(errorMessage);
                });
    }
}
