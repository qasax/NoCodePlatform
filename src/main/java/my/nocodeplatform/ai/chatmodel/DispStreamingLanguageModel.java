package my.nocodeplatform.ai.chatmodel;

import dev.langchain4j.community.model.dashscope.QwenStreamingLanguageModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.streaming-language-model")
@Data
public class DispStreamingLanguageModel {
    private String modelName;
    private String apiKey;


    @Bean
    public QwenStreamingLanguageModel myQwenStreamingLanguageModel() {
        return QwenStreamingLanguageModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
