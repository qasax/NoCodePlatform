package my.nocodeplatform.ai.chatmodel;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
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
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.chat-model")
@Data
public class DispChatModel {
    private String modelName;

    private String apiKey;
    private Boolean isMultimodalModel;
    @Value("${langchain4j.community.dashscope.streaming-chat-model.parameters.format-on}")
    private Boolean formatOn;
    @Value("${langchain4j.community.dashscope.chat-model.parameters.response-format}")
    private String responseFormat;
    @Resource
    private ChatModelListener chatModelListener;

    @Bean(name = "JsonQwenChatModel")
    @Scope("prototype")
    public QwenChatModel JsonQwenChatModel() {
        if(formatOn){
            ChatRequestParameters chatRequestParameters =
                    ChatRequestParameters.builder().
                            responseFormat(ResponseFormat.builder()
                                    .type(responseFormat.equals("json")? ResponseFormatType.JSON:ResponseFormatType.TEXT)
                                    .build())
                            .build();
            return QwenChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .defaultRequestParameters(chatRequestParameters)
                    .isMultimodalModel(isMultimodalModel)
                    .listeners(List.of(chatModelListener))
                    .build();
        }
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .isMultimodalModel(isMultimodalModel)
                .listeners(List.of(chatModelListener))
                .build();
    }

    @Bean(name = "NormalQwenChatModel")
    @Scope("prototype")
    public QwenChatModel NormalQwenChatModel() {

        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .isMultimodalModel(isMultimodalModel)
                .listeners(List.of(chatModelListener))
                .build();
    }
}
