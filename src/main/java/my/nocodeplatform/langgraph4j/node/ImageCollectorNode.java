package my.nocodeplatform.langgraph4j.node;

import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.langgraph4j.ai.ImageCollectionService;
import my.nocodeplatform.langgraph4j.ai.ImageCollectionServiceFactory;
import my.nocodeplatform.langgraph4j.state.ImageResource;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import my.nocodeplatform.progress.WorkflowProgressBroadcaster;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            log.info("执行节点：图片收集");
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = "";
            
            try {
                // 发送阶段开始消息
                WorkflowProgressBroadcaster broadcaster = SpringContextUtil.getBean(WorkflowProgressBroadcaster.class);
                broadcaster.sendStageStart(context.getAppId(), WorkflowStageEnum.IMAGE_COLLECTION, 0);
                
                // 获取 AI 图片收集服务
                ImageCollectionServiceFactory imageCollectionServiceFactory = SpringContextUtil.getBean(ImageCollectionServiceFactory.class);
                ImageCollectionService imageCollectionService = imageCollectionServiceFactory.getImageCollectionService();
                // 使用 AI 服务进行智能图片收集
                imageListStr = imageCollectionService.collectImages(originalPrompt);
                
                // 发送阶段完成消息
                int imageCount = parseImageCount(imageListStr);
                broadcaster.sendStageComplete(
                    context.getAppId(), 
                    WorkflowStageEnum.IMAGE_COLLECTION, 
                    WorkflowStageEnum.IMAGE_COLLECTION.getWeight(),
                    "已找到 " + imageCount + " 张相关图片",
                    "{\"imageCount\":" + imageCount + "}"
                );
            } catch (Exception e) {
                log.error("图片收集失败：{}", e.getMessage(), e);
            }
            
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }
    
    /**
     * 解析图片数量
     */
    private static int parseImageCount(String imageListStr) {
        if (imageListStr == null || imageListStr.isEmpty()) {
            return 0;
        }
        // 简单统计 URL 数量
        return imageListStr.split("http").length - 1;
    }
}