package my.nocodeplatform.progress;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import my.nocodeplatform.model.dto.progress.WorkflowProgressDTO;
import my.nocodeplatform.model.enums.ProgressStatusEnum;
import my.nocodeplatform.model.enums.WorkflowStageEnum;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流进度广播器
 * 用于管理和发送工作流执行进度到 SSE 流
 */
@Slf4j
@Component
public class WorkflowProgressBroadcaster {
    
    @Resource
    private ObjectMapper objectMapper;
    
    /**
     * 存储每个 appId 对应的 FluxSink
     */
    private final Map<Long, FluxSink<String>> sinkMap = new ConcurrentHashMap<>();
    
    /**
     * 每个 appId 的最后发送时间
     */
    private final Map<Long, Instant> lastSendTimeMap = new ConcurrentHashMap<>();
    
    /**
     * 最小发送间隔（毫秒）
     */
    private static final long MIN_SEND_INTERVAL_MS = 500;
    
    /**
     * 心跳间隔（毫秒）
     */
    private static final long HEARTBEAT_INTERVAL_MS = 5000;
    
    /**
     * 注册 FluxSink
     */
    public void registerSink(Long appId, FluxSink<String> sink) {
        sinkMap.put(appId, sink);
        lastSendTimeMap.put(appId, Instant.now());
        log.info("注册进度监听器：appId={}", appId);
    }
    
    /**
     * 移除 FluxSink
     */
    public void removeSink(Long appId) {
        sinkMap.remove(appId);
        lastSendTimeMap.remove(appId);
        log.info("移除进度监听器：appId={}", appId);
    }
    
    /**
     * 发送进度更新（带频率控制）
     */
    public void sendProgress(Long appId, WorkflowProgressDTO progress) {
        sendProgress(appId, progress, true);
    }
    
    /**
     * 发送进度更新
     * @param appId 应用 ID
     * @param progress 进度数据
     * @param rateLimit 是否启用频率控制
     */
    public void sendProgress(Long appId, WorkflowProgressDTO progress, boolean rateLimit) {
        FluxSink<String> sink = sinkMap.get(appId);
        if (sink == null) {
            log.warn("未找到进度监听器：appId={}", appId);
            return;
        }
        
        // 检查发送频率（如果启用）
        if (rateLimit && !shouldSend(appId)) {
            log.debug("频率控制跳过：appId={}, stage={}", appId, progress.getStage());
            return;
        }
        
        try {
            String jsonMessage = formatSseMessage("progress_update", progress);
            sink.next(jsonMessage);
            lastSendTimeMap.put(appId, Instant.now());
            log.info("发送进度更新：appId={}, stage={}, percent={}, status={}", 
                     appId, progress.getStage(), progress.getPercentComplete(), progress.getStatus());
        } catch (Exception e) {
            log.error("发送进度更新失败：appId={}, error={}", appId, e.getMessage(), e);
        }
    }
    
    /**
     * 发送工作流开始消息（不启用频率控制）
     */
    public void sendWorkflowStart(Long appId, String originalPrompt) {
        WorkflowProgressDTO progress = WorkflowProgressDTO.builder()
                .progressId(UUID.randomUUID().toString())
                .appId(appId)
                .stage(WorkflowStageEnum.WORKFLOW_START)
                .stageName(WorkflowStageEnum.WORKFLOW_START.getName())
                .stageDescription(WorkflowStageEnum.WORKFLOW_START.getDescription())
                .status(ProgressStatusEnum.STARTED)
                .percentComplete(0)
                .estimatedRemainingSeconds(120)
                .message("正在启动代码生成工作流，准备分析您的需求...")
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(appId, progress, false); // 不启用频率控制，确保发送
    }
    
    /**
     * 发送阶段开始消息（不启用频率控制）
     */
    public void sendStageStart(Long appId, WorkflowStageEnum stage, Integer basePercent) {
        WorkflowProgressDTO progress = WorkflowProgressDTO.builder()
                .progressId(UUID.randomUUID().toString())
                .appId(appId)
                .stage(stage)
                .stageName(stage.getName())
                .stageDescription(stage.getDescription())
                .status(ProgressStatusEnum.STARTED)
                .percentComplete(basePercent)
                .estimatedRemainingSeconds(estimateRemainingTime(stage))
                .message("正在" + stage.getDescription() + "...")
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(appId, progress, false); // 不启用频率控制，确保发送
    }
    
    /**
     * 发送阶段进行中消息（启用频率控制）
     */
    public void sendStageProgress(Long appId, WorkflowStageEnum stage, Integer percentComplete, 
                                   String message, String details) {
        WorkflowProgressDTO progress = WorkflowProgressDTO.builder()
                .progressId(UUID.randomUUID().toString())
                .appId(appId)
                .stage(stage)
                .stageName(stage.getName())
                .stageDescription(stage.getDescription())
                .status(ProgressStatusEnum.IN_PROGRESS)
                .percentComplete(percentComplete)
                .estimatedRemainingSeconds(estimateRemainingTime(stage))
                .message(message)
                .details(details)
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(appId, progress, true); // 启用频率控制
    }
    
