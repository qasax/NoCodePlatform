package my.nocodeplatform.langgraph4j.config;

import my.nocodeplatform.langgraph4j.workflow.CodeGenWorkflow;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangGraphStudioConfig {

    @Bean
    public LangGraphStudioServer.Instance codeGenGraph() throws Exception {

        var workflow = new CodeGenWorkflow()
                .createWorkflow()
                .stateGraph;

        return LangGraphStudioServer.Instance.builder()
                .title("LangGraph Studio")
                .graph(workflow)
                .compileConfig(
                        CompileConfig.builder()
                                .checkpointSaver(new MemorySaver())
                                .build()
                )
                .addInputStringArg("input")
                .build();
    }
}