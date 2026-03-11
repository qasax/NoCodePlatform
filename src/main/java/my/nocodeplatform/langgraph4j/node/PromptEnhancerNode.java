package my.nocodeplatform.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.langgraph4j.state.ImageResource;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import my.nocodeplatform.progress.WorkflowProgressBroadcaster;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class PromptEnhancerNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点：提示词增强");
            
            // 获取原始提示词和图片列表
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();
            
            // 发送阶段开始消息
            WorkflowProgressBroadcaster broadcaster = SpringContextUtil.getBean(WorkflowProgressBroadcaster.class);
            broadcaster.sendStageStart(context.getAppId(), WorkflowStageEnum.PROMPT_ENHANCEMENT, WorkflowStageEnum.IMAGE_COLLECTION.getWeight());
            
            // 构建增强后的提示词
            StringBuilder enhancedPromptBuilder = new StringBuilder();
            enhancedPromptBuilder.append(originalPrompt);
            // 如果有图片资源，则添加图片信息
            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotBlank(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## 可用素材资源\n");
                enhancedPromptBuilder.append("请在生成网站使用以下图片资源，将这些图片合理地嵌入到网站的相应位置中。\n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append("：")
                                .append(image.getDescription())
                                .append("（")
                                .append(image.getUrl())
                                .append("）\n");
                    }
                } else {
                    enhancedPromptBuilder.append(imageListStr);
                }
            }
            String enhancedPrompt = enhancedPromptBuilder.toString();
            
            // 发送阶段完成消息
            broadcaster.sendStageComplete(
                context.getAppId(),
                WorkflowStageEnum.PROMPT_ENHANCEMENT,
                WorkflowStageEnum.PROMPT_ENHANCEMENT.getWeight(),
                "提示词增强完成，共 " + enhancedPrompt.length() + " 字符",
                "{\"originalLength\":" + originalPrompt.length() + ",\"enhancedLength\":" + enhancedPrompt.length() + "}"
            );
            
            // 更新状态
            context.setCurrentStep("提示词增强");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("提示词增强完成，增强后长度：{} 字符", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }
}