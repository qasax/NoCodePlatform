package my.nocodeplatform.test;

import dev.langchain4j.spi.services.AiServiceContextFactory;
import jakarta.annotation.Resource;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.ai.service.AiCodeGenTypeRoutingService;
import my.nocodeplatform.ai.service.AiCodeGenTypeRoutingServiceFactory;
import my.nocodeplatform.ai.service.AiCodeGeneratorFacade;
import my.nocodeplatform.ai.service.CodeQualityCheckServiceFactory;
import my.nocodeplatform.langgraph4j.model.QualityResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
public class ModelTest {
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;
    @Resource
    private CodeQualityCheckServiceFactory codeQualityCheckServiceFactory;
    @Test
    //streaming chat model
    public void testStreamingwenChatModel() {
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream("一个购物网站", CodeGenTypeEnum.VUE_PROJECT, 0L);
        System.out.println(stringFlux);
    }
    //normal chat-modle
    @Test
    public void testRouting() {
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.getAiCodeGenTypeRoutingService();
        CodeGenTypeEnum codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType("网页");
        System.out.println(codeGenTypeEnum);
    }
    //json chat - model
    @Test
    public void testCodeQualityCheckServiceFactory() {
        QualityResult qualityResult = codeQualityCheckServiceFactory.getCodeQualityCheckService().checkCodeQuality("测试");
        System.out.println(qualityResult);
    }
}
