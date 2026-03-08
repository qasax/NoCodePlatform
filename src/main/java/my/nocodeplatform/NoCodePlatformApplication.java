package my.nocodeplatform;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("my.nocodeplatform.mapper")
public class NoCodePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoCodePlatformApplication.class, args);
    }

}
