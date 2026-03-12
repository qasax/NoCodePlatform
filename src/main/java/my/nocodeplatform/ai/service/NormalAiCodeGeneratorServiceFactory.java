package my.nocodeplatform.ai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.ai.tool.ToolManager;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.langgraph4j.node.SpringContextUtil;
import my.nocodeplatform.service.ChatHistoryService;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
@Slf4j
@Configuration
public class NormalAiCodeGeneratorServiceFactory {
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    /**
     * AI 服务实例缓存
     */
    private final Cache<String, NormalAiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();
    /**
     * 默认提供一个 Bean
     */
//    @Bean
//    public NormalAiCodeGeneratorService NormalAiCodeGeneratorService() {
//        return getNormalAiCodeGeneratorService(0L);
//    }


    /**
     * 根据 appId 获取服务（带缓存）这个方法是为了兼容历史逻辑
     */
    public NormalAiCodeGeneratorService getNormalAiCodeGeneratorService(long appId) {
        return getNormalAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public NormalAiCodeGeneratorService getNormalAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createNormalAiCodeGeneratorService(appId, codeGenType));
    }


    @Resource
    private ToolManager toolManager;

    /**
     * 创建新的 AI 服务实例
     */
    private NormalAiCodeGeneratorService createNormalAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        //获取prototype Bean
        QwenStreamingChatModel myQwenStreamingChatModel = (QwenStreamingChatModel) SpringContextUtil.getBean("myQwenStreamingChatModel");
        QwenStreamingChatModel multiQwenStreamingChatModel = (QwenStreamingChatModel) SpringContextUtil.getBean("multiQwenStreamingChatModel");
        QwenChatModel myQwenChatModel = (QwenChatModel) SpringContextUtil.getBean("JsonQwenChatModel");


        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        MessageWindowChatMemory noChatMemory = MessageWindowChatMemory
                .builder()
                .maxMessages(1)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型
            //chatMemoryProvider与chatMemory互斥
            case VUE_PROJECT -> AiServices.builder(NormalAiCodeGeneratorService.class)
                    .streamingChatModel(myQwenStreamingChatModel)
                    //.chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    .chatMemory(noChatMemory)
                    .build();
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> AiServices.builder(NormalAiCodeGeneratorService.class)
                    .chatModel(myQwenChatModel)
                    .streamingChatModel(multiQwenStreamingChatModel)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }
}
