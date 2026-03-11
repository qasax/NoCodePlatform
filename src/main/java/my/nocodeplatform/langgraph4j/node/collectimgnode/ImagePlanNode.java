package my.nocodeplatform.langgraph4j.node.collectimgnode;

import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.langgraph4j.ai.ImageCollectionPlanService;
import my.nocodeplatform.langgraph4j.ai.ImageCollectionPlanServiceFactory;
import my.nocodeplatform.langgraph4j.model.ImageCollectionPlan;
import my.nocodeplatform.langgraph4j.node.SpringContextUtil;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImagePlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            try {
                // 获取图片收集计划服务
                ImageCollectionPlanServiceFactory imageCollectionPlanServiceFactory = SpringContextUtil.getBean(ImageCollectionPlanServiceFactory.class);
                ImageCollectionPlanService imageCollectionPlanService = imageCollectionPlanServiceFactory.getImageCollectionPlanService();
                ImageCollectionPlan plan = imageCollectionPlanService.planImageCollection(originalPrompt);
                log.info("生成图片收集计划，准备启动并发分支");
                // 将计划存储到上下文中
                context.setImageCollectionPlan(plan);
                context.setCurrentStep("图片计划");
            } catch (Exception e) {
                log.error("图片计划生成失败: {}", e.getMessage(), e);
            }
            return WorkflowContext.saveContext(context);
        });
    }
}