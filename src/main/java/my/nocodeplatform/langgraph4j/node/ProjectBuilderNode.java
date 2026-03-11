package my.nocodeplatform.langgraph4j.node;

import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.model.core.builder.VueProjectBuilder;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.langgraph4j.model.BuildResult;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import my.nocodeplatform.progress.WorkflowProgressBroadcaster;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProjectBuilderNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点：项目构建");
            
            // 获取进度广播器并发送阶段开始消息
            WorkflowProgressBroadcaster broadcaster = SpringContextUtil.getBean(WorkflowProgressBroadcaster.class);
            broadcaster.sendStageStart(context.getAppId(), WorkflowStageEnum.PROJECT_BUILD, WorkflowStageEnum.QUALITY_CHECK.getWeight());
            
            // 获取必要的参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            BuildResult buildResult = new BuildResult();
            String buildResultDir;
            
            // 一定是 Vue 项目类型：使用 VueProjectBuilder 进行构建
            try {
                VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                
                // 发送构建进度更新
                broadcaster.sendStageProgress(
                    context.getAppId(),
                    WorkflowStageEnum.PROJECT_BUILD,
                    WorkflowStageEnum.QUALITY_CHECK.getWeight() + 5,
                    "正在安装项目依赖...",
                    "{\"buildStep\":\"npm_install\",\"buildProgress\":10}"
                );
                
                // 执行 Vue 项目构建（npm install + npm run build）
                boolean buildSuccess = vueBuilder.buildProject(generatedCodeDir);
                
                if (buildSuccess) {
                    // 构建成功，返回 dist 目录路径
                    buildResultDir = generatedCodeDir + File.separator + "dist";
                    log.info("Vue 项目构建成功，dist 目录：{}", buildResultDir);
                    buildResult.setIsValid(true);
                    // 发送构建完成消息
                    broadcaster.sendStageComplete(
                        context.getAppId(),
                        WorkflowStageEnum.PROJECT_BUILD,
                        WorkflowStageEnum.PROJECT_BUILD.getWeight(),
                        "Vue 项目构建成功，已生成可部署产物",
                        "{\"buildStep\":\"completed\",\"buildProgress\":100,\"outputDir\":\"" + buildResultDir + "\"}"
                    );
                } else {
                    buildResult.setIsValid(false);
                    buildResult.setErrors(List.of("build failed"));
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                }
            } catch (Exception e) {
                log.error("Vue 项目构建异常：{}", e.getMessage(), e);
                buildResultDir = generatedCodeDir; // 异常时返回原路径
                buildResult.setIsValid(false);
                buildResult.setErrors(Collections.singletonList(e.getMessage()));
                // 发送错误消息
                broadcaster.sendError(
                    context.getAppId(),
                    WorkflowStageEnum.PROJECT_BUILD,
                    "项目构建失败：" + e.getMessage()
                );
            }
            
            // 更新状态
            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            context.setBuildResult(buildResult);
            log.info("项目构建节点完成，最终目录：{}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}