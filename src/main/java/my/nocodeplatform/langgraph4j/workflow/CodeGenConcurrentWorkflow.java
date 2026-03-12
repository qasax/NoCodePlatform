package my.nocodeplatform.langgraph4j.workflow;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.langgraph4j.model.BuildResult;
import my.nocodeplatform.langgraph4j.model.QualityResult;
import my.nocodeplatform.langgraph4j.node.*;
import my.nocodeplatform.langgraph4j.node.collectimgnode.*;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import my.nocodeplatform.progress.WorkflowProgressBroadcaster;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 *
 * 工作流：并发收集图片
 */
@Slf4j
public class CodeGenConcurrentWorkflow {
    /**
     * 进度广播器（通过 SpringContextUtil 获取）
     */
    private WorkflowProgressBroadcaster progressBroadcaster;

    /**
     * 设置进度广播器
     */
    public void setProgressBroadcaster(WorkflowProgressBroadcaster progressBroadcaster) {
        this.progressBroadcaster = progressBroadcaster;
    }

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_plan", ImagePlanNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    // 添加并发图片收集节点
                    .addNode("content_image_collector", ContentImageCollectorNode.create())
                    .addNode("illustration_collector", IllustrationCollectorNode.create())
                    .addNode("diagram_collector", DiagramCollectorNode.create())
                    .addNode("logo_collector", LogoCollectorNode.create())
                    .addNode("image_aggregator", ImageAggregatorNode.create())

                    // 添加边
//                    .addEdge(START, "image_plan")
                    //如果为第一次对话，走正常流程。第二次及以后，直接从代码生成(code_generator)开始
                    .addConditionalEdges(START,edge_async(this::isFirstChat),
                            Map.of("first","image_plan",
                                    "not_first","code_generator"))

                    // 并发分支：从计划节点分发到各个收集节点
                    .addEdge("image_plan", "content_image_collector")
                    .addEdge("image_plan", "illustration_collector")
                    .addEdge("image_plan", "diagram_collector")
                    .addEdge("image_plan", "logo_collector")

                    // 汇聚：所有收集节点都汇聚到聚合器
                    .addEdge("content_image_collector", "image_aggregator")
                    .addEdge("illustration_collector", "image_aggregator")
                    .addEdge("diagram_collector", "image_aggregator")
                    .addEdge("logo_collector", "image_aggregator")

                    .addEdge("image_aggregator", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    // 使用条件边：根据代码生成类型决定是否需要构建
                    .addConditionalEdges("code_generator",edge_async(this::isVueProject),
                            Map.of("vue","project_builder",
                                    "else","code_quality_check"))
                    // 新增质检条件边：根据质检结果决定下一步
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",   // 质检通过且需要构建
                                    "skip_build", END,            // 质检通过但跳过构建
                                    "fail", "code_generator"      // 质检失败，重新生成
                            ))
                    .addConditionalEdges("project_builder",
                            edge_async(this::isBuildSuccess),
                            Map.of("success",END,
                                    "fail","code_generator"))
                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            log.error("工作流创建失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }
    /**
     * 执行工作流（Flux 流式输出版本，带进度反馈）
     */
    public Flux<String> executeWorkflowWithFlux(Long appId, String codeGenType,String originalPrompt) {
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> {
                WorkflowProgressBroadcaster broadcaster = null;
                try {

                    // 获取进度广播器
                    broadcaster = SpringContextUtil.getBean(WorkflowProgressBroadcaster.class);
                    this.setProgressBroadcaster(broadcaster);

                    // 注册 Sink
                    broadcaster.registerSink(appId, sink);

                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    WorkflowContext initialContext = null;
                    //如果存在codeGenType，说明为后续对话，跳过图片搜集，直接生成代码
                    if(codeGenType!=null){
                        initialContext = WorkflowContext.builder()
                                .appId(appId)
                                .generationType(CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType) ?CodeGenTypeEnum.VUE_PROJECT
                                        :CodeGenTypeEnum.MULTI_FILE.getValue().equals(codeGenType)?CodeGenTypeEnum.MULTI_FILE
                                         :CodeGenTypeEnum.HTML)
                                .originalPrompt(originalPrompt)
                                .currentStep("初始化")
                                .build();
                    }else{
                        initialContext = WorkflowContext.builder()
                                .appId(appId)
                                .originalPrompt(originalPrompt)
                                .currentStep("初始化")
                                .build();
                    }


                    // 发送工作流开始消息
                    broadcaster.sendWorkflowStart(appId, originalPrompt);

                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    // 配置并发执行
                    ExecutorService pool = ExecutorBuilder.create()
                            .setCorePoolSize(10)
                            .setMaxPoolSize(20)
                            .setWorkQueue(new LinkedBlockingQueue<>(100))
                            .setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("Parallel-Image-Collect").build())
                            .build();
                    RunnableConfig runnableConfig = RunnableConfig.builder()
                            .addParallelNodeExecutor("image_plan", pool)
                            .build();
                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),runnableConfig)) {
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            log.info("第 {} 步收到的上下文：{}", stepCounter,currentContext);
                        }
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        stepCounter++;
                    }

                    // 发送工作流完成消息
                    String details = JSONUtil.toJsonStr(Map.of(
                            "totalTime", System.currentTimeMillis(),
                            "generatedFiles", "unknown",
                            "buildSuccess", true,
                            "estimatedRemainingSeconds",0
                    ));
                    broadcaster.sendWorkflowComplete(appId, details);

                    log.info("代码生成工作流执行完成！");
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败：{}", e.getMessage(), e);
                    if (broadcaster != null) {
                        broadcaster.sendError(appId, WorkflowStageEnum.CODE_GENERATION, e.getMessage());
                    }
                    sink.error(e);
                } finally {
                    // 清理资源
                    if (broadcaster != null) {
                        broadcaster.removeSink(appId);
                    }
                }
            });
        });
    }

    private String isFirstChat(MessagesState<String> stringMessagesState) {
        WorkflowContext context = WorkflowContext.getContext(stringMessagesState);
        if(context.getGenerationType()!=null){
            log.info("本次为后续对话，跳过图片搜集、提示词增强，从代码生成开始");
            return "not_first";
        }else{
            log.info("本次为首次对话，正常工作流程");
            return "first";
        }
    }


    /**
     * 格式化 SSE 事件的辅助方法
     */
    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }



    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // VUE_PROJECT 需要构建
        return "build";
    }
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        // 如果质检失败，重新生成代码
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

    private String isVueProject(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        if(generationType == CodeGenTypeEnum.VUE_PROJECT) {
            return "vue";
        }else{
            return "else";
        }
    }

    private String isBuildSuccess(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        BuildResult buildResult = context.getBuildResult();
        if(buildResult.getIsValid().equals(true)) {
            return "success";
        }else{
            return "fail";
        }
    }

}
