package my.nocodeplatform.ai.chatmodel;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.streaming-chat-model")
@Data
public class DispStreamingChatModel {
        private String modelName;
        private String apiKey;
        private Boolean isMultimodalModel;
        @Value("${langchain4j.community.dashscope.streaming-chat-model.parameters.format-on}")
        private Boolean formatOn;
        @Value("${langchain4j.community.dashscope.streaming-chat-model.parameters.response-format}")
        private String responseFormat;
        @Resource
        private ChatModelListener chatModelListener;
        //普通模型
        @Bean(name = "myQwenStreamingChatModel")
        @Scope("prototype")
        public QwenStreamingChatModel myQwenStreamingChatModel() {
                if(formatOn){
                        ChatRequestParameters chatRequestParameters =
                                ChatRequestParameters.builder().
                                        responseFormat(ResponseFormat.builder()
                                                .type(responseFormat.equals("json")? ResponseFormatType.JSON:ResponseFormatType.TEXT)
                                                .build())
                                        .build();
                        return QwenStreamingChatModel.builder()
                                .apiKey(apiKey)
                                .modelName(modelName)
                                .defaultRequestParameters(chatRequestParameters)
                                .listeners(List.of(chatModelListener))
                                .build();
                }


            return QwenStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .listeners(List.of(chatModelListener))
                    .build();
        }

        //多模态
        @Bean(name = "multiQwenStreamingChatModel")
        @Scope("prototype")
        public QwenStreamingChatModel MultiQwenStreamingChatModel() {
                return QwenStreamingChatModel.builder()
                        .isMultimodalModel(true)
                        .apiKey(apiKey)
                        .modelName("qwen3.5-plus")
                        .listeners(List.of(chatModelListener))
                        .build();
        }
}

