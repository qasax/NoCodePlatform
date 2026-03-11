package my.nocodeplatform.langgraph4j.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingLanguageModel;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.langgraph4j.node.SpringContextUtil;
import my.nocodeplatform.langgraph4j.tool.ImageSearchTool;
import my.nocodeplatform.langgraph4j.tool.LogoGeneratorTool;
import my.nocodeplatform.langgraph4j.tool.MermaidDiagramTool;
import my.nocodeplatform.langgraph4j.tool.UndrawIllustrationTool;
import my.nocodeplatform.service.ChatHistoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    public ImageCollectionService getImageCollectionService() {
        //获取prototype Bean
        QwenChatModel myQwenChatModel = (QwenChatModel) SpringContextUtil.getBean("JsonQwenChatModel");

        return AiServices.builder(ImageCollectionService.class)
                .chatModel(myQwenChatModel)
                .tools(
                        imageSearchTool,
                        undrawIllustrationTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .build();
    }
}