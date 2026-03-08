package my.nocodeplatform.ai.chatmodel;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.streaming-chat-model")
@Data
public class DispStreamingChatModel {
        private String modelName;
        private String apiKey;

        @Resource
        private ChatModelListener chatModelListener;

        @Bean
        public QwenStreamingChatModel myQwenStreamingChatModel() {
            return QwenStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .listeners(List.of(chatModelListener))
                    .build();
        }

}

