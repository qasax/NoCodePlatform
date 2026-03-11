package my.nocodeplatform.ai.service;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.langgraph4j.node.SpringContextUtil;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CodeQualityCheckServiceFactory {


    /**
     * 创建代码质量检查 AI 服务
     */

    public CodeQualityCheckService getCodeQualityCheckService() {
        //获取prototype Bean
        QwenChatModel myQwenChatModel = (QwenChatModel) SpringContextUtil.getBean("JsonQwenChatModel");

        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(myQwenChatModel)
                .build();
    }
}
