package my.nocodeplatform.langgraph4j.node;

import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.service.AiCodeGenTypeRoutingService;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.ai.service.AiCodeGenTypeRoutingServiceFactory;
import my.nocodeplatform.entity.App;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import my.nocodeplatform.progress.WorkflowProgressBroadcaster;
import my.nocodeplatform.service.AppService;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点：智能路由");
            
            // 发送阶段开始消息
            WorkflowProgressBroadcaster broadcaster = SpringContextUtil.getBean(WorkflowProgressBroadcaster.class);
            broadcaster.sendStageStart(context.getAppId(), WorkflowStageEnum.INTELLIGENT_ROUTING, WorkflowStageEnum.PROMPT_ENHANCEMENT.getWeight());
            
            CodeGenTypeEnum generationType = null;
            try {
                // 获取 AI 路由服务
                AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory = SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.getAiCodeGenTypeRoutingService();
                // 根据原始提示词进行智能路由
                AppService appService = SpringContextUtil.getBean(AppService.class);
                App app = appService.getById(context.getAppId());
                //没有生成方式时，进行路由
                if(app.getCodeGenType()==null||app.getCodeGenType().isEmpty())
                {
                    generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                    app.setCodeGenType(generationType.getValue());
                    appService.updateById(app);
                    log.info("AI 智能路由完成，选择类型：{} ({})", generationType.getValue(), generationType.getText());
                }else{
                    generationType = CodeGenTypeEnum.valueOf(app.getCodeGenType());
                    log.info("AI 智能路由跳过，已有类型：{} ({})", generationType.getValue(), generationType.getText());
                }


            } catch (Exception e) {
                log.error("AI 智能路由失败，使用默认 HTML 类型：{}", e.getMessage(),e);
                generationType = CodeGenTypeEnum.HTML;
                AppService appService = SpringContextUtil.getBean(AppService.class);
                App app = appService.getById(context.getAppId());
                app.setCodeGenType(generationType.getValue());
                appService.updateById(app);
            }
            
            // 发送阶段完成消息
            broadcaster.sendStageComplete(
                context.getAppId(),
                WorkflowStageEnum.INTELLIGENT_ROUTING,
                WorkflowStageEnum.INTELLIGENT_ROUTING.getWeight(),
                "已选择 " + generationType.getText() + " 生成方案",
                "{\"selectedType\":\"" + generationType.getValue() + "\",\"typeName\":\"" + generationType.getText() + "\"}"
            );
            
            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}