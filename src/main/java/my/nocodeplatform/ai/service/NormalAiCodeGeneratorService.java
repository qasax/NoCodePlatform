package my.nocodeplatform.ai.service;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface NormalAiCodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-vue-file-system-prompt.txt")
    Flux<String> generateVueProjectCodeStreamNoMemory(String userMessage);
}
