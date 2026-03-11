package my.nocodeplatform.langgraph4j.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import my.nocodeplatform.langgraph4j.node.SpringContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageCollectionPlanServiceFactory {


    public ImageCollectionPlanService getImageCollectionPlanService() {
        //获取prototype Bean
        QwenChatModel myQwenChatModel = (QwenChatModel) SpringContextUtil.getBean("JsonQwenChatModel");

        return AiServices.builder(ImageCollectionPlanService.class)
                .chatModel(myQwenChatModel)
                .build();
    }
}
