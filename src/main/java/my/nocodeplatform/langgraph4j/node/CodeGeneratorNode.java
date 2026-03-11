package my.nocodeplatform.langgraph4j.node;

import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.service.AiCodeGeneratorFacade;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.constant.AppConstant;
import my.nocodeplatform.langgraph4j.model.QualityResult;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import my.nocodeplatform.progress.WorkflowProgressBroadcaster;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CodeGeneratorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点：代码生成");
            
            // 获取进度广播器并发送阶段开始消息
            WorkflowProgressBroadcaster broadcaster = SpringContextUtil.getBean(WorkflowProgressBroadcaster.class);
            broadcaster.sendStageStart(context.getAppId(), WorkflowStageEnum.CODE_GENERATION, WorkflowStageEnum.INTELLIGENT_ROUTING.getWeight());
            
            // 使用增强提示词作为发给 AI 的用户消息
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            // 获取 AI 代码生成外观服务
            AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("开始生成代码，类型：{} ({})", generationType.getValue(), generationType.getText());
            // 获取应用 ID
            Long appId = context.getAppId();
            
            // 调用流式代码生成并监控进度
            int[] fileCount = {0};
            try {
                //构建过程中出现问题
                if(context.getBuildResult()!=null&&context.getBuildResult().getErrors()!=null) {
                    userMessage = context.getBuildResult().getErrors()+userMessage;
                }
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType, appId);
            
            // 同步等待流式输出完成，并定期发送进度更新
            codeStream.doOnNext(chunk -> {
                // 简单估算进度（实际应该根据生成的文件数）
                fileCount[0]++;
                
                // 每 3 个 chunk 更新一次进度（更频繁）
                if (fileCount[0] % 100 == 0) {
                    int progress = Math.min(WorkflowStageEnum.CODE_GENERATION.getWeight() - 1, 
                                           WorkflowStageEnum.INTELLIGENT_ROUTING.getWeight() + (fileCount[0] / 2));
                    broadcaster.sendStageProgress(
                        appId,
                        WorkflowStageEnum.CODE_GENERATION,
                        progress,
                        "正在生成代码，已生成 " + fileCount[0] + " 个代码块...",
                        "{\"generatedChunks\":" + fileCount[0] + "}"
                    );
                }
            }).blockLast(Duration.ofMinutes(30));
            }catch (Exception e){
                log.error("代码生成出现异常",e);
            }
            // 根据类型设置生成目录
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI 代码生成完成，生成目录：{}", generatedCodeDir);
            
            // 发送阶段完成消息
            broadcaster.sendStageComplete(
                appId,
                WorkflowStageEnum.CODE_GENERATION,
                WorkflowStageEnum.CODE_GENERATION.getWeight(),
                "代码生成完成，共生成 " + fileCount[0] + " 个代码块",
                "{\"generatedChunks\":" + fileCount[0] + ",\"codeDir\":\"" + generatedCodeDir + "\"}"
            );
            
            // 更新状态
            context.setCurrentStep("代码生成");
            context.setGeneratedCodeDir(generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }
    
    /**
     * 构建用户消息
     */
    private static String buildUserMessage(WorkflowContext context) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(context.getEnhancedPrompt());
        return messageBuilder.toString();
    }
    /**
     * 构造用户消息，如果存在质检失败结果则添加错误修复信息
     */
//    private static String buildUserMessage(WorkflowContext context) {
//        String userMessage = context.getEnhancedPrompt();
//        // 检查是否存在质检失败结果
//        QualityResult qualityResult = context.getQualityResult();
//        if (isQualityCheckFailed(qualityResult)) {
//            // 直接将错误修复信息作为新的提示词（起到了修改的作用）
//            userMessage = buildErrorFixPrompt(qualityResult);
//        }
//        return userMessage;
//    }

    /**
     * 判断质检是否失败
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * 构造错误修复提示词
     */
    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        // 添加错误列表
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // 添加修复建议（如果有）
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }

}