    /**
     * 发送阶段完成消息（不启用频率控制）
     */
    public void sendStageComplete(Long appId, WorkflowStageEnum stage, Integer percentComplete, 
                                   String message, String details) {
        WorkflowProgressDTO progress = WorkflowProgressDTO.builder()
                .progressId(UUID.randomUUID().toString())
                .appId(appId)
                .stage(stage)
                .stageName(stage.getName())
                .stageDescription(stage.getDescription())
                .status(ProgressStatusEnum.COMPLETED)
                .percentComplete(percentComplete)
                .estimatedRemainingSeconds(estimateRemainingTime(stage))
                .message(message)
                .details(details)
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(appId, progress, false); // 不启用频率控制，确保发送
    }
    
    /**
     * 发送工作流完成消息
     */
    public void sendWorkflowComplete(Long appId, String details) {
        WorkflowProgressDTO progress = WorkflowProgressDTO.builder()
                .progressId(UUID.randomUUID().toString())
                .appId(appId)
                .stage(WorkflowStageEnum.WORKFLOW_COMPLETE)
                .stageName(WorkflowStageEnum.WORKFLOW_COMPLETE.getName())
                .stageDescription(WorkflowStageEnum.WORKFLOW_COMPLETE.getDescription())
                .status(ProgressStatusEnum.COMPLETED)
                .percentComplete(100)
                .estimatedRemainingSeconds(0)
                .message("恭喜！您的应用已生成完成，可以立即部署使用")
                .details(details)
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(appId, progress);
        
        // 发送完成事件
        try {
            String completeMessage = formatSseMessage("progress_complete", Map.of(
                    "message", "应用生成完成",
                    "timestamp", System.currentTimeMillis(),
                    "percentComplete",100
            ));
            sinkMap.get(appId).next(completeMessage);
        } catch (Exception e) {
            log.error("发送完成消息失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送错误消息
     */
    public void sendError(Long appId, WorkflowStageEnum stage, String errorMessage) {
        WorkflowProgressDTO progress = WorkflowProgressDTO.builder()
                .progressId(UUID.randomUUID().toString())
                .appId(appId)
                .stage(stage)
                .stageName(stage.getName())
                .stageDescription(stage.getDescription())
                .status(ProgressStatusEnum.FAILED)
                .percentComplete(0)
                .message("处理过程中遇到错误")
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(appId, progress);
        
        // 发送错误事件
        try {
            String errorMessageJson = formatSseMessage("progress_error", Map.of(
                    "error", errorMessage,
                    "stage", stage.getName(),
                    "timestamp", System.currentTimeMillis()
            ));
            FluxSink<String> sink = sinkMap.get(appId);
            if (sink != null) {
                sink.next(errorMessageJson);
                sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage));
            }
        } catch (Exception e) {
            log.error("发送错误消息失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 格式化 SSE 消息
     */
    private String formatSseMessage(String eventType, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 消息失败：{}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"消息格式化失败\"}\n\n";
        }
    }
    
    /**
     * 检查是否应该发送消息（频率控制）
     */
    private boolean shouldSend(Long appId) {
        Instant lastSend = lastSendTimeMap.get(appId);
        if (lastSend == null) {
            return true;
        }
        
        long elapsed = Duration.between(lastSend, Instant.now()).toMillis();
        return elapsed >= MIN_SEND_INTERVAL_MS;
    }
    
    /**
     * 检查并发送心跳消息
     */
    public void checkAndSendHeartbeat(Long appId, WorkflowStageEnum currentStage, Integer currentPercent) {
        Instant lastSend = lastSendTimeMap.get(appId);
        if (lastSend == null) {
            return;
        }
        
        long elapsed = Duration.between(lastSend, Instant.now()).toMillis();
        if (elapsed >= HEARTBEAT_INTERVAL_MS) {
            WorkflowProgressDTO heartbeat = WorkflowProgressDTO.builder()
                    .progressId("heartbeat-" + UUID.randomUUID())
                    .appId(appId)
                    .stage(currentStage)
                    .stageName(currentStage.getName())
                    .status(ProgressStatusEnum.IN_PROGRESS)
                    .percentComplete(currentPercent)
                    .message("正在处理中，请稍候...")
                    .isHeartbeat(true)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            sendProgress(appId, heartbeat);
        }
    }
    
    /**
     * 估算剩余时间
     */
    private int estimateRemainingTime(WorkflowStageEnum stage) {
        // 根据阶段权重估算剩余时间
        int remainingPercent = 100 - stage.getWeight();
        // 假设每秒完成 2% 的进度
        return Math.max(5, remainingPercent / 2);
    }
}
