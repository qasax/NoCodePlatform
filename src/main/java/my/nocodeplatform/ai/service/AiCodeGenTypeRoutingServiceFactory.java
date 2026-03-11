package my.nocodeplatform.ai.service;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.langgraph4j.node.SpringContextUtil;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AiCodeGenTypeRoutingServiceFactory {


    /**
     * 创建AI代码生成类型路由服务实例
     */

    public AiCodeGenTypeRoutingService getAiCodeGenTypeRoutingService() {
        //获取prototype Bean
        QwenChatModel myQwenChatModel = (QwenChatModel) SpringContextUtil.getBean("NormalQwenChatModel");

        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(myQwenChatModel)
                .build();
    }
}